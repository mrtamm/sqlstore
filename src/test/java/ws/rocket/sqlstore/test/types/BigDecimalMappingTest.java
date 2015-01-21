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

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.types.BigDecimalMapper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Tests for the {@link BigDecimalMapper} class.
 */
@Test
public class BigDecimalMappingTest {

  private static final BigDecimal TEST_VALUE = new BigDecimal("3.14159265359");

  private final BigDecimalMapper mapper = new BigDecimalMapper();

  public void shouldAcceptBigDecimal() {
    assertTrue(this.mapper.supports(BigDecimal.class));
  }

  public void shouldDefaultToNumeric() {
    assertEquals(this.mapper.confirmSqlType(null), Types.NUMERIC);
  }

  public void shouldWriteBigDecimalToStatement() throws SQLException {
    PreparedStatement stmt = mock(PreparedStatement.class);

    this.mapper.write(stmt, 1, TEST_VALUE, -1);

    verify(stmt).setBigDecimal(1, TEST_VALUE);
  }

  public void shouldWriteNullToStatement() throws SQLException {
    PreparedStatement stmt = mock(PreparedStatement.class);

    this.mapper.write(stmt, 1, null, -1);

    verify(stmt).setBigDecimal(1, null);
  }

  public void shouldReadBigDecimalFromStatement() throws SQLException {
    CallableStatement stmt = mock(CallableStatement.class);
    when(stmt.getBigDecimal(1)).thenReturn(TEST_VALUE);

    assertSame(this.mapper.read(stmt, 1, -1), TEST_VALUE);
  }

  public void shouldReadNullFromStatement() throws SQLException {
    CallableStatement stmt = mock(CallableStatement.class);
    when(stmt.getBigDecimal(1)).thenReturn(null);

    assertNull(this.mapper.read(stmt, 1, -1));
  }

  public void shouldReadBigDecimalFromResultSet() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getBigDecimal(1)).thenReturn(TEST_VALUE);

    assertEquals(this.mapper.read(rs, 1, -1), TEST_VALUE);
  }

  public void shouldReadNullFromResultSet() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getBigDecimal(1)).thenReturn(null);

    assertNull(this.mapper.read(rs, 1, -1));
  }

}
