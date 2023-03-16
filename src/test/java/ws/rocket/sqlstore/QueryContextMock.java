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

package ws.rocket.sqlstore;

import ws.rocket.sqlstore.script.Script;

/**
 * Helper class for creating {@link QueryContext} objects, which can be created only from the same
 * package (protected constructor access).
 */
public final class QueryContextMock {

  /**
   * Creates a new <code>QueryContext</code> object for given script and script arguments.
   *
   * @param script A script object for the context.
   * @param args Optional arguments.
   * @return A new instance of <code>QueryContext</code>.
   */
  public static QueryContext init(Script script, Object... args) {
    return new QueryContext(script, args);
  }

}
