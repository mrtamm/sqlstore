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

package ws.rocket.sqlstore.script.params;

import ws.rocket.sqlstore.execute.QueryContext;

/**
 * A write-only parameter that is only described by its types. This kind of parameter is used only
 * in OUT-parameters-list to describe a result-set item at the same index/position as the parameter
 * instance. This parameter also requires the SQL-type value at the moment of construction.
 */
public final class TypeParam extends Param {

  private final int resultParamIndex;

  /**
   * Initializes the parameter properties.
   *
   * @param javaType The Java type of this parameter value (mandatory).
   * @param sqlType SQL type of this parameter value (mandatory).
   * @param resultParamIndex The zero-based column index for storing the retrieved value.
   */
  public TypeParam(Class<?> javaType, int sqlType, int resultParamIndex) {
    super(javaType, sqlType);
    this.resultParamIndex = resultParamIndex;
  }

  @Override
  public Object read(QueryContext ctx) {
    throw new RuntimeException("Detected illegal attempt to read result parameter");
  }

  @Override
  public void write(QueryContext ctx, Object value) {
    ctx.getResultsCollector().setRowValue(this.resultParamIndex, value);
  }

}
