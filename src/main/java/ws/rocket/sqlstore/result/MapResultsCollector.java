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
public final class MapResultsCollector implements ResultsCollector {

  private final Map<Object, Object> result = new HashMap<>();

  private Object key;

  private Object value;

  private boolean keyDefined;

  private boolean valueDefined;

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
  public void setRowValue(int columnIndex, Object value) {
    if (columnIndex == 0) {
      if (this.keyDefined) {
        throw new IllegalStateException("Attempted to set the map key twice.");
      }
      this.keyDefined = true;
      this.key = value;

    } else if (columnIndex == 1) {
      if (this.valueDefined) {
        throw new IllegalStateException("Attempted to set the map value twice.");
      }
      this.valueDefined = true;
      this.value = value;

    } else {
      throw new IllegalArgumentException("Expected column index 0 or 1, got: " + columnIndex);
    }
  }

  @Override
  public Object getRowValue(int columnIndex) {
    Object val;

    if (columnIndex == 0) {
      val = this.key;
    } else if (columnIndex == 1) {
      val = this.value;
    } else {
      throw new IllegalArgumentException("Expected column index 0 or 1, got: " + columnIndex);
    }

    return val;
  }

  @Override
  public void rowCompleted() {
    if (this.keyDefined || this.valueDefined) {
      this.result.put(this.key, this.value);
      this.key = null;
      this.value = null;
      this.keyDefined = false;
      this.valueDefined = false;
    }
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
