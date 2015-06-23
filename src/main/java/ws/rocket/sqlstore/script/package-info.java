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
 * Contains data objects for maintaining complete information about a {@literal Script} in memory in
 * order to later execute it properly and possibly read query results. In addition, contains
 * functionality for parsing such scripts from a textual stream.
 * <p>
 * The runtime model of a {@literal Script} must be capable of
 * <ol>
 * <li>describing query parameters;
 * <li>describing whether prepared, callable, or simple statement must be used;
 * <li>validating query input parameters (values and types);
 * <li>validating query output parameters (values and types) and that they are returned;
 * <li>detecting possible conflicts with parameters and SQL script at creation time;
 * <li>describing query hints to customize query execution.
 * </ol>
 * <p>
 * For better performance, information about a {@literal Script} is not maintained in the same
 * format as it is laid out in the scripts file, and the memory may contain more information.
 * Regarding this, the future versions of this library ought to reduce memory usage by not creating
 * instances of parameters over and over again.
 * <p>
 * All in all, once parsing is completed, best effort has been made to detect problems with query,
 * and assuming that query gets correct parameters, the query should return results in correct
 * format. A {@literal Script} object can be sure that the query string is non-empty but cannot
 * guarantee its correctness. That can be verified only by executing the statement.
 * <p>
 * Currently there is only one provision way for constructing {@literal Script} objects: via {@link
 * ws.rocket.sqlstore.script.read.ScriptReader}.
 */
package ws.rocket.sqlstore.script;
