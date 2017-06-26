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

import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptExecuteException;
import ws.rocket.sqlstore.result.ArrayResultsCollector;
import ws.rocket.sqlstore.result.ListResultsCollector;
import ws.rocket.sqlstore.result.MapResultsCollector;
import ws.rocket.sqlstore.result.ResultsCollector;
import ws.rocket.sqlstore.result.VoidResultsCollector;
import ws.rocket.sqlstore.script.OutputParams;
import ws.rocket.sqlstore.script.params.Param;
import ws.rocket.sqlstore.script.params.TypeParam;
import ws.rocket.sqlstore.script.params.TypePropParam;
import ws.rocket.sqlstore.test.db.model.Person;
import ws.rocket.sqlstore.test.helper.Factory;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Tests the {@link OutputParams} class.
 */
@Test
public final class OutputParamsTest {

  public void shouldBeEmpty() {
    assertTrue(OutputParams.EMPTY.isEmpty());
  }

  public void shouldNotBeEmpty() {
    OutputParams params = new OutputParams(new Param[] { Factory.stringParam("param") });
    assertFalse(params.isEmpty());
  }

  public void shouldGetParamByName() {
    Param param = Factory.stringParam("param");
    OutputParams params = new OutputParams(new Param[] { param });

    assertSame(params.get("param"), param);
  }

  public void shouldGetNullParam() {
    Param param = Factory.stringParam("param");
    OutputParams params = new OutputParams(new Param[] { param });

    assertNull(params.get("paramElse"));
  }

  public void shouldReturnEmptyToString() {
    assertEquals(OutputParams.EMPTY.toString(), "");
  }

  public void shouldReturnParamsInToString() {
    Param param = new TypeParam(Long.class, 2, 0);
    OutputParams params = new OutputParams(new Param[] { param });

    assertEquals(params.toString(), "OUT(Long|2)");
  }

  public void shouldSupportVoid() {
    ResultsCollector collector = OutputParams.EMPTY.createResultsCollector(Void.class);
    assertTrue(collector instanceof VoidResultsCollector,
        "Expected VoidResultsCollector but got: " + collector);
  }

  public void shouldSupportList() {
    Param[] params = new Param[] { new TypeParam(Long.class, Types.NUMERIC, 0) };
    OutputParams output = new OutputParams(params);

    ResultsCollector collector = output.createResultsCollector(List.class, Long.class);

    assertTrue(collector instanceof ListResultsCollector,
        "Expected ListResultsCollector but got: " + collector);
  }

  public void shouldSupportMap() {
    Param[] params = new Param[] {
      new TypeParam(Long.class, Types.NUMERIC, 0),
      new TypeParam(String.class, Types.VARCHAR, 1)
    };
    OutputParams output = new OutputParams(params);

    ResultsCollector collector = output.createResultsCollector(Map.class, Long.class, String.class);

    assertTrue(collector instanceof MapResultsCollector,
        "Expected MapResultsCollector but got: " + collector);
  }

  public void shouldSupportArray() {
    Param[] params = new Param[] {
      new TypeParam(Long.class, Types.NUMERIC, 0),
      new TypeParam(String.class, Types.VARCHAR, 1),
      new TypeParam(Date.class, Types.TIMESTAMP, 2)
    };
    OutputParams output = new OutputParams(params);

    ResultsCollector collector = output.createResultsCollector(Object[][].class, Long.class,
        String.class, Date.class);

    assertTrue(collector instanceof ArrayResultsCollector,
        "Expected ArrayResultsCollector but got: " + collector);
  }

  @Test(expectedExceptions = ScriptExecuteException.class, expectedExceptionsMessageRegExp
      = "Query does not support return type java.util.List "
      + "with column types \\[class java.lang.Long\\]")
  public void shouldFailWhenParamsDoNotMatch() {
    Param[] params = new Param[] { new TypeParam(String.class, Types.VARCHAR, 0), };
    OutputParams output = new OutputParams(params);
    output.createResultsCollector(List.class, Long.class);
  }

  public void shouldSupportBeanPropParams() {
    Param[] params = new Param[] {
      new TypeParam(Long.class, Types.NUMERIC, 0),
      TypePropParam.create(Person.class, "id", null, 1),
      TypePropParam.create(Person.class, "name", null, 1),
      TypePropParam.create(Person.class, "dateOfBirth", null, 1)
    };

    OutputParams output = new OutputParams(params);

    assertNotNull(output.createResultsCollector(Map.class, Long.class, Person.class));
  }

}
