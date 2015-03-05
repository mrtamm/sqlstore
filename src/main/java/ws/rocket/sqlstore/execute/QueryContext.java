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

package ws.rocket.sqlstore.execute;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import ws.rocket.sqlstore.ScriptExecuteException;
import ws.rocket.sqlstore.result.ResultsCollector;
import ws.rocket.sqlstore.script.QueryHints;
import ws.rocket.sqlstore.script.QueryParam;
import ws.rocket.sqlstore.script.Script;
import ws.rocket.sqlstore.script.params.Param;
import ws.rocket.sqlstore.types.Bindings;

/**
 * A context for script execution. Unlike {@link Script}, this class is not thread-safe, and
 * maintains parameters and results for a single execution of a <code>Script</code>.
 * <p>
 * To simplify the work done by other classes in order to execute an SQL statement, this context
 * class also serves as the main facade over a script, providing lots of methods to understand or
 * update the context.
 */
public final class QueryContext {

  private final Script script;

  private final Map<String, Object> variables;

  private ResultsCollector resultsCollector;

  private int updateCount;

  /**
   * Establishes a new context for given script to be executed with given parameters. The parameters
   * will be validated immediately and it may result with
   * {@link ws.rocket.sqlstore.ScriptExecuteException} upon failure to qualify.
   *
   * @param script The script to execute (required).
   * @param args A not null array of parameter values.
   */
  public QueryContext(Script script, Object[] args) {
    this.script = script;
    this.variables = script.getInputParams().bind(args);
    this.resultsCollector = script.getOutputParams().createResultsCollector();
  }

  public void initResultsContainer(Class<?> resultContainerType, Class<?>... columnTypes) {
    if (this.resultsCollector != null) {
      throw new ScriptExecuteException("A results collector has already been set.");
    }
    this.resultsCollector = this.script.getOutputParams().createResultsCollector(
        resultContainerType, columnTypes);
  }

  /**
   * Provides the name of the script wrapped in current context.
   *
   * @return The name of the script.
   */
  public String getName() {
    return this.script.getName();
  }

  /**
   * Informs whether the current script should be executed in database in read-only mode (i.e.
   * script should not alter data).
   *
   * @return A Boolean that is true for read-only mode, and false for read-write mode.
   */
  public boolean isReadOnly() {
    return this.script.getHints() != null && this.script.getHints().isReadOnly();
  }

  /**
   * Provides the value of a parameter with given name. (This method does not check whether variable
   * exists.)
   *
   * @param name The name of the parameter from which value is to be received.
   * @return The current value of the parameter.
   */
  public Object getVariable(String name) {
    return this.variables.get(name);
  }

  /**
   * Sets or updates a variable value.
   *
   * @param name The name of the parameter from which value is to be received.
   * @param value The value for the parameter.
   */
  public void updateVariable(String name, Object value) {
    this.variables.put(name, value);
  }

  /**
   * Provides the SQL script to be executed.
   *
   * @return The SQL script.
   */
  public String getSqlQuery() {
    return this.script.getSql();
  }

  /**
   * Informs whether the script should be executed with a <code>CallableStatement</code>.
   *
   * @return A Boolean that is true when current SQL script should be executed with a
   * <code>CallableStatement</code>.
   */
  public boolean isCallStatement() {
    return this.script.getStatementType() == Script.StatementType.CALL;
  }

  /**
   * Informs whether the script should be executed with a <code>PreparedStatement</code>.
   *
   * @return A Boolean that is true when current SQL script should be executed with a
   * <code>PreparedStatement</code>.
   */
  public boolean isPreparedStatement() {
    return this.script.getStatementType() == Script.StatementType.PREPARED;
  }

  /**
   * Informs whether the script should be executed with a <code>Statement</code>.
   *
   * @return A Boolean that is true when current SQL script should be executed with a
   * <code>Statement</code>.
   */
  public boolean isSimpleStatement() {
    return this.script.getStatementType() == Script.StatementType.SIMPLE;
  }

  /**
   * Sets custom execution hints on given statement when such hints are present. Otherwise, this
   * method does nothing.
   *
   * @param stmt The statement being executed for setting hints.
   * @throws SQLException When an unexpected problem should occur while setting a hint.
   */
  public void setHints(Statement stmt) throws SQLException {
    QueryHints hints = this.script.getHints();
    if (hints != null) {
      hints.setHints(stmt);
    }
  }

