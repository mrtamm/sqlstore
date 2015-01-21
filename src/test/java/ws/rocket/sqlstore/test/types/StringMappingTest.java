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
import ws.rocket.sqlstore.types.StringMapper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests for the {@link StringMapper} class.
 */
@Test
public class StringMappingTest {

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

  public void shouldReadStringFromStatement() throws SQLException {
    CallableStatement stmt = mock(CallableStatement.class);
    when(stmt.getString(1)).thenReturn("expected value");

    assertEquals(this.mapper.read(stmt, 1, -1), "expected value");
  }

  public void shouldReadStringFromResultSet() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getString(1)).thenReturn("expected value");

    assertEquals(this.mapper.read(rs, 1, -1), "expected value");
  }

}
