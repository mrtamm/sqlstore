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

package ws.rocket.sqlstore.test.types;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.types.LongMapper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests for the {@link LongMapper} class.
 */
@Test
public class LongMappingTest {

  private final LongMapper mapper = new LongMapper();

  public void shouldAcceptLong() {
    assertTrue(this.mapper.supports(long.class));
    assertTrue(this.mapper.supports(Long.class));
  }

  public void shouldDefaultToNumeric() {
    assertEquals(this.mapper.confirmSqlType(null), Types.NUMERIC);
  }

  public void shouldWriteLongToStatement() throws SQLException {
    PreparedStatement stmt = mock(PreparedStatement.class);

    this.mapper.write(stmt, 1, 2L, Types.NUMERIC);

    verify(stmt).setLong(1, 2);
  }

  public void shouldWriteNullToStatement() throws SQLException {
    PreparedStatement stmt = mock(PreparedStatement.class);

    this.mapper.write(stmt, 1, null, -1);

    verify(stmt).setNull(1, -1);
  }

  public void shouldReadLongFromStatement() throws SQLException {
    CallableStatement stmt = mock(CallableStatement.class);
    when(stmt.getLong(1)).thenReturn(2L);
    when(stmt.wasNull()).thenReturn(false);

    assertEquals(this.mapper.read(stmt, 1, Types.NUMERIC), Long.valueOf(2));
  }

  public void shouldReadNullFromStatement() throws SQLException {
    CallableStatement stmt = mock(CallableStatement.class);
    when(stmt.getLong(1)).thenReturn(2L);
    when(stmt.wasNull()).thenReturn(true);

    assertNull(this.mapper.read(stmt, 1, Types.NUMERIC));
  }

  public void shouldReadLongFromResultSet() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getLong(1)).thenReturn(2L);
    when(rs.wasNull()).thenReturn(false);

    assertEquals(this.mapper.read(rs, 1, Types.BOOLEAN), Long.valueOf(2));
  }

  public void shouldReadNullFromResultSet() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getLong(1)).thenReturn(2L);
    when(rs.wasNull()).thenReturn(true);

    assertNull(this.mapper.read(rs, 1, Types.NUMERIC));
  }

}
