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

package ws.rocket.sqlstore.test.script.params;

import static org.testng.Assert.assertEquals;

import java.sql.Types;
import java.util.List;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptExecuteException;
import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.params.TypeNameParam;
import ws.rocket.sqlstore.test.helper.ScriptBuilder;

/**
 * Tests the {@link TypeNameParam} class.
 */
@Test
public final class TypeNameParamTest extends ParamTest {

  public void shouldStoreParamProperties() {
    TypeNameParam param = new TypeNameParam(String.class, Types.VARCHAR, "paramName");
    checkTypes(param, String.class, Types.VARCHAR);
    assertEquals(param.getName(), "paramName");

    param = new TypeNameParam(String.class, Types.VARCHAR, "paramName", 1);
    checkTypes(param, String.class, Types.VARCHAR);
    assertEquals(param.getName(), "paramName");
  }

  public void shouldReadParamValue() {
    QueryContext queryCtx = new ScriptBuilder()
        .addInParam(String.class, "paramName")
        .toQueryContext("some value");

    TypeNameParam param = new TypeNameParam(String.class, Types.VARCHAR, "paramName");

    assertEquals(param.read(queryCtx), "some value");
  }

  public void shouldWriteParamValue() {
    QueryContext queryCtx = new ScriptBuilder()
        .addOutParam(String.class, null)
        .toQueryContext();
    queryCtx.initResultsContainer(List.class, String.class);

    TypeNameParam param = new TypeNameParam(String.class, Types.VARCHAR, "paramName", 0);
    param.write(queryCtx, "some value");

    assertEquals(queryCtx.getResultsCollector().getRowValue(0), "some value");
  }

  public void shouldUpdateParamValue() {
    QueryContext queryCtx = new ScriptBuilder()
        .addInParam(String.class, "paramName")
        .toQueryContext("in-value");

    TypeNameParam param = new TypeNameParam(String.class, Types.VARCHAR, "paramName");
    param.write(queryCtx, "out-value");

    assertEquals(queryCtx.getVariable("paramName"), "out-value");
  }

  @Test(expectedExceptions = ScriptExecuteException.class, expectedExceptionsMessageRegExp
      = "Script argument with index 1 value mismatch: expected a not null value of boolean")
  public void shouldRejectValueDueToBeingNull() {
    TypeNameParam param = new TypeNameParam(boolean.class, Types.BOOLEAN, "paramName");
    param.validate(null, 1);
  }

  @Test(expectedExceptions = ScriptExecuteException.class, expectedExceptionsMessageRegExp
      = "Script argument with index 2 type mismatch: expected boolean, got java\\.lang\\.Long")
  public void shouldRejectValueDueToBeingIncompatibleType() {
    TypeNameParam param = new TypeNameParam(boolean.class, Types.BOOLEAN, "paramName");
    param.validate(123L, 2);
  }

  public void shouldProvideToString() {
    TypeNameParam param = new TypeNameParam(String.class, Types.VARCHAR, "paramName");
    assertEquals(param.toString(), "String|12 paramName");
  }

}
