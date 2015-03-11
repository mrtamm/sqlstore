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
import ws.rocket.sqlstore.result.ArrayResultsCollector;
import ws.rocket.sqlstore.result.ListResultsCollector;
import ws.rocket.sqlstore.result.MapResultsCollector;
import ws.rocket.sqlstore.result.ResultsCollector;
import ws.rocket.sqlstore.result.VoidResultsCollector;
import ws.rocket.sqlstore.script.OutputParams;
import ws.rocket.sqlstore.script.params.Param;
import ws.rocket.sqlstore.script.params.TypeParam;

import static org.testng.Assert.assertTrue;

/**
 *
 * @author martti
 */
@Test
public class OutputParamsTest {

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

}
