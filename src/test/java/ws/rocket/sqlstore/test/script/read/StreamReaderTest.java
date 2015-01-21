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
import java.io.UnsupportedEncodingException;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.script.read.ParamsCategory;
import ws.rocket.sqlstore.script.read.StreamReader;

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
    try {
      byte[] bytes = input.getBytes("UTF-8");
      return new StreamReader(new ByteArrayInputStream(bytes));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 is not supported?", e);
    }
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

  @Test(expectedExceptions = RuntimeException.class)
  public void shouldParseAlphaNumFail() throws IOException {
    createReader("!314AlphaNumeRIC").parseAlphaNum();
  }

  public void shouldParseName() throws IOException {
    String result = createReader("_abcName()").parseName("test");
    assertEquals(result, "_abcName");
  }

  @Test(expectedExceptions = RuntimeException.class)
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
    StreamReader reader = createReader("DECIMAL 8000");

    assertEquals(reader.parseSqlType(), java.sql.Types.DECIMAL);
    reader.skipNext();
    assertEquals(reader.parseSqlType(), 8000);
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

  public void shouldParseSql() throws IOException {
    StreamReader reader = createReader("SELECT id, name, birthday\n FROM people  }");
    StringBuilder sb = new StringBuilder();

    reader.parseSql(sb);

    assertEquals(sb.toString(), "SELECT id, name, birthday\n FROM people");
  }

  public void shouldParseSqlWithParams() throws IOException {
    StreamReader reader = createReader("UPDATE people SET name=${}, birthday=${} WHERE ID=${}}");
    StringBuilder sb = new StringBuilder();

    assertTrue(reader.parseSql(sb), "Expecting an expression for binding value");
    assertEquals(sb.toString(), "UPDATE people SET name=?");
    reader.requireNext('}');

    assertTrue(reader.parseSql(sb), "Expecting an expression for binding value");
    assertEquals(sb.toString(), "UPDATE people SET name=?, birthday=?");
    reader.requireNext('}');

    assertTrue(reader.parseSql(sb), "Expecting an expression for binding value");
    assertEquals(sb.toString(), "UPDATE people SET name=?, birthday=? WHERE ID=?");
    reader.requireNext('}');

    assertFalse(reader.parseSql(sb), "Expecting the end of the SQL script.");
    assertTrue(reader.isEndOfStream(), "Expecting end of stream.");
  }

  public void shouldParseHandleBraces() throws IOException {
    StreamReader reader = createReader("SELECT '\\\\a $ {} \\\\b' FROM temp}");
    StringBuilder sb = new StringBuilder();

    assertFalse(reader.parseSql(sb), "Expecting the end of the SQL script.");
    assertEquals(sb.toString(), "SELECT '\\a $ {} \\b' FROM temp");
    assertTrue(reader.isEndOfStream(), "Expecting end of stream.");
  }

  public void shouldParseHandleEscaping() throws IOException {
    StreamReader reader = createReader("SELECT '\\\\ \\${} \\{ \\} \\' FROM temp}");
    StringBuilder sb = new StringBuilder();

    assertFalse(reader.parseSql(sb), "Expecting the end of the SQL script.");
    assertEquals(sb.toString(), "SELECT '\\ ${} { } \\' FROM temp");
    assertTrue(reader.isEndOfStream(), "Expecting end of stream.");
  }

}
