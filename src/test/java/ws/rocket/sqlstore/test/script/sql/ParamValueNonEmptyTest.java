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
import ws.rocket.sqlstore.script.sql.ParamValueNonEmpty;
import ws.rocket.sqlstore.script.sql.SqlPartCondition;

/**
 * Tests the {@link ParamValueNonEmpty} class.
 */
@Test
public final class ParamValueNonEmptyTest extends AbstractParamConditionTest {

  @Override
  protected SqlPartCondition createCondition(QueryParam param) {
    return new ParamValueNonEmpty(param);
  }

  public void shouldApplyWhenParamIsNotEmpty() {
    assertTrue(isApplicable(new Date()));
  }

  public void shouldApplyWhenParamIsBoolean() {
    assertTrue(isApplicable(Boolean.TRUE));
    assertTrue(isApplicable(Boolean.FALSE));
  }

  public void shouldNotApplyWhenParamIsNull() {
    assertFalse(isApplicable(null));
  }

  public void shouldNotApplyWhenParamIsEmptyString() {
    assertFalse(isApplicable(""));
  }

  public void shouldNotApplyWhenParamIsEmptyArray() {
    assertFalse(isApplicable(new long[0]));
    assertFalse(isApplicable(new int[0]));
    assertFalse(isApplicable(new short[0]));
    assertFalse(isApplicable(new byte[0]));
    assertFalse(isApplicable(new double[0]));
    assertFalse(isApplicable(new float[0]));
    assertFalse(isApplicable(new char[0]));
    assertFalse(isApplicable(new boolean[0]));
    assertFalse(isApplicable(new Integer[0]));
  }

  public void shouldNotApplyWhenParamIsEmptyCollection() {
    assertFalse(isApplicable(new ArrayList<>()));
    assertFalse(isApplicable(new HashSet<>()));
  }

  public void shouldProvideCustomToString() {
    expectToStringText(" is not null and also not an empty string, an empty array nor an empty "
        + "collection");
  }

}
