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

import java.sql.SQLException;
import ws.rocket.sqlstore.execute.QueryContext;

/**
 * The runtime exception class for all errors reported at script execution. When an error is related
 * to JDBC execution, the wrapped exception is an <code>SQLException</code>. When the script
 * execution was in progress, the related query context is also made available in the thrown
 * exception.
 */
@SuppressWarnings("serial")
public final class ScriptExecuteException extends RuntimeException {

  private final QueryContext context;

  /**
   * Constructor for problems that occurred before a script and/or its parameters were determined.
   *
   * @param message A message describing the problem.
   */
  public ScriptExecuteException(String message) {
    this(message, null, null);
  }

  /**
   * Constructor for problems that occurred after a script and its parameters were determined.
   *
   * @param message A message describing the problem.
   * @param context The current query context.
   */
  public ScriptExecuteException(String message, QueryContext context) {
    this(message, null, context);
  }

  /**
   * Constructor for problems that are related to a database connection.
   *
   * @param message A message describing the problem.
   * @param exception A database/connection exception.
   */
  public ScriptExecuteException(String message, SQLException exception) {
    this(message, exception, null);
  }

  /**
   * Constructor for problems that are related to an SQL execution.
   *
   * @param exception A script execution exception.
   * @param context The current execution context.
   */
  public ScriptExecuteException(SQLException exception, QueryContext context) {
    this(exception != null ? exception.getMessage() : null, exception, null);
  }

  private ScriptExecuteException(String message, SQLException exception, QueryContext context) {
    super(message, exception);
    this.context = context;
  }

  @Override
  public SQLException getCause() {
    return (SQLException) super.getCause();
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

}
