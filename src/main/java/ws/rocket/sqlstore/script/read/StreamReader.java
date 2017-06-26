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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.script.BeanUtil;
import ws.rocket.sqlstore.script.read.block.SqlBuffer;

/**
 * A reader that provides granular API for reading SqlStore configuration file. To simplify usage,
 * there is also a more high-level API.
 *
 * @see ScriptReader
 */
public final class StreamReader implements Closeable {

  private final StringBuilder buffer = new StringBuilder();

  private final Map<String, Integer> sqlTypes = new HashMap<>();

  private final Map<String, Class<?>> javaTypes = new HashMap<>();

  private final InputStream input;

  private int line = 1;

  private int column;

  private int nextChar;

  private int escapedChar = -1;

  /**
   * Creates a new reader for reading the given input stream. This reader assumes that the stream
   * object is not null and is configured correctly to handle character encoding.
   *
   * @param input The text stream to parse.
   * @throws IOException When a stream-related exception occurs during initial reading.
   */
  public StreamReader(InputStream input) throws IOException {
    this.input = input;
    moveNext();
    loadSqlTypes();
  }

  private int readNext() throws IOException {
    if (this.nextChar == '\n') {
      this.line++;
      this.column = 0;
    }

    this.column++;

    int c = this.input.read();
    return c == '\r' ? '\n' : c;
  }

  private int moveUntilEndOfLine() throws IOException {
    while (this.nextChar != -1 && this.nextChar != '\n') {
      this.nextChar = readNext();
    }

    return this.nextChar;
  }

  private int moveNext() throws IOException {
    while (true) {
      if (this.escapedChar != -1) {
        this.nextChar = this.escapedChar;
        this.escapedChar = -1;
        this.column++;
        break;
      }

      this.nextChar = readNext();

      if (this.nextChar == '#') {
        this.nextChar = moveUntilEndOfLine();

      } else if (this.nextChar == '\\') {
        this.escapedChar = this.input.read();

        if (this.escapedChar == '#') {
          continue;
        } else if (this.escapedChar == -1) {
          this.nextChar = -1;
        }
      }

      break;
    }

    return this.nextChar;
  }

  /**
   * Closes the underlying stream.
   *
   * @throws IOException When a stream-related exception occurs during closing.
   */
  @Override
  public void close() throws IOException {
    this.buffer.setLength(0);
    this.nextChar = -1;
    this.javaTypes.clear();
    this.sqlTypes.clear();
    this.input.close();
  }

  /**
   * Parses and remembers Java type alias declarations. This method must be called at the point
   * where these alias declarations are expected, however, preceding whitespace and comments will be
   * skipped. If the following character is not '!' (exclamation mark), alias declarations will be
   * skipped.
   * <p>
   * Alias declarations are expected to look like following:
   * <pre>!aliasName=java.type.Name
   * !another=java.type.Another
   * !lastOne=java.type.LastOne
   * </pre>
   * <p>
   * When later a Java type is expected but alias name (without exclamation mark) is encountered,
   * this reader will return the type associated with the name.
   * <p>
   * Alias name must be a valid Java identifier, and all aliases must be unique within the scripts
   * file.
   *
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public void parseJavaTypeAliases() throws IOException {
    while (skipWsp() == '!') {
      skipNext();
      String typeAlias = parseName("Java type alias");
      if (this.javaTypes.containsKey(typeAlias)) {
        fail("The Java type alias is already in use!");
      }
      requireNext('=');
      this.javaTypes.put(typeAlias, BeanUtil.getClass(parseClassName()));
    }
  }

  /**
   * Informs whether the next unprocessed symbol has given character code. This method does not mark
   * the symbol processed.
   *
   * @param cp The character code to test against.
   * @return A Boolean that is true when when the character codes match.
   */
  public boolean isNext(int cp) {
    return this.nextChar == cp;
  }

