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

import static java.util.Objects.requireNonNull;

/**
 * A condition for <code>SqlPart</code> where the provided query parameter value must be either
 * null, an empty string, an empty array or an empty collection to be included in the actually
 * executed query.
 * <p>
 * This condition is expressed in SQLS file as
 * <code>!(<strong>empty(</strong>var.props<strong>)</strong>){...}</code>.
 */
public final class ParamValueEmpty implements SqlPartCondition {

  private final QueryParam param;

  /**
   * Initializes condition with the parameter of which value needs to be checked.
   *
   * @param param The query parameter (value) to check.
   */
  public ParamValueEmpty(QueryParam param) {
    this.param = requireNonNull(param, "The query parameter is undefined.");
  }

  @Override
  public boolean isApplicable(QueryContext ctx) {
    return !ParamValueNonEmpty.isNonEmpty(this.param.getValue(ctx));
  }

  @Override
  public String toString() {
    return this.param + " is null, empty string, empty array, or empty collection";
  }

}
