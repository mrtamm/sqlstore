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

/**
 * A results container that stores and returns execution results in an <code>Object[][]</code>.
 */
public final class ArrayResultsCollector implements ResultsCollector {

  private final List<Object> result = new ArrayList<>();

  private final int rowLength;

  private final boolean[] valueDefined;

  private Object[] row;

  /**
   * Initializes an array-based results collector. Each row has the same number of columns, which
   * will be fixed here.
   *
   * @param columnCount The number of columns expected in the results.
   */
  public ArrayResultsCollector(int columnCount) {
    this.rowLength = columnCount;
    this.row = new Object[columnCount];
    this.valueDefined = new boolean[columnCount];
  }

  /**
   * {@inheritDoc}
   *
   * @return Class of <code>Object[][]</code>.
   */
  @Override
  public Class<?> getType() {
    return Object[][].class;
  }

  @Override
  public void setRowValue(int columnIndex, Object value) {
    if (columnIndex < 0 || columnIndex >= this.rowLength) {
      throw new IllegalArgumentException("Expected column index from 0 to " + (columnIndex - 1)
          + ", got: " + columnIndex);
    } else if (this.valueDefined[columnIndex]) {
      throw new IllegalStateException("Attempted to set a row value twice.");
    }

    this.valueDefined[columnIndex] = true;
    this.row[columnIndex] = value;
  }

  @Override
  public Object getRowValue(int columnIndex) {
    if (columnIndex < 0 || columnIndex >= this.rowLength) {
      throw new IllegalArgumentException("Expected column index from 0 to " + (columnIndex - 1)
          + ", got: " + columnIndex);
    }
    return this.row[columnIndex];
  }

  @Override
  public void rowCompleted() {
    boolean ready = false;

    for (int i = 0; i < this.valueDefined.length; i++) {
      if (this.valueDefined[i]) {
        this.valueDefined[i] = false;
        ready = true;
      }
    }

    if (ready) {
      this.result.add(this.row);
      this.row = new Object[this.rowLength];
    }
  }

  @Override
  public boolean isEmpty() {
    return this.result.isEmpty();
  }

  @Override
  public Object[] getResult() {
    return this.result.toArray();
  }

}
