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

import java.io.Serial;
import java.sql.SQLException;

/**
 * The runtime exception class for all errors reported at script execution. When an error is related
 * to JDBC execution, the wrapped exception is an <code>SQLException</code>. When the script
 * execution was in progress, the related query context is also made available in the thrown
 * exception.
 */
public final class ScriptExecuteException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 0L;

  private final QueryContext context;

  /**
   * Constructor for problems that occurred before a script and/or its parameters were determined.
   *
   * @param message Message describing the problem.
   * @param msgParams Optional parameters for the message.
   */
  public ScriptExecuteException(String message, Object... msgParams) {
    this(null, null, message, msgParams);
  }

  /**
   * Constructor for problems that occurred after a script and its parameters were determined.
   *
   * @param context The current query context.
   * @param message Message describing the problem.
   * @param msgParams Optional parameters for the message.
   */
  public ScriptExecuteException(QueryContext context, String message, Object... msgParams) {
    this(null, context, message, msgParams);
  }

  /**
   * Constructor for problems that are related to a database connection.
   *
   * @param exception A database/connection exception.
   * @param message Message describing the problem.
   * @param msgParams Optional parameters for the message.
   */
  public ScriptExecuteException(SQLException exception, String message, Object... msgParams) {
    this(exception, null, message, msgParams);
  }

  /**
   * Constructor for problems that are related to an SQL execution.
   *
   * @param exception A script execution exception.
   * @param context The current execution context.
   */
  public ScriptExecuteException(SQLException exception, QueryContext context) {
    this(exception, context, exception != null ? exception.getMessage() : null);
  }

  private ScriptExecuteException(SQLException exception, QueryContext context, String message,
      Object... msgParams) {
    super(fmt(message, msgParams), exception);
    this.context = context;
  }

  @Override
  public SQLException getCause() {
    return (SQLException) super.getCause();
  }

  @Override
  public String toString() {
    SQLException cause = getCause();

    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getName()).append(": ").append(getMessage()).append('\n');

    if (cause != null) {
      sb.append("SQL state: ").append(cause.getSQLState()).append('\n');
      sb.append("SQL error code: ").append(cause.getErrorCode());
    }

    int counter = 1;
    Throwable t = cause;
    while (t != null) {
      sb.append("\nCause ").append(counter++).append(". ");
      sb.append(t.toString());
      t = t.getCause();
    }
    return sb.toString();
  }

  /**
   * The query context that was being executed. This information is optional, and may be
   * <code>null</code>.
   *
   * @return The query context being executed, or null.
   */
  public QueryContext getContext() {
    return this.context;
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
