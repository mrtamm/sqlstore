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

package ws.rocket.sqlstore.script.read;

/**
 * The parameter categories that can be passed to a script (right after script name).
 */
public enum ParamsCategory {

  /**
   * Category of parameters required for input and for executing the script.
   */
  IN,
  /**
   * Category of parameters that must be returned back by the script.
   */
  OUT,
  /**
   * Category of parameters (expressions referring to input parameters) that must be updated by the
   * script but not returned.
   */
  UPDATE,
  /**
   * Category of parameters that are JDBC statement hints about how the query should be executed.
   */
  HINT;

  /**
   * Provides the value for given string. The string must match an enum value name or null will be
   * returned.
   *
   * @param str A parameter category as string.
   * @return The matching value, or null.
   */
  public static ParamsCategory get(String str) {
    ParamsCategory result = null;

    for (ParamsCategory cat : values()) {
      if (cat.name().equals(str)) {
        result = cat;
        break;
      }
    }

    return result;
  }

}
