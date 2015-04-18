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
import ws.rocket.sqlstore.script.QueryParam;

/**
 * A condition for <code>SqlPart</code> where the provided query parameter must equal to
 * <code>Boolean.TRUE</code> to be included in the actually executed query.
 * <p>
 * This condition is expressed in SQLS file as
 * <code>!(<strong>true(</strong>var.props<strong>)</strong>){...}</code>.
 */
public final class ParamValueTrue implements SqlPartCondition {

  private final QueryParam param;

  /**
   * Initializes condition with the parameter of which value needs to be checked.
   *
   * @param param The query parameter (value) to check.
   */
  public ParamValueTrue(QueryParam param) {
    if (param == null) {
      throw new NullPointerException("The query parameter is undefined.");
    }
    this.param = param;
  }

  @Override
  public boolean isApplicable(QueryContext ctx) {
    return Boolean.TRUE.equals(this.param.getValue(ctx));
  }

  @Override
  public String toString() {
    return param + " == true";
  }

}
