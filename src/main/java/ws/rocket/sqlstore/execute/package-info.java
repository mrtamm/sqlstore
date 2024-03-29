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
 * Classes for executing a <code>Script</code> (with runtime parameter values).
 * This package is for internal use.
 *
 * <p>The smallest example for executing a script is following:
 *
 * <pre>QueryContext ctx = new QueryContext(script, args);
 * new JdbcExecutor(connectionManager).execute(ctx);
 * return ctx.getResult();</pre>
 */
package ws.rocket.sqlstore.execute;
