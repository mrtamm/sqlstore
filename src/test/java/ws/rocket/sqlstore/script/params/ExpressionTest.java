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

package ws.rocket.sqlstore.script.params;

import static org.testng.Assert.assertEquals;

import java.sql.Types;
import java.util.List;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.QueryContext;
import ws.rocket.sqlstore.db.model.Person;
import ws.rocket.sqlstore.helper.ScriptBuilder;

/**
 * Tests the {@link Expression} class.
 */
@Test
public final class ExpressionTest extends ParamTest {

  public void shouldStoreSimpleExpressionProperties() {
    TypeNameParam param = new TypeNameParam(String.class, Types.VARCHAR, "paramName");
    Expression expr = Expression.create(param, null, null);

    checkTypes(expr, String.class, Types.VARCHAR);
  }

  public void shouldStoreBeanPropExpressionProperties() {
    TypeNameParam param = new TypeNameParam(Person.class, null, "paramName");
    Expression expr = Expression.create(param, List.of("name"), null);

    checkTypes(expr, String.class, Types.VARCHAR);
  }

  public void shouldReadSimpleParamValue() {
    QueryContext queryCtx = new ScriptBuilder()
        .addInParam(String.class, "paramName")
        .toQueryContext("some value");

    TypeNameParam param = new TypeNameParam(String.class, Types.VARCHAR, "paramName");
    Expression expr = Expression.create(param, null, null);

    assertEquals(expr.read(queryCtx), "some value");
  }

  public void shouldReadBeanPropParamValue() {
    Person value = new Person();
    value.setName("person name");

    QueryContext queryCtx = new ScriptBuilder()
        .addInPersonParam("paramName")
        .toQueryContext(value);

    TypeNameParam param = new TypeNameParam(Person.class, null, "paramName");
    Expression expr = Expression.create(param, List.of("name"), null);

    assertEquals(expr.read(queryCtx), "person name");
  }

  public void shouldWriteSimpleParamValue() {
    QueryContext queryCtx = new ScriptBuilder()
        .addOutStringParam("paramName")
        .toQueryContext();
    queryCtx.initResultsContainer(List.class, String.class);

    TypeNameParam param = new TypeNameParam(String.class, Types.VARCHAR, "paramName", 0);
    Expression expr = Expression.create(param, null, null);

    expr.write(queryCtx, "some value");

    assertEquals(queryCtx.getResultsCollector().getRowValue(0), "some value");
  }

  public void shouldWriteBeanPropParamValue() {
    QueryContext queryCtx = new ScriptBuilder()
        .addPersonOutParam("name")
        .toQueryContext();
    queryCtx.initResultsContainer(List.class, Person.class);

    TypeNameParam param = new TypeNameParam(Person.class, null, "paramName", 0);
    Expression expr = Expression.create(param, List.of("name"), null);

    expr.write(queryCtx, "person name");

    Person rowValue = (Person) queryCtx.getResultsCollector().getRowValue(0);
    assertEquals(rowValue.getName(), "person name");
  }

  public void shouldProvideToString() {
    TypeNameParam param = new TypeNameParam(String.class, Types.VARCHAR, "paramName");
    Expression expr = Expression.create(param, null, null);
    assertEquals(expr.toString(), "String|12 paramName");
  }

}