  /**
   * Provides whether the JDBC statement should also query any auto-generated keys.
   *
   * @return A constant value from <code>Statement</code> specifying a mode for generated keys.
   */
  public int getQueryKeys() {
    boolean noKeys = getKeysColumnCount() == 0;
    return noKeys ? Statement.NO_GENERATED_KEYS : Statement.RETURN_GENERATED_KEYS;
  }

  /**
   * Provides the current value of updated [rows] count. The value is negative, if not known, zero
   * for DDL statements or when nothing was updated, and otherwise a positive number.
   * <p>
   * Upon execution this value will be updated whenever available.
   *
   * @return An integer for updated count (may be negative!).
   */
  public int getUpdateCount() {
    return this.updateCount;
  }

  /**
   * Updates the number of rows updated during last execution.
   *
   * @param count The new value.
   */
  public void setUpdateCount(int count) {
    this.updateCount = count;
  }

  /**
   * Sets parameter values for given prepared (or even callable) statement. (This method is not
   * called when there are no query parameters by design.) This method also registers OUT-parameters
   * on a callable statement.
   * <p>
   * Note: this method is to be called with <code>PreparedStatement</code> or
   * <code>CallableStatement</code>. Callers do not have to check whether the script accepts any
   * parameters.
   *
   * @param stmt The statement where to bind the values.
   * @throws SQLException When an unexpected problem should occur while setting a value.
   */
  public void setParameters(PreparedStatement stmt) throws SQLException {
    int index = 1;
    Bindings binder = Bindings.getInstance();
    for (QueryParam param : this.script.getQueryParams()) {
      binder.bindParam(this, param, stmt, index++);
    }
  }

  /**
   * Reads the values of OUT-parameters (after the statement has been executed).
   * <p>
   * Note: this method is to be called only with <code>CallableStatement</code>. Callers do not have
   * to check whether the script expects any OUT-parameters.
   *
   * @param stmt The statement from which the values can be read.
   * @throws SQLException When an unexpected problem should occur while reading a value.
   */
  public void readParameters(CallableStatement stmt) throws SQLException {
    int index = 1;
    Bindings binder = Bindings.getInstance();
    for (QueryParam param : this.script.getQueryParams()) {
      binder.readParam(this, param, stmt, index++);
    }
    this.resultsCollector.rowCompleted();
  }

  /**
   * Reads the generated keys from the result-set of generated keys.
   *
   * @param key The result-set of generated keys.
   * @throws SQLException When an unexpected problem should occur while reading a value.
   */
  public void readKey(ResultSet key) throws SQLException {
    int index = 1;
    Bindings binder = Bindings.getInstance();
    for (Param param : this.script.getKeysParams()) {
      binder.readParam(this, param, key, index++);
    }
    this.resultsCollector.rowCompleted();
  }

  /**
   * Informs about the expected amount of columns in a generated keys result-set.
   *
   * @return The number of columns in generated keys result-set.
   */
  public int getKeysColumnCount() {
    return this.script.getKeysParams().length;
  }

  /**
   * Informs about the expected amount of columns in a query results result-set.
   *
   * @return The number of columns in generated keys result-set.
   */
  public int getRowColumnCount() {
    return this.script.getResultsParams().length;
  }

  /**
   * Reads the values/columns from the active row of given result-set. This method does not advance
   * the result-set to the next row.
   *
   * @param row The result-set where active row will be read for extracting values.
   * @throws SQLException When an unexpected problem should occur while reading a value.
   */
  public void readRow(ResultSet row) throws SQLException {
    int index = 1;
    Bindings binder = Bindings.getInstance();
    for (Param param : this.script.getResultsParams()) {
      binder.readParam(this, param, row, index++);
    }
    this.resultsCollector.rowCompleted();
  }

  /**
   * Provides the results collector instance used for collecting returnable results after query
   * execution. When the query is not supposed to return anything, the results collector will be a
   * special instance, which does not return anything (a void). Therefore, the results collector is
   * always null-safe.
   *
   * @return A results collector instance.
   */
  public ResultsCollector getResultsCollector() {
    return this.resultsCollector;
  }

}
