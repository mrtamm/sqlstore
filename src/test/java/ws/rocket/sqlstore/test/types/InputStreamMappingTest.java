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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static ws.rocket.sqlstore.test.types.ConversionHelper.asString;
import static ws.rocket.sqlstore.test.types.ConversionHelper.makeBlob;
import static ws.rocket.sqlstore.test.types.ConversionHelper.makeClob;
import static ws.rocket.sqlstore.test.types.ConversionHelper.makeNclob;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.types.InputStreamMapper;

/**
 * Tests for the {@link InputStreamMapper} class.
 */
@Test
public final class InputStreamMappingTest {

  private final InputStreamMapper mapper = new InputStreamMapper();

  public void shouldAcceptInputStream() {
    mapper.supports(InputStream.class);
    mapper.supports(FileInputStream.class);
  }

  public void shouldSupportLobs() {
    assertEquals(mapper.confirmSqlType(null), Types.BLOB);
    assertEquals(mapper.confirmSqlType(Types.BLOB), Types.BLOB);
    assertEquals(mapper.confirmSqlType(Types.CLOB), Types.CLOB);
    assertEquals(mapper.confirmSqlType(Types.NCLOB), Types.NCLOB);
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "java\\.io\\.InputStream value binding for SQL type #12 is not supported\\.")
  public void shouldRejectOtherTypes() {
    mapper.confirmSqlType(Types.VARCHAR);
  }

  public void shouldWriteNull() throws SQLException {
    PreparedStatement ps = mock(PreparedStatement.class);

    mapper.write(ps, 1, null, Types.BLOB);

    verify(ps).setNull(1, Types.BLOB);
  }

  public void shouldWriteBlobInputStream() throws SQLException {
    PreparedStatement ps = mock(PreparedStatement.class);
    InputStream value = mock(InputStream.class);

    mapper.write(ps, 1, value, Types.BLOB);

    verify(ps).setBlob(1, value);
  }

  public void shouldWriteClobInputStream() throws SQLException {
    PreparedStatement ps = mock(PreparedStatement.class);

    mapper.write(ps, 1, mock(InputStream.class), Types.CLOB);

    verify(ps).setClob(eq(1), isA(InputStreamReader.class));
  }

  public void shouldWriteNclobInputStream() throws SQLException {
    PreparedStatement ps = mock(PreparedStatement.class);

    mapper.write(ps, 1, mock(InputStream.class), Types.NCLOB);

    verify(ps).setNClob(eq(1), isA(InputStreamReader.class));
  }

  public void shouldReadNullsFromCallableStatement() throws SQLException {
    CallableStatement ps = mock(CallableStatement.class);
    Blob blob = makeBlob("");
    Clob clob = makeClob("");
    NClob nclob = makeNclob("");

    when(ps.getBlob(1)).thenReturn(blob);
    when(ps.getClob(2)).thenReturn(clob);
    when(ps.getNClob(3)).thenReturn(nclob);

    assertNull(mapper.read(ps, 1, Types.BLOB));
    assertNull(mapper.read(ps, 2, Types.CLOB));
    assertNull(mapper.read(ps, 3, Types.NCLOB));
  }

  public void shouldReadStreamValuesFromCallableStatement() throws SQLException, IOException {
    CallableStatement ps = mock(CallableStatement.class);
    Blob blob = makeBlob("blob value");
    Clob clob = makeClob("clob value");
    NClob nclob = makeNclob("nclob value");

    when(ps.getBlob(1)).thenReturn(blob);
    when(ps.getClob(2)).thenReturn(clob);
    when(ps.getNClob(3)).thenReturn(nclob);

    assertEquals(asString(mapper.read(ps, 1, Types.BLOB)), "blob value");
    assertEquals(asString(mapper.read(ps, 2, Types.CLOB)), "clob value");
    assertEquals(asString(mapper.read(ps, 3, Types.NCLOB)), "nclob value");
  }

  public void shouldReadNullsFromResultSet() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    Blob blob = makeBlob("");
    Clob clob = makeClob("");
    NClob nclob = makeNclob("");

    when(rs.getBlob(1)).thenReturn(blob);
    when(rs.getClob(2)).thenReturn(clob);
    when(rs.getNClob(3)).thenReturn(nclob);

    assertNull(mapper.read(rs, 1, Types.BLOB));
    assertNull(mapper.read(rs, 2, Types.CLOB));
    assertNull(mapper.read(rs, 3, Types.NCLOB));
  }

  public void shouldReadStreamValuesFromResultSet() throws SQLException, IOException {
    ResultSet rs = mock(ResultSet.class);
    Blob blob = makeBlob("blob value");
    Clob clob = makeClob("clob value");
    NClob nclob = makeNclob("nclob value");

    when(rs.getBlob(1)).thenReturn(blob);
    when(rs.getClob(2)).thenReturn(clob);
    when(rs.getNClob(3)).thenReturn(nclob);

    assertEquals(asString(mapper.read(rs, 1, Types.BLOB)), "blob value");
    assertEquals(asString(mapper.read(rs, 2, Types.CLOB)), "clob value");
    assertEquals(asString(mapper.read(rs, 3, Types.NCLOB)), "nclob value");
  }

}
