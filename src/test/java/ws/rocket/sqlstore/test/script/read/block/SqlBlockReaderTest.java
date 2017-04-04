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

package ws.rocket.sqlstore.test.script.read.block;

import java.io.IOException;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.script.read.ParamsSet;
import ws.rocket.sqlstore.script.read.StreamReader;
import ws.rocket.sqlstore.script.read.block.SqlBlockReader;
import ws.rocket.sqlstore.script.sql.SqlPart;
import ws.rocket.sqlstore.script.sql.SqlParts;
import ws.rocket.sqlstore.script.sql.SqlScript;
import ws.rocket.sqlstore.test.helper.Factory;
import ws.rocket.sqlstore.test.helper.ScriptBuilder;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests the {@link SqlBlockReader} class.
 */
@Test
public final class SqlBlockReaderTest {

  private static ParamsSet emptyParams() {
    return new ScriptBuilder().initParams();
  }

  private static void assertStreamPosition(StreamReader stream, int line, int column) {
    assertEquals(stream.getLine(), line, "Unexpected line in StreamReader.");
    assertEquals(stream.getColumn(), column, "Unexpected column in StreamReader.");
  }

  public void shouldParseSimpleSql() throws IOException {
    StreamReader text = Factory.streamOf("====\n SELECT 1\n====\n");

    SqlScript script = new SqlBlockReader(text, emptyParams()).parseSql();

    assertNotNull(script, "Expecting an SqlScript instance to be returned.");
    assertTrue(script instanceof SqlPart, "Expecting an SqlPart object type.");
    assertStreamPosition(text, 4, 1);
  }

  public void shouldParseConditionalSqlWithExpression() throws IOException {
    ParamsSet params = new ScriptBuilder().addInStringParam("paramName").initParams();
    StreamReader text = Factory.streamOf(
        "====\n"
        + "SELECT sample\n"
        + "FROM test_data\n"
        + "!(paramName){\n"
        + "WHERE paramCode = ?{paramName}\n"
        + "}\n"
        + "====\n");

    SqlScript script = new SqlBlockReader(text, params).parseSql();

    assertNotNull(script, "Expecting an SqlScript instance to be returned.");
    assertTrue(script instanceof SqlParts, "Expecting an SqlParts object type.");
    assertStreamPosition(text, 8, 1);
  }

  @Test(expectedExceptions = ScriptSetupException.class,
      expectedExceptionsMessageRegExp = "Empty script block ending right before line 3\\.")
  public void shouldFailDueToEmptyBlock() throws IOException {
    StreamReader text = Factory.streamOf("====\n====\n");
    text.skipWsp();

    new SqlBlockReader(text, emptyParams()).parseSql();
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "Expected script block separator \\(====\\) to begin on first column; "
      + "on line 1 and column 2\\.")
  public void shouldFailDueToWhitespaceBeforeStartingMarker() throws IOException {
    StreamReader text = Factory.streamOf(" ====\nSELECT 1\n====\n");
    text.skipWsp();

    new SqlBlockReader(text, emptyParams()).parseSql();
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "Expected '=' but got '\n' on line 1 and column 4.")
  public void shouldFailDueToInvalidStartingMarker() throws IOException {
    StreamReader text = Factory.streamOf("===\nSELECT 1\n====\n");

    new SqlBlockReader(text, emptyParams()).parseSql();
  }

}
