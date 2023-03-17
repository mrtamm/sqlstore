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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.script.Script;
import ws.rocket.sqlstore.script.read.block.SqlBlockReader;
import ws.rocket.sqlstore.script.sql.SqlScript;

/**
 * Helper class for parsing meta-data of a script from an SQLS file and produces a valid
 * {@link Script} object to be executed via SqlStore.
 *
 * <p>This reader exposes an API for directing the parse process. The correct order for parsing, is
 * following:
 *
 * <ol>
 * <li>{@link #parseJavaTypeAliases()} (first time and only once)
 * <li>{@link #parseName()}
 * <li>{@link #parseParams()}
 * <li>{@link #parseSql()}
 * <li>{@link #createScript()}
 * </ol>
 *
 * <p>To simplify that work, this class exposes static method {@link #load(java.lang.Class)}, which
 * parses all the scripts from the scripts file associated with given class.
 *
 * <p>A <code>ScriptReader</code> instance is bound to a single SQLS file and can be used to parse
 * all the scripts from the file.
 *
 * @see #load(Class)
 */
public final class ScriptReader implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(ScriptReader.class);

  private static final String PATH_PREFIX;

  private static final String PATH_SUFFIX;

  static {
    PATH_PREFIX = System.getProperty("sqlstore.path.prefix", "/sql/");
    PATH_SUFFIX = System.getProperty("sqlstore.path.suffix", ".sqls");
  }

  private static String getResourcePath(Class<?> clazz) {
    return PATH_PREFIX + clazz.getSimpleName() + PATH_SUFFIX;
  }

  /**
   * Loads scripts from an SQLS resource of given class.
   *
   * <p>This reader assumes that for the provided class name (e.g.
   * <code>com.company.dao.Sample</code>) there exists a text file with the same name as the class
   * but with ".sqls" suffix (case-sensitive; e.g. <code>/com/company/dao/Sample.sqls</code>).
   * When the resource is not found, this method will return an empty map.
   * Otherwise, this method will return a map containing parsed scripts by their names.
   *
   * <p>In addition to <code>IOException</code>, this method is likely to throw
   * {@link ws.rocket.sqlstore.ScriptSetupException} when the SQLS resource contains syntax or logic
   * errors. In such case the loading process is halted.
   *
   * <p>Description of the expected resource content is given with the class-level documentation of
   * this class.
   *
   * @param clazz The class for which the related SQLS resource is to be loaded.
   * @return A map of scripts by their name, or an empty map when the resource is not found.
   *
   * @throws IOException The stream errors are delegated for diagnosing issues. None of which are
   *     thrown by this method.
   */
  public static Map<String, Script> load(Class<?> clazz) throws IOException {
    Map<String, Script> results = new HashMap<>();
    long time = System.currentTimeMillis();
    String resourceName = getResourcePath(clazz);
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

    time = System.currentTimeMillis() - time;
    LOG.info("Read {} script(s) from {} in {} ms.", results.size(), resourceName, time);

    return results;
  }

  private final StreamReader reader;

  private final ParamsReader paramsReader;

  private final SqlBlockReader sqlBlockReader;

  private int line;

  private String name;

  private SqlScript sqlScript;

  private final ParamsSet params = new ParamsSet();

  /**
   * Creates a new scripts reader for given input stream.
   *
   * @param input A valid stream to be read. May be empty stream but not null.
   * @throws IOException When a stream-related exception occurs during initial reading.
   */
  public ScriptReader(InputStream input) throws IOException {
    Objects.requireNonNull(input, "No stream was provided for parsing SQL scripts.");

    this.reader = new StreamReader(input);
    this.paramsReader = new ParamsReader(this.reader, this.params);
    this.sqlBlockReader = new SqlBlockReader(this.reader, this.params);
  }

  /**
   * Reports whether there's more data available to be read, i.e. end of stream is not reached. This
   * method skips whitespace since it is not the data parsers are interested in. Once a
   * non-whitespace character or end of stream is encountered, the decision is made on whether there
   * is more data to be read.
   *
   * @return A Boolean that is true when there is more data to be read.
   *
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
   *
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
   *
   * <p>Each parameter-group begins with keyword "IN", "OUT', or "UPDATE" directly followed by an
   * opening parenthesis (no whitespace between). Each group has its own rules for specifying
   * parameters. All groups end with closing parenthesis. The order of these groups is not defined.
   * Each group can be specified only once.
   *
   * <p>The parsed parameters will be stored in this reader. Once the script unit is parsed, they
   * will be used for constructing the resulting script unit info object.
   *
   * @return The current reader.
   *
   * @throws IOException When a stream-related exception occurs during reading.
   * @see #createScript()
   */
  public ScriptReader parseParams() throws IOException {
    this.paramsReader.parseParams();
    return this;
  }

  /**
   * Parses the SQL of the script unit (SQL comes the output parameters, surrounded by
   * curly-braces). The parsed SQL will be stored in this reader. Once the script unit is parsed, it
   * can be used for constructing the resulting script unit info object.
   *
   * @return The current reader.
   *
   * @throws IOException When a stream-related exception occurs during reading.
   * @see #createScript()
   */
  public ScriptReader parseSql() throws IOException {
    this.sqlScript = this.sqlBlockReader.parseSql();
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
    Script result = new Script(this.name, this.line, this.sqlScript, this.params);
    this.params.cleanup(result);
    this.name = null;
    this.sqlScript = null;
    return result;
  }

  @Override
  public void close() throws IOException {
    this.params.cleanup(null);
    this.name = null;
    this.sqlScript = null;
    this.reader.close();
  }

}
