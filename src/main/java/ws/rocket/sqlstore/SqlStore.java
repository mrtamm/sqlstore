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

package ws.rocket.sqlstore;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.util.Map;
import javax.sql.DataSource;
import ws.rocket.sqlstore.connection.ConnectionManager;
import ws.rocket.sqlstore.connection.DataSourceConnectionManager;
import ws.rocket.sqlstore.connection.ScopedConnectionManager;
import ws.rocket.sqlstore.connection.SharedConnectionManager;
import ws.rocket.sqlstore.connection.SingleConnectionManager;
import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.Script;
import ws.rocket.sqlstore.script.read.ScriptReader;

/**
 * Stores SQL scripts loaded from an SQLS file. This is the main class of the library, and holds all
 * loaded scripts of a file, and executes them when needed (with runtime parameters).
 * <p>
 * An instance can be created by calling one of the <code>load(...)</code> methods. They load the
 * scripts from an SQLS file for given class, and also register the data-source or SQL connection in
 * order to execute a script when needed.
 * <p>
 * To debug an SQL store, use the {@link #printState(java.io.PrintStream)} method, which outputs all
 * contained scripts and their details.
 * <p>
 * Upon parsing scripts, the <code>load(...)</code> methods may throw {@link ScriptSetupException}s.
 * After that, an application should stop loading further scripts, possibly halting so that the SQLS
 * file could be fixed before loading the resource again.
 * <p>
 * Instances of this class do not need any special destroy procedures to discard them. However,
 * applications should not load scripts from a resource more than once per application life-cycle,
 * unless the previous instance is properly released.
 */
public final class SqlStore {

  private final ConnectionManager connectionManager;

  private final Map<String, Script> scripts;

  /**
   * Attempts to load scripts for given class, and register a data-source so that loaded scripts
   * could be executed by name at any time.
   * <p>
   * The resource containing the scripts is expected to have same path as the given class package,
   * the file must have the same name (case-sensitive) as the class, and the file must have ".sqls"
   * extension (case-sensitive, again).
   *
   * @param forClass The class for which the SQLS script is parsed.
   * @return An SQL store containing all the scripts from the SQLS file.
   * @throws ScriptSetupException When the resource could not be properly read, or it contained
   * syntax or setup errors.
   * @see DataSourceConnectionManager
   */
  public static SqlStore load(Class<?> forClass) {
    try {
      return new SqlStore(null, ScriptReader.load(forClass));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Attempts to load scripts for given class, and register a data-source so that loaded scripts
   * could be executed by name at any time.
   * <p>
   * The resource containing the scripts is expected to have same path as the given class package,
   * the file must have the same name (case-sensitive) as the class, and the file must have ".sqls"
   * extension (case-sensitive, again).
   *
   * @param forClass The class for which the SQLS script is parsed.
   * @param dataSource The data-source to use at runtime in order to obtain connections and execute
   * scripts.
   * @return An SQL store containing all the scripts from the SQLS file.
   * @throws ScriptSetupException When the resource could not be properly read, or it contained
   * syntax or setup errors.
   * @see DataSourceConnectionManager
   */
  public static SqlStore load(Class<?> forClass, DataSource dataSource) {
    try {
      return new SqlStore(
          new DataSourceConnectionManager(dataSource),
          ScriptReader.load(forClass));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Attempts to load scripts for given class, and register a database connection so that loaded
   * scripts could be executed by name at any time.
   * <p>
   * The resource containing the scripts is expected to have same path as the given class package,
   * the file must have the same name (case-sensitive) as the class, and the file must have ".sqls"
   * extension (case-sensitive, again).
   *
   * @param forClass The class for which the SQLS script is parsed.
   * @param connection The database connection to use at runtime in order to execute scripts.
   * @return An SQL store containing all the scripts from the SQLS file.
   * @throws ScriptSetupException When the resource could not be properly read, or it contained
   * syntax or setup errors.
   * @see SingleConnectionManager
   */
  public static SqlStore load(Class<?> forClass, Connection connection) {
    try {
      return new SqlStore(new SingleConnectionManager(connection),
          ScriptReader.load(forClass));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private SqlStore(ConnectionManager connectionHandler, Map<String, Script> scripts) {
    this.connectionManager = connectionHandler;
    this.scripts = scripts;
  }

  /**
   * Performs a contained query lookup by name, and validates the passed script arguments.
   * <p>
   * When the any of the activities should fail, a runtime exception will be raised.
   *
   * @param name The query name (case-sensitive).
   * @param args Arguments to the query to be executed.
   * @return A query wrapper to also specify the return-value types and to execute the returned
   * query.
   */
  public Query query(String name, Object... args) {
    return composeQuery(name, args);
  }

  /**
   * Executes a block of (multiple) SQL queries in a single transaction.
   * <p>
   * The block is given a different <code>SqlStore</code> instance, which does not commit changes
   * after executing each script, but just sets a save-point instead.
   * <p>
   * The transaction isolation with this method is <code>read-committed</code>.
   *
   * @param block The block to be executed. When null, this process will be skipped.
   */
  public void execBlock(Block block) {
    execBlock(block, Connection.TRANSACTION_READ_COMMITTED);
  }

  /**
   * Executes a block of (multiple) SQL queries in a single transaction.
   * <p>
   * The block is given a different <code>SqlStore</code> instance, which does not commit changes
   * after executing each script, but just sets a save-point instead.
   *
   * @param block The block to be executed. When null, this process will be skipped.
   * @param transactionIsolation A custom transaction isolation option from the {@link Connection}
   * class.
   * @see ScopedConnectionManager
   */
  public void execBlock(Block block, int transactionIsolation) {
    if (block != null) {
      ScopedConnectionManager scopedManager = new ScopedConnectionManager(this.connectionManager);
      try {
        block.execute(new SqlStore(scopedManager, this.scripts));
      } finally {
        scopedManager.releaseFinally();
      }
    }
  }

  /**
   * Prints the internal state with details on contained scripts to given stream. The output is
   * non-standard and is subject to changing. It's main purpose is debugging.
   *
   * @param out The stream to write to. When null, the state won't be printed.
   */
  public void printState(PrintStream out) {
    if (out != null) {
      for (Script script : this.scripts.values()) {
        out.println(script.toString());
      }
    }
  }

  private Query composeQuery(String name, Object[] args) {
    Script script = this.scripts.get(name);
    if (script == null) {
      throw new RuntimeException("Unrecognized script name: " + name);
    }

    QueryContext ctx = new QueryContext(script, args);
    ConnectionManager con = this.connectionManager;
    if (con == null) {
      con = SharedConnectionManager.getInstance();
    }
    return new Query(con, ctx);
  }

}
