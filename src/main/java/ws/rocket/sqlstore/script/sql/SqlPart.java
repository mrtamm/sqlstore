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

import java.util.Arrays;
import java.util.List;
import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.QueryParam;

/**
 * Abstraction for SQL, which may be divided into parts, some of which may be rendered differently
 * depending on input parameters. For each script there is always an SQL part which will be always
 * included (condition = always).
 */
public final class SqlPart implements SqlScript {

  private final SqlPartCondition condition;

  private final String sql;

  private final QueryParam[] params;

  /**
   * Initializes the SQL part with permanent data.
   *
   * @param condition The condition when this part is allowed in the query (required).
   * @param sql The SQL (may be partial) to be included in the final query when condition is
   * satisfied. Must not be empty.
   * @param params A parameters array for the query. May be empty bot not null.
   */
  public SqlPart(SqlPartCondition condition, String sql, QueryParam[] params) {
    if (condition == null) {
      throw new NullPointerException("Query condition is undefined.");
    } else if (sql == null || sql.trim().isEmpty()) {
      throw new IllegalArgumentException("Query SQL must not be empty.");
    } else if (params == null) {
      throw new NullPointerException("Query parameters array is undefined.");
    }

    this.condition = condition;
    this.sql = sql;
    this.params = params;
  }

  @Override
  public void appendSql(QueryContext ctx, StringBuilder script, List<QueryParam> params) {
    if (this.condition.isApplicable(ctx)) {
      script.append(this.sql);
      params.addAll(Arrays.asList(this.params));
    }
  }

  @Override
  public boolean containsOutParam() {
    boolean containsOut = false;

    for (QueryParam p : this.params) {
      if (p.isForOutput()) {
        containsOut = true;
        break;
      }
    }

    return containsOut;
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();

    if (this.condition != ConditionAlways.INSTANCE) {
      str.append("-- When: ").append(this.condition).append('\n');
    }

    if (this.params.length > 0) {
      str.append("-- SQL parameters:\n");
      for (int i = 0; i < this.params.length; i++) {
        str.append("--  ").append(i + 1).append(": ").append(this.params[i]).append('\n');
      }
    }

    str.append(this.sql);

    return str.toString();
  }

}
