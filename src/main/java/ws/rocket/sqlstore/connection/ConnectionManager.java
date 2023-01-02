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

/**
 * Connection manager is used to hide details of how a database connection is managed from a JDBC
 * executor.
 * <p>
 * Connection managers focus on opening and closing connections and transactions. Except for the
 * <code>readOnly</code> parameter, a manager typically never configures connection settings.
 * <p>
 * This interface and implementing classes are only used by SqlStore itself, and users of the
 * library can't customize it.
 * <h2>Usage</h2>
 * <p>
 * When a connection is obtained by calling {@link #obtain(boolean)}, it must always be released by
 * calling {@link #release()} after executing scripts. The <code>release()</code> must be called
 * even if obtaining connection failed (and most likely was not created) as the method accepts the
 * fact that the connection does not exist or is possibly already closed. While connection is
 * active, the methods {@link #commit()} and {@link #release()} may be called to store data changes
 * or revert them. However, calling these two methods is optional, for example, when the JDBC
 * executor has detected that no data changes occurred (by checking update count of statements).
 * These methods may be called several times during a connection. However, when these two methods
 * detect that active connection does not exist, will throw an <code>IllegalStateException</code>.
 * <p>
 * The common aspect of all these methods is that all checked <code>SQLException</code>s will be
 * wrapped into unchecked <code>ScriptExecuteException</code>s, which may contain information about
 * the current query context for better inspection of the cause.
 *
 * @see ws.rocket.sqlstore.ScriptExecuteException
 */
public interface ConnectionManager {

  /**
   * Provides possibly fresh connection to a database. This method marks the beginning of a
   * transaction. This method may be called multiple times. When an active connection exists, it
   * will be returned instead of creating a new one.
   *
   * @param readOnly Whether the connection should be read-only.
   * @return A valid connection.
   * @throws ws.rocket.sqlstore.ScriptExecuteException When there are issues with obtaining a
   * connection.
   */
  Connection obtain(boolean readOnly);

  /**
   * Rolls back uncommitted changes in current transaction.
   * <p>
   * This method may be called (even multiple times) when a connection has been obtained and before
   * the connection is released.
   *
   * @throws IllegalStateException When current connection does not exist.
   * @throws ws.rocket.sqlstore.ScriptExecuteException When there are issues with the rollback.
   */
  void rollback();

  /**
   * Commits uncommitted changes in current transaction.
   * <p>
   * This method may be called (even multiple times) when a connection has been obtained and before
   * the connection is released.
   *
   * @throws IllegalStateException When current connection does not exist.
   * @throws ws.rocket.sqlstore.ScriptExecuteException When there are issues with the commit.
   */
  void commit();

  /**
   * Releases current connection. This method marks the end of a transaction.
   * <p>
   * However, note that either commit or roll-back is expected to be called beforehand to store data
   * changes, if necessary, as this method typically just closes the connection (depending on
   * implementation).
   * <p>
   * This method may be called multiple times. When no active connection exists, this method just
   * skips the release procedure.
   *
   * @throws ws.rocket.sqlstore.ScriptExecuteException When there are issues with closing the
   * existing connection.
   */
  void release();

}
