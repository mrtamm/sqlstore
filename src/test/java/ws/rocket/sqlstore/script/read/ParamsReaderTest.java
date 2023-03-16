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

package ws.rocket.sqlstore.script.read;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.sql.Types;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.db.model.Person;
import ws.rocket.sqlstore.helper.Factory;
import ws.rocket.sqlstore.script.InputParams;
import ws.rocket.sqlstore.script.OutputParams;
import ws.rocket.sqlstore.script.params.TypeNameParam;

/**
 * Tests the {@link ParamsReader} class.
 */
@Test
public final class ParamsReaderTest {

  public void shouldParseInParam() throws IOException {
    StreamReader stream = Factory.streamOf("""
        IN(String|VARCHAR test)
        ====""");
    ParamsSet params = new ParamsSet();
    ParamsReader reader = new ParamsReader(stream, params);

    reader.parseParams();

    assertNotSame(params.getInputParams(), InputParams.EMPTY);
    TypeNameParam param = params.getInputParams().get("test");
    assertNotNull(param);
    assertSame(param.getJavaType(), String.class);
    assertEquals(param.getSqlType(), Integer.valueOf(Types.VARCHAR));

    assertEquals(params.getKeysParams().length, 0);
    assertEquals(params.getResultsParams().length, 0);
    assertSame(params.getOutputParams(), OutputParams.EMPTY);
    assertNull(params.getGenerateKeyColumns());
    assertNull(params.getQueryHints());
  }

  public void shouldParseInParams() throws IOException {
    StreamReader stream = Factory.streamOf("""
        IN(String|VARCHAR test, Long num)
        ====""");
    ParamsSet params = new ParamsSet();
    ParamsReader reader = new ParamsReader(stream, params);

    reader.parseParams();

    assertNotSame(params.getInputParams(), InputParams.EMPTY);

    TypeNameParam param = params.getInputParams().get("test");
    assertNotNull(param, "Expecting first parameter 'test'");
    assertSame(param.getJavaType(), String.class);
    assertEquals(param.getSqlType(), Integer.valueOf(Types.VARCHAR));

    param = params.getInputParams().get("num");
    assertNotNull(param, "Expecting second parameter 'num'");
    assertSame(param.getJavaType(), Long.class);
    assertNull(param.getSqlType());

    assertEquals(params.getKeysParams().length, 0);
    assertEquals(params.getResultsParams().length, 0);
    assertSame(params.getOutputParams(), OutputParams.EMPTY);
    assertNull(params.getGenerateKeyColumns());
    assertNull(params.getQueryHints());
  }

  public void shouldParseOutParam() throws IOException {
    StreamReader stream = Factory.streamOf("""
        OUT(String|VARCHAR, Integer)
        ====""");
    ParamsSet params = new ParamsSet();
    ParamsReader reader = new ParamsReader(stream, params);

    reader.parseParams();

    assertEquals(params.getResultsParams().length, 2);
    assertSame(params.getResultsParams()[0].getJavaType(), String.class);
    assertSame(params.getResultsParams()[1].getJavaType(), Integer.class);

    assertNotSame(params.getOutputParams(), OutputParams.EMPTY);
    assertFalse(params.getOutputParams().isEmpty());

    assertSame(params.getInputParams(), InputParams.EMPTY);
    assertEquals(params.getKeysParams().length, 0);
    assertNull(params.getGenerateKeyColumns());
    assertNull(params.getQueryHints());
  }

  public void shouldParseNamedOutParam() throws IOException {
    StreamReader stream = Factory.streamOf("""
        OUT(String|VARCHAR param1, Integer param2)
        ====""");
    ParamsSet params = new ParamsSet();
    ParamsReader reader = new ParamsReader(stream, params);

    reader.parseParams();

    assertEquals(params.getResultsParams().length, 2);
    assertSame(params.getResultsParams()[0].getJavaType(), String.class);
    assertSame(params.getResultsParams()[1].getJavaType(), Integer.class);

    assertTrue(params.getResultsParams()[0] instanceof TypeNameParam);
    assertTrue(params.getResultsParams()[1] instanceof TypeNameParam);

    assertNotSame(params.getOutputParams(), OutputParams.EMPTY);
    assertFalse(params.getOutputParams().isEmpty());

    assertSame(params.getInputParams(), InputParams.EMPTY);
    assertEquals(params.getKeysParams().length, 0);
    assertNull(params.getGenerateKeyColumns());
    assertNull(params.getQueryHints());
  }

