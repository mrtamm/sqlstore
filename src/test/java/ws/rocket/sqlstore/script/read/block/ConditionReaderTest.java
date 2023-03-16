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

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.helper.Factory;
import ws.rocket.sqlstore.helper.ScriptBuilder;
import ws.rocket.sqlstore.script.read.ParamsSet;
import ws.rocket.sqlstore.script.sql.ParamValueEmpty;
import ws.rocket.sqlstore.script.sql.ParamValueNonEmpty;
import ws.rocket.sqlstore.script.sql.ParamValueTrue;
import ws.rocket.sqlstore.script.sql.SqlPartCondition;

/**
 * Tests the {@link ConditionReader} class.
 */
@Test
public final class ConditionReaderTest {

  private static SqlPartCondition createCondition(String stream, ParamsSet params)
      throws IOException {
    return new ConditionReader(Factory.streamOf(stream))
        .parseCondition()
        .resolveParam(params)
        .build();
  }

  private static ParamsSet stringParam() {
    return new ScriptBuilder().addInStringParam("demo").initParams();
  }

  private static ParamsSet personParam() {
    return new ScriptBuilder().addInPersonParam("person").initParams();
  }

  private static void checkCondition(SqlPartCondition condition, Class<?> targetType) {
    assertNotNull(condition, "Condition handling object must be returned.");
    assertTrue(targetType.isInstance(condition),
        "Expected " + targetType.getSimpleName() + " condition.");
  }

  public void shouldSucceedWithNonEmptyParamNameCondition() throws IOException {
    SqlPartCondition condition = createCondition("demo){", stringParam());
    checkCondition(condition, ParamValueNonEmpty.class);
  }

  public void shouldSucceedWithNonEmptyNestedParamCondition() throws IOException {
    SqlPartCondition condition = createCondition("person.name){", personParam());
    checkCondition(condition, ParamValueNonEmpty.class);
  }

  public void shouldSucceedWithEmptyParamNameCondition() throws IOException {
    SqlPartCondition condition = createCondition("empty(demo)){", stringParam());
    checkCondition(condition, ParamValueEmpty.class);
  }

  public void shouldSucceedWithEmptyNestedParamNameCondition() throws IOException {
    SqlPartCondition condition = createCondition("empty(person.name)){", personParam());
    checkCondition(condition, ParamValueEmpty.class);
  }

  public void shouldSucceedWithParamValueTrueCondition() throws IOException {
    SqlPartCondition condition = createCondition("true(demo)){", stringParam());
    checkCondition(condition, ParamValueTrue.class);
  }

  public void shouldSucceedWithNestedParamValueTrueCondition() throws IOException {
    SqlPartCondition condition = createCondition("true(person.active)){", personParam());
    checkCondition(condition, ParamValueTrue.class);
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "Expected script inclusion condition 'empty' or 'true' but got 'false'.")
  public void shouldFailWithUnknownCondition() throws IOException {
    createCondition("false(person.active)){", personParam());
  }

}
