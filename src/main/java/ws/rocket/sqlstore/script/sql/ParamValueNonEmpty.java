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

import java.util.Collection;
import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.QueryParam;

import static java.util.Objects.requireNonNull;

/**
 * A condition for <code>SqlPart</code> where the provided query parameter value must not be null
 * and also not an empty string, not an empty array and not an empty collection to be included in
 * the actually executed query.
 * <p>
 * This condition is expressed in SQLS file as <code>!(var.props){...}</code>. Notice that this is
 * default behaviour when the expression is not wrapped by a condition name. There is no explicit
 * name to activate this condition.
 */
public final class ParamValueNonEmpty implements SqlPartCondition {

  private final QueryParam param;

  /**
   * Initializes condition with the parameter of which value needs to be checked.
   *
   * @param param The query parameter (value) to check.
   */
  public ParamValueNonEmpty(QueryParam param) {
    this.param = requireNonNull(param, "The query parameter is undefined.");
  }

  @Override
  public boolean isApplicable(QueryContext ctx) {
    return isNonEmpty(this.param.getValue(ctx));
  }

  @Override
  public String toString() {
    return this.param + " is not null and also not an empty string, an empty array nor an empty "
        + "collection";
  }

  /**
   * Checks that the given value is not null, not an empty string, not an empty array and not an
   * empty collection.
   *
   * @param value The value to check.
   * @return A boolean true when the value qualifies to an non-empty value.
   */
  protected static boolean isNonEmpty(Object value) {
    if (value instanceof Collection) {
      return !((Collection) value).isEmpty();
    }
    if (value != null && value.getClass().isArray()) {
      return isNonEmptyArray(value);
    }
    return value != null && !"".equals(value);
  }

  private static boolean isNonEmptyArray(Object value) {
    if (value instanceof Object[]) {
      return ((Object[]) value).length > 0;
    }
    if (value instanceof long[]) {
      return ((long[]) value).length > 0;
    }
    if (value instanceof int[]) {
      return ((int[]) value).length > 0;
    }
    if (value instanceof short[]) {
      return ((short[]) value).length > 0;
    }
    if (value instanceof byte[]) {
      return ((byte[]) value).length > 0;
    }
    if (value instanceof double[]) {
      return ((double[]) value).length > 0;
    }
    if (value instanceof float[]) {
      return ((float[]) value).length > 0;
    }
    if (value instanceof char[]) {
      return ((char[]) value).length > 0;
    }
    if (value instanceof boolean[]) {
      return ((boolean[]) value).length > 0;
    }
    throw new IllegalArgumentException("Expected an array argument but its type is not covered: "
        + value.getClass());
  }

}
