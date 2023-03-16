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

package ws.rocket.sqlstore.script.read.block;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.sql.Types;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.helper.Factory;
import ws.rocket.sqlstore.script.QueryParam;
import ws.rocket.sqlstore.script.params.TypeNameParam;
import ws.rocket.sqlstore.script.read.ParamsSet;

/**
 * Tests the {@link ExpressionReader} class.
 */
@Test
public final class ExpressionReaderTest {

  private static QueryParam parseExpression(String stream, ParamsSet params) throws IOException {
    params.initInOutUpdateParams();

    new ExpressionReader(Factory.streamOf(stream))
        .parseExpression()
        .register(params);

    QueryParam[] expressions = params.resetQueryParams();
    assertNotNull(expressions, "Expected QueryParam[] array.");
    assertEquals(expressions.length, 1, "Expecting exactly one expression in the array.");
    return expressions[0];
  }

  private static ParamsSet inParams(TypeNameParam inParam) {
    ParamsSet params = new ParamsSet();
    params.addInParam(inParam);
    params.initInOutUpdateParams();
    return params;
  }

  private static ParamsSet outParam() {
    ParamsSet params = new ParamsSet();
    params.addOutParam(Long.class, Types.BIGINT, "outParamName", false);
    params.initInOutUpdateParams();
    return params;
  }

  public void shouldSucceedWithSimpleParamExpression() throws IOException {
    TypeNameParam inParam = Factory.stringParam("paramName");
    QueryParam expression = parseExpression("paramName}", inParams(inParam));

    assertTrue(expression.isForInput(), "Expecting expression to be used as input to SQL-script.");
    assertSame(expression.getParam(), inParam, "Expecting it to refer to the script IN-parameter.");
  }

  public void shouldSucceedWithTypeParamExpression() throws IOException {
    TypeNameParam inParam = Factory.stringParam("paramName");
    QueryParam expression = parseExpression("paramName|DECIMAL}", inParams(inParam));

    assertTrue(expression.isForInput(), "Expecting expression to be used as input to SQL-script.");

    TypeNameParam exprParam = (TypeNameParam) expression.getParam();
    assertNotSame(exprParam, inParam, "Expecting a new parameter.");
    assertSame(exprParam.getName(), inParam.getName());
    assertEquals(exprParam.getSqlType().intValue(), Types.DECIMAL);
  }

  public void shouldSucceedWithSimpleInParamExpression() throws IOException {
    TypeNameParam inParam = Factory.stringParam("paramName");
    QueryParam expression = parseExpression("IN(paramName)}", inParams(inParam));

    assertTrue(expression.isForInput(), "Expecting expression to be used as input to SQL-script.");
    assertSame(expression.getParam(), inParam, "Expecting it to refer to the script IN-parameter.");
  }

  public void shouldSucceedWithSimpleInOutParamExpression() throws IOException {
    TypeNameParam inParam = Factory.stringParam("paramName");
    QueryParam expression = parseExpression("INOUT(paramName)}", inParams(inParam));

    assertTrue(expression.isForInput(), "Expecting expression to be used as input to SQL-script.");
    assertSame(expression.getParam(), inParam, "Expecting it to refer to the script IN-parameter.");
  }

  public void shouldSucceedWithSimpleOutParamExpression() throws IOException {
    QueryParam expression = parseExpression("OUT(outParamName)}", outParam());

    assertTrue(expression.isForOutput(), "Expected expression to be used as output to SQL-script.");
    TypeNameParam exprParam = (TypeNameParam) expression.getParam();
    assertEquals(exprParam.getName(), "outParamName");
    assertEquals(exprParam.getJavaType(), Long.class);
    assertEquals(exprParam.getSqlType().intValue(), Types.BIGINT);
  }

}
