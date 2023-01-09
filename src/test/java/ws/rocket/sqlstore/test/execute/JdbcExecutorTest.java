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

package ws.rocket.sqlstore.test.execute;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptExecuteException;
import ws.rocket.sqlstore.connection.ConnectionManager;
import ws.rocket.sqlstore.execute.JdbcExecutor;
import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.test.helper.ScriptBuilder;

/**
 * Tests for {@link QueryContext} class.
 */
@Test
public final class JdbcExecutorTest {

  private final ConnectionManager connections = mock(ConnectionManager.class);

  private final Connection connection = mock(Connection.class);

  private final Statement statement = mock(Statement.class);

  private final CallableStatement callStatement = mock(CallableStatement.class);

  private final PreparedStatement preparedStatement = mock(PreparedStatement.class);

  @BeforeMethod
  private void resetMocks() {
    reset(connections, connection, statement, callStatement, preparedStatement);
    try {
      when(connections.obtain(false)).thenReturn(connection);
      when(connection.createStatement()).thenReturn(statement);
      when(connection.prepareCall(anyString())).thenReturn(callStatement);
      when(connection.prepareStatement(anyString(), any(String[].class)))
          .thenReturn(preparedStatement);
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void shouldExecuteStatement() throws SQLException {
    QueryContext queryCtx = new ScriptBuilder().toQueryContext();
    assertTrue(queryCtx.isSimpleStatement());

    new JdbcExecutor(connections).execute(queryCtx);

    verify(statement).execute(anyString());
  }

  public void shouldExecuteStatementWithKeys() throws SQLException {
    QueryContext queryCtx = new ScriptBuilder()
        .addKeysOutStringParam("result")
        .toQueryContext();
    assertTrue(queryCtx.isSimpleStatement());

    ResultSet results = mock(ResultSet.class);
    ResultSetMetaData metaData = mock(ResultSetMetaData.class);

    when(statement.getGeneratedKeys()).thenReturn(results);
    when(results.getMetaData()).thenReturn(metaData);
    when(metaData.getColumnCount()).thenReturn(1);

    new JdbcExecutor(connections).execute(queryCtx);

    verify(statement).execute(anyString(), eq(new String[] { "result" }));
  }

  public void shouldExecutePreparedStatement() throws SQLException {
    QueryContext queryCtx = new ScriptBuilder()
        .addInLongParam("id")
        .addScriptInParam("id")
        .toQueryContext(123L);
    assertTrue(queryCtx.isPreparedStatement());

    new JdbcExecutor(connections).execute(queryCtx);

    verify(preparedStatement).execute();
  }

  public void shouldExecuteCallStatement() throws SQLException {
    QueryContext queryCtx = new ScriptBuilder()
        .addOutLongParam("id")
        .addScriptOutParam("id")
        .toQueryContext();
    queryCtx.initResultsContainer(List.class, Long.class);
    assertTrue(queryCtx.isCallStatement());

    new JdbcExecutor(connections).execute(queryCtx);

    verify(callStatement).execute();
  }

  @Test(expectedExceptions = ScriptExecuteException.class)
  public void shouldHandleStatementSqlException() throws SQLException {
    try {
      QueryContext queryCtx = new ScriptBuilder().toQueryContext();
      assertTrue(queryCtx.isSimpleStatement());

      when(connection.createStatement()).thenThrow(SQLException.class);

      new JdbcExecutor(connections).execute(queryCtx);
    } finally {
      verify(connections).rollback();
    }
  }

  @Test(expectedExceptions = ScriptExecuteException.class)
  public void shouldHandleCallStatementSqlException() throws SQLException {
    try {
      QueryContext queryCtx = new ScriptBuilder()
          .addOutLongParam("id")
          .addScriptOutParam("id")
          .toQueryContext();
      assertTrue(queryCtx.isCallStatement());

      when(connection.prepareCall(anyString())).thenThrow(SQLException.class);

      new JdbcExecutor(connections).execute(queryCtx);
    } finally {
      verify(connections).rollback();
    }
  }

  @Test(expectedExceptions = ScriptExecuteException.class)
  public void shouldHandlePreparedStatementSqlException() throws SQLException {
    try {
      QueryContext queryCtx = new ScriptBuilder()
          .addInLongParam("id")
          .addScriptInParam("id")
          .toQueryContext(123L);
      assertTrue(queryCtx.isPreparedStatement());

      when(connection.prepareStatement(anyString(), any(String[].class)))
          .thenThrow(SQLException.class);

      new JdbcExecutor(connections).execute(queryCtx);
    } finally {
      verify(connections).rollback();
    }
  }

}
