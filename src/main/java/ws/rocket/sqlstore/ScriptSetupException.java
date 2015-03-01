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

/**
 * This exception class is used to inform about bad SqlStore setup and SQLS files handled by
 * SqlStore.
 */
@SuppressWarnings("serial")
public final class ScriptSetupException extends RuntimeException {

  /**
   * Creates a new instance for wrapping the explicit SQL exception.
   *
   * @param message Message describing the problem.
   * @param msgParams Optional parameters for the message.
   *
   * @see String#format(java.lang.String, java.lang.Object...)
   */
  public ScriptSetupException(String message, Object... msgParams) {
    super(fmt(message, msgParams));
  }

  /**
   * Creates a new instance for wrapping the explicit SQL exception.
   *
   * @param cause The original exception.
   * @param message Message describing the problem.
   * @param msgParams Optional parameters for the message.
   *
   * @see String#format(java.lang.String, java.lang.Object...)
   */
  public ScriptSetupException(Throwable cause, String message, Object... msgParams) {
    super(fmt(message, msgParams), cause);
  }

  private static String fmt(String msg, Object[] params) {
    if (msg == null) {
      msg = "[no error description was given]";
    } else if (params != null && params.length > 0) {
      msg = String.format(msg, params);
    }
    return msg;
  }

}
