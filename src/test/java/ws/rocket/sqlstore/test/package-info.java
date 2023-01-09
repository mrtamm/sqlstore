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
 *
 * <p>These are mostly unit tests for specific classes. Except the classes under the
 * <code>ws.rocket.sqlstore.test.db</code> package, which tests the main API, integrity of
 * components, script parsing, and executes scripts against an actual targeted database.
 *
 * <p>Integration test also has external resources:
 *
 * <ul>
 * <li>src/test/resources/config/<em>dbname</em>/ScriptsFacade.sqls &mdash; contains SQL scripts to
 * be tested;
 * <li>src/test/resources/config/<em>dbname</em>/test.properties &mdash; contains environment
 * dependant parameters for connecting to a database.
 * </ul>
 *
 * <p>These resources are copied to package <code>ws.rocket.sqlstore.test.db</code> by build script
 * (see pom.xml) when parameter <code>-DtestDatabase={oracle|derby|postgresql}</code> is provided.
 * When the code of integration tests does not see the resources in the package, it skips the tests
 * with a warning but does not fail.
 */
package ws.rocket.sqlstore.test;
