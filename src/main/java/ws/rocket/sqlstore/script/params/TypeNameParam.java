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

import ws.rocket.sqlstore.ScriptExecuteException;
import ws.rocket.sqlstore.execute.QueryContext;

/**
 * A read-write parameter that is described by its types and a simple name. This kind of parameter
 * is used to bind a query input or output value to a name (and position among others of its kind).
 * <p>
 * When used in script input parameters, the {@link ParamMode} is restricted to {@literal IN}. When
 * used in script output parameters, the {@link ParamMode} is restricted to {@literal OUT}. However,
 * a parameter with the same name cannot be used as both input and output ({@literal INOUT})
 * parameter of the same script!
 * <p>
 * When the value is a Java bean to be interpreted for reading or setting its properties (see
 * {@link Expression}), the parameter should not have an SQL type (since its not used in SQL script
 * directly). The SQL type can be provided in the expressions for particular properties where
 * default SQL types need to be clarified or overridden.
 * <p>
 * When the value of this parameter is directly used in the script (also via {@literal Expression}),
 * the SQL type will be derived from value mapper capabilities if not explicitly provided with
 * parameter declaration or in the script together with expression (note that SQL type in expression
 * overrides the one in parameter declaration).
 */
public final class TypeNameParam extends Param {

  private final String name;

  private final int resultParamIndex;

  /**
   * Initializes the parameter properties of a named IN/UPDATE-parameter.
   *
   * @param javaType The Java type of this parameter value (mandatory).
   * @param sqlType SQL type of this parameter value (optional).
   * @param name The parameter name (mandatory).
   */
  public TypeNameParam(Class<?> javaType, Integer sqlType, String name) {
    this(javaType, sqlType, name, -1);
  }

  /**
   * Initializes the parameter properties of a named OUT-parameter.
   *
   * @param javaType The Java type of this parameter value (mandatory).
   * @param sqlType SQL type of this parameter value (optional).
   * @param name The parameter name (mandatory).
   * @param resultParamIndex Zero-based position is needed for named OUT-parameters to store the
   * value correctly.
   */
  public TypeNameParam(Class<?> javaType, Integer sqlType, String name, int resultParamIndex) {
    super(javaType, sqlType);
    this.name = name;
    this.resultParamIndex = resultParamIndex;
  }

  /**
   * Provides the name of the parameter.
   *
   * @return A string as the name of the parameter.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Performs value validation by comparing the value type to the Java type of this parameter. This
   * is used to validate input parameters passed to the query.
   * <p>
   * For the check to succeed, the value must be null or must be assignable to the Java type of this
   * parameter. Upon failure, a runtime exception will be raised.
   *
   * @param value The value to be checked (may be null).
   * @param argIndex The index of the parameter to be used in error message to be more informative.
   */
  public void validate(Object value, int argIndex) {
    if (value == null && getJavaType().isPrimitive()) {
      throw new ScriptExecuteException(
          "Script argument with index %d value mismatch: expected a not null value of %s",
          argIndex, getJavaType());
    }
    if (value != null && !supports(value.getClass())) {
      throw new ScriptExecuteException(
          "Script argument with index %d type mismatch: expected %s, got %s",
          argIndex, getJavaType(), value.getClass().getName());
    }
  }

  @Override
  public Object read(QueryContext ctx) {
    return ctx.getVariable(this.name);
  }

  @Override
  public void write(QueryContext ctx, Object value) {
    if (this.resultParamIndex < 0) { // UPDATE-param
      ctx.updateVariable(this.name, value);
    } else { // OUT-param
      ctx.getResultsCollector().setRowValue(this.resultParamIndex, value);
    }
  }

  /**
   * Provides textual representation of this parameter. The returned value is either:
   * <ul>
   * <li>"&lt;simple class name&gt; &lt;param name&gt;"
   * <li>"&lt;simple class name&gt;|&lt;SQL type int value&gt; &lt;param name&gt;"
   * </ul>
   * <p>
   * When this is a named parameter from OUT-parameters, the returned representation will end with
   * the index of the OUT-parameter in square brackets, e.g."[0]".
   *
   * @return Textual representation of this parameter instance.
   */
  @Override
  public String toString() {
    String txt = super.toString() + " " + this.name;
    if (this.resultParamIndex >= 0) {
      txt += "[" + this.resultParamIndex + "]";
    }
    return txt;
  }

}
