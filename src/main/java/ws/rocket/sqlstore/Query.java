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

package ws.rocket.sqlstore;

import java.util.List;
import java.util.Map;
import ws.rocket.sqlstore.connection.ConnectionManager;
import ws.rocket.sqlstore.execute.JdbcExecutor;
import ws.rocket.sqlstore.execute.QueryContext;

/**
 * Wraps an SQL script context and waits for a command to execute it.
 * <p>
 * The main purpose of this class is to be a bridge between the Java code and the SQL script
 * results: it validates whether the script can return the script results in expected Java types.
 * <p>
 * The options are simple but should cover most use-cases (only one of them per query execution):
 * <ol>
 * <li>{@link #execute()} &ndash; execution that ignores results;
 * <li>{@link #forValue(java.lang.Class) } &ndash; execution that reads just the first result and
 * ignores others;
 * <li>{@link #forValues(java.lang.Class) } &ndash; execution that reads all results;
 * <li>{@link #forMap(java.lang.Class, java.lang.Class)} &ndash; execution that expects key-value
 * pairs;
 * <li>{@link #forUpdateCount()} &ndash; execution with feedback about the amount of rows updated;
 * </ol>
 * <p>
 * When the return type is not supported for the script, the method will throw a
 * {@link ScriptExecuteException} without attempting to execute the script. However, note that
 * {@link #execute()} and {@link #forUpdateCount()} are supported with every SQL script.
 * <p>
 * In case of SQL execution errors, the exceptions will be wrapped into
 * {@link ScriptExecuteException}, which may provide information what script was being executed and
 * what error condition was caused. That exception is not checked, however, the invocation
 * procedures does it best to close all created connections, statements, and result-sets.
 */
public final class Query {

  private final ConnectionManager connectionManager;

  private final QueryContext ctx;

  private boolean executed;

  /**
   * Initiates a query wrapper in order to execute it while expecting some kind of results
   * interpretation mode.
   *
   * @param connectionManager A manager for obtaining a connection.
   * @param ctx The query context to execute.
   */
  Query(ConnectionManager connectionManager, QueryContext ctx) {
    if (connectionManager == null || ctx == null) {
      throw new NullPointerException("A parameter is undefined");
    }
    this.connectionManager = connectionManager;
    this.ctx = ctx;
  }

  /**
   * Just executes the query without expecting any results. Upon failure, a
   * {@link ScriptExecuteException} may be thrown.
   */
  public void execute() {
    if (this.executed) {
      throw new ScriptExecuteException("Query has been executed and the object cannot be reused",
          this.ctx);
    }

    this.executed = true;
    new JdbcExecutor(this.connectionManager).execute(this.ctx);
  }

  /**
   * Executes the query while expecting for a row as a result. When no row is returned, this method
   * will return null. When more than one is returned by the query, only the first will be returned,
   * though all returned rows will be extracted into Java objects. The corresponding SQLS script
   * must return a result (exactly one return-parameter).
   *
   * @param <V> Return-value type.
   * @param valueType The value type into which the value will be converted. This must be same as in
   * the SQLS file, or null. When present, this helps fail faster with unexpected class-cast
   * problems.
   * @return The first extracted value, or null.
   */
  public <V> V forValue(Class<V> valueType) {
    checkSupportsListResult(valueType);
    execute();
    return extractValue(valueType);
  }

  /**
   * Executes the query while expecting for zero, one, or more rows as a result. When no row is
   * returned, this method will return an empty list. A query hint, when present, may affect the
   * number of rows so that not all of them would be returned. The corresponding SQLS script must
   * return a result (exactly one return-parameter).
   *
   * @param <V> Return-value type.
   * @param valueType The value type into which the value will be converted. This must be same as in
   * the SQLS file, or null. When present, this helps fail faster with unexpected class-cast
   * problems.
   * @return A list with extracted values.
   */
  public <V> List<V> forValues(Class<V> valueType) {
    checkSupportsListResult(valueType);
    execute();
    return extractList(valueType);
  }

  /**
   * Executes the query while expecting for zero, one, or more value pairs as a result. When no
   * value is returned, this method will return an empty map. A query hint, when present, may affect
   * the number of results so that not all of them would be returned. The corresponding SQLS script
   * must return a result (exactly two return-parameters).
   *
   * @param <K> Return-value type for keys.
   * @param <V> Return-value type for values.
   * @param value1Type The value type into which the key-value will be converted. This must be same
   * as in the SQLS file for the first OUT-parameter, or null. When present, this helps fail faster
   * with unexpected class-cast problems.
   * @param value2Type The value type into which the value-value will be converted. This must be
   * same as in the SQLS file for the second OUT-parameter, or null. When present, this helps fail
   * faster with unexpected class-cast problems.
   * @return A map with extracted values with an entry per row.
   */
  public <K, V> Map<K, V> forMap(Class<K> value1Type, Class<V> value2Type) {
    checkSupportsMapResult(value1Type, value2Type);
    execute();
    return extractMap(value1Type, value2Type);
  }

  /**
   * Executes the query and returns the number of rows affected. The returned number may also be
   * negative when the information is unavailable after execution.
   *
   * @return An integer (typically, -1, 0, or a positive number).
   */
  public int forUpdateCount() {
    execute();
    return this.ctx.getUpdateCount();
  }

  private <T> T extractValue(Class<T> valueType) {
    List<T> values = extractList(valueType);
    return values.isEmpty() ? null : values.get(0);
  }

  @SuppressWarnings("unchecked")
  private <T> List<T> extractList(Class<T> valueType) {
    return (List<T>) this.ctx.getResult();
  }

  @SuppressWarnings("unchecked")
  private <K, V> Map<K, V> extractMap(Class<K> keyType, Class<V> valueType) {
    return (Map<K, V>) this.ctx.getResult();
  }

  private void checkSupportsListResult(Class<?> javaType) {
    if (!this.ctx.supportsList(javaType)) {
      throw new ScriptExecuteException("Query does not support return type " + javaType, this.ctx);
    }
  }

  private void checkSupportsMapResult(Class<?> keyType, Class<?> valueType) {
    if (!this.ctx.supportsMap(keyType, valueType)) {
      throw new ScriptExecuteException("Query does not support return type Map<" + keyType
          + "," + valueType + ">.", this.ctx);
    }
  }

}
