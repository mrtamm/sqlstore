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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.rocket.sqlstore.ScriptExecuteException;

/**
 * A thread-safe connection manager for applications where a single connection is maintained for
 * communicating with a database. This manager is used when <code>SqlStore</code> is given a
 * connection instance during initialization. In such cases, <code>SqlStore</code> assumes that the
 * application is mostly single-threaded and connection/database access must be synchronized.
 * Therefore, this manager uses a lock to limit concurrent access to the connection instance.
 *
 * <p>The lock is enforced before connection is obtained in {@link #obtain(boolean)}, and unlocking
 * occurs after {@link #release()}. Therefore, calling the {@link #release()} method is mandatory
 * even if the {@link #obtain(boolean)} method failed with a runtime exception to avoid locking
 * issues.
 */
public final class SingleConnectionManager implements ConnectionManager {

  private static final Logger LOG = LoggerFactory.getLogger(SingleConnectionManager.class);

  private final ReentrantLock connectionLock = new ReentrantLock(true);

  private final Connection connection;

  /**
   * Creates a new manager for given database connection.
   *
   * <p>The provided connection must not be null nor closed. Failure to qualify will result in a
   * runtime exception.
   *
   * @param connection A valid connection to database.
   */
  public SingleConnectionManager(Connection connection) {
    Objects.requireNonNull(connection, "Connection to database is null.");

    try {
      if (connection.isClosed()) {
        throw new IllegalArgumentException("Connection to database is closed");
      }
    } catch (SQLException e) {
      throw new ScriptExecuteException(e, "Failed to test whether connection is closed");
    }

    this.connection = connection;
  }

  @Override
  public Connection obtain(boolean readOnly) {
    if (!this.connectionLock.isHeldByCurrentThread()) {
      this.connectionLock.lock();

      try {
        if (this.connection.isReadOnly() != readOnly) {
          this.connection.setReadOnly(readOnly);
        }

        LOG.trace("Locking current DB connection for a new session (read-only={}, autocommit={})",
            readOnly, this.connection.getAutoCommit());

      } catch (SQLException e) {
        throw new ScriptExecuteException(e, "Failed to configure connection");
      }
    }

    return this.connection;
  }

  @Override
  public void commit() {
    if (!this.connectionLock.isHeldByCurrentThread()) {
      throw new IllegalStateException("Detected attempt to commit transaction while the current "
          + "connection to database is held by another thread.");
    }

    try {
      this.connection.commit();
      LOG.trace("Committed current transaction.");
    } catch (SQLException e) {
      throw new ScriptExecuteException(e, "Failed to commit transaction");
    }
  }

  @Override
  public void rollback() {
    if (!this.connectionLock.isHeldByCurrentThread()) {
      throw new IllegalStateException("Detected attempt to roll back transaction while the current "
          + "connection to database is held by another thread.");
    }

    try {
      this.connection.rollback();
      LOG.trace("Rolled back current transaction.");
    } catch (SQLException e) {
      throw new ScriptExecuteException(e, "Failed to roll back transaction");
    }
  }

  @Override
  public void release() {
    if (this.connectionLock.isHeldByCurrentThread()) {
      LOG.trace("Unlocking access to current DB connection for other threads.");
      this.connectionLock.unlock();
    }
  }

}
