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

import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import javax.sql.rowset.serial.SerialBlob;
import ws.rocket.sqlstore.ScriptSetupException;

/**
 * Default value mapper for <code>byte[]</code>.
 * <p>
 * This mapper makes restrictions on SQL types:
 * <ol>
 * <li><code>BLOB</code> (default) &ndash; the upper limit of how much data is read from database is
 * restricted (all data might not fit into a byte array), however all the byte array content can be
 * stored within the BLOB in database.
 * <li><code>BINARY</code>, <code>VARBINARY</code>, <code>LONGVARBINARY</code> &ndash; the value
 * will be stored and read as an array;
 * <li><code>null</code> for <code>byte[]</code> values will always be read and stored as NULL;
 * <li>unmentioned SQL types will raise an {@link ScriptSetupException} when encountered.
 * </ol>
 * <p>
 * When data size increases above the limits of the byte array, alternatives for this type could be
 * <code>java.io.InputStream</code> and <code>java.io.File</code>.
 *
 * @see EnvSupport#getLobSizeForArray(long)
 * @see InputStreamMapper
 * @see FileMapper
 */
public final class ByteArrayMapper implements ValueMapper {

  @Override
  public boolean supports(Class<?> type) {
    return type == byte[].class;
  }

  @Override
  public int confirmSqlType(Integer providedType) {
    if (providedType == null) {
      providedType = Types.BLOB;
    } else if (providedType != Types.BLOB
        && providedType != Types.BINARY
        && providedType != Types.VARBINARY
        && providedType != Types.LONGVARBINARY) {
      throw new ScriptSetupException("byte[] value binding for SQL type #%d is not supported.",
          providedType);
    }
    return providedType;
  }

  @Override
  public void write(PreparedStatement ps, int index, Object value, int sqlType)
      throws SQLException {

    byte[] b = (byte[]) value;

    if (b == null || b.length == 0) {
      ps.setNull(index, sqlType);
    } else if (sqlType == Types.BLOB) {
      ps.setBlob(index, new SerialBlob(b));
    } else {
      ps.setBytes(index, b);
    }
  }

  @Override
  public byte[] read(ResultSet rs, int index, int sqlType) throws SQLException {
    if (sqlType == Types.BLOB) {
      return readBlob(rs.getBlob(index));
    }
    return rs.getBytes(index);
  }

  @Override
  public byte[] read(CallableStatement stmt, int index, int sqlType) throws SQLException {
    if (sqlType == Types.BLOB) {
      return readBlob(stmt.getBlob(index));
    }
    return stmt.getBytes(index);
  }

  private byte[] readBlob(Blob blob) throws SQLException {
    if (blob == null || blob.length() == 0) {
      return null;
    }

    int len = EnvSupport.getInstance().getLobSizeForArray(blob.length());
    return len < 0 ? null : blob.getBytes(0L, len);
  }

}
