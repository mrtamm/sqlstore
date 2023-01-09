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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.Types;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.script.InputParams;
import ws.rocket.sqlstore.script.OutputParams;
import ws.rocket.sqlstore.script.QueryParam;
import ws.rocket.sqlstore.script.Script;
import ws.rocket.sqlstore.script.Script.StatementType;
import ws.rocket.sqlstore.script.params.Param;
import ws.rocket.sqlstore.script.params.ParamMode;
import ws.rocket.sqlstore.script.params.TypeNameParam;
import ws.rocket.sqlstore.script.read.ParamsSet;
import ws.rocket.sqlstore.script.sql.ConditionAlways;
import ws.rocket.sqlstore.script.sql.SqlPart;
import ws.rocket.sqlstore.script.sql.SqlScript;
import ws.rocket.sqlstore.test.helper.Factory;

/**
 * Tests the {@link Script} class.
 */
@Test
public final class ScriptTest {

  public void shouldProvideScriptName() {
    Script script = new Script("simpleScript", 1, simpleSql(), params());

    assertEquals(script.getName(), "simpleScript");
  }

  public void shouldProvideScriptLine() {
    Script script = new Script("script", 123, simpleSql(), params());

    assertEquals(script.getLine(), 123);
  }

  public void shouldProvideEmptyInputParams() {
    Script script = new Script("script", 1, simpleSql(), params());

    assertSame(script.getInputParams(), InputParams.EMPTY);
  }

  public void shouldProvideNonEmptyInputParams() {
    TypeNameParam inputParam = Factory.stringParam("test");
    Script script = new Script("script", 1, simpleSql(), params(inputParam));

    assertFalse(script.getInputParams().isEmpty());
    assertSame(script.getInputParams().get("test"), inputParam);
  }

  public void shouldProvideEmptyOutputParams() {
    Script script = new Script("script", 1, simpleSql(), params());

    assertSame(script.getOutputParams(), OutputParams.EMPTY);
  }

  public void shouldProvideNonEmptyOutputParams() {
    ParamsSet params = new ParamsSet();
    params.addOutParam(BigDecimal.class, Types.NUMERIC, "result", false);
    params.initInOutUpdateParams();

    Script script = new Script("script", 1, simpleSql(), params);

    assertFalse(script.getOutputParams().isEmpty());
    assertSame(script.getOutputParams().get("result").getJavaType(), BigDecimal.class);
  }

  public void shouldProvideEmptyKeysParams() {
    Script script = new Script("script", 1, simpleSql(), params());

    assertSame(script.getKeysParams(), Param.NO_PARAMS);
  }

  public void shouldProvideNonEmptyKeysParams() {
    ParamsSet params = new ParamsSet();
    params.addOutParam(BigDecimal.class, Types.NUMERIC, "result", true);
    params.initInOutUpdateParams();

    Script script = new Script("script", 1, simpleSql(), params);

    assertEquals(script.getKeysParams().length, 1);
    assertSame(script.getKeysParams()[0].getJavaType(), BigDecimal.class);
  }

  public void shouldProvideEmptyGeneratedKeys() {
    Script script = new Script("script", 1, simpleSql(), params());

    assertNull(script.getGeneratedKeys());
  }

  public void shouldProvideNonEmptyGeneratedKeys() {
    ParamsSet params = new ParamsSet();
    params.addOutParam(BigDecimal.class, Types.NUMERIC, "RESULT", true);
    params.initInOutUpdateParams();

    Script script = new Script("script", 1, simpleSql(), params);

    assertEquals(script.getGeneratedKeys().length, 1);
    assertSame(script.getGeneratedKeys()[0], "RESULT");
  }

  public void shouldProvideEmptyResultsParams() {
    Script script = new Script("script", 1, simpleSql(), params());

    assertSame(script.getResultsParams(), Param.NO_PARAMS);
  }

  public void shouldProvideNonEmptyResultsParams() {
    ParamsSet params = new ParamsSet();
    params.addOutParam(BigDecimal.class, Types.NUMERIC, null, false);
    params.initInOutUpdateParams();

    Script script = new Script("script", 1, simpleSql(), params);

    assertEquals(script.getResultsParams().length, 1);
    assertSame(script.getResultsParams()[0].getJavaType(), BigDecimal.class);
  }

  public void shouldProvideNoHints() {
    Script script = new Script("script", 1, simpleSql(), params());

    assertNull(script.getHints());
  }

