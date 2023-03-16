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

package ws.rocket.sqlstore.script;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.sql.Statement;
import org.testng.Assert;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptSetupException;

/**
 * Tests the {@link QueryHints} class.
 */
@Test
public class QueryHintTest {

  private final QueryHints hints = new QueryHints();

  public void shouldDoNothing() throws SQLException {
    // The statement (which is null here) should not be used
    // because initially no hints are specified.
    new QueryHints().setHints(null);
  }

  @Test(expectedExceptions = ScriptSetupException.class)
  public void shouldFailWithUnknownParam() {
    this.hints.setHint("unknown", "123");
  }

  public void shouldUseMaxRows() throws SQLException {
    this.hints.setHint("maxRows", "1");

    Statement stmt = mock(Statement.class);
    this.hints.setHints(stmt);
    verify(stmt).setMaxRows(1);
  }

  public void shouldUseMaxFieldSize() throws SQLException {
    this.hints.setHint("maxFieldSize", "1");

    Statement stmt = mock(Statement.class);
    this.hints.setHints(stmt);
    verify(stmt).setMaxFieldSize(1);
  }

  public void shouldUseQueryTimeout() throws SQLException {
    this.hints.setHint("queryTimeout", "1");

    Statement stmt = mock(Statement.class);
    this.hints.setHints(stmt);
    verify(stmt).setQueryTimeout(1);
  }

  public void shouldUseFetchSize() throws SQLException {
    this.hints.setHint("fetchSize", "1");

    Statement stmt = mock(Statement.class);
    this.hints.setHints(stmt);
    verify(stmt).setFetchSize(1);
  }

  public void shouldUsePoolable() throws SQLException {
    this.hints.setHint("poolable", "true");

    Statement stmt = mock(Statement.class);
    this.hints.setHints(stmt);
    verify(stmt).setPoolable(true);
  }

  public void shouldUseEscapeProcessing() throws SQLException {
    this.hints.setHint("escapeProcessing", "true");

    Statement stmt = mock(Statement.class);
    this.hints.setHints(stmt);
    verify(stmt).setEscapeProcessing(true);
  }

  public void shouldUseReadOnly() {
    Assert.assertFalse(this.hints.isReadOnly());

    this.hints.setHint("readOnly", "true");

    Assert.assertTrue(this.hints.isReadOnly());
  }

}
