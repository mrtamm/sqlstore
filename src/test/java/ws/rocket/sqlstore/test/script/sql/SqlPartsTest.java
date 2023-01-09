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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

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
import ws.rocket.sqlstore.script.sql.SqlParts;
import ws.rocket.sqlstore.test.helper.Factory;

/**
 * Tests the {@link SqlParts} class.
 */
@Test
public final class SqlPartsTest {

  public void shouldAppendSql() throws IOException {
    final StringBuilder sqlResult = new StringBuilder();
    final List<QueryParam> paramsResult = new ArrayList<>();

    SqlParts sqlParts = createSqlParts();
    sqlParts.appendSql(createQueryContext(true), sqlResult, paramsResult);

    assertEquals(sqlResult.toString(), "[primary SQL][secondary SQL]", "Expecting same SQL text");
    assertEquals(paramsResult.size(), 2, "Expecting 2 QueryParams from SqlParts.");
  }

  public void shouldNotAppendSql() throws IOException {
    final StringBuilder sqlResult = new StringBuilder();
    final List<QueryParam> paramsResult = new ArrayList<>();

    SqlParts sqlParts = createSqlParts();
    sqlParts.appendSql(createQueryContext(false), sqlResult, paramsResult);

    assertEquals(sqlResult.toString(), "[primary SQL]", "Expecting partial SQL text");
    assertEquals(paramsResult.size(), 1, "Expecting 1 QueryParam from SqlParts");
  }

  public void shouldNotAppendAnySql() throws IOException {
    final StringBuilder sqlResult = new StringBuilder();
    final List<QueryParam> paramsResult = new ArrayList<>();

    QueryParam param = Factory.queryParam(boolean.class, "test");
    SqlPart[] innerParts = {
        new SqlPart(ConditionAlways.INSTANCE, "[primary SQL]", QueryParam.NO_PARAMS),
        new SqlPart(ConditionAlways.INSTANCE, "[secondary SQL]", QueryParam.NO_PARAMS)
    };

    SqlParts sqlParts = new SqlParts(new ParamValueTrue(param), innerParts);
    sqlParts.appendSql(createQueryContext(false), sqlResult, paramsResult);

    assertEquals(sqlResult.toString(), "", "Expecting partial SQL text");
    assertTrue(paramsResult.isEmpty(), "Expecting no QueryParams from SqlParts");
  }

  public void shouldProvideSqlInToString() {
    SqlParts sqlParts = createSqlParts();

    assertEquals(sqlParts.toString(), """
        # SQL statement parameters {
        #  1: IN String|12 param1
        # }
        [primary SQL]
        # When: INOUT String|12 param2 == true
        # SQL statement parameters {
        #  1: INOUT String|12 param2
        # }
        [secondary SQL]
        # End of when: INOUT String|12 param2 == true""");
  }

  public void shouldContainOutParam() {
    SqlParts parts = createSqlParts();

    boolean containsOutParam = parts.containsOutParam();

    assertTrue(containsOutParam);
  }

  public void shouldNotContainOutParam() {
    QueryParam[] allParams = { Factory.queryParam(Boolean.class, "testParam") };
    SqlPart[] innerParts = {
        new SqlPart(ConditionAlways.INSTANCE, "primary SQL", allParams),
        new SqlPart(ConditionAlways.INSTANCE, "secondary SQL", allParams)
    };
    SqlParts parts = new SqlParts(ConditionAlways.INSTANCE, innerParts);

    boolean containsOutParam = parts.containsOutParam();

    assertFalse(containsOutParam);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "At least 2 inner parts expected\\.")
  public void shouldFailWhenLessThanTwoInnerScripts() {
    SqlPart[] innerParts = {
        new SqlPart(ConditionAlways.INSTANCE, "primary SQL", QueryParam.NO_PARAMS)
    };
    new SqlParts(ConditionAlways.INSTANCE, innerParts).toString();
  }

  private static SqlParts createSqlParts() {
    QueryParam[] params1 = { Factory.queryStringParam("param1") };
    QueryParam[] params2 = { Factory.queryInOutStringParam("param2") };
    SqlPart[] innerParts = {
        new SqlPart(ConditionAlways.INSTANCE, "[primary SQL]", params1),
        new SqlPart(new ParamValueTrue(params2[0]), "[secondary SQL]", params2)
    };
    return new SqlParts(ConditionAlways.INSTANCE, innerParts);
  }

  private static QueryContext createQueryContext(boolean paramValue) throws IOException {
    String scriptText = """
        testScript IN(Boolean param1, Boolean param2)
        ====
        some SQL with param ?{param1} and ?{param2}
        ====
        """;

    Script script = new ScriptReader(Factory.inputStreamOf(scriptText))
        .parseName()
        .parseParams()
        .parseSql()
        .createScript();

    return new QueryContext(script, new Object[] { true, paramValue });
  }

}