  public void shouldProvideHints() {
    ParamsSet params = new ParamsSet();
    params.setQueryHint("queryTimeout", "10");
    params.setQueryHint("readOnly", "true");
    params.initInOutUpdateParams();

    Script script = new Script("script", 1, simpleSql(), params);

    assertNotNull(script.getHints());
    assertTrue(script.getHints().isReadOnly());
  }

  public void shouldProvideSimpleStatement() {
    Script script = new Script("script", 1, simpleSql(), params());

    assertEquals(script.getStatementType(), StatementType.SIMPLE);
  }

  public void shouldProvideCallStatement() {
    TypeNameParam scriptParam = Factory.stringParam("scriptParam");

    ParamsSet params = new ParamsSet();
    params.addInParam(scriptParam);
    params.addOutParam(BigDecimal.class, Types.NUMERIC, "result", false);
    params.initInOutUpdateParams();

    SqlScript sql = simpleSql(new QueryParam(ParamMode.OUT, scriptParam));

    Script script = new Script("script", 1, sql, params);

    assertEquals(script.getStatementType(), StatementType.CALL);
  }

  public void shouldProvidePreparedStatement() {
    TypeNameParam scriptParam = Factory.stringParam("scriptParam");
    ParamsSet params = params(scriptParam);
    SqlScript sql = simpleSql(new QueryParam(ParamMode.IN, scriptParam));

    Script script = new Script("script", 1, sql, params);

    assertEquals(script.getStatementType(), StatementType.PREPARED);
  }

  public void shouldProvideSimpleToString() {
    Script script = new Script("script1", 1, simpleSql(), params());

    assertEquals(script.toString(), """
        script1
        ====
        SELECT 1
        ====
        """);
  }

  public void shouldProvideFullToString() {
    TypeNameParam scriptParam = Factory.stringParam("scriptParam");

    ParamsSet params = new ParamsSet();
    params.addInParam(scriptParam);
    params.addOutParam(BigDecimal.class, Types.NUMERIC, "result", false);
    params.initInOutUpdateParams();

    SqlScript sql = simpleSql(new QueryParam(ParamMode.IN, scriptParam));

    Script script = new Script("script2", 2, sql, params);

    assertEquals(script.toString(), """
        script2
            IN(String|12 scriptParam)
            OUT(BigDecimal|2 result[results row column index: 0])
        # ResultSet {
        #  1: BigDecimal|2 result[results row column index: 0]
        # }
        ====
        # SQL statement parameters {
        #  1: IN String|12 scriptParam
        # }
        SELECT 1
        ====
        """);
  }

  public void shouldProvideFullToStringWithKeys() {
    TypeNameParam scriptParam = Factory.stringParam("scriptParam");

    ParamsSet params = new ParamsSet();
    params.addInParam(scriptParam);
    params.addOutParam(BigDecimal.class, Types.NUMERIC, "result", true);
    params.initInOutUpdateParams();

    SqlScript sql = simpleSql(new QueryParam(ParamMode.IN, scriptParam));

    Script script = new Script("script3", 3, sql, params);

    assertEquals(script.toString(), """
        script3
            IN(String|12 scriptParam)
            OUT(BigDecimal|2 result[results row column index: 0])
        # GeneratedKeys {
        #  1: result -> BigDecimal|2 result[results row column index: 0]
        # }
        ====
        # SQL statement parameters {
        #  1: IN String|12 scriptParam
        # }
        SELECT 1
        ====
        """);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp
      = "Query name is undefined")
  public void shouldNotAllowNullScriptName() {
    new Script(null, 1, null, new ParamsSet()).toString();
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp
      = "Query name is undefined")
  public void shouldNotAllowEmptyScriptName() {
    new Script("", 1, null, new ParamsSet()).toString();
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp
      = "Bad line number for query location")
  public void shouldNotAllowNonPositiveLineNumber() {
    new Script("scriptName", 0, null, new ParamsSet()).toString();
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp
      = "SQL script is undefined")
  public void shouldNotAllowNullSqlScript() {
    new Script("scriptName", 1, null, new ParamsSet()).toString();
  }

  private static ParamsSet params(TypeNameParam... inputParams) {
    ParamsSet params = new ParamsSet();
    for (TypeNameParam param : inputParams) {
      params.addInParam(param);
    }
    params.initInOutUpdateParams();
    return params;
  }

  private static SqlScript simpleSql(QueryParam... params) {
    QueryParam[] queryParams = params.length == 0 ? QueryParam.NO_PARAMS : params;
    return new SqlPart(ConditionAlways.INSTANCE, "SELECT 1", queryParams);
  }

}
