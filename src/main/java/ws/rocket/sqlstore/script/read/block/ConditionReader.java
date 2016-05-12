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

package ws.rocket.sqlstore.script.read.block;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.script.QueryParam;
import ws.rocket.sqlstore.script.params.Expression;
import ws.rocket.sqlstore.script.params.ParamMode;
import ws.rocket.sqlstore.script.params.TypeNameParam;
import ws.rocket.sqlstore.script.read.ParamsSet;
import ws.rocket.sqlstore.script.read.StreamReader;
import ws.rocket.sqlstore.script.sql.ParamValueEmpty;
import ws.rocket.sqlstore.script.sql.ParamValueNonEmpty;
import ws.rocket.sqlstore.script.sql.ParamValueTrue;
import ws.rocket.sqlstore.script.sql.SqlPartCondition;

import static java.util.Objects.requireNonNull;

/**
 * Reads the condition of a conditional SQL script part (to evaluate whether that part is to be
 * included in the executed script). The condition is an expression that evaluates to either true or
 * false.
 * <p>
 * It is declared within script block where a line begins with an exclamation mark and opening
 * parenthesis and ends with "){": <code>!(...){</code>. This reader handles the content of the
 * condition, beginning right after the opening parenthesis, and stopping right after the opening
 * curly brace. The condition within parenthesis may by be surrounded by whitespace. (After the
 * opening curly brace comes the SQL script until the closing curly brace.)
 * <p>
 * Examples of valid condition expressions:
 * <pre>
 * paramName
 * paramName.nested.prop
 * empty( paramName )
 * empty( paramName.nested.prop )
 * true( paramName )
 * true( paramName.nested.prop )
 * </pre>
 * <p>
 * Here <code>empty</code> and <code>true</code> are predicates indicating that the expression
 * within parenthesis should evaluate to an empty value (String or List, including nulls) and to
 * boolean true to activate the script block. Without predicates, the expression is evaluated to
 * true when the parameter (or its [nested] property) is not null nor empty (Object, Boolean, String
 * or List). Without predicates, Boolean false value of the property evaluates to true because it is
 * not undefined, but empty string evaluates to false because it is empty.
 * <p>
 * Example of how to use this class:
 * <pre>SqlPartCondition cond = conditionReader
 *     .parseCondition()
 *     .resolveParam(paramsSet)
 *     .build();</pre>
 */
public final class ConditionReader {

  private static final String COND_FUNC_EMPTY = "empty";

  private static final String COND_FUNC_TRUE = "true";

  private final StreamReader reader;

  private String func;

  private String expr;

  private List<String> properties;

  private QueryParam param;

  /**
   * Initiates the parameters reader instance, which may be used for parsing several scripts as long
   * as they are within one scripts file.
   *
   * @param reader The reader for current scripts file.
   */
  public ConditionReader(StreamReader reader) {
    this.reader = requireNonNull(reader, "StreamReader is undefined.");
  }

  /**
   * Parses the text from stream to identify the predicate mode and referenced parameters.
   * <p>
   * The next step is to evaluate the extracted parameters.
   *
   * @return The current reader instance.
   * @throws IOException When a stream-related exception occurs during reading.
   * @see #resolveParam(ParamsSet)
   */
  public ConditionReader parseCondition() throws IOException {
    parseFuncAndName();
    parseNestedProps();
    parseConditionEnd();
    return this;
  }

  /**
   * Performs referenced parameter lookup and stores a parameter/expression definition in this
   * object.
   *
   * @param params There parameters-set for current script, used for referred parameter lookup.
   * @return The current reader instance.
   * @see #build()
   */
  public ConditionReader resolveParam(ParamsSet params) {
    TypeNameParam inParam = params.getInputParams().get(this.expr);
    params.markInParamAsUsed(this.expr);

    if (this.properties.isEmpty()) {
      this.param = new QueryParam(ParamMode.IN, inParam);
    } else {
      this.param = new QueryParam(ParamMode.IN, Expression.create(inParam, this.properties, null));
    }
    return this;
  }

  /**
   * Creates an SQL part condition object with all the necessary data for evaluating it correctly.
   *
   * @return The created condition object.
   */
  public SqlPartCondition build() {
    SqlPartCondition result;

    if (this.func == null) {
      result = new ParamValueNonEmpty(this.param);
    } else if (COND_FUNC_EMPTY.equals(this.func)) {
      result = new ParamValueEmpty(this.param);
    } else if (COND_FUNC_TRUE.equals(this.func)) {
      result = new ParamValueTrue(this.param);
    } else {
      throw new ScriptSetupException("Unsupported query part condition function: '%s'", this.func);
    }

    return result;
  }

  private void parseFuncAndName() throws IOException {
    this.expr = this.reader.parseParamName();

    if (this.reader.isNext('(')) {
      if (!COND_FUNC_EMPTY.equals(this.expr) && !COND_FUNC_TRUE.equals(this.expr)) {
        throw new ScriptSetupException("Expected script inclusion condition '%s' or '%s' "
            + "but got '%s'.", COND_FUNC_EMPTY, COND_FUNC_TRUE, expr);
      }

      this.func = this.expr;
      this.reader.skipNext();
      this.reader.skipWsp();
      this.expr = this.reader.parseParamName();
    }
  }

  private void parseNestedProps() throws IOException {
    this.properties = new ArrayList<>();

    while (this.reader.skipIfNext('.')) {
      this.properties.add(this.reader.parseParamName());
    }

    this.reader.skipWsp();
    if (this.func != null) {
      this.reader.requireNext(')');
    }
  }

  private void parseConditionEnd() throws IOException {
    this.reader.requireNext(')');
    this.reader.requireNext('{');
  }

}
