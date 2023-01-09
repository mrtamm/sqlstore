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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.sql.Types;
import java.util.List;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.QueryParam;
import ws.rocket.sqlstore.script.Script;
import ws.rocket.sqlstore.script.params.Expression;
import ws.rocket.sqlstore.script.params.ParamMode;
import ws.rocket.sqlstore.script.params.TypeNameParam;
import ws.rocket.sqlstore.test.db.model.Person;
import ws.rocket.sqlstore.test.helper.Factory;

/**
 * Tests the {@link QueryParam} class.
 */
@Test
public final class QueryParamTest {

  public void shouldExposeParamInToString() {
    QueryParam param = new QueryParam(ParamMode.IN, Factory.stringParam("param"));

    assertEquals(param.toString(), "IN String|12 param");
  }

  public void shouldExposeExpressionInToString() {
    Expression expression = Expression.create(Factory.stringParam("param"), null, null);
    QueryParam param = new QueryParam(ParamMode.IN, expression);

    assertEquals(param.toString(), "IN String|12 param");
  }

  public void shouldExposeParam() {
    final TypeNameParam stringParam = Factory.stringParam("param");
    QueryParam param = new QueryParam(ParamMode.IN, stringParam);

    assertEquals(param.getParam(), stringParam);
  }

  public void shouldExposeParamFromExpression() {
    Expression expression = Expression.create(Factory.stringParam("param"), null, null);
    QueryParam param = new QueryParam(ParamMode.IN, expression);

    assertEquals(param.getParam(), expression);
  }

  public void shouldBeInMode() {
    QueryParam param = new QueryParam(ParamMode.IN, Factory.stringParam("param"));

    assertTrue(param.isForInput());
    assertFalse(param.isForOutput());
  }

  public void shouldBeOutMode() {
    QueryParam param = new QueryParam(ParamMode.OUT, Factory.stringParam("param"));

    assertFalse(param.isForInput());
    assertTrue(param.isForOutput());
  }

  public void shouldBeInOutMode() {
    QueryParam param = new QueryParam(ParamMode.INOUT, Factory.stringParam("param"));

    assertTrue(param.isForInput());
    assertTrue(param.isForOutput());
  }

  public void shouldGetParamValue() {
    Script script = Factory.script("testScript IN(String param)\n====\nSELECT ?{param}\n====\n");
    QueryContext ctx = new QueryContext(script, new Object[] { "testValue" });
    QueryParam param = new QueryParam(ParamMode.IN, Factory.stringParam("param"));

    assertEquals(param.getValue(ctx), "testValue");
  }

  public void shouldGetExpressionValue() {
    Script script = Factory.script("""
        testScript IN(ws.rocket.sqlstore.test.db.model.Person param)
        ====
        SELECT ?{param.name}
        ====
        """);
    Person paramValue = new Person();
    paramValue.setName("testValue");
    QueryContext ctx = new QueryContext(script, new Object[] { paramValue });

    Expression expression = Expression.create(Factory.typeParam(Person.class, "param"),
        List.of("name"), null);
    QueryParam param = new QueryParam(ParamMode.IN, expression);

    assertEquals(param.getValue(ctx), "testValue");
  }

  public void shouldSetParamValue() {
    Script script = Factory.script("""
        testScript OUT(String param)
        ====
        SELECT ?{OUT(param)}
        ====
        """);
    QueryContext ctx = new QueryContext(script, new Object[0]);
    ctx.initResultsContainer(List.class, String.class);

    TypeNameParam typedParam = new TypeNameParam(String.class, Types.VARCHAR, "param", 0);
    QueryParam param = new QueryParam(ParamMode.OUT, typedParam);

    param.setValue(ctx, "testValue");

    assertEquals(ctx.getResultsCollector().getRowValue(0), "testValue");
  }

}
