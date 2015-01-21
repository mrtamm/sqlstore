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

import ws.rocket.sqlstore.ScriptExecuteException;

/**
 * A results container that checks that no result item is returned from a script execution.
 */
public final class VoidResult implements Result {

  /**
   * {@inheritDoc}
   * 
   * @return Class of {@link Void}.
   */
  @Override
  public Class<?> getType() {
    return Void.class;
  }

  /**
   * Always throws a runtime exception as this results container expects no results.
   * 
   * @param value (Not used.)
   */
  @Override
  public void addValue(Object value) {
    throw new ScriptExecuteException("Expected no results from query");
  }

  @Override
  public Object getLastValue() {
    return null;
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
