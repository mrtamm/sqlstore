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

package ws.rocket.sqlstore.script.sql;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;
import ws.rocket.sqlstore.script.QueryParam;

/**
 * Tests the {@link ParamValueTrue} class.
 */
@Test
public final class ParamValueTrueTest extends AbstractParamConditionTest {

  @Override
  protected SqlPartCondition createCondition(QueryParam param) {
    return new ParamValueTrue(param);
  }

  public void shouldNotApplyWhenParamIsNull() {
    assertFalse(isApplicable(null));
  }

  public void shouldNotApplyWhenParamIsNotBoolean() {
    assertFalse(isApplicable("true"));
  }

  public void shouldNotApplyWhenParamIsBooleanFalse() {
    assertFalse(isApplicable(false));
  }

  public void shouldApplyWhenParamIsBooleanTrue() {
    assertTrue(isApplicable(true));
  }

  public void shouldProvideCustomToString() {
    expectToStringText(" == true");
  }

}
