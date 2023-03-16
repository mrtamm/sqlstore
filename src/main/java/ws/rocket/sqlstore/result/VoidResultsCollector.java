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

import ws.rocket.sqlstore.ResultsCollector;
import ws.rocket.sqlstore.ScriptExecuteException;

/**
 * A results container that checks that no result item is returned from a script execution.
 * Setting a value through {@link #setRowValue(int, Object)} will trigger an exception.
 */
public final class VoidResultsCollector implements ResultsCollector {

  /**
   * Since <code>VoidResultsCollector</code> does not have any instance data, it's safe to use this
   * global instance.
   */
  public static final VoidResultsCollector INSTANCE = new VoidResultsCollector();

  /**
   * {@inheritDoc}
   *
   * @return Class of {@link Void}.
   */
  @Override
  public Class<?> getType() {
    return Void.class;
  }

  @Override
  public void setRowValue(int columnIndex, Object value) {
    throw new ScriptExecuteException("Expected no results from query");
  }

  @Override
  public Object getRowValue(int columnIndex) {
    return null;
  }

  @Override
  public void rowCompleted() {
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  /**
   * {@inheritDoc}
   *
   * @return Always <code>null</code>.
   */
  @Override
  public Object getResult() {
    return null;
  }

}
