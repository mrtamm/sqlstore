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

import java.util.ArrayList;
import java.util.List;
import ws.rocket.sqlstore.ResultsCollector;

/**
 * A results container that stores and returns execution results in an <code>ArrayList</code>.
 */
public final class ListResultsCollector implements ResultsCollector {

  private final List<Object> result = new ArrayList<>();

  private Object value;

  private boolean valueDefined;

  /**
   * {@inheritDoc}
   *
   * @return Class of {@link List}.
   */
  @Override
  public Class<?> getType() {
    return List.class;
  }

  @Override
  public void setRowValue(int columnIndex, Object value) {
    if (columnIndex != 0) {
      throw new IllegalArgumentException("Expected column index 0, got: " + columnIndex);
    }
    if (this.valueDefined) {
      throw new IllegalStateException("Attempted to set the list row twice.");
    }

    this.valueDefined = true;
    this.value = value;
  }

  @Override
  public Object getRowValue(int columnIndex) {
    if (columnIndex != 0) {
      throw new IllegalArgumentException("Expected column index 0, got: " + columnIndex);
    }
    return this.value;
  }

  @Override
  public void rowCompleted() {
    if (this.valueDefined) {
      this.result.add(this.value);
      this.value = null;
      this.valueDefined = false;
    }
  }

  @Override
  public boolean isEmpty() {
    return this.result.isEmpty();
  }

  @Override
  public List<Object> getResult() {
    return this.result;
  }

}
