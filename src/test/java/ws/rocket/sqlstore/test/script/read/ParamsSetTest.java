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

package ws.rocket.sqlstore.test.script.read;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.script.InputParams;
import ws.rocket.sqlstore.script.OutputParams;
import ws.rocket.sqlstore.script.QueryHints;
import ws.rocket.sqlstore.script.QueryParam;
import ws.rocket.sqlstore.script.Script;
import ws.rocket.sqlstore.script.params.Expression;
import ws.rocket.sqlstore.script.params.Param;
import ws.rocket.sqlstore.script.params.ParamMode;
import ws.rocket.sqlstore.script.params.TypeNameParam;
import ws.rocket.sqlstore.script.read.ParamsSet;
import ws.rocket.sqlstore.script.sql.SqlScript;
import ws.rocket.sqlstore.test.db.model.Organization;
import ws.rocket.sqlstore.test.db.model.Person;
import ws.rocket.sqlstore.test.helper.Factory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Tests the {@link ParamsReader} class.
 */
@Test
public final class ParamsSetTest {

  private static final List<String> NO_FIELDS = Collections.emptyList();

  public void shouldProvideEmptyParams() {
    ParamsSet params = new ParamsSet();
    params.initInOutUpdateParams();

    assertSame(params.getInputParams(), InputParams.EMPTY);
    assertSame(params.getOutputParams(), OutputParams.EMPTY);
    assertSame(params.getResultsParams(), Param.NO_PARAMS, "Not expecting results params");
    assertNull(params.getGenerateKeyColumns(), "Not expecting generated key columns array.");
    assertSame(params.getKeysParams(), Param.NO_PARAMS, "Not expecting generated key params");
    assertNull(params.getQueryHints(), "Not expecting query hints.");
  }

  public void shouldProvideInputParam() {
    final TypeNameParam testParam = Factory.stringParam("testParam");

    ParamsSet params = new ParamsSet();
    params.addInParam(testParam);
    params.initInOutUpdateParams();

    assertSame(params.getInputParams().get("testParam"), testParam);
  }

  public void shouldProvideNamedOutputParam() {
    ParamsSet params = new ParamsSet();
    params.addOutParam(Long.class, null, "outParam", false);
    params.initInOutUpdateParams();

    assertEquals(params.getOutputParams().get("outParam").getName(), "outParam");
    assertSame(params.getResultsParams()[0].getJavaType(), Long.class, "Expecting 1 results param");
  }

  public void shouldProvideUnamedOutputParam() {
    ParamsSet params = new ParamsSet();
    params.addOutParam(String.class, null, null, true);
    params.initInOutUpdateParams();

    assertFalse(params.getOutputParams().isEmpty());
    assertSame(params.getKeysParams()[0].getJavaType(), String.class, "Expecting 1 keys param");
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "Another parameter with name 'testParam' is already defined")
  public void shouldFailOnDuplicateInputParam() {
    final TypeNameParam testParam = Factory.stringParam("testParam");
    ParamsSet params = new ParamsSet();
    params.addInParam(testParam);
    params.addInParam(testParam);
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "Another parameter with name 'outParam' is already defined")
  public void shouldFailOnDuplicateOutputParam() {
    ParamsSet params = new ParamsSet();
    params.addOutParam(Long.class, null, "outParam", false);
    params.addOutParam(Long.class, null, "outParam", false);
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "Another parameter with name 'testParam' is already defined")
  public void shouldFailOnDuplicateInOrOutputParam() {
    final TypeNameParam testParam = Factory.stringParam("testParam");

    ParamsSet params = new ParamsSet();
    params.addInParam(testParam);
    params.addOutParam(Long.class, null, "testParam", false);
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "OUT-params with names and without names cannot be mixed")
  public void shouldFailWhenMixingNamedAndUnnamedOutputParams() {
    ParamsSet params = new ParamsSet();
    params.addOutParam(Long.class, null, "outParam", false);
    params.addOutParam(String.class, null, null, false);
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "OUT-params with names and without names cannot be mixed")
  public void shouldFailWhenMixingUnnamedAndNamedOutputParams() {
    ParamsSet params = new ParamsSet();
    params.addOutParam(String.class, null, null, false);
    params.addOutParam(Long.class, null, "outParam", false);
  }

  public void shouldRegisterBeanOutputParam() {
    ParamsSet params = new ParamsSet();
    params.registerBean(Organization.class);
    params.addOutParamBeanProp("name", null, null);
    params.addOutParamBeanProp("yearFounded", null, null);
    params.addOutParamBeanProp("description", null, null);

    params.initInOutUpdateParams();

    assertFalse(params.getOutputParams().isEmpty());

    Param[] resultsParams = params.getResultsParams();
    assertEquals(resultsParams.length, 3, "Expecting 3 result params");
    assertSame(resultsParams[0].getJavaType(), String.class);
    assertSame(resultsParams[1].getJavaType(), int.class);
    assertSame(resultsParams[2].getJavaType(), String.class);
  }

