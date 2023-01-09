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
import java.sql.Savepoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.rocket.sqlstore.ScriptExecuteException;

/**
 * A non-thread-safe connection manager for cases when a block of multiple queries is executed
 * within a single transaction. Therefore, it is assumed that this manager is never used by more
 * than one thread.
 *
 * <p>This manager wraps previous connection manager and creates a save-point when a "transaction"
 * is started in {@link #obtain(boolean)} method. The save-point is released in the
 * {@link #release()} method. Theand {@link #rollback()} methods, although the latter also reverts
 * changes.
 *
 * <p>The {@link #release()} method is does not anything to support executing scripts in a block as
 * in a single transaction. The connection can be released properly by calling
 * {@link #releaseFinally()}. This should be done in the code block that creates an instance of this
 * manager.
 */
public final class ScopedConnectionManager implements ConnectionManager {

  private static final Logger LOG = LoggerFactory.getLogger(ScopedConnectionManager.class);

  private final ConnectionManager connectionManager;

  private Connection connection;

  private Savepoint savepoint;

  /**
   * Creates a new scoped manager using the provided connection manager under the hood.
   *
   * <p>The provided manager must not be null, or it will cause a runtime exception.
   *
   * @param connectionManager A valid parent connection manger to use.
   */
  public ScopedConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = requireNonNull(connectionManager, "ConnectionManager is null.");
  }

  /**
   * {@inheritDoc}
   *
   * <p><strong>This implementation obtains the connection from parent connection manager when the
   * connection does not already exist. The auto-commit property is explicitly set to false once the
   * connection is obtained. In addition, a save-point is created when one does not exist yet in
   * this manager.</strong>
   */
  @Override
  public Connection obtain(boolean readOnly) {
    if (this.connection == null) {
      try {
        this.connection = this.connectionManager.obtain(false);
        this.connection.setAutoCommit(false);
      } catch (SQLException e) {
        throw new ScriptExecuteException(e, "Failed to turn off auto-commit");
      }
    }

    if (this.savepoint == null) {
      try {
        this.savepoint = this.connection.setSavepoint();
        LOG.trace("Began a scoped session by creating a savepoint.");
      } catch (SQLException e) {
        this.savepoint = null;
        throw new ScriptExecuteException(e, "Failed to create a savepoint");
      }
    }

    return this.connection;
  }

  /**
   * {@inheritDoc}
   *
   * <p><strong>This implementation renews current save-point to update its position within current
   * transaction.</strong>
   */
  @Override
  public void commit() {
    if (this.connection == null) {
      throw new IllegalStateException("Detected attempt to commit transaction while connection has"
          + "not been obtained or it is already released.");
    }

    if (this.savepoint != null) {
      try {
        this.connection.releaseSavepoint(this.savepoint);
        this.savepoint = this.connection.setSavepoint();
        LOG.trace("Scoped session commit by renewing current savepoint.");
      } catch (SQLException e) {
        throw new ScriptExecuteException(e, "Failed to release previous savepoint");
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p><strong>This implementation reverts back to the current save-point discarding changes after
   * its creation.</strong>
   */
  @Override
  public void rollback() {
    if (this.connection == null) {
      throw new IllegalStateException("Detected attempt to roll back transaction while connection "
          + "has not been obtained or it is already released.");
    }

    if (this.savepoint != null) {
      try {
        this.connection.rollback(this.savepoint);
        LOG.trace("Scoped session rollback by discarding changes after current savepoint.");
      } catch (SQLException e) {
        throw new ScriptExecuteException(e, "Failed to rollback to a savepoint");
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p><strong>This implementation releases current save-point.</strong>
   */
  @Override
  public void release() {
    if (this.connection != null && this.savepoint != null) {
      try {
        this.connection.releaseSavepoint(this.savepoint);
        LOG.trace("Scoped session released by releasing current savepoint.");
      } catch (SQLException e) {
        throw new ScriptExecuteException(e, "Failed to release current savepoint");
      } finally {
        this.savepoint = null;
      }
    }
  }

  /**
   * Performs the ultimate connection release by delegating this task to parent connection manager.
   */
  public void releaseFinally() {
    this.connection = null;
    this.connectionManager.release();
  }

}
