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
 * All parameter information in one package: script input and output parameters, SQL statement
 * parameters, also parameters for reading results from (callable) statement, generated keys, and
 * query results. Since parameters are universal in the context of this library, it deserves its own
 * package.
 * <p>
 * The common parent class of parameters is {@link ws.rocket.sqlstore.data.params.Param}. Here is a
 * quick run-down on which parameter types are currently accepted in certain scenarios:
 * <ol>
 * <li>Script input parameters: {@link ws.rocket.sqlstore.data.params.TypeNameParam}
 * <li>Script output parameters: {@link ws.rocket.sqlstore.data.params.TypeNameParam},
 * {@link ws.rocket.sqlstore.data.params.TypePropParam},
 * {@link ws.rocket.sqlstore.data.params.TypeParam}
 * <li>Script update parameters: {@link ws.rocket.sqlstore.data.params.Expression}
 * <li>Query parameters: {@link ws.rocket.sqlstore.data.params.Expression}
 * </ol>
 */
package ws.rocket.sqlstore.script.params;
