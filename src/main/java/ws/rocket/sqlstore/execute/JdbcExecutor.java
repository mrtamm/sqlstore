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

import static java.util.Objects.requireNonNull;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.rocket.sqlstore.QueryContext;
import ws.rocket.sqlstore.ScriptExecuteException;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.connection.ConnectionManager;

/**
 * Executes an SQL query via JDBC API. This class contains the main work-flow of a query execution
 * while some tasks are delegated to other classes.
 *
 * <p>Being a central place for query execution, this class also provides several logging features.
 * Here they are listed by logger name:
 *
 * <ol>
 * <li><code>ws.rocket.sqlstore.timer.EXEC</code> at TRACE level – the time it took to
 * prepare, execute, and handle results of a query;
 * <li><code>ws.rocket.sqlstore.timer.DB</code> at TRACE level – just the time of the
 * execution in database;
 * <li><code>ws.rocket.sqlstore.execute.JdbcExecutor</code> at DEBUG level – logs the steps
 * taken for preparing, executing, and handling the results of a script.
 * </ol>
 *
 * <p>An instance of this class should be used to execute a script with arguments ONCE, and is not
 * currently meant to be reused for multiple executes.
 */
public final class JdbcExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(JdbcExecutor.class);

  private static final Logger TIME_DB = LoggerFactory.getLogger("ws.rocket.sqlstore.timer.DB");

  private static final Logger TIME_EXEC = LoggerFactory.getLogger("ws.rocket.sqlstore.timer.EXEC");

  private final ConnectionManager connections;

  /**
   * Creates a new executor for given connection manager.
   *
   * @param connections A manager instance for obtaining connection(s).
   */
  public JdbcExecutor(ConnectionManager connections) {
    this.connections = requireNonNull(connections, "ConnectionManager is undefined.");
  }

  /**
   * Executes given query and stores its results in given context.
   *
   * @param ctx A query execution context.
   */
  public void execute(QueryContext ctx) {
    long beginTime = System.nanoTime();

    try {
      // This connection is closed below through this.connections.release().
      final Connection connection = this.connections.obtain(ctx.isReadOnly());

      if (ctx.isCallStatement()) {
        executeCall(ctx, connection);
      } else if (ctx.isPreparedStatement()) {
        executePrepared(ctx, connection);
      } else {
        executeStatement(ctx, connection);
      }

      this.connections.commit();
    } catch (RuntimeException e) {
      this.connections.rollback();
      throw e;
    } finally {
      if (TIME_EXEC.isTraceEnabled()) {
        TIME_EXEC.trace("Execution of script '{}' took {} ms.", ctx.getName(),
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - beginTime));
      }
      this.connections.release();
    }
  }

  private void executeCall(QueryContext ctx, Connection connection) {
    LOG.debug("About to execute a JDBC CallableStatement for '{}'", ctx.getName());

    try (CallableStatement stmt = connection.prepareCall(ctx.getSqlQuery())) {
      ctx.setHints(stmt);
      ctx.setParameters(stmt);

      long time = System.currentTimeMillis();
      final boolean hasData = stmt.execute();

      time = System.currentTimeMillis() - time;
      if (TIME_DB.isTraceEnabled()) {
        TIME_DB.trace("Execution of script '{}' in database took {} ms.", ctx.getName(), time);
      }

      ctx.readParameters(stmt);
      readResults(stmt, hasData, 0, ctx);

    } catch (SQLException e) {
      throw new ScriptExecuteException(e, ctx);
    }
  }

  private void executePrepared(QueryContext ctx, Connection connection) {
    String[] keys = ctx.getQueryKeys();

    LOG.debug("About to execute a JDBC PreparedStatement for '{}'", ctx.getName());

    try (PreparedStatement stmt = connection.prepareStatement(ctx.getSqlQuery(), keys)) {
      ctx.setHints(stmt);
      ctx.setParameters(stmt);

      long time = System.currentTimeMillis();
      boolean hasData = stmt.execute();

      time = System.currentTimeMillis() - time;
      if (TIME_DB.isTraceEnabled()) {
        TIME_DB.trace("Execution of script '{}' in database took {} ms.", ctx.getName(), time);
      }

      readResults(stmt, hasData, keys.length, ctx);

    } catch (SQLException e) {
      throw new ScriptExecuteException(e, ctx);
    }
  }

  private void executeStatement(QueryContext ctx, Connection connection) {
    LOG.debug("About to execute a JDBC Statement for '{}'", ctx.getName());

    try (Statement stmt = connection.createStatement()) {
      ctx.setHints(stmt);

      String[] keys = ctx.getQueryKeys();
      long time = System.currentTimeMillis();

      boolean hasData;
      if (keys.length == 0) {
        hasData = stmt.execute(ctx.getSqlQuery());
      } else {
        hasData = stmt.execute(ctx.getSqlQuery(), keys);
      }

      time = System.currentTimeMillis() - time;
      if (TIME_DB.isTraceEnabled()) {
        TIME_DB.trace("Execution of script '{}' in database took {} ms.", ctx.getName(), time);
      }

      readResults(stmt, hasData, keys.length, ctx);
    } catch (SQLException e) {
      throw new ScriptExecuteException(e, ctx);
    }
  }

  private void readResults(Statement stmt, boolean hasData, int keysCount, QueryContext ctx)
      throws SQLException {

    while (hasData) {
      try (ResultSet results = stmt.getResultSet()) {
        int columnCount = results.getMetaData().getColumnCount();

        if (LOG.isDebugEnabled()) {
          LOG.debug("Reading ResultSet which has {} columns", columnCount);
        }

        if (columnCount != ctx.getRowColumnCount()) {
          throw new ScriptSetupException("The result set has %d but the script expects %d columns.",
              columnCount, ctx.getRowColumnCount());
        }

        while (results.next()) {
          ctx.readRow(results);
        }
      }
      hasData = stmt.getMoreResults();
    }

    int count = stmt.getUpdateCount();
    if (count != -1) {
      LOG.debug("JDBC query update count: {}", count);
      ctx.setUpdateCount(count);
    }

    if (keysCount > 0) {
      try (ResultSet results = stmt.getGeneratedKeys()) {
        int columnCount = results.getMetaData().getColumnCount();

        if (LOG.isDebugEnabled()) {
          LOG.debug("Reading GeneratedKeys which has {} columns", columnCount);
        }

        if (columnCount != keysCount) {
          throw new ScriptSetupException("The KEYS result set has %d but the script expects %d "
              + "columns.", columnCount, keysCount);
        }

        while (results.next()) {
          ctx.readKey(results);
        }
      }
    }
  }

}
