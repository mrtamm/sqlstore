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

/**
 * Represents a script results container.
 *
 * <p>Each container implementation holds results in a specific object type (e.g. Map, List), and
 * that type of result will be returned to the script executor.
 *
 * <p>The results container type for a script will be determined by the properties of the script
 * being executed. An instance will be created for each query execution, since container instances
 * are not reusable for another execution.
 *
 * <p>Implementation notes:
 *
 * <ol>
 * <li>result containers should be lightweight (no synchronization);
 * <li>result containers may throw runtime exceptions when something seems wrong;
 * <li>this interface may go through some changes as practice will show more advanced needs.
 * </ol>
 *
 * <p>Currently, there is no factory that would choose an appropriate implementation from the
 * registered result types. For now, all the choices are hard-coded, and this interface serves as
 * the common API of the implementation classes.
 */
public interface ResultsCollector {

  /**
   * The kind of collection class that will be returned by {@link #getResult()}. The returned type
   * should be a general type (e.g. <code>java.util.List</code> instead of
   * <code>java.util.ArrayList</code>.
   *
   * @return The collection type for storing results used by this container.
   */
  Class<?> getType();

  /**
   * Sets a value for current row.
   *
   * <p>ResultsCollector containers may support multiple Java objects per row, such as
   * {@link MapResultsCollector}, which supports two values per row (key and its value). Therefore,
   * column index is needed to show the position where the value needs to be stored (e.g. for
   * <code>MapResultsCollector</code>, index 0 stores the value as the key of the map, 1 as the
   * corresponding map value). ResultsCollector container must validate the column index to spot
   * suspicious behaviour. That includes storing a value for same column multiple times.
   *
   * @param columnIndex Zero-based index indicating the column where the value needs to be stored.
   * @param value The value to add.
   */
  void setRowValue(int columnIndex, Object value);

  /**
   * Provides the last registered value for current row. May return null when such value is
   * undefined. The value returned is the same value that was set using
   * {@link #setRowValue(int, java.lang.Object)} in the most recent call.
   *
   * <p>When no value has been registered for current row at given column index, the result will be
   * null.
   *
   * @param columnIndex Zero-based index indicating the column from where the value needs to be
   *     returned.
   * @return The previously registered value in given column index, or null.
   */
  Object getRowValue(int columnIndex);

  /**
   * Sends a signal to this result container that the current result-set row has been processed. The
   * implementation can safely prepare for the next row.
   */
  void rowCompleted();

  /**
   * Informs whether at least one result has been added or not. This means that the
   * {@link #rowCompleted()} method has been called at least once to be not empty.
   *
   * @return A Boolean that is true when no result item has been added to this container instance.
   */
  boolean isEmpty();

  /**
   * Provides the query results. The returned value must be assignable to variables of the same type
   * as returned by {@link #getType()}.
   *
   * @return The query results.
   */
  Object getResult();

}
