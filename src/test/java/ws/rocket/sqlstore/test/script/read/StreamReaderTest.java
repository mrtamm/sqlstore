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
import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.script.read.ParamsCategory;
import ws.rocket.sqlstore.script.read.StreamReader;
import ws.rocket.sqlstore.script.read.block.SqlBuffer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests the {@link StreamReader} class.
 */
@Test
public final class StreamReaderTest {

  private StreamReader createReader(String input) throws IOException {
    byte[] bytes = input.getBytes("UTF-8");
    return new StreamReader(new ByteArrayInputStream(bytes));
  }

  public void shouldBeEmpty() throws IOException {
    StreamReader reader = createReader("");
    assertTrue(reader.isEndOfStream());
  }

  public void shouldBeEmptyWithComment() throws IOException {
    StreamReader reader = createReader("# This comment should be immediately skipped");
    assertTrue(reader.isEndOfStream());
  }

  public void shouldBeNotEmptyWithNewLine() throws IOException {
    StreamReader reader = createReader("# This comment should be immediately skipped\n");
    assertFalse(reader.isEndOfStream());

    assertTrue(reader.skipIfNext('\n'));
    assertTrue(reader.isEndOfStream());

    assertEquals(reader.getLine(), 2, "Line number check failed");
    assertEquals(reader.getColumn(), 1, "Column number check failed");
  }

  public void shouldParseJavaTypeAliases() throws IOException {
    StreamReader reader = createReader("# This comment should be immediately skipped\n"
        + "!alias1=java.util.Date#Type1\n"
        + "!alias2=java.math.BigInteger#Type2");
    assertFalse(reader.isEndOfStream());

    reader.parseJavaTypeAliases();

    assertTrue(reader.isEndOfStream());
  }

  public void shouldParseAlphaNum() throws IOException {
    final String testValue = "314AlphaNumeRIC";

    String result = createReader(testValue).parseAlphaNum();

    assertEquals(result, testValue);
  }

  @Test(expectedExceptions = ScriptSetupException.class)
  public void shouldParseAlphaNumFail() throws IOException {
    createReader("!314AlphaNumeRIC").parseAlphaNum();
  }

  public void shouldParseName() throws IOException {
    String result = createReader("_abcName()").parseName("test");
    assertEquals(result, "_abcName");
  }

  public void shouldParseColumnNameNoWsp() throws IOException {
    StreamReader reader = createReader("mytable.column->");

    String result = reader.parseKeyColumnName();

    assertEquals(result, "mytable.column");
    assertTrue(reader.isEndOfStream(), "Expecting end of stream.");
  }

  public void shouldParseColumnNameWsp() throws IOException {
    StreamReader reader = createReader("mytable.column -> ");

    String result = reader.parseKeyColumnName();

    assertEquals(result, "mytable.column");
    assertTrue(reader.isEndOfStream(), "Expecting end of stream.");
  }

  @Test(expectedExceptions = ScriptSetupException.class)
  public void shouldParseNameFail() throws IOException {
    createReader(" 12312").parseName("test");
  }

  public void shouldParseJavaTypeByClass() throws IOException {
    Class<?> result = createReader("java.math.BigDecimal").parseJavaType();
    assertEquals(result, java.math.BigDecimal.class);
  }

  public void shouldParseJavaTypePrimitive() throws IOException {
    Class<?> result = createReader("char").parseJavaType();
    assertEquals(result, char.class);
  }

  public void shouldParseJavaTypeFromJavaLang() throws IOException {
    Class<?> result = createReader("Double").parseJavaType();
    assertEquals(result, Double.class);
  }

  public void shouldParseJavaTypeFromJavaUtil() throws IOException {
    Class<?> result = createReader("Date").parseJavaType();
    assertEquals(result, java.util.Date.class);
  }

  public void shouldParseJavaTypeFromJavaSql() throws IOException {
    Class<?> result = createReader("Time").parseJavaType();
    assertEquals(result, java.sql.Time.class);
  }

  public void shouldParseJavaTypeByAlias() throws IOException {
    StreamReader reader = createReader("!alias1=java.math.BigDecimal\nalias1");
    reader.parseJavaTypeAliases();

    assertEquals(reader.parseJavaType(), java.math.BigDecimal.class);
  }

  public void shouldParseParamsTypes() throws IOException {
    StreamReader reader = createReader("IN OUT UPDATE HINT");

    assertEquals(reader.parseParamsType(), ParamsCategory.IN);
    reader.skipNext();
    assertEquals(reader.parseParamsType(), ParamsCategory.OUT);
    reader.skipNext();
    assertEquals(reader.parseParamsType(), ParamsCategory.UPDATE);
    reader.skipNext();
    assertEquals(reader.parseParamsType(), ParamsCategory.HINT);
  }

  public void shouldParseSqlType() throws IOException {
    StreamReader reader = createReader("|DECIMAL |8000");

    assertEquals(reader.parseSqlType(), Integer.valueOf(java.sql.Types.DECIMAL));
    reader.skipNext();
    assertEquals(reader.parseSqlType(), Integer.valueOf(8000));
  }

