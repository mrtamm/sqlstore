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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.script.QueryParam;
import ws.rocket.sqlstore.script.sql.ParamValueEmpty;
import ws.rocket.sqlstore.script.sql.SqlPartCondition;

/**
 * Tests the {@link ParamValueEmpty} class.
 */
@Test
public final class ParamValueEmptyTest extends AbstractParamConditionTest {

  @Override
  protected SqlPartCondition createCondition(QueryParam param) {
    return new ParamValueEmpty(param);
  }

  public void shouldNotApplyWhenParamIsNotEmpty() {
    assertFalse(isApplicable(new Date()));
  }

  public void shouldNotApplyWhenParamIsBoolean() {
    assertFalse(isApplicable(Boolean.TRUE));
    assertFalse(isApplicable(Boolean.FALSE));
  }

  public void shouldApplyWhenParamIsNull() {
    assertTrue(isApplicable(null));
  }

  public void shouldApplyWhenParamIsEmptyString() {
    assertTrue(isApplicable(""));
  }

  public void shouldApplyWhenParamIsEmptyArray() {
    assertTrue(isApplicable(new long[0]));
    assertTrue(isApplicable(new int[0]));
    assertTrue(isApplicable(new short[0]));
    assertTrue(isApplicable(new byte[0]));
    assertTrue(isApplicable(new double[0]));
    assertTrue(isApplicable(new float[0]));
    assertTrue(isApplicable(new char[0]));
    assertTrue(isApplicable(new boolean[0]));
    assertTrue(isApplicable(new Integer[0]));
    assertTrue(isApplicable(new Integer[0]));
  }

  public void shouldApplyWhenParamIsEmptyCollection() {
    assertTrue(isApplicable(new ArrayList<>()));
    assertTrue(isApplicable(new HashSet<>()));
  }

  public void shouldProvideCustomToString() {
    expectToStringText(" is null, empty string, empty array, or empty collection");
  }

}
