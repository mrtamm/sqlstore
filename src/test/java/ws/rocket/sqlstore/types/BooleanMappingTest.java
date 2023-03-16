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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptSetupException;

/**
 * Tests for the {@link BooleanMapper} class.
 */
@Test
public final class BooleanMappingTest {

  private final BooleanMapper mapper = new BooleanMapper();

  public void shouldAcceptBoolean() {
    assertTrue(this.mapper.supports(boolean.class));
    assertTrue(this.mapper.supports(Boolean.class));
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "Boolean value binding for SQL type #6 is not supported\\.")
  public void shouldRejectOtherTypes() {
    mapper.confirmSqlType(Types.FLOAT);
  }

  public void shouldDefaultToBoolean() {
    assertEquals(this.mapper.confirmSqlType(null), Types.BOOLEAN);
  }

  @Test(dataProvider = "getSupportedSqlTypes")
  public void shouldAcceptOtherSqlTypes(int sqlType) {
    assertEquals(this.mapper.confirmSqlType(sqlType), sqlType);
  }

  public void shouldWriteNullToStatement() throws SQLException {
    PreparedStatement stmt = mock(PreparedStatement.class);

    this.mapper.write(stmt, 1, null, -1);

    verify(stmt).setNull(1, -1);
  }

  public void shouldWriteBooleanToStatement() throws SQLException {
    PreparedStatement stmt = mock(PreparedStatement.class);

    this.mapper.write(stmt, 1, Boolean.TRUE, Types.BOOLEAN);

    verify(stmt).setBoolean(1, Boolean.TRUE);
  }

  public void shouldWriteStringToStatement() throws SQLException {
    PreparedStatement stmt = mock(PreparedStatement.class);

    this.mapper.write(stmt, 1, Boolean.TRUE, Types.VARCHAR);

    verify(stmt).setString(1, "Y");
  }

  public void shouldWriteIntToStatement() throws SQLException {
    PreparedStatement stmt = mock(PreparedStatement.class);

    this.mapper.write(stmt, 1, Boolean.TRUE, Types.NUMERIC);

    verify(stmt).setInt(1, 1);
  }

  public void shouldReadBooleanFromStatement() throws SQLException {
    CallableStatement stmt = mock(CallableStatement.class);
    when(stmt.getBoolean(1)).thenReturn(true);
    when(stmt.wasNull()).thenReturn(false);

    assertEquals(this.mapper.read(stmt, 1, Types.BOOLEAN), Boolean.TRUE);
  }

  public void shouldReadStringFromStatement() throws SQLException {
    CallableStatement stmt = mock(CallableStatement.class);
    when(stmt.getString(1)).thenReturn("Y");
    when(stmt.wasNull()).thenReturn(false);

    assertEquals(this.mapper.read(stmt, 1, Types.VARCHAR), Boolean.TRUE);
  }

  public void shouldReadIntFromStatement() throws SQLException {
    CallableStatement stmt = mock(CallableStatement.class);
    when(stmt.getInt(1)).thenReturn(1);
    when(stmt.wasNull()).thenReturn(false);

    assertEquals(this.mapper.read(stmt, 1, Types.DECIMAL), Boolean.TRUE);
  }

  public void shouldReadBooleanFromResultSet() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getBoolean(1)).thenReturn(true);
    when(rs.wasNull()).thenReturn(false);

    assertEquals(this.mapper.read(rs, 1, Types.BOOLEAN), Boolean.TRUE);
  }

  public void shouldReadStringFromResultSet() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getString(1)).thenReturn("Y");
    when(rs.wasNull()).thenReturn(false);

    assertEquals(this.mapper.read(rs, 1, Types.VARCHAR), Boolean.TRUE);
  }

  public void shouldReadIntFromResultSet() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getInt(1)).thenReturn(1);
    when(rs.wasNull()).thenReturn(false);

    assertEquals(this.mapper.read(rs, 1, Types.DECIMAL), Boolean.TRUE);
  }

  @DataProvider
  public Object[][] getSupportedSqlTypes() {
    return new Object[][] {
      { Types.VARCHAR },
      { Types.CHAR },
      { Types.DECIMAL },
      { Types.NUMERIC },
      { Types.INTEGER },
      { Types.SMALLINT },
      { Types.TINYINT }
    };
  }

}