  public void shouldParseOutBeanParam() throws IOException {
    StreamReader stream = Factory.streamOf("""
        OUT(ws.rocket.sqlstore.db.model.Person[id,name])
        ====""");
    ParamsSet params = new ParamsSet();
    ParamsReader reader = new ParamsReader(stream, params);

    reader.parseParams();

    assertEquals(params.getResultsParams().length, 2);
    assertSame(params.getResultsParams()[0].getJavaType(), Long.class);
    assertSame(params.getResultsParams()[1].getJavaType(), String.class);

    assertNotSame(params.getOutputParams(), OutputParams.EMPTY);
    assertFalse(params.getOutputParams().isEmpty());

    assertSame(params.getInputParams(), InputParams.EMPTY);
    assertEquals(params.getKeysParams().length, 0);
    assertNull(params.getGenerateKeyColumns());
    assertNull(params.getQueryHints());
  }

  public void shouldParseKeysOutParam() throws IOException {
    StreamReader stream = Factory.streamOf("""
        OUT(KEYS(COL1 -> String|VARCHAR, COL2 -> Integer))
        ====""");
    ParamsSet params = new ParamsSet();
    ParamsReader reader = new ParamsReader(stream, params);

    reader.parseParams();

    assertEquals(params.getKeysParams().length, 2);
    assertSame(params.getKeysParams()[0].getJavaType(), String.class);
    assertSame(params.getKeysParams()[1].getJavaType(), Integer.class);

    assertEquals(params.getGenerateKeyColumns().length, 2);
    assertEquals(params.getGenerateKeyColumns()[0], "COL1");
    assertEquals(params.getGenerateKeyColumns()[1], "COL2");

    assertNotSame(params.getOutputParams(), OutputParams.EMPTY);
    assertFalse(params.getOutputParams().isEmpty());

    assertSame(params.getInputParams(), InputParams.EMPTY);
    assertEquals(params.getResultsParams().length, 0);
    assertNull(params.getQueryHints());
  }

  public void shouldParseKeysOutBeanParam() throws IOException {
    StreamReader stream = Factory.streamOf("""
        OUT(KEYS(ws.rocket.sqlstore.db.model.Person[COL1 -> id, COL2 -> name]) )
        ====""");
    ParamsSet params = new ParamsSet();
    ParamsReader reader = new ParamsReader(stream, params);

    reader.parseParams();

    assertEquals(params.getKeysParams().length, 2);
    assertSame(params.getKeysParams()[0].getJavaType(), Long.class);
    assertSame(params.getKeysParams()[1].getJavaType(), String.class);

    assertEquals(params.getGenerateKeyColumns().length, 2);
    assertEquals(params.getGenerateKeyColumns()[0], "COL1");
    assertEquals(params.getGenerateKeyColumns()[1], "COL2");

    assertNotSame(params.getOutputParams(), OutputParams.EMPTY);
    assertFalse(params.getOutputParams().isEmpty());

    assertSame(params.getInputParams(), InputParams.EMPTY);
    assertEquals(params.getResultsParams().length, 0);
    assertNull(params.getQueryHints());
  }

  public void shouldParseUpdateParam() throws IOException {
    StreamReader stream = Factory.streamOf("""
        IN(ws.rocket.sqlstore.db.model.Person p) UPDATE(KEYS(ID -> p.id))
        ====""");
    ParamsSet params = new ParamsSet();
    ParamsReader reader = new ParamsReader(stream, params);

    reader.parseParams();

    assertNotSame(params.getInputParams(), InputParams.EMPTY);
    TypeNameParam param = params.getInputParams().get("p");
    assertNotNull(param);
    assertSame(param.getJavaType(), Person.class);
    assertNull(param.getSqlType());

    assertEquals(params.getGenerateKeyColumns().length, 1);
    assertEquals(params.getGenerateKeyColumns()[0], "ID");

    assertEquals(params.getKeysParams().length, 1);
    assertSame(params.getKeysParams()[0].getJavaType(), Long.class);

    assertSame(params.getOutputParams(), OutputParams.EMPTY);
    assertEquals(params.getResultsParams().length, 0);
    assertNull(params.getQueryHints());
  }

  public void shouldParseHintParam() throws IOException {
    StreamReader stream = Factory.streamOf("HINT(maxRows=3, queryTimeout=60, fetchSize=4, "
        + "maxFieldSize=1000, readOnly=true, poolable=false, escapeProcessing=false)"
        + "\n====");
    ParamsSet params = new ParamsSet();
    ParamsReader reader = new ParamsReader(stream, params);

    reader.parseParams();

    assertNotNull(params.getQueryHints());
  }

  @Test(expectedExceptions = ScriptSetupException.class,
      expectedExceptionsMessageRegExp = "Duplicate parameters category on line 1 and column 13\\.")
  public void shouldFailOnDuplicateParamsCategory() throws IOException {
    StreamReader stream = Factory.streamOf("IN(Long id) IN(");
    new ParamsReader(stream, new ParamsSet()).parseParams();
  }

}