  public void shouldRegisterBeanOutputKeyParam() {
    ParamsSet params = new ParamsSet();
    params.registerBean(Organization.class);
    params.addOutParamBeanProp("name", null, "NAME");
    params.addOutParamBeanProp("yearFounded", null, "YEAR_FOUNDED");
    params.addOutParamBeanProp("description", null, "DESCRIPTION");
    params.unregisterBean();

    params.initInOutUpdateParams();

    assertFalse(params.getOutputParams().isEmpty());

    Param[] resultsParams = params.getKeysParams();
    assertEquals(resultsParams.length, 3, "Expecting 3 key params");
    assertSame(resultsParams[0].getJavaType(), String.class);
    assertSame(resultsParams[1].getJavaType(), int.class);
    assertSame(resultsParams[2].getJavaType(), String.class);
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "OUT-params with names and without names cannot be mixed")
  public void shouldFailWhenMixingNamedAndBeanOutputParams() {
    ParamsSet params = new ParamsSet();
    params.addOutParam(Long.class, null, "outParam", false);
    params.registerBean(Organization.class);
    params.addOutParamBeanProp("name", null, null);
  }

  public void shouldRegisterUpdateParam() {
    ParamsSet params = new ParamsSet();
    params.addInParam(Factory.typeParam(Organization.class, "testParam"));
    params.addUpdateParam("testParam", "yearFounded", "YEAR_FOUNDED");

    Param[] keys = params.getKeysParams();

    assertEquals(keys.length, 1, "Expecting 1 keys-param");
    assertTrue(keys[0] instanceof Expression, "Expecting Expression-param.");
    assertSame(((Expression) keys[0]).getJavaType(), int.class);

    assertEquals(params.getGenerateKeyColumns(), new String[] { "YEAR_FOUNDED" });
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "There is no parameter with name 'testParam' defined on this script")
  public void shouldFailUpdateParamWhenNoSuchInParam() {
    ParamsSet params = new ParamsSet();
    params.addUpdateParam("testParam", "yearFounded", "YEAR_FOUNDED");
  }

  // SQL Script params
  public void shouldRegisterBasicScriptInParam() {
    final TypeNameParam stringParam = Factory.stringParam("testParam");

    ParamsSet params = new ParamsSet();
    params.addInParam(stringParam);
    params.initInOutUpdateParams();

    params.addScriptParam(null, "testParam", NO_FIELDS, null);

    QueryParam[] queryParams = params.resetQueryParams();
    assertEquals(queryParams.length, 1);

    QueryParam queryParam = queryParams[0];
    assertTrue(queryParam.isForInput());
    assertFalse(queryParam.isForOutput());
    assertSame(queryParam.getParam(), stringParam);
  }

  public void shouldRegisterBasicScriptOutParam() {
    ParamsSet params = new ParamsSet();
    params.addOutParam(String.class, null, "testParam", false);
    params.initInOutUpdateParams();

    params.addScriptParam(null, "testParam", NO_FIELDS, null);

    QueryParam[] queryParams = params.resetQueryParams();
    assertEquals(queryParams.length, 1);

    QueryParam queryParam = queryParams[0];
    assertFalse(queryParam.isForInput());
    assertTrue(queryParam.isForOutput());

    TypeNameParam namedParam = (TypeNameParam) queryParam.getParam();
    assertSame(namedParam.getName(), "testParam");
    assertEquals(namedParam.getJavaType(), String.class);
    assertEquals(namedParam.getSqlType(), Integer.valueOf(Types.VARCHAR));
  }

  public void shouldRegisterByPropertyScriptInParam() {
    final TypeNameParam stringParam = Factory.typeParam(Person.class, "testParam");

    ParamsSet params = new ParamsSet();
    params.addInParam(stringParam);
    params.initInOutUpdateParams();
    List<String> fields = Arrays.asList("name");

    params.addScriptParam(null, "testParam", fields, null);

    QueryParam[] queryParams = params.resetQueryParams();
    assertEquals(queryParams.length, 1);

    QueryParam queryParam = queryParams[0];
    assertTrue(queryParam.isForInput());
    assertFalse(queryParam.isForOutput());
    assertTrue(queryParam.getParam() instanceof Expression, "Expecting ref to Expression-param.");

    Expression expr = (Expression) queryParam.getParam();
    assertEquals(expr.getJavaType(), String.class);
    assertEquals(expr.getSqlType(), Integer.valueOf(Types.VARCHAR));
  }

