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

package ws.rocket.sqlstore.test.script;

import java.util.Map;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptExecuteException;
import ws.rocket.sqlstore.script.InputParams;
import ws.rocket.sqlstore.script.params.TypeNameParam;
import ws.rocket.sqlstore.test.helper.Factory;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Tests the {@link InputParams} class.
 */
@Test
public final class InputParamsTest {

  public void shouldBeEmpty() {
    assertTrue(InputParams.EMPTY.isEmpty());
  }

  public void shouldBeNotEmpty() {
    InputParams params = new InputParams(new TypeNameParam[] { Factory.stringParam("param") });
    assertFalse(params.isEmpty());
  }

  public void shouldGetParam() {
    final TypeNameParam param = Factory.stringParam("param");
    InputParams params = new InputParams(new TypeNameParam[] { param });

    assertSame(params.get("param"), param);
  }

  public void shouldGetNullParam() {
    assertNull(InputParams.EMPTY.get("param"));
  }

  public void shouldSupportGivenTypes() {
    final TypeNameParam[] typeParams = {
      Factory.stringParam("param1"),
      Factory.typeParam(boolean.class, "param2")
    };

    InputParams params = new InputParams(typeParams);

    assertTrue(params.supportsTypes(String.class, Boolean.class));
  }

  public void shouldNotSupportGivenTypesWhenIncorrectOrder() {
    final TypeNameParam[] typeParams = {
      Factory.stringParam("param1"),
      Factory.typeParam(boolean.class, "param2")
    };

    InputParams params = new InputParams(typeParams);

    assertFalse(params.supportsTypes(Boolean.class, String.class));
  }

  public void shouldBindToEmptyMap() {
    assertTrue(InputParams.EMPTY.bind(null).isEmpty());
  }

  public void shouldBindToMapWithValues() {
    final TypeNameParam[] typeParams = {
      Factory.stringParam("param1"),
      Factory.typeParam(boolean.class, "param2")
    };

    InputParams params = new InputParams(typeParams);
    Map<String, Object> bindMap = params.bind(new Object[] { "value", true });

    assertEquals(bindMap.size(), 2);
    assertEquals(bindMap.get("param1"), "value");
    assertEquals(bindMap.get("param2"), true);
  }

  @Test(expectedExceptions = ScriptExecuteException.class, expectedExceptionsMessageRegExp
      = "Script input arguments amount mismatch\\: expected 2, got 1")
  public void shouldFailToBindWhenValuesCountDoesNotMatch() {
    final TypeNameParam[] typeParams = {
      Factory.stringParam("param1"),
      Factory.typeParam(boolean.class, "param2")
    };

    InputParams params = new InputParams(typeParams);
    params.bind(new Object[] { "value" });
  }

  public void shouldReturnEmptyToString() {
    assertEquals(InputParams.EMPTY.toString(), "");
  }

  public void shouldProvideParamsInToString() {
    final TypeNameParam[] typeParams = {
      Factory.stringParam("param1"),
      Factory.typeParam(boolean.class, "param2")
    };

    InputParams params = new InputParams(typeParams);
    assertEquals(params.toString(), "IN(String|12 param1, boolean param2)");
  }

}
