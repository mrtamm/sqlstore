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

/**
 * Enlists all allowed modes for a statement parameter. The values are also the keywords for
 * wrapping expressions (e.g. <em>OUT(x)</em> ). The default mode is {@literal IN}.
 */
public enum ParamMode {

  /**
   * Marks that the parameter must be set on the statement before execution. This is the only mode
   * that does not mandate the use of callable statement!
   */
  IN,

  /**
   * Marks that the parameter must be read from the callable statement after execution.
   */
  OUT,

  /**
   * Marks that the parameter must be set on the callable statement before execution and must be
   * read from the callable statement after execution.
   */
  INOUT

}
