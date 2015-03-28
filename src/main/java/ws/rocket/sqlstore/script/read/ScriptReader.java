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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.script.QueryHints;
import ws.rocket.sqlstore.script.QueryParam;
import ws.rocket.sqlstore.script.Script;
import ws.rocket.sqlstore.script.params.Expression;
import ws.rocket.sqlstore.script.params.ParamMode;
import ws.rocket.sqlstore.script.params.TypeNameParam;
import ws.rocket.sqlstore.script.sql.ConditionAlways;
import ws.rocket.sqlstore.script.sql.ParamValueEmpty;
import ws.rocket.sqlstore.script.sql.ParamValueNonEmpty;
import ws.rocket.sqlstore.script.sql.ParamValueTrue;
import ws.rocket.sqlstore.script.sql.SqlPart;
import ws.rocket.sqlstore.script.sql.SqlPartCondition;

/**
 * Helper class for parsing meta-data of a script from an SQLS file and produces a valid
 * {@link Script} object to be executed via SqlStore.
 * <p>
 * This reader exposes an API for directing the parse process. The correct order for parsing, is
 * following:
 * <ol>
 * <li>{@link #parseJavaTypeAliases()} (first time and only once)
 * <li>{@link #parseName()}
 * <li>{@link #parseParams()}
 * <li>{@link #parseSql()}
 * <li>{@link #createScript()}
 * </ol>
 * <p>
 * A <code>ScriptReader</code> instance is bound to a single SQLS file and can be used to parse all
 * the scripts from the file.
 *
 * @see ScriptsReader
 */
