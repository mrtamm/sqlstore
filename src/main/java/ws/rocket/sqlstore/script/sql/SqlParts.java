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

import static java.util.Objects.requireNonNull;

import java.util.List;
import ws.rocket.sqlstore.QueryContext;
import ws.rocket.sqlstore.script.QueryParam;

/**
 * An SQL script, which is divided into two or more parts. Before evaluating inner parts, the main
 * condition must evaluate to true.
 */
public final class SqlParts implements SqlScript {

  private final SqlPartCondition condition;

  private final SqlScript[] innerParts;

  /**
   * Initializes the SQL part which acts as container for inner SQL parts. This part itself does not
   * contain any SQL.
   *
   * @param condition The condition when this part is allowed in the query (required).
   * @param innerParts At least two inner SQL parts.
   */
  public SqlParts(SqlPartCondition condition, SqlScript[] innerParts) {
    this.condition = requireNonNull(condition, "Query condition is undefined.");
    this.innerParts = requireNonNull(innerParts, "SQL inner parts are undefined");

    if (innerParts.length < 2) {
      throw new IllegalArgumentException("At least 2 inner parts expected.");
    }
  }

  @Override
  public boolean containsOutParam() {
    boolean containsOut = false;

    for (SqlScript part : this.innerParts) {
      if (part.containsOutParam()) {
        containsOut = true;
        break;
      }
    }

    return containsOut;
  }

  @Override
  public void appendSql(QueryContext ctx, StringBuilder script, List<QueryParam> params) {
    if (!this.condition.isApplicable(ctx)) {
      return;
    }

    for (SqlScript part : this.innerParts) {
      part.appendSql(ctx, script, params);
    }
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    for (SqlScript part : this.innerParts) {
      str.append(part.toString()).append('\n');
    }
    str.setLength(str.length() - 1);
    return str.toString();
  }

}
