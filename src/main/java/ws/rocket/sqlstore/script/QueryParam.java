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

package ws.rocket.sqlstore.script;

import static java.util.Objects.requireNonNull;

import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.params.Expression;
import ws.rocket.sqlstore.script.params.Param;
import ws.rocket.sqlstore.script.params.ParamMode;
import ws.rocket.sqlstore.script.params.TypeNameParam;

/**
 * A parameter or an expression to be used as statement IN/OUT parameter. This class adds the mode
 * information to a named parameter or an expression. The mode specifies whether the value is to be
 * set before execution (IN), the value is to be read after execution (OUT), or both (INOUT).
 */
public final class QueryParam {

  /**
   * A shared value for parameters when query does not contain any expression.
   */
  public static final QueryParam[] NO_PARAMS = new QueryParam[0];

  private final Param param;

  private final ParamMode mode;

  /**
   * Creates a new query parameter for a named parameter.
   *
   * @param mode The execution mode for the parameter.
   * @param param The parameter to use.
   */
  public QueryParam(ParamMode mode, TypeNameParam param) {
    this.mode = requireNonNull(mode, "The mode parameter is undefined.");
    this.param = requireNonNull(param, "The named query parameter is undefined.");
  }

  /**
   * Creates a new query parameter for an expression.
   *
   * @param mode The execution mode for the expression.
   * @param expression The expression to use.
   */
  public QueryParam(ParamMode mode, Expression expression) {
    this.mode = requireNonNull(mode, "The mode parameter is undefined.");
    this.param = requireNonNull(expression, "The expression is undefined.");
  }

  /**
   * Provides the wrapped named parameter or expression instance.
   *
   * @return A parameter instance.
   */
  public Param getParam() {
    return this.param;
  }

  /**
   * Informs whether the wrapped parameter value must be set on the statement before execution.
   *
   * @return A Boolean that is true when the value must be set before execution.
   * @see #getValue(ws.rocket.sqlstore.execute.QueryContext)
   */
  public boolean isForInput() {
    return this.mode != ParamMode.OUT;
  }

  /**
   * Informs whether the wrapped parameter value must be set on the statement after execution.
   *
   * @return A Boolean that is true when the value must be set after execution.
   * @see #setValue(ws.rocket.sqlstore.execute.QueryContext, java.lang.Object)
   */
  public boolean isForOutput() {
    return this.mode != ParamMode.IN;
  }

  /**
   * Resolves the value of the parameter.
   *
   * @param ctx The current query execution context.
   * @return The resolved value, may be null.
   */
  public Object getValue(QueryContext ctx) {
    return this.param.read(ctx);
  }

  /**
   * Sets a value on given parameter.
   *
   * @param ctx The current query execution context.
   * @param value The value to be assigned, may be null.
   */
  public void setValue(QueryContext ctx, Object value) {
    this.param.write(ctx, value);
  }

  /**
   * Provides a textual representation of a query parameter. It consists of the parameter mode and
   * the textual representation of the parameter.
   *
   * @return A textual representation of this query parameter.
   */
  @Override
  public String toString() {
    return this.mode.name() + " " + this.param.toString();
  }

}
