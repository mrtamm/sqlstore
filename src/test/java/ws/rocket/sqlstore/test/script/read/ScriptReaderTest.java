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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.script.QueryParam;
import ws.rocket.sqlstore.script.Script;
import ws.rocket.sqlstore.script.read.ScriptReader;
import ws.rocket.sqlstore.test.script.read.model.InvalidTestModel;
import ws.rocket.sqlstore.test.script.read.model.ValidTestModel;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static ws.rocket.sqlstore.test.helper.Factory.inputStreamOf;

/**
 * Tests the {@link ScriptReader} class.
 */
@Test
public final class ScriptReaderTest {

  public void shouldHaveMoreContentToRead() throws IOException {
    ScriptReader reader = new ScriptReader(inputStreamOf(" ."));
    assertTrue(reader.hasMore());
  }

  public void shouldNotHaveMoreContentToRead() throws IOException {
    ScriptReader reader = new ScriptReader(inputStreamOf(" "));
    assertFalse(reader.hasMore());
  }

  public void shouldParseJavaAlias() throws IOException {
    ScriptReader reader = new ScriptReader(inputStreamOf("!Alias=java.util.List"));
    assertTrue(reader.hasMore());

    reader.parseJavaTypeAliases();

    assertFalse(reader.hasMore());
  }

  public void shouldNotParseJavaAlias() throws IOException {
    ScriptReader reader = new ScriptReader(inputStreamOf("Alias=java.util.List"));
    assertTrue(reader.hasMore());

    reader.parseJavaTypeAliases();

    assertTrue(reader.hasMore());
  }

  public void shouldParseScriptName() throws IOException {
    ScriptReader reader = new ScriptReader(inputStreamOf("scriptName\n"));
    assertTrue(reader.hasMore());

    reader.parseName();

    assertFalse(reader.hasMore());
  }

  public void shouldParseParams() throws IOException {
    InputStream stream = inputStreamOf("IN(Long id) OUT(String) HINT(readOnly=true)\n=");
    ScriptReader reader = new ScriptReader(stream);
    assertTrue(reader.hasMore());

    reader.parseParams();

    assertTrue(reader.hasMore()); // Reader will stop at (before) the last character.
  }

  public void shouldParseSqlScript() throws IOException {
    InputStream stream = inputStreamOf("====\nSELECT COUNT(*) FROM person \n====\n");
    ScriptReader reader = new ScriptReader(stream);
    assertTrue(reader.hasMore());

    reader.parseSql();

    assertFalse(reader.hasMore());
  }

  public void shouldCreateScript() throws IOException {
    InputStream stream = inputStreamOf("testScript\n"
        + "====\n"
        + "   SELECT COUNT(*) FROM person   \n"
        + "====\n");

    ScriptReader reader = new ScriptReader(stream);
    assertTrue(reader.hasMore());

    reader.parseName();
    reader.parseParams();
    reader.parseSql();
    Script script = reader.createScript();

    assertFalse(reader.hasMore());
    reader.close();

    assertEquals(script.getName(), "testScript");
    assertEquals(script.getLine(), 1);

    ArrayList<QueryParam> params = new ArrayList<>();
    String sql = script.getSqlAndParams(null, params);

    assertEquals(sql, "SELECT COUNT(*) FROM person");
    assertTrue(params.isEmpty(), "Expecting no SQL parameters");
    reader.close();
  }

  public void shouldLoadScriptsFromFile() throws IOException {
    Map<String, Script> result = ScriptReader.load(ValidTestModel.class);
    assertEquals(result.size(), 1);
    assertNotNull(result.get("testScript"), "Expecting a loaded test-script");
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "The SQL script name testScript is defined more than once in InvalidTestModel.sqls\\: 8")
  public void shouldFailToLoadScriptsFromFile() throws IOException {
    ScriptReader.load(InvalidTestModel.class);
  }

}
