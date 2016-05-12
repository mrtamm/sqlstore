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

package ws.rocket.sqlstore.script.params;

import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.BeanUtil;

import static java.util.Objects.requireNonNull;

/**
 * The base solution of all kinds of parameters. A parameter must always have a Java type. SQL type
 * is not always required as it can be derived by asking from value converters when needed. However,
 * for all script in/out and result-set parameters must have a resolved SQL type so that it would be
 * known in advance that the Java type can be converted to SQL type and vice-versa.
 * <p>
 * The SQL type value can be either null or one of the values of constants from
 * {@link java.sql.Types}.
 */
public abstract class Param {

  /**
   * A shared value for parameters when a category is not provided.
   */
  public static final Param[] NO_PARAMS = new Param[0];

  private final Class<?> javaType;

  private final Integer sqlType;

  /**
   * Initializes the parameter properties.
   *
   * @param javaType The Java type of this parameter value (mandatory).
   * @param sqlType SQL type of this parameter value (optional).
   */
  public Param(Class<?> javaType, Integer sqlType) {
    this.javaType = requireNonNull(javaType, "The Java type of the parameter is undefined.");
    this.sqlType = sqlType;
  }

  /**
   * Provides the Java type of the value of this parameter.
   *
   * @return A Java class (never null).
   */
  public final Class<?> getJavaType() {
    return this.javaType;
  }

  /**
   * Provides the SQL type of the value of this parameter.
   *
   * @return The SQL type as defined in {@link java.sql.Types}, or null.
   */
  public final Integer getSqlType() {
    return this.sqlType;
  }

  /**
   * Informs whether the value of this parameter is assignable to a variable of given Java type.
   * When the given Java type is null, the result will be false.
   * <p>
   * When the target type is primitive, the input type must match either the primitive or its
   * wrapper type. However, when the target type is not a primitive type, the input type must also
   * not be a primitive type.
   *
   * @param javaType The type to check against, may be null.
   * @return A Boolean that is true when the values of this parameter are assignable to given
   * variables of given type.
   */
  public final boolean supports(Class<?> javaType) {
    return javaType != null && (javaType.isAssignableFrom(this.javaType)
        || this.javaType.isPrimitive()
        && javaType.isAssignableFrom(BeanUtil.getPrimitiveWrapperClass(this.javaType)));
  }

  /**
   * Provides textual representation of this parameter. The returned value informs about its types
   * and is either the simple class name of the Java type, or "&lt;simple class name&gt;|&lt;SQL
   * type int value&gt;".
   * <p>
   * (Sub-classes may enrich the textual representation.)
   *
   * @return Textual representation of this parameter instance.
   */
  @Override
  public String toString() {
    String result = this.javaType.getSimpleName();
    if (this.sqlType != null) {
      result += "|" + this.sqlType;
    }
    return result;
  }

  /**
   * Parameters, that are readable, implement this method in order to obtain the value and use it in
   * a query. Parameters that are not readable are encouraged to throw an exception, such as
   * {@literal RuntimeException} to signal wrong use of the parameter and to fail fast. (When such
   * error should occur, it is most likely due to bad construction of the {@literal Script} object.)
   *
   * @param ctx The current query context, which also contains the values of input parameters.
   * @return The value of this parameter.
   */
  public abstract Object read(QueryContext ctx);

  /**
   * Parameters, that are writable, implement this method in order to persist the value in the query
   * results. Parameters that are not writable are encouraged to throw an exception, such as
   * {@literal RuntimeException} to signal wrong use of the parameter and to fail fast. (When such
   * error should occur, it is most likely due to bad construction of the {@literal Script} object.)
   *
   * @param ctx The current query context, which also contains the {@literal Result} object.
   * @param value The value to be persisted (may also be null).
   */
  public abstract void write(QueryContext ctx, Object value);

}