  public void shouldRegisterByPropertyScriptOutParam() {
    ParamsSet params = new ParamsSet();
    params.addOutParam(Person.class, null, "testParam", false);
    params.initInOutUpdateParams();
    List<String> fields = Arrays.asList("name");

    params.addScriptParam(null, "testParam", fields, null);

    QueryParam[] queryParams = params.resetQueryParams();
    assertEquals(queryParams.length, 1);

    QueryParam queryParam = queryParams[0];
    assertFalse(queryParam.isForInput());
    assertTrue(queryParam.isForOutput());
    assertTrue(queryParam.getParam() instanceof Expression, "Expecting ref to Expression-param.");

    Expression expr = (Expression) queryParam.getParam();
    assertEquals(expr.getJavaType(), String.class);
    assertEquals(expr.getSqlType(), Integer.valueOf(Types.VARCHAR));
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "There is no parameter with name 'testParam' defined on this script")
  public void shouldFailNoInOutParam() {
    ParamsSet params = new ParamsSet();
    params.initInOutUpdateParams();
    params.addScriptParam(null, "testParam", NO_FIELDS, null);
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "Expression referring to parameter 'testParam' was "
      + "specified as IN-parameter but the parameter is not among IN-parameters.")
  public void shouldFailNoInParam() {
    ParamsSet params = new ParamsSet();
    params.initInOutUpdateParams();
    params.addScriptParam(ParamMode.IN, "testParam", NO_FIELDS, null);
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "Expression referring to parameter 'testParam' was "
      + "specified as OUT-parameter but the parameter is not among OUT-parameters.")
  public void shouldFailNoOutParam() {
    ParamsSet params = new ParamsSet();
    params.initInOutUpdateParams();
    params.addScriptParam(ParamMode.OUT, "testParam", NO_FIELDS, null);
  }

  // Query hints
  public void shouldHaveDefaultQueryHintsAsNull() {
    assertNull(new ParamsSet().getQueryHints());
  }

  public void shouldAcceptAllHints() throws SQLException {
    ParamsSet params = new ParamsSet();
    params.setQueryHint("maxRows", "100");
    params.setQueryHint("queryTimeout", "30");
    params.setQueryHint("fetchSize", "10");
    params.setQueryHint("maxFieldSize", "10000");
    params.setQueryHint("readOnly", "true");
    params.setQueryHint("poolable", "false");
    params.setQueryHint("escapeProcessing", "true");

    QueryHints hints = params.getQueryHints();

    assertNotNull(hints, "Expecting hints object to be present");
    assertTrue(hints.isReadOnly(), "Expecting read-only");

    Statement statement = mock(Statement.class);
    hints.setHints(statement);

    verify(statement).setMaxRows(100);
    verify(statement).setQueryTimeout(30);
    verify(statement).setFetchSize(10);
    verify(statement).setMaxFieldSize(10000);
    verify(statement).setPoolable(false);
    verify(statement).setEscapeProcessing(true);
  }

  @Test(expectedExceptions = ScriptSetupException.class,
      expectedExceptionsMessageRegExp = "Hint name is not supported\\.")
  public void shouldRejectUnknownHint() {
    new ParamsSet().setQueryHint("unknown", "");
  }

  @Test(expectedExceptions = NumberFormatException.class,
      expectedExceptionsMessageRegExp = "For input string: \"0x1\"")
  public void shouldRejectNonIntParam() {
    new ParamsSet().setQueryHint("maxRows", "0x1");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "maxRows \\(JDBC hint\\) cannot be negative\\.")
  public void shouldRejectNegativeIntParam() {
    new ParamsSet().setQueryHint("maxRows", "-1");
  }

  public void shouldDoCleanup() {
    ParamsSet params = new ParamsSet();
    params.cleanup(null);

    assertNull(params.getInputParams());
    assertNull(params.getOutputParams());
    assertNull(params.getQueryHints());
    assertSame(params.resetQueryParams(), QueryParam.NO_PARAMS);
    assertSame(params.getKeysParams(), Param.NO_PARAMS);
    assertNull(params.getGenerateKeyColumns());
    assertSame(params.getResultsParams(), Param.NO_PARAMS);
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "Script \\[testScript\\] \\(line 101\\)\\: following IN-parameters were not used\\: "
      + "\\[String\\|12 testParam\\]")
  public void shouldFailCleanupDueToUnusedInParam() {
    ParamsSet params = new ParamsSet();
    params.addInParam(Factory.stringParam("testParam"));
    params.initInOutUpdateParams();

    Script script = new Script("testScript", 101, mock(SqlScript.class), params);

    params.cleanup(script);
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "Script \\[testScript\\] \\(line 102\\)\\: following OUT-parameters were not used\\: "
      + "\\[Long outParam\\[results row column index\\: 0\\]\\]")
  public void shouldFailCleanupDueToUnusedOutParam() {
    ParamsSet params = new ParamsSet();
    params.addOutParam(Long.class, null, "outParam", false);
    params.initInOutUpdateParams();

    Script script = new Script("testScript", 102, mock(SqlScript.class), params);

    params.cleanup(script);
  }

}