  public void shouldParseKeysNotParamName() throws IOException {
    StreamReader reader = createReader("KEYS(");
    assertNull(reader.parseKeysOrParamName());
  }

  public void shouldParseParamNameNotKeys() throws IOException {
    StreamReader reader = createReader("keys");
    assertEquals(reader.parseKeysOrParamName(), "keys");
  }

  public void shouldParseKeysNotJavaType() throws IOException {
    StreamReader reader = createReader("KEYS(");
    assertNull(reader.parseKeysOrJavaType());
  }

  public void shouldParseJavaTypeNotKeys() throws IOException {
    StreamReader reader = createReader("Integer");
    assertEquals(reader.parseKeysOrJavaType(), Integer.class);
  }

  public void shouldParseSqlSeparatorShort() throws IOException {
    StreamReader reader = createReader("====\n");

    reader.parseSqlSeparator();

    assertTrue(reader.isEndOfStream());
  }

  public void shouldParseSqlSeparatorLong() throws IOException {
    StreamReader reader = createReader("============================\n");

    reader.parseSqlSeparator();

    assertTrue(reader.isEndOfStream());
  }

  public void shouldParseSqlSeparatorAnyChar() throws IOException {
    StreamReader reader = createReader("==== ABCDE 12345 !@#$%\n");

    reader.parseSqlSeparator();

    assertTrue(reader.isEndOfStream());
  }

  public void shouldParseSql() throws IOException {
    StreamReader reader = createReader("SELECT id, name, birthday\n FROM people \n====\n");
    SqlBuffer sql = new SqlBuffer();

    reader.parseSql(sql);
    sql.trimEnd();

    assertEquals(sql.resetSqlContent(), "SELECT id, name, birthday\n FROM people");
  }

  public void shouldParseSqlWithParams() throws IOException {
    StreamReader reader = createReader("UPDATE people SET name=?{}, birthday=?{} WHERE ID=?{} "
        + "\n====\n");
    SqlBuffer sql = new SqlBuffer();

    assertEquals(reader.parseSql(sql), '{');
    reader.requireNext('}');

    assertEquals(reader.parseSql(sql), '{');
    reader.requireNext('}');

    assertEquals(reader.parseSql(sql), '{');
    reader.requireNext('}');

    assertEquals(reader.parseSql(sql), '\n');
    assertTrue(reader.isEndOfStream(), "Expecting end of stream.");

    String sqlContent = sql.trimEnd().resetSqlContent();
    assertEquals(sqlContent, "UPDATE people SET name=?, birthday=? WHERE ID=?");
  }

  public void shouldParseHandleBraces() throws IOException {
    StreamReader reader = createReader("SELECT '\\\\a ? {} \\\\b' FROM temp \n====\n");
    SqlBuffer sql = new SqlBuffer();

    int lastChar = reader.parseSql(sql);
    String sqlContent = sql.trimEnd().resetSqlContent();

    assertEquals(lastChar, '\n');
    assertTrue(reader.isEndOfStream(), "Expecting end of stream.");
    assertEquals(sqlContent, "SELECT '\\a ? {} \\b' FROM temp");
  }

  public void shouldParseHandleEscaping() throws IOException {
    StreamReader reader = createReader("SELECT '\\\\ \\?{} \\{ \\} \\' FROM temp \n====\n");
    SqlBuffer sql = new SqlBuffer();

    int lastChar = reader.parseSql(sql);
    String sqlContent = sql.trimEnd().resetSqlContent();

    assertEquals(lastChar, '\n');
    assertTrue(reader.isEndOfStream(), "Expecting end of stream.");
    assertEquals(sqlContent, "SELECT '\\ ?{} { } \\' FROM temp");
  }

  public void shouldParseConditional() throws IOException {
    StreamReader reader = createReader("SELECT name FROM user\r\n!(condition){ WHERE id = ${}}\n");
    SqlBuffer sql = new SqlBuffer();

    int lastChar = reader.parseSql(sql);
    String sqlContent = sql.trimEnd().resetSqlContent();

    assertEquals(lastChar, '(');
    assertFalse(reader.isEndOfStream(), "Expecting not to be at the end of stream.");
    assertEquals(sqlContent, "SELECT name FROM user");
  }

  public void shouldNotParseConditional() throws IOException {
    StreamReader reader = createReader("SELECT name FROM user!(c1){a} \n !(c2){b}\n! (c3){c} "
        + "\n===========\n");
    SqlBuffer sql = new SqlBuffer();

    int lastChar = reader.parseSql(sql);
    String sqlContent = sql.trimEnd().resetSqlContent();

    assertEquals(lastChar, '\n');
    assertTrue(reader.isEndOfStream(), "Expecting end of stream.");
    assertEquals(sqlContent, "SELECT name FROM user!(c1){a} \n !(c2){b}\n! (c3){c}");
  }

}
