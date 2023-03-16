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

import ws.rocket.sqlstore.QueryContext;

/**
 * Contract condition implementations to be used by {@link SqlPart}.
 */
public interface SqlPartCondition {

  /**
   * Informs whether an SQL part is applicable within the query context.
   *
   * @param ctx The query context (not null).
   * @return A Boolean true when the SQL and parameters of the part must be included in the query.
   */
  boolean isApplicable(QueryContext ctx);

}
