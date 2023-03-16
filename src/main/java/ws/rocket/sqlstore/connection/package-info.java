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

/**
 * Strategies for managing database connections and transactions in different scenarios (single
 * connection, data source connections, scoped connection). Since <code>SqlStore</code> instances
 * need to be thread-safe, connection managers also need to comply with that.
 *
 * <p>Users of <code>SqlStore</code> are most likely interested in
 * {@link ws.rocket.sqlstore.connection.SharedConnectionManager}, which enables to define common
 * connection information though this class. Otherwise, custom <code>DataSource</code> or
 * <code>Connection</code> objects can be provided as a parameter to the
 * {@link ws.rocket.sqlstore.SqlStore} methods.</p>
 *
 * <p>Note: connection managers are determined and used by <code>SqlStore</code> internally and
 * cannot be customized directly by the users of the library. In addition, connection managers log
 * their activity but only at the TRACE level. Exceptions are not logged as they are handled by a
 * JDBC executor.
 */
package ws.rocket.sqlstore.connection;
