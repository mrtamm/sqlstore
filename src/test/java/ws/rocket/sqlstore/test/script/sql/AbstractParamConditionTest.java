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

package ws.rocket.sqlstore.test.script.sql;

import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.QueryParam;
import ws.rocket.sqlstore.script.sql.SqlPartCondition;
import ws.rocket.sqlstore.test.helper.Factory;
import ws.rocket.sqlstore.test.helper.ScriptBuilder;

import static org.testng.Assert.assertEquals;

/**
 * Contains common code for testing parameter-value-based {@link SqlPartCondition}s.
 */
abstract class AbstractParamConditionTest {

  /**
   * Creates the condition evaluating object for given query parameter.
   *
   * @param param The parameter used in the test.
   * @return The condition evaluating object being tested.
   */
  protected abstract SqlPartCondition createCondition(QueryParam param);

  /**
   * Prepares a script context for testing whether the parameter-based condition enables a dummy SQL
   * part or not.
   *
   * @param value A sample <code>QueryParam</code> value to be passed to the condition object.
   * @return A Boolean that is <code>true</code> when condition was satisfied.
   */
  protected final boolean isApplicable(Object value) {
    Class<?> paramType = value != null ? value.getClass() : String.class;
    QueryParam queryParam = Factory.queryParam(paramType, "testParam");

    QueryContext ctx = new ScriptBuilder()
        .addInParam(paramType, "testParam")
        .toQueryContext(value);

    return createCondition(queryParam).isApplicable(ctx);
  }

  protected final void expectToStringText(String text) {
    QueryParam queryParam = Factory.queryParam(String.class, "testParam");
    String toString = createCondition(queryParam).toString();

    assertEquals(toString, queryParam + text);
  }

}
