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

package ws.rocket.sqlstore.result;

import java.util.HashMap;
import java.util.Map;

/**
 * A results container that stores and returns execution results in an <code>HashMap</code>.
 */
public final class MapResult implements Result {

  private final Map<Object, Object> result = new HashMap<>();

  private Object keyValue;

  private boolean firstValueNext = true;

  /**
   * {@inheritDoc}
   *
   * @return Class of {@link Map}.
   */
  @Override
  public Class<?> getType() {
    return Map.class;
  }

  @Override
  public void addValue(Object value) {
    if (this.firstValueNext) {
      this.keyValue = value;
    } else {
      this.result.put(this.keyValue, value);
    }
    this.firstValueNext = !this.firstValueNext;
  }

  @Override
  public Object getLastValue() {
    return this.firstValueNext ? this.result.get(this.keyValue) : this.keyValue;
  }

  @Override
  public boolean isEmpty() {
    return this.result.isEmpty();
  }

  @Override
  public Map<Object, Object> getResult() {
    return this.result;
  }

}
