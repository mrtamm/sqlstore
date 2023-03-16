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

package ws.rocket.sqlstore.types;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.testng.annotations.Test;

/**
 * Tests for the {@link StringMapper} class.
 */
@Test
public final class StringMappingTest {

  private final StringMapper mapper = new StringMapper();

  public void shouldAcceptString() {
    assertTrue(this.mapper.supports(String.class));
  }

  public void shouldDefaultToVarchar() {
    assertEquals(this.mapper.confirmSqlType(null), Types.VARCHAR);
  }

  public void shouldAcceptOtherSqlTypes() {
    assertEquals(this.mapper.confirmSqlType(Types.BINARY), Types.BINARY);
  }

  public void shouldWriteStringToStatement() throws SQLException {
    PreparedStatement stmt = mock(PreparedStatement.class);

    this.mapper.write(stmt, 1, "tested value", -1);

    verify(stmt).setString(1, "tested value");
  }

  public void shouldWriteNullStringToStatement() throws SQLException {
    PreparedStatement stmt = mock(PreparedStatement.class);

    this.mapper.write(stmt, 1, null, -1);

    verify(stmt).setNull(1, -1);
  }

  public void shouldWriteClobStringToStatement() throws SQLException {
    PreparedStatement stmt = mock(PreparedStatement.class);

    this.mapper.write(stmt, 1, "expected value", Types.CLOB);

    verify(stmt).setClob(eq(1), isA(Clob.class));
  }

  public void shouldReadStringFromStatement() throws SQLException {
    CallableStatement stmt = mock(CallableStatement.class);
    when(stmt.getString(1)).thenReturn("expected value");

    assertEquals(this.mapper.read(stmt, 1, -1), "expected value");
  }

  public void shouldReadNullStringFromStatement() throws SQLException {
    CallableStatement stmt = mock(CallableStatement.class);
    when(stmt.getString(1)).thenReturn(null);

    assertNull(this.mapper.read(stmt, 1, -1));
  }

  public void shouldReadClobFromStatement() throws SQLException {
    CallableStatement stmt = mock(CallableStatement.class);
    Clob clob = mockClob(Clob.class, "expected value");
    when(stmt.getClob(1)).thenReturn(clob);

    assertEquals(this.mapper.read(stmt, 1, Types.CLOB), "expected value");
  }

  public void shouldReadNclobFromStatement() throws SQLException {
    CallableStatement stmt = mock(CallableStatement.class);
    NClob clob = mockClob(NClob.class, "expected value");
    when(stmt.getNClob(1)).thenReturn(clob);

    assertEquals(this.mapper.read(stmt, 1, Types.NCLOB), "expected value");
  }

  public void shouldReadNullClobFromStatement() throws SQLException {
    CallableStatement stmt = mock(CallableStatement.class);
    Clob clob = mockClob(Clob.class, "");
    when(stmt.getClob(1)).thenReturn(clob);

    assertNull(this.mapper.read(stmt, 1, Types.CLOB));
  }

  public void shouldReadStringFromResultSet() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getString(1)).thenReturn("expected value");

    assertEquals(this.mapper.read(rs, 1, -1), "expected value");
  }

  public void shouldReadNullStringFromResultSet() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getString(1)).thenReturn(null);

    assertNull(this.mapper.read(rs, 1, -1));
  }

  public void shouldReadClobFromResultSet() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    Clob clob = mockClob(Clob.class, "expected value");
    when(rs.getClob(1)).thenReturn(clob);

    assertEquals(this.mapper.read(rs, 1, Types.CLOB), "expected value");
  }

  public void shouldReadNclobFromResultSet() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    NClob clob = mockClob(NClob.class, "expected value");
    when(rs.getNClob(1)).thenReturn(clob);

    assertEquals(this.mapper.read(rs, 1, Types.NCLOB), "expected value");
  }

  public void shouldReadNullClobFromResultSet() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    Clob clob = mockClob(Clob.class, "");
    when(rs.getClob(1)).thenReturn(clob);

    assertNull(this.mapper.read(rs, 1, Types.CLOB));
  }

  private static <T extends Clob> T mockClob(Class<T> clobType, String content) {
    int contentLength = content.length();
    T result = mock(clobType);

    try {
      when(result.length()).thenReturn((long) contentLength);
      when(result.getSubString(0, contentLength)).thenReturn(content);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return result;
  }

}
