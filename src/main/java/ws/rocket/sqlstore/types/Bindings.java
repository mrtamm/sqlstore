/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ws.rocket.sqlstore.types;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import ws.rocket.sqlstore.QueryContext;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.script.QueryParam;
import ws.rocket.sqlstore.script.params.Param;

/**
 * A registry of {@link ValueMapper}s that is used by SqlStore at runtime in order to set and later
 * read JDBC statement and result-set parameters.
 *
 * <p>The registry can be initialized only once. When custom handlers are not registered before
 * parsing any SQLS file, default handlers will be registered and used throughout the JVM instance
 * lifetime.
 *
 * <p>The <code>ValueMapper</code>s are provided as an array. The order of handlers is a bit
 * important: every time a mapper is searched for, the array will be traversed from the beginning
 * until a matching mapper is found. Therefore, the most common mappers should be placed before less
 * common ones. Also, when a mapper handles a subtype of a Java class handled by another mapper, the
 * subtype handler should be placed before that other so that the generic version would not
 * accidentally handle the subcase.
 *
 * <p>This class is initialized by registering custom handlers (via
 * {@link #register(ws.rocket.sqlstore.types.ValueMapper...)}) or using the default ones defined in
 * the ({@link #getInstance()} method. After initialization, reference to the instance will be
 * maintained in the class and used until the JVM terminates.
 *
 * <p>A custom registry should be initialized by the main thread of an application before loading
 * scripts from SQLS files and before starting other threads that use SqlStore to guarantee that
 * custom registry will be created successfully.
 *
 * <p>The methods in this class are similar to the methods of {@link ValueMapper}. However, the
 * methods here are more generic and enable working at the query context and at the current
 * parameter level &mdash; <code>ValueMapper</code> lookup is handled by the methods.
 */
public final class Bindings {

  private static Bindings instance;

  /**
   * Discards the current set of value bindings.
   * Currently used only for testing.
   */
  public static void reset() {
    instance = null;
  }

  /**
   * Creates a new registry based on given value handlers. The registry can be created only once.
   * When it already exists, this method will not alter the existing registry.
   *
   * @param handlers A non-empty array of value handlers.
   * @return The effective registry instance.
   */
  public static Bindings register(ValueMapper... handlers) {
    if (instance == null) {
      instance = new Bindings(handlers);
    }
    return instance;
  }

  /**
   * Provides the effective registry instance. When the registry does not exist, a default one will
   * be created that will also remain effective.
   *
   * @return The effective registry instance.
   */
  public static Bindings getInstance() {
    if (instance == null) {
      register(new StringMapper(),
          new LongMapper(),
          new IntMapper(),
          new ShortMapper(),
          new BigDecimalMapper(),
          new DateMapper(),
          new BooleanMapper(),
          new ByteArrayMapper(),
          new FileMapper(),
          new InputStreamMapper()
      );
    }
    return instance;
  }

  private final ValueMapper[] handlers;

  private Bindings(ValueMapper[] handlers) {
    if (handlers == null || handlers.length == 0
        || Arrays.asList(handlers).contains(null)) {
      throw new IllegalArgumentException("ValueMappers array is empty or contains a null");
    }
    this.handlers = handlers;
  }

  /**
   * Confirms that a handler exists for given Java type and also validates the related SQL type.
   * This method may throw a runtime exception when either of the types is not supported.
   *
   * @param type The Java value type (of a parameter) for ValueMapper lookup, must not be null.
   * @param sqlType A constant value from <code>java.sql.Types</code>, or null for default type.
   * @return The SQL type to use, as decided by the resolved ValueMapper instance.
   */
  public int confirmTypes(Class<?> type, Integer sqlType) {
    return getHandler(type).confirmSqlType(sqlType);
  }

  /**
   * Registers a parameter value on a statement.
   *
   * <p>This method performs the parameter value lookup from the context and sets the value at given
   * index on the statement.
   *
   * @param ctx The current query context.
   * @param param The parameter definition.
   * @param stmt The statement to update.
   * @param index The position of the parameter (1-based).
   * @throws SQLException When an unexpected problem would occur while setting the value.
   */
  public void bindParam(QueryContext ctx, QueryParam param, PreparedStatement stmt, int index)
      throws SQLException {
    ValueMapper handler = getHandler(param.getParam().getJavaType());
    Integer sqlType = param.getParam().getSqlType();

    if (param.isForInput()) {
      handler.write(stmt, index, param.getValue(ctx), sqlType);
    }
    if (param.isForOutput() && stmt instanceof CallableStatement) {
      ((CallableStatement) stmt).registerOutParameter(index, sqlType);
    }
  }

  /**
   * Reads a parameter value from a callable statement (after execute).
   *
   * <p>This method reads the value at given index from the statement and lets the parameter
   * definition update the query context with the value read.
   *
   * @param ctx The current query context.
   * @param param The parameter definition.
   * @param stmt The statement to read.
   * @param index The position of the parameter (1-based).
   * @throws SQLException When an unexpected problem would occur while reading the value.
   */
  public void readParam(QueryContext ctx, QueryParam param, CallableStatement stmt, int index)
      throws SQLException {

    if (param.isForOutput()) {
      ValueMapper handler = getHandler(param.getParam().getJavaType());
      param.setValue(ctx, handler.read(stmt, index, param.getParam().getSqlType()));
    }
  }

  /**
   * Reads a parameter value from a result-set row (after execute).
   *
   * <p>This method reads the value at given index from the row and lets the parameter definition
   * update the query context with the value read.
   *
   * @param ctx The current query context.
   * @param param The parameter definition.
   * @param row The result-set row to read.
   * @param index The position of the parameter (1-based).
   * @throws SQLException When an unexpected problem would occur while reading the value.
   */
  public void readParam(QueryContext ctx, Param param, ResultSet row, int index)
      throws SQLException {
    ValueMapper handler = getHandler(param.getJavaType());
    param.write(ctx, handler.read(row, index, param.getSqlType()));
  }

  private ValueMapper getHandler(Class<?> type) {
    Objects.requireNonNull(type, "No Java type was given for ValueMapper lookup.");

    ValueMapper result = null;

    for (ValueMapper handler : this.handlers) {
      if (handler.supports(type)) {
        result = handler;
        break;
      }
    }

    if (result == null) {
      throw new ScriptSetupException("Value of %s is not supported (no value handler was found "
          + "that would support it).", type.getName());
    }

    return result;
  }

}
