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

import ws.rocket.sqlstore.execute.QueryContext;

/**
 * A condition for <code>SqlPart</code> that must always be included in the actually executed query.
 */
public class ConditionAlways implements SqlPartCondition {

  /**
   * This condition is accessible using this reference to singleton instance.
   */
  public static final ConditionAlways INSTANCE = new ConditionAlways();

  private ConditionAlways() {
  }

  @Override
  public boolean isApplicable(QueryContext ctx) {
    return true;
  }

  @Override
  public String toString() {
    return "always";
  }

}
