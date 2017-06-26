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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.QueryParam;
import ws.rocket.sqlstore.script.Script;
import ws.rocket.sqlstore.script.read.ScriptReader;
import ws.rocket.sqlstore.script.sql.ConditionAlways;
import ws.rocket.sqlstore.script.sql.ParamValueTrue;
import ws.rocket.sqlstore.script.sql.SqlPart;
import ws.rocket.sqlstore.test.helper.Factory;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Tests the {@link SqlPart} class.
 */
@Test
public final class SqlPartTest {

  public void shouldHandleSimpleSql() {
    SqlPart sqlPart = new SqlPart(ConditionAlways.INSTANCE, "some SQL", QueryParam.NO_PARAMS);
    assertFalse(sqlPart.containsOutParam());

    StringBuilder sqlResult = new StringBuilder();
    List<QueryParam> paramsResult = new ArrayList<>();

    sqlPart.appendSql(null, sqlResult, paramsResult);

    assertEquals(sqlResult.toString(), "some SQL", "Expecting same SQL text");
    assertTrue(paramsResult.isEmpty(), "Expecting no params");
    assertEquals(sqlPart.toString(), "some SQL", "Expecting same SQL text from toString()");
  }

  public void shouldAppendSql() throws IOException {
    final StringBuilder sqlResult = new StringBuilder();
    final List<QueryParam> paramsResult = new ArrayList<>();

    SqlPart sqlPart = createSqlPartWithBooleanParam();
    sqlPart.appendSql(createQueryContext(true), sqlResult, paramsResult);

    assertEquals(sqlResult.toString(), "some SQL", "Expecting same SQL text");
    assertEquals(paramsResult.size(), 1, "Expecting 1 QueryParam from SqlPart.");
  }

  public void shouldNotAppendSql() throws IOException {
    final StringBuilder sqlResult = new StringBuilder();
    final List<QueryParam> paramsResult = new ArrayList<>();

    SqlPart sqlPart = createSqlPartWithBooleanParam();
    sqlPart.appendSql(createQueryContext(false), sqlResult, paramsResult);

    assertEquals(sqlResult.toString(), "", "Expecting no SQL text");
    assertTrue(paramsResult.isEmpty(), "Expecting no params");
  }

  public void shouldProvideSqlInToString() throws IOException {
    SqlPart sqlPart = createSqlPartWithBooleanParam();

    assertEquals(sqlPart.toString(),
        "# When: IN Boolean testParam == true\n"
        + "# SQL statement parameters {\n"
        + "#  1: IN Boolean testParam\n"
        + "# }\n"
        + "some SQL\n"
        + "# End of when: IN Boolean testParam == true");
  }

  public void shouldContainOutParam() throws IOException {
    QueryParam[] params = new QueryParam[] { Factory.queryInOutStringParam("testParam") };
    SqlPart sqlPart = new SqlPart(ConditionAlways.INSTANCE, "some SQL", params);

    boolean containsOutParam = sqlPart.containsOutParam();

    assertTrue(containsOutParam);
  }

  public void shouldNotContainOutParam() throws IOException {
    QueryParam[] params = new QueryParam[] { Factory.queryStringParam("testParam") };
    SqlPart sqlPart = new SqlPart(ConditionAlways.INSTANCE, "some SQL", params);

    boolean containsOutParam = sqlPart.containsOutParam();

    assertFalse(containsOutParam);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "Query SQL must not be empty\\.")
  public void shouldFailWhenSqlIsBlank() throws IOException {
    QueryParam[] params = new QueryParam[] { Factory.queryStringParam("testParam") };
    new SqlPart(ConditionAlways.INSTANCE, " ", params).toString();
  }

  private static SqlPart createSqlPartWithBooleanParam() throws IOException {
    QueryParam queryParam = Factory.queryParam(Boolean.class, "testParam");
    QueryParam[] allParams = { queryParam };
    return new SqlPart(new ParamValueTrue(queryParam), "some SQL", allParams);
  }

  private static QueryContext createQueryContext(boolean paramValue) throws IOException {
    String scriptText = "testScript IN(boolean testParam)\n"
        + "====\n"
        + "some SQL with param ?{testParam}\n"
        + "====\n";

    Script script = new ScriptReader(Factory.inputStreamOf(scriptText))
        .parseName()
        .parseParams()
        .parseSql()
        .createScript();

    return new QueryContext(script, new Object[] { paramValue });
  }

}
