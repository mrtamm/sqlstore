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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.sql.DataSource;

/**
 * Globally shared connection manager for <code>SqlStore</code> instances where a connection manger
 * has not been explicitly set. This class acts like a simple registry for holding and sharing a
 * reference to a connection manager that can also be updated during the life-time of an application
 * without needing to reload all scripts. In contrast, this is not possible without reloading
 * scripts when a connection manager is explicit on an <code>SqlStore</code> instance at load time.
 * <p>
 * The methods of this class use locking to regulate updates and reads. It is safe to update and
 * read connection managers in multi-thread scenarios.
 */
public final class SharedConnectionManager {

  private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();

  private static ConnectionManager currentManager;

  /**
   * Registers a data-source to be used by <code>SqlStore</code> instances where a connection manger
   * has not been explicitly set.
   *
   * @param dataSource Reference to a data-source to be shared.
   */
  public static void register(DataSource dataSource) {
    try {
      LOCK.writeLock().lock();
      currentManager = new DataSourceConnectionManager(dataSource);
    } finally {
      LOCK.writeLock().unlock();
    }
  }

  /**
   * Registers a database connection to be used by <code>SqlStore</code> instances where a
   * connection manger has not been explicitly set.
   *
   * @param connection Reference to a connection to be shared.
   */
  public static void register(Connection connection) {
    try {
      LOCK.writeLock().lock();
      currentManager = new SingleConnectionManager(connection);
    } finally {
      LOCK.writeLock().unlock();
    }
  }

  /**
   * Unregisters any currently shared connection manager. After this method completes, there won't
   * be shared connection managers until a new one is explicitly registered.
   */
  public static void unregister() {
    try {
      LOCK.writeLock().lock();
      currentManager = null;
    } finally {
      LOCK.writeLock().unlock();
    }
  }

  /**
   * Provides the currently shared connection manager instance. When none is defined (registered)
   * yet, <code>null</code> will be returned.
   *
   * @return A connection manager or <code>null</code>.
   */
  public static ConnectionManager getInstance() {
    try {
      LOCK.readLock().lock();
      return currentManager;
    } finally {
      LOCK.readLock().unlock();
    }
  }

  private SharedConnectionManager() {
    throw new AssertionError("Cannot create an instance of this class");
  }

}
