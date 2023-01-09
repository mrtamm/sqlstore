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
 * Contains integration-testing code that is executed against actual database. The main testing code
 * is in class <code>DatabaseTest</code> and the SqlStore queries interface is
 * <code>ScriptsFacade</code>.
 *
 * <p><code>DatabaseTest</code> works only when it is able to read valid and suitable configuration
 * form "test.properties" resource in package <code>ws.rocket.sqlstore.test.db.test</code>. To
 * enable this test, the "mvn test" script must be called with JVM parameter
 * <code>testDatabase</code>, where its value is one of "derby", "postgresql", or "oracle". This
 * parameter enables the configuration to be copied into target package from
 * <em>src/test/resources/config/${testDatabase}</em>.
 */
package ws.rocket.sqlstore.test.db;