public final class ScriptReader implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(ScriptReader.class);

  private static final String COND_FUNC_EMPTY = "empty";

  private static final String COND_FUNC_TRUE = "true";

  /**
   * Loads scripts from an SQLS resource of given class.
   * <p>
   * This reader assumes that for the provided class name (e.g. <code>com.company.dao.Sample</code>)
   * there exists a text file with the same name as the class but with ".sqls" suffix
   * (case-sensitive; e.g. <code>/com/company/dao/Sample.sqls</code>). When the resource is not
   * found, this method will return an empty map. Otherwise, this method will return a map
   * containing parsed scripts by their names.
   * <p>
   * In addition to <code>IOException</code>, this method is likely to throw
   * {@link ws.rocket.sqlstore.ScriptSetupException} when the SQLS resource contains syntax or logic
   * errors. In such case the loading process is halted.
   * <p>
   * Description of the expected resource content is given with the class-level documentation of
   * this class.
   *
   * @param clazz The class for which the related SQLS resource is to be loaded.
   * @return A map of scripts by their name, or an empty map when the resource is not found.
   * @throws IOException The stream errors are delegated for diagnosing issues. None of which are
   * thrown by this method.
   */
  public static Map<String, Script> load(Class<?> clazz) throws IOException {
    Map<String, Script> results = new HashMap<>();
    String resourceName = clazz.getSimpleName() + ".sqls";
    InputStream input = clazz.getResourceAsStream(resourceName);

    if (input != null) {
      try (ScriptReader reader = new ScriptReader(input)) {
        reader.parseJavaTypeAliases();

        while (reader.hasMore()) {
          Script script = reader
              .parseName()
              .parseParams()
              .parseSql()
              .createScript();

          if (results.containsKey(script.getName())) {
            throw new ScriptSetupException(
                "The SQL script name %s is defined more than once in %s: %d",
                script.getName(), resourceName, script.getLine());
          }

          results.put(script.getName(), script);
        }
      }
    }

    LOG.info("Read {} script(s) from {}", results.size(), resourceName);

    return results;
  }

  private final StreamReader reader;

  private int line;

  private String name;

  private SqlPart[] sqlParts;

  private QueryHints hints;

  private final ParamsSet params = new ParamsSet();

  /**
   * Creates a new scripts reader for given input stream.
   *
   * @param input A valid stream to be read. May be empty stream but not null.
   * @throws IOException When a stream-related exception occurs during initial reading.
   */
  public ScriptReader(InputStream input) throws IOException {
    if (input == null) {
      throw new NullPointerException("No stream to parse for SQL scripts.");
    }
    this.reader = new StreamReader(input);
  }

  /**
   * Reports whether there's more data available to be read, i.e. end of stream is not reached. This
   * method skips whitespace since it is not the data parsers are interested in. Once a
   * non-whitespace character or end of stream is encountered, the decision is made on whether there
   * is more data to be read.
   *
   * @return A Boolean that is true when there is more data to be read.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public boolean hasMore() throws IOException {
    return this.reader.skipWsp() != -1;
  }

  /**
   * Delegates the underlying reader to parse alias names for Java types in the current stream
   * position. This method is defined separately to highlight this parsing step.
   *
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public void parseJavaTypeAliases() throws IOException {
    this.reader.parseJavaTypeAliases();
  }

  /**
   * Parses the script unit name (the block begins with a name). The parsed name will be stored in
   * this reader. Once the script unit is parsed, the name will be used for constructing the
   * resulting script unit info object.
   *
   * @return The current reader.
   * @throws IOException When a stream-related exception occurs during reading.
   * @see #createScript()
   */
  public ScriptReader parseName() throws IOException {
    this.line = this.reader.getLine();
    this.name = this.reader.parseName("an SQL script");
    this.reader.skipWsp();
    return this;
  }

  /**
   * Parses IN, OUT, UPDATE parameters of the script.
   * <p>
   * Each parameters group begins with keyword "IN", "OUT', or "UPDATE" directly followed by an
   * opening parenthesis (no whitespace between). Each group has its own rules for specifying
   * parameters. All groups end with closing parenthesis. The order of these groups is not defined.
   * Each group can be specified only once.
   * <p>
   * The parsed parameters will be stored in this reader. Once the script unit is parsed, they will
   * be used for constructing the resulting script unit info object.
   *
   * @return The current reader.
   * @throws IOException When a stream-related exception occurs during reading.
   * @see #createScript()
   */
  public ScriptReader parseParams() throws IOException {
    Set<ParamsCategory> parsedParamTypes = new HashSet<>();

    while (!this.reader.isNext('{')) {
      int column = this.reader.getColumn();
      ParamsCategory category = this.reader.parseParamsType();

      if (parsedParamTypes.contains(category)) {
        throw new ScriptSetupException("Duplicate parameters category on line %d and column %d.",
            this.reader.getLine(), column);
      }
      parsedParamTypes.add(category);

      this.reader.parenthesisOpen();

      if (ParamsCategory.IN == category) {
        parseInParams();
      } else if (ParamsCategory.OUT == category) {
        parseOutParams();
      } else if (ParamsCategory.UPDATE == category) {
        parseUpdateParams();
      } else if (ParamsCategory.HINT == category) {
        parseHintParams();
      } else {
        throw new ScriptSetupException("Support for category %s is not yet implemented.", category);
      }

      this.reader.requireNext(')');
      this.reader.skipWsp();
    }

    this.params.initInOutUpdateParams();

    return this;
  }

  /**
   * Parses the SQL of the script unit (SQL comes the output parameters, surrounded by
   * curly-braces). The parsed SQL will be stored in this reader. Once the script unit is parsed, it
   * be used for constructing the resulting script unit info object.
   *
   * @return The current reader.
   * @throws IOException When a stream-related exception occurs during reading.
   * @see #createScript()
   */
  public ScriptReader parseSql() throws IOException {
    this.reader.requireNext('{');
    this.reader.skipWsp();

    List<SqlPart> parts = new ArrayList<>();
    SqlPartCondition condition = ConditionAlways.INSTANCE;

    StringBuilder sql = new StringBuilder(1024);
    int cp = this.reader.parseSql(sql);

    while (true) {
      if (cp == '{') {
        parseScriptParam();
      } else {
        String script = sql.toString();
        boolean hasScript = script.trim().length() > 0;

        if (hasScript) {
          parts.add(new SqlPart(condition, script, this.params.resetQueryParams()));
          sql.setLength(0);
        }

        if (cp == '(') {
          condition = parseScriptCondition();
        } else if (cp == '}') {
          if (parts.size() == 1 || !hasScript) {
            break;
          }
          condition = ConditionAlways.INSTANCE;
        }
      }

      cp = this.reader.parseSql(sql);
    }

    this.sqlParts = parts.toArray(new SqlPart[parts.size()]);

    return this;
  }

  /**
   * Creates a script unit info object that will contain the parsed data of a script unit. After
   * this method completes, the state of this reader will be reset to be ready for reading and
   * parsing the next script unit.
   *
   * @return The current script information.
   */
  public Script createScript() {
    Script result = new Script(this.name, this.line, this.sqlParts, this.params, this.hints);
    this.params.cleanup(result);
    this.name = null;
    this.sqlParts = null;
    this.hints = null;
    return result;
  }

  @Override
  public void close() throws IOException {
    this.params.cleanup(null);
    this.name = null;
    this.sqlParts = null;
    this.hints = null;
    this.reader.close();
  }

  // Type[|SQLTYPE] name
  private void parseInParams() throws IOException {
    do {
      Class<?> javaType = this.reader.parseJavaType();
      Integer sqlType = parseSqlType();

      this.reader.skipWsp();

      String paramName = this.reader.parseParamName();

      this.params.addInParam(new TypeNameParam(javaType, sqlType, paramName));
    } while (this.reader.skipWsp() == ',');
  }

  // Type
  // Type|SQLTYPE
  // Type[field]
  // Type[field|SQLTYPE]
  private void parseOutParams() throws IOException {
    boolean keys = false;
    boolean evalKeys = true;
    Class<?> javaType;

    do {
      if (!evalKeys) {
        javaType = this.reader.parseJavaType();
      } else {
        javaType = this.reader.parseKeysOrJavaType();
        keys = javaType == null;
        evalKeys = false;

        if (keys) {
          this.params.setOutFromKeys(true);
          javaType = this.reader.parseJavaType();
        }
      }

      if (this.reader.skipIfNext('[')) {
        this.params.registerBean(javaType);

        do {
          this.reader.skipWsp();

          String fieldName = this.reader.parseName("field");
          Integer sqlType = parseSqlType();

          this.params.addOutParamBeanProp(fieldName, sqlType);
          this.reader.skipWsp();
        } while (this.reader.skipIfNext(','));

        this.params.unregisterBean();
        this.reader.requireNext(']');
      } else {
        Integer sqlType = parseSqlType();
        int cp = this.reader.skipWsp();
        String paramName = null;

        if (cp != ',' && cp != ')') {
          paramName = this.reader.parseParamName();
        }

        this.params.addOutParam(javaType, sqlType, paramName);
      }

    } while (this.reader.skipWsp() == ',');

    if (keys && Character.isWhitespace(this.reader.requireNext(')'))) {
      this.reader.skipWsp();
    }
  }

  private void parseUpdateParams() throws IOException {
    boolean evalKeys = true;
    boolean keys = false;
    String paramName;

    do {
      if (evalKeys) {
        paramName = this.reader.parseKeysOrParamName();
        keys = paramName == null;

        if (keys) {
          evalKeys = false;
          paramName = this.reader.parseParamName();
        }
      } else {
        paramName = this.reader.parseParamName();
      }

      this.reader.requireNext('.');
      String property = this.reader.parseParamPropName();
      this.params.addUpdateParam(paramName, property, keys);

      if (keys && this.reader.skipWsp() == ')') {
        this.reader.skipNext();
        keys = false;
      }

    } while (this.reader.skipWsp() == ',');
  }

  private void parseHintParams() throws IOException {
    this.hints = new QueryHints();

    do {
      this.reader.skipWsp();

      String hintName = this.reader.parseName("hint");
      this.reader.requireNext('=');
      String hintValue = this.reader.parseAlphaNum();

      this.hints.setHint(hintName, hintValue);

      this.reader.skipWsp();
    } while (this.reader.skipIfNext(','));
  }

  private void parseScriptParam() throws IOException {
    List<String> fields = new ArrayList<>();
    ParamMode mode = ParamMode.IN;
    boolean modeSpecified = false;

    this.reader.skipWsp();

    String varName = this.reader.parseParamName();

    if (this.reader.isNext('(')) {
      mode = ParamMode.valueOf(varName);
      modeSpecified = true;
      this.reader.parenthesisOpen();
      varName = this.reader.parseParamName();
    }

    while (this.reader.skipIfNext('.')) {
      fields.add(this.reader.parseParamName());
    }

    Integer sqlType = null;
    if (this.reader.skipWsp() == '|') {
      sqlType = parseSqlType();
    }

    this.params.addScriptParam(mode, varName, fields, sqlType);

    if (modeSpecified) {
      this.reader.skipWsp();
      this.reader.requireNext(')');
    }

    this.reader.skipWsp();
    this.reader.requireNext('}');
  }

  private SqlPartCondition parseScriptCondition() throws IOException {
    String func = null;
    String expr = this.reader.parseParamName();

    if (this.reader.isNext('(')) {
      if (!COND_FUNC_EMPTY.equals(expr) && !COND_FUNC_TRUE.equals(expr)) {
        throw new ScriptSetupException("Expected script inclusion condition '%s' or '%s' "
            + "but got '%s'.", COND_FUNC_EMPTY, COND_FUNC_TRUE, expr);
      }

      func = expr;
      this.reader.skipNext();
      this.reader.skipWsp();
      expr = this.reader.parseParamName();
    }

    List<String> properties = new ArrayList<>();
    while (this.reader.skipIfNext('.')) {
      properties.add(this.reader.parseParamName());
    }

    this.reader.skipWsp();
    if (func != null) {
      this.reader.requireNext(')');
    }
    this.reader.requireNext(')');
    this.reader.requireNext('{');

    TypeNameParam param = this.params.getInputParams().get(expr);
    this.params.markInParamAsUsed(expr);
    QueryParam qp;

    if (properties.isEmpty()) {
      qp = new QueryParam(ParamMode.IN, param);
    } else {
      qp = new QueryParam(ParamMode.IN, Expression.create(param, properties, null));
    }

    SqlPartCondition result;
    if (func == null) {
      result = new ParamValueNonEmpty(qp);
    } else if (COND_FUNC_EMPTY.equals(func)) {
      result = new ParamValueEmpty(qp);
    } else if (COND_FUNC_TRUE.equals(func)) {
      result = new ParamValueTrue(qp);
    } else {
      throw new ScriptSetupException("Unsupported query part condition function: '%s'", func);
    }
    return result;
  }

  private Integer parseSqlType() throws IOException {
    return this.reader.skipIfNext('|') ? this.reader.parseSqlType() : null;
  }

}
