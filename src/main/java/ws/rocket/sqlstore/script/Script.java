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

package ws.rocket.sqlstore.script;

import java.util.List;
import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.params.Param;
import ws.rocket.sqlstore.script.read.ParamsSet;
import ws.rocket.sqlstore.script.sql.SqlScript;

/**
 * Contains loaded information about a script.
 * <p>
 * An instance of a script should encapsulate enough data in order to validate input parameter
 * values, determine JDBC statement type, bind the values to the statement, execute the SQL script,
 * extract results and return them to the caller. The script itself does not deal with all the
 * activities but just needs to be able provide the details so that an executor could work properly.
 * Therefore, the Script class is designed to contain the data in the format most suitable for fast
 * execution (minimal evaluation during execution).
 * <p>
 * Note that Script must be thread-safe so that multiple threads could execute the same script. A
 * non-thread-safe <code>QueryContext</code> wraps the script to provide current execution specific
 * details to the executor.
 *
 * @see ws.rocket.sqlstore.execute.QueryContext
 */
public final class Script {

  /**
   * JDBC statement types.
   */
  public enum StatementType {

    /**
     * The most basic statement that just executes an SQL string as it is.
     */
    SIMPLE,
    /**
     * Prepared statement also accepts bound parameters to a script.
     */
    PREPARED,
    /**
     * Callable statement is for executing procedures and also enables OUT-parameters to a script.
     */
    CALL

  };

  private final String name;

  private final int line;

  private final SqlScript sqlScript;

  private final QueryHints hints;

  private final InputParams inputParams;

  private final OutputParams outputParams;

  private final Param[] keysParams;

  private final Param[] resultsParams;

  private final String[] generatedKeys;

  private final StatementType statementType;

  /**
   * Creates a new script information object. This constructor assumes the following:
   * <ol>
   * <li>The SQL is ready for use, i.e. the parameters are extracted and replaced with question
   * marks as expected by JDBC.
   * <li>All parameters are validated and correct.
   * <li>There are same amount of script parameters as there are question marks (some script
   * parameters may appear more than once).
   * </ol>
   *
   * @param name The script name by which it is called. Required.
   * @param line The line number where this script is defined. Required.
   * @param sqlScript The contained SQL statement/script with parameters. The script may be divided
   * into multiple parts some of which may be conditional and thus omitted from an actual query
   * depending on input parameters. Required.
   * @param params An engine instance taking care of the work related to binding parameters for
   * input and output.
   */
  public Script(String name, int line, SqlScript sqlScript, ParamsSet params) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Query name is undefined");
    } else if (line < 1) {
      throw new IllegalArgumentException("Bad line number for query location");
    } else if (sqlScript == null) {
      throw new IllegalArgumentException("SQL script is undefined");
    }

    this.name = name;
    this.line = line;
    this.sqlScript = sqlScript;

    this.hints = params.getQueryHints();
    this.inputParams = params.getInputParams();
    this.outputParams = params.getOutputParams();
    this.keysParams = params.getKeysParams();
    this.resultsParams = params.getResultsParams();
    this.generatedKeys = params.getGenerateKeyColumns();

    if (this.inputParams.isEmpty()) {
      this.statementType = StatementType.SIMPLE;
    } else if (this.sqlScript.containsOutParam()) {
      this.statementType = StatementType.CALL;
    } else {
      this.statementType = StatementType.PREPARED;
    }
  }

  /**
   * Provides the line number where this script was defined in a resource file.
   *
   * @return The line number (beginning with 1).
   */
  public int getLine() {
    return this.line;
  }

  /**
   * Provides the name of this script.
   *
   * @return The name of this script.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Provides the SQL query to be used as it is.
   *
   * @param ctx The query context where the SQL is about to be executed.
   * @param params A list to be filled with query parameters to be used with the returned SQL.
   * @return The SQL query string.
   */
  public String getSqlAndParams(QueryContext ctx, List<QueryParam> params) {
    StringBuilder sql = new StringBuilder(128);
    this.sqlScript.appendSql(ctx, sql, params);
    return sql.toString();
  }

  /**
   * Provides the kind of JDBC statement to use with this SQL.
   *
   * @return The JDBC statement type;
   */
  public StatementType getStatementType() {
    return this.statementType;
  }

  /**
   * Provides custom SQL execution hints, which are optional.
   *
   * @return An hints object, or null.
   */
  public QueryHints getHints() {
    return this.hints;
  }

  /**
   * Provides access to query input parameters.
   * <p>
   * Even when the query has no input parameters, this method will still provide the parameters
   * wrapper object. Use {@link InputParams#isEmpty()} to check whether there is at least one such
   * parameter.
   *
   * @return The wrapper object for input parameters.
   */
  public InputParams getInputParams() {
    return this.inputParams;
  }

  /**
   * Provides access to query output parameters.
   * <p>
   * Even when the query has no output parameters, this method will still provide the parameters
   * wrapper object. Use {@link OutputParams#isEmpty()} to check whether there is at least one such
   * parameter.
   *
   * @return The wrapper object for output parameters.
   */
  public OutputParams getOutputParams() {
    return this.outputParams;
  }

  /**
   * Provides the list of parameters for extracting values from a generated keys result-set.
   *
   * @return An array (possibly empty) of generated keys result-set parameters.
   */
  public Param[] getKeysParams() {
    return this.keysParams;
  }

  /**
   * Provides the list of parameters for extracting values from a query result-set.
   *
   * @return An array (possibly empty) of query result-set parameters.
   */
  public Param[] getResultsParams() {
    return this.resultsParams;
  }

  /**
   * Provides the list of column names to be used for retrieving generated keys.
   *
   * @return An array (not empty) of column names, or null when this script does not handle
   * generated keys.
   */
  public String[] getGeneratedKeys() {
    return this.generatedKeys;
  }

  /**
   * Provides a compact representation of this script information, similar to its original
   * representation in an SQLS file.
   *
   * @return Representation of this script information.
   */
  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    str.append(this.name).append(' ');

    if (!this.inputParams.isEmpty()) {
      str.append("\n    ").append(this.inputParams);
    }

    if (!this.outputParams.isEmpty()) {
      str.append("\n    ").append(this.outputParams);
    }

    if (this.keysParams.length > 0) {
      str.append("\n# GeneratedKeys {");
      for (int i = 0; i < this.keysParams.length; i++) {
        str.append("\n#  ").append(i + 1).append(": ")
            .append(this.generatedKeys[i])
            .append(" -> ")
            .append(this.keysParams[i]);
      }
      str.append("\n# }");
    }

    if (this.resultsParams.length > 0) {
      str.append("\n# ResultSet {");
      for (int i = 0; i < this.resultsParams.length; i++) {
        str.append("\n#  ").append(i + 1).append(": ").append(this.resultsParams[i]);
      }
      str.append("\n# }");
    }

    str.append("\n====\n");
    str.append(this.sqlScript);
    str.append("\n====\n");

    return str.toString();
  }

}