  /**
   * Checks that next unprocessed character has given character code. When the check fails, this
   * method will throw a runtime exception. When the check succeeds, this method moves this reader
   * to the next character.
   *
   * @param cp The character code to test against.
   * @return The code of the next character.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public int requireNext(int cp) throws IOException {
    if (this.nextChar != cp) {
      fail("Expected " + toString(cp) + " but got " + toString(this.nextChar));
    }
    return moveNext();
  }

  /**
   * Skips the current unprocessed character and moves this reader to the next character.
   *
   * @return The character code of the next character.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public int skipNext() throws IOException {
    return moveNext();
  }

  /**
   * Skips the current unprocessed character only if it matches the provided character code, and
   * moves this reader to the next character.
   *
   * @param cp The character code to test against.
   * @return The character code of the next character.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public boolean skipIfNext(int cp) throws IOException {
    boolean next = isNext(cp);
    if (next) {
      moveNext();
    }
    return next;
  }

  /**
   * The line number of the current unprocessed character. Counting lines starts with one.
   *
   * @return The current line number.
   */
  public int getLine() {
    return this.line;
  }

  /**
   * The column number (on a line) of the current unprocessed character. Counting columns starts
   * with one (zero is the initial value when stream or row parsing has not started).
   *
   * @return The current column number, 1 or greater integer.
   */
  public int getColumn() {
    return this.column;
  }

  /**
   * Informs whether this reader has reached the end of stream. End of stream can also be checked by
   * comparing the character code of the current character to -1.
   *
   * @return A Boolean that is true when this reader has reached the end of stream.
   */
  public boolean isEndOfStream() {
    return this.nextChar == -1;
  }

