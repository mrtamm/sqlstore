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
 * The main classes that users of <em>SqlStore</em> library interact with.
 * <p>
 * Class {@link ws.rocket.sqlstore.SqlStore} and its minor companions
 * {@link ws.rocket.sqlstore.Query} is used for executing preloaded SQL queries via JDBC API over a
 * (<code>DataSource</code>-pooled or direct) database connection.
 * <p>
 * To categorize failure scenarios, {@link ws.rocket.sqlstore.ScriptSetupException} and
 * {@link ws.rocket.sqlstore.ScriptExecuteException} are used to wrap errors at scripts loading time
 * and at script execution time correspondingly.
 * <p>
 * Unlike <code>java.sql.SQLException</code>, these are runtime exceptions and therefore simplify
 * working with the library. With JDBC, the exception must be checked so that code would be forced
 * to catch failures and close resources. Since SqlStore does that under its hood, and also logs the
 * errors, catching exceptions is not that relevant, though still possible.
 */
package ws.rocket.sqlstore;
