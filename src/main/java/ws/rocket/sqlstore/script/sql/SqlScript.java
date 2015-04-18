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

package ws.rocket.sqlstore.script.sql;

import java.util.List;
import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.QueryParam;

/**
 * Abstraction to support dynamic SQL script structures. Each SQL script has an SQL and query
 * parameters used in that script. An SQL may be divided into (nested) parts which are included in
 * the final SQL when their inner conditions are satisfied in current query context.
 */
public interface SqlScript {

  /**
   * Appends SQL script and parameters depending on the given query context and the conditions of
   * the (this or inner) SQL parts.
   *
   * @param ctx The current query context for evaluating conditions.
   * @param script A script buffer for append SQL parts as needed.
   * @param params A list for appending query parameters as needed.
   */
  void appendSql(QueryContext ctx, StringBuilder script, List<QueryParam> params);

  /**
   * Informs whether this SQL part contains a reference to parameter in the OUT-parameters.
   *
   * @return A boolean true when this part refers to an OUT-parameter of the script.
   */
  boolean containsOutParam();

}