  /**
   * Skips all white-space characters until a non-white-space character or end-of-stream is met.
   *
   * @return The character code of the next (non-white-space) character.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public int skipWsp() throws IOException {
    int cp = this.nextChar;
    while (cp != -1 && Character.isWhitespace(cp)) {
      cp = moveNext();
    }
    return cp;
  }

  /**
   * Requires an opening parenthesis character and skips any whitespace following it.
   *
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public void parenthesisOpen() throws IOException {
    if (Character.isWhitespace(requireNext('('))) {
      skipWsp();
    }
  }

  /**
   * Parses a name, which must follow Java identifier (name) rules. The parsing will begin from
   * current position and ends with first non-Java-identifier character or with end-of stream. This
   * method will throw a runtime exception when the current character is not a Java-identifier
   * character.
   *
   * @param description Information for error message about what is being parsed, e.g. "query name".
   * @return The parsed name.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public String parseName(String description) throws IOException {
    this.buffer.setLength(0);
    int cp = this.nextChar;

    while (cp != -1 && isJavaIdentifier(cp)) {
      this.buffer.appendCodePoint(cp);
      cp = moveNext();
    }

    if (this.buffer.length() == 0) {
      fail("Expected " + description + " name (a valid Java identifier)");
    }

    return this.buffer.toString();
  }

  /**
   * Parses an alphanumeric value. The parsing will begin from current position and ends with end-of
   * stream or with first character that is neither a digit nor an alphabetic character. This method
   * will throw a runtime exception when the current character is not an alphabetic nor numeric
   * character.
   *
   * @return The parsed alphanumeric value.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public String parseAlphaNum() throws IOException {
    this.buffer.setLength(0);
    int cp = this.nextChar;

    while (cp != -1 && ((cp >= '0' && cp <= '9') || Character.isAlphabetic(cp))) {
      this.buffer.appendCodePoint(cp);
      cp = moveNext();
    }

    if (this.buffer.length() == 0) {
      fail("Expected an alpha-numeric value");
    }

    return this.buffer.toString();
  }

  /**
   * Parses a parameters category name and returns the corresponding enum value. This method will
   * raise an exception when the category name is not correct (case-sensitive, i.e. name must be in
   * upper-case).
   *
   * @return The parse parameters category name.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public ParamsCategory parseParamsType() throws IOException {
    this.buffer.setLength(0);
    int cp = this.nextChar;

    while (cp != -1 && Character.isAlphabetic(cp) && Character.isUpperCase(cp)) {
      this.buffer.appendCodePoint(cp);
      cp = moveNext();
    }

    ParamsCategory result = null;
    if (this.buffer.length() > 0) {
      result = ParamsCategory.get(this.buffer.toString());
    }

    if (result == null) {
      fail("Expected keyword: either IN, OUT, or UPDATE.");
    }

    return result;
  }

  /**
   * Parses a Java type starting from current position. Allowed characters include all Java
   * identifier characters, dot, and dollar sign. This method will throw a runtime exception when
   * the current character is not a Java-identifier character.
   *
   * @return The parsed type.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public Class<?> parseJavaType() throws IOException {
    return resolveJavaType(parseClassName());
  }

  /**
   * Parses a parameter name and returns it. This method will fail when the name cannot be parsed.
   *
   * @return The parsed parameter name.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public String parseParamName() throws IOException {
    return parseName("parameter");
  }

  /**
   * Parses a property name of a parameter and returns it. This method will fail when the name
   * cannot be parsed.
   *
   * @return The parsed property name.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public String parseParamPropName() throws IOException {
    return parseName("parameter property name");
  }

  /**
   * Parses and returns a column name that is used for retrieving the value of the generated key.
   * The expected expression is "COLUMN_NAME -&amp; " (case-insensitive).
   *
   * @return The parsed column name.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public String parseKeyColumnName() throws IOException {
    String keysColName = parseName("key column name");
    while (skipIfNext('.')) {
      keysColName += "." + parseName("key column name");
    }

    skipWsp();
    requireNext('-');
    if (Character.isWhitespace(requireNext('>'))) {
      skipWsp();
    }

    return keysColName;
  }

  /**
   * Parses the <code>KEYS</code> identifier or a Java type, depending which is next.
   * <p>
   * When the next token is "KEYS", this method also moves the reading position right after the
   * identifier, an opening parenthesis, and optional whitespace. The result will be
   * <code>null</code>.
   * <p>
   * When the next token is a Java type, this method will also determine the type class. The result
   * will be the resolved class. The method will fail when the class cannot be determined.
   *
   * @return The resolved Java type or <code>null</code> (in case of KEYS).
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public Class<?> parseKeysOrJavaType() throws IOException {
    String name = parseClassName();
    Class<?> result = null;

    if (isKeys(name)) {
      parenthesisOpen();
    } else {
      result = resolveJavaType(name);
    }

    return result;
  }

  /**
   * Parses the key-column name or a Java type, depending which is next. This is used in a special
   * case of <code>UPDATE(KEYS(...))</code> parsing where it allows either key-column name or a bean
   * type to be specified.
   *
   * @return The resolve Java type class or the string containing key-column name.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public Object parseKeyColumnNameOrJavaType() throws IOException {
    String name = parseClassName();

    if (isNext('[')) {
      return resolveJavaType(name);
    }

    skipWsp();
    requireNext('-');
    if (Character.isWhitespace(requireNext('>'))) {
      skipWsp();
    }
    return name;
  }

  /**
   * Parses the <code>KEYS</code> identifier or a parameter name, depending which is next.
   * <p>
   * When the next token is "KEYS", this method also moves the reading position right after the
   * identifier, an opening parenthesis, and optional whitespace. The result will be
   * <code>null</code>.
   *
   * @return The parsed parameter name or <code>null</code>.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public String parseKeysOrParamName() throws IOException {
    String name = parseName("'KEYS' or parameter name");

    if (isKeys(name)) {
      name = null;
      parenthesisOpen();
    }

    return name;
  }

  /**
   * Parses an SQL type starting from current position. Allowed characters include all letters (but
   * not other symbols and signs). This method will throw a runtime exception when the current
   * character is not a letter and when there is no corresponding constant in
   * <code>java.sql.Types</code> (not case-sensitive matching).
   *
   * @return The parsed SQL type as a <code>java.sql.Types</code> constant.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public Integer parseSqlType() throws IOException {
    if (!skipIfNext('|')) {
      return null;
    }

    this.buffer.setLength(0);
    int cp = this.nextChar;

    while (cp != -1 && Character.isLetterOrDigit(cp)) {
      this.buffer.appendCodePoint(cp);
      cp = moveNext();
    }

    String sqlType = this.buffer.toString();
    Integer result;

    if (this.sqlTypes.containsKey(sqlType)) {
      result = this.sqlTypes.get(sqlType);
    } else {
      try {
        result = Integer.valueOf(sqlType);
      } catch (NumberFormatException e) {
        result = null;
      }
    }

    if (result == null) {
      fail("Expected an SQL type (java.sql.Types constant name or positive integer) of a variable");
    }

    return result;
  }

  /**
   * Parses the script block separator. It must begin on first column of the row and at least four
   * consecutive equal-signs (====), and the rest of the symbols will be ignored until the end of
   * line.
   *
   * @return The first character (on the first column) on the next line.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public int parseSqlSeparator() throws IOException {
    if (this.column != 1) {
      fail("Expected script block separator (====) to begin on first column;");
    }

    for (int i = 0; i < 4; i++) {
      requireNext('=');
    }

    int cp = moveUntilEndOfLine();
    return cp != -1 ? moveNext() : -1;
  }

  /**
   * Parses an SQL query starting from current position. The query must be wrapped by curly braces.
   * This method does not check for allowed characters. However, this method trims white-space
   * surrounding lines and joins subsequent lines with a single space character. This method will
   * throw a runtime exception when the current character is not an opening curly brace letter, the
   * closing curly brace is not reached, or the contained SQL query is empty.
   *
   * @param sql The buffer where the parsed SQL must be stored.
   * @return The last code-point ("{" = parameter; "}" = end of script (part); "(" = condition).
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public int parseSql(SqlBuffer sql) throws IOException {
    int cp = this.nextChar;

    while (sql.next(cp, this.line, this.column)) {
      cp = moveNext();

      if (cp == -1) {
        fail("Unexpected end of SQL statement block.");
      }
    }

    moveNext();
    return cp;
  }

  private boolean isJavaIdentifier(int cp) {
    boolean result;
    if (this.buffer.length() == 0) {
      result = Character.isJavaIdentifierStart(cp);
    } else {
      result = Character.isJavaIdentifierPart(cp);
    }
    return result;
  }

  private void fail(String reason) {
    throw new ScriptSetupException("%s on line %d and column %d.", reason, this.line, this.column);
  }

  private String parseClassName() throws IOException {
    this.buffer.setLength(0);
    int cp = this.nextChar;

    while (cp != -1 && (cp == '.' || cp == '$' || isJavaIdentifier(cp))) {
      this.buffer.appendCodePoint(cp);
      cp = moveNext();
    }

    if (this.buffer.length() == 0) {
      fail("Expected a full name of a Java class.");
    }

    return this.buffer.toString();
  }

  private Class<?> resolveJavaType(String javaType) {
    Class<?> result = this.javaTypes.get(javaType);

    if (result == null) {
      if (Character.isLowerCase(javaType.charAt(0))) {
        result = BeanUtil.getPrimitiveClass(javaType);

      } else if (Character.isUpperCase(javaType.charAt(0))) {
        result = BeanUtil.getClass("java.lang." + javaType);

        if (result == null) {
          result = BeanUtil.getClass("java.util." + javaType);
        }

        if (result == null) {
          result = BeanUtil.getClass("java.sql." + javaType);
        }
      }

      if (result == null) {
        result = BeanUtil.getClass(javaType);
      }

      if (result != null) {
        this.javaTypes.put(javaType, result);
      }
    }

    if (result == null) {
      fail("Could not resolve Java type: [" + javaType
          + "]. It was not defined in the beginning of the SQLS file as "
          + "'!typeAlias=full.type.Name', it is not a primitive, and the type "
          + "does not belong to packages java.lang, java.util and java.sql.");
    }

    return result;
  }

  private void loadSqlTypes() {
    for (Field field : Types.class.getDeclaredFields()) {
      try {
        int mod = field.getModifiers();
        if (field.getType() == int.class
            && Modifier.isPublic(mod)
            && Modifier.isStatic(mod)
            && Modifier.isFinal(mod)) {
          this.sqlTypes.put(field.getName(), field.getInt(null));
        }
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e); // Expected not to happen but just in case...
      }
    }
  }

  private static String toString(int cp) {
    return cp == -1 ? "end-of-stream" : "'" + String.copyValueOf(Character.toChars(cp)) + "'";
  }

  private static boolean isKeys(String value) {
    return "KEYS".equals(value);
  }

}
