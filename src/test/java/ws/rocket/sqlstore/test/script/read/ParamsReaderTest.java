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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Types;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.script.InputParams;
import ws.rocket.sqlstore.script.OutputParams;
import ws.rocket.sqlstore.script.params.TypeNameParam;
import ws.rocket.sqlstore.script.read.ParamsReader;
import ws.rocket.sqlstore.script.read.ParamsSet;
import ws.rocket.sqlstore.script.read.StreamReader;
import ws.rocket.sqlstore.test.model.Person;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

/**
 * Tests the {@link ParamsReader} class.
 */
@Test
public class ParamsReaderTest {

  public void shouldParseInParam() throws IOException {
    ParamsSet params = new ParamsSet();
    StreamReader stream = getStream("IN(String|VARCHAR test)\n====");
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

  public void shouldParseOutParam() throws IOException {
    ParamsSet params = new ParamsSet();
    StreamReader stream = getStream("OUT(String|VARCHAR)\n====");
    ParamsReader reader = new ParamsReader(stream, params);

    reader.parseParams();

    assertEquals(params.getResultsParams().length, 1);
    assertSame(params.getResultsParams()[0].getJavaType(), String.class);
    assertNotSame(params.getOutputParams(), OutputParams.EMPTY);
    assertFalse(params.getOutputParams().isEmpty());

    assertSame(params.getInputParams(), InputParams.EMPTY);
    assertEquals(params.getKeysParams().length, 0);
    assertNull(params.getGenerateKeyColumns());
    assertNull(params.getQueryHints());
  }

  public void shouldParseUpdateParam() throws IOException {
    ParamsSet params = new ParamsSet();
    StreamReader stream = getStream("IN(ws.rocket.sqlstore.test.model.Person p) "
        + "UPDATE(KEYS(ID -> p.id))"
        + "\n====");
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

  private static StreamReader getStream(String input) throws IOException {
    return new StreamReader(new ByteArrayInputStream(input.getBytes()));
  }

}
