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
 * Tests for verifying the functionality of SqlStore source code.
 * <p>
 * These are mostly unit tests for specific classes. Except <code>SqlStoreTest</code> which tests
 * the main API and script parsing, and executing scripts against a targeted database.
 * <code>SqlStoreTest</code> also has external resources:
 * <ul>
 * <li>src/test/resources/ws/rocket/sqlstore/test/SqlStoreTest.sqls &mdash; contains SQL scripts to
 * be tested;
 * <li>src/test/resources/ws/rocket/sqlstore/test/test.properties &mdash; contains environment
 * dependant parameters for connecting to a database.
 * </ul>
 */
package ws.rocket.sqlstore.test;
