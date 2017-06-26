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

import java.io.IOException;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import javax.sql.rowset.serial.SerialBlob;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.types.ByteArrayMapper;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static ws.rocket.sqlstore.test.types.ConversionHelper.asString;
import static ws.rocket.sqlstore.test.types.ConversionHelper.makeBlobBytes;

/**
 * Tests for the {@link ByteArrayMapper} class.
 */
@Test
public final class ByteArrayMappingTest {

  private final ByteArrayMapper mapper = new ByteArrayMapper();

  public void shouldAcceptByteArray() {
    mapper.supports(byte[].class);
  }

  public void shouldSupportLobs() {
    assertEquals(mapper.confirmSqlType(null), Types.BLOB);
    assertEquals(mapper.confirmSqlType(Types.BLOB), Types.BLOB);
    assertEquals(mapper.confirmSqlType(Types.BINARY), Types.BINARY);
    assertEquals(mapper.confirmSqlType(Types.VARBINARY), Types.VARBINARY);
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "byte\\[] value binding for SQL type #12 is not supported\\.")
  public void shouldRejectOtherTypes() {
    mapper.confirmSqlType(Types.VARCHAR);
  }

  public void shouldWriteNull() throws SQLException {
    PreparedStatement ps = mock(PreparedStatement.class);

    mapper.write(ps, 1, new byte[0], Types.BLOB);

    verify(ps).setNull(1, Types.BLOB);
  }

  public void shouldWriteBlob() throws SQLException {
    PreparedStatement ps = mock(PreparedStatement.class);

    mapper.write(ps, 1, "value".getBytes(), Types.BLOB);

    verify(ps).setBlob(eq(1), isA(SerialBlob.class));
  }

  public void shouldWriteBytes() throws SQLException {
    PreparedStatement ps = mock(PreparedStatement.class);
    byte[] value = "value".getBytes();

    mapper.write(ps, 1, value, Types.BINARY);

    verify(ps).setBytes(1, value);
  }

  public void shouldReadNullsFromCallableStatement() throws SQLException {
    CallableStatement ps = mock(CallableStatement.class);
    Blob blob = makeBlobBytes("");
    when(ps.getBlob(1)).thenReturn(blob);

    assertNull(mapper.read(ps, 1, Types.BLOB));
    assertNull(mapper.read(ps, 2, Types.BINARY));
  }

  public void shouldReadBytesFromCallableStatement() throws SQLException, IOException {
    CallableStatement ps = mock(CallableStatement.class);
    Blob blob = makeBlobBytes("blob value");

    when(ps.getBlob(1)).thenReturn(blob);
    when(ps.getBytes(2)).thenReturn("binary value".getBytes());

    assertEquals(asString(mapper.read(ps, 1, Types.BLOB)), "blob value");
    assertEquals(asString(mapper.read(ps, 2, Types.BINARY)), "binary value");
  }

  public void shouldReadNullsFromResultSet() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    Blob blob = makeBlobBytes("");
    when(rs.getBlob(1)).thenReturn(blob);

    assertNull(mapper.read(rs, 1, Types.BLOB));
    assertNull(mapper.read(rs, 2, Types.BINARY));
  }

  public void shouldReadStreamValuesFromResultSet() throws SQLException, IOException {
    ResultSet rs = mock(ResultSet.class);
    Blob blob = makeBlobBytes("blob value");

    when(rs.getBlob(1)).thenReturn(blob);
    when(rs.getBytes(2)).thenReturn("binary value".getBytes());

    assertEquals(asString(mapper.read(rs, 1, Types.BLOB)), "blob value");
    assertEquals(asString(mapper.read(rs, 2, Types.BINARY)), "binary value");
  }

}
