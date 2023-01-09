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

package ws.rocket.sqlstore.connection;

import static java.util.Objects.requireNonNull;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.rocket.sqlstore.ScriptExecuteException;

/**
 * A thread-safe connection manager for applications where a data source is used for obtaining
 * connections for communicating with a database.
 *
 * <p>This manager is used when <code>SqlStore</code> is given a data source instance during
 * initialization. In such cases, <code>SqlStore</code> assumes that the application is often
 * multithreaded and, therefore, maintains a database connection per thread (multiple open
 * connections per application at a time instance is assumed normal).
 */
public final class DataSourceConnectionManager implements ConnectionManager {

  private static final Logger LOG = LoggerFactory.getLogger(DataSourceConnectionManager.class);

  private final ThreadLocal<Connection> connection = new ThreadLocal<>();

  private final DataSource dataSource;

  /**
   * Creates a new manager for given database data source.
   *
   * <p>The provided data source must not be null, or it will cause a runtime exception.
   *
   * @param dataSource A valid data source for obtaining connections to database.
   */
  public DataSourceConnectionManager(DataSource dataSource) {
    this.dataSource = requireNonNull(dataSource, "DataSource for a database is undefined.");
  }

  @Override
  public Connection obtain(boolean readOnly) {
    Connection c = this.connection.get();

    if (c == null) {
      try {
        c = this.dataSource.getConnection();
        this.connection.set(c);
        c.setReadOnly(readOnly);

        LOG.trace("Obtained a connection from data source (read-only={}, autocommit={})",
            readOnly, c.getAutoCommit());

      } catch (SQLException e) {
        throw new ScriptExecuteException(e, "Failed to retrieve a database connection from "
            + "data source");
      }
    }

    return c;
  }

  @Override
  public void commit() {
    Connection c = this.connection.get();

    if (c == null) {
      throw new IllegalStateException("Detected attempt to commit transaction while the current "
          + "thread has not started a connection to database or it is already closed.");
    }

    try {
      c.commit();
      LOG.trace("Committed current transaction.");
    } catch (SQLException e) {
      throw new ScriptExecuteException(e, "Failed to commit transaction");
    }
  }

  @Override
  public void rollback() {
    Connection c = this.connection.get();

    if (c == null) {
      throw new IllegalStateException("Detected attempt to roll back transaction while the current"
          + "thread has not started a connection to database or it is already closed.");
    }

    try {
      c.rollback();
      LOG.trace("Rolled back current transaction.");
    } catch (SQLException e) {
      throw new ScriptExecuteException(e, "Failed to roll back transaction");
    }
  }

  @Override
  public void release() {
    Connection c = this.connection.get();
    if (c != null) {
      try {
        c.close();
        LOG.trace("Released current connection.");
      } catch (SQLException e) {
        throw new ScriptExecuteException(e, "Failed to release a database connection "
            + "to data source");
      } finally {
        this.connection.remove();
      }
    }
  }

}
