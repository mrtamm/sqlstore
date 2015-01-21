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

import java.sql.SQLException;
import java.sql.Statement;
import ws.rocket.sqlstore.ScriptSetupException;

/**
 * Non-SQL custom hints regarding on what conditions an SQL script must be executed. When present,
 * these hints usually override defaults on per-script basis. SqlStore, by default, does not specify
 * any hints itself.
 * <p>
 * A hint and its value is specified via method
 * {@link #setHint(java.lang.String, java.lang.String)}, later the hints are set on a statement
 * instance via {@link #setHints(java.sql.Statement)}. The following hints are supported (names are
 * case-sensitive):
 * <ol>
 * <li><code>queryTimeout</code> in seconds (zero or a positive number);
 * <li><code>fetchSize</code> (zero or a positive number);
 * <li><code>maxRows</code> (zero or a positive number);
 * <li><code>maxFieldSize</code> (zero or a positive number);
 * <li><code>poolable</code> (true or false);
 * <li><code>escapeProcessing</code> (true or false);
 * <li><code>readOnly</code> (true or false);
 * </ol>
 * <p>
 * These hints correspond to the writable properties of {@link Statement}. When a property is not as
 * a hint, the property will remain as it is (not modified by this object).
 */
public final class QueryHints {

  private int queryTimeout = -1;

  private int fetchSize = -1;

  private int maxRows = -1;

  private int maxFieldSize = -1;

  private Boolean poolable;

  private Boolean escapeProcessing;

  private boolean readOnly;

  /**
   * Sets the value for a hint with given name. The list of valid hint names (and corresponding
   * valid values set) is given with class description.
   *
   * @param name A hint name.
   * @param value The value for the hint.
   */
  public void setHint(String name, String value) {
    switch (name) {
      case "maxRows":
        this.maxRows = checkInt(value, "maxRows (JDBC hint) cannot be negative.");
        break;
      case "queryTimeout":
        this.queryTimeout = checkInt(value, "queryTimeout (JDBC hint) cannot be negative.");
        break;
      case "fetchSize":
        this.fetchSize = checkInt(value, "fetchSize (JDBC hint) cannot be negative.");
        break;
      case "maxFieldSize":
        this.maxFieldSize = checkInt(value, "maxFieldSize (JDBC hint) cannot be negative.");
        break;
      case "readOnly":
        this.readOnly = Boolean.parseBoolean(value);
        break;
      case "poolable":
        this.poolable = Boolean.valueOf(value);
        break;
      case "escapeProcessing":
        this.escapeProcessing = Boolean.valueOf(value);
        break;
      default:
        throw new ScriptSetupException("Hint name is not supported.");
    }
  }

  /**
   * Informs whether the script is read-only. By default, it is not read-only.
   * <p>
   * This property is explicitly exposed because it must be set on connection.
   *
   * @return A Boolean that is true when the SQL script must be executed in read-only mode.
   */
  public boolean isReadOnly() {
    return this.readOnly;
  }

  /**
   * Sets customized hints on given statement. Hints that are not customized, will be skipped.
   *
   * @param stmt The statement to alter; must not be null.
   * @throws SQLException While altering hints.
   */
  public void setHints(Statement stmt) throws SQLException {
    if (this.queryTimeout >= 0) {
      stmt.setQueryTimeout(this.queryTimeout);
    }
    if (this.fetchSize >= 0) {
      stmt.setFetchSize(this.fetchSize);
    }
    if (this.maxRows >= 0) {
      stmt.setMaxRows(this.maxRows);
    }
    if (this.maxFieldSize >= 0) {
      stmt.setMaxFieldSize(this.maxFieldSize);
    }
    if (this.poolable != null) {
      stmt.setPoolable(this.poolable);
    }
    if (this.escapeProcessing != null) {
      stmt.setEscapeProcessing(this.escapeProcessing);
    }
  }

  private int checkInt(String value, String errorMsg) {
    int num = Integer.valueOf(value);
    if (num < 0) {
      throw new IllegalArgumentException(errorMsg);
    }
    return num;
  }

}
