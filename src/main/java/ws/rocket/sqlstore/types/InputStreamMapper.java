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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import ws.rocket.sqlstore.ScriptSetupException;

/**
 * A mapper that converts BLOB, CLOB and NBLOB into an {@link InputStream}. To achieve this, the LOB
 * content will be temporarily stored on the disk. The file will be deleted once the stream is
 * closed, or when the JVM exits. When the LOB is empty, this mapper will return null.
 *
 * @see EnvSupport#copyToFileForStream(InputStream, long)
 * @see EnvSupport#copyToFileForStream(Reader, long)
 */
public class InputStreamMapper implements ValueMapper {

  @Override
  public boolean supports(Class<?> type) {
    return InputStream.class.isAssignableFrom(type);
  }

  @Override
  public int confirmSqlType(Integer providedType) {
    if (providedType == null) {
      providedType = Types.BLOB;

    } else if (providedType != Types.BLOB
        && providedType != Types.CLOB
        && providedType != Types.NCLOB) {
      throw new ScriptSetupException(
          "java.io.InputStream value binding for SQL type #%d is not supported.", providedType);
    }

    return providedType;
  }

  @Override
  public void write(PreparedStatement ps, int index, Object value, int sqlType)
      throws SQLException {
    InputStream is = (InputStream) value;

    if (is == null) {
      ps.setNull(index, sqlType);
    } else if (sqlType == Types.BLOB) {
      ps.setBlob(index, is);
    } else if (sqlType == Types.CLOB) {
      ps.setClob(index, new InputStreamReader(is));
    } else if (sqlType == Types.NCLOB) {
      ps.setNClob(index, new InputStreamReader(is));
    }
  }

  @Override
  public InputStream read(CallableStatement stmt, int index, int sqlType) throws SQLException {
    InputStream result = null;

    if (sqlType == Types.BLOB) {
      result = copyBlobForStream(stmt.getBlob(index));
    } else if (sqlType == Types.CLOB) {
      result = copyClobForStream(stmt.getClob(index));
    } else if (sqlType == Types.NCLOB) {
      result = copyClobForStream(stmt.getNClob(index));
    }

    return result;
  }

  @Override
  public InputStream read(ResultSet rs, int index, int sqlType) throws SQLException {
    InputStream result = null;

    if (sqlType == Types.BLOB) {
      result = copyBlobForStream(rs.getBlob(index));
    } else if (sqlType == Types.CLOB) {
      result = copyClobForStream(rs.getClob(index));
    } else if (sqlType == Types.NCLOB) {
      result = copyClobForStream(rs.getNClob(index));
    }

    return result;
  }

  private InputStream copyBlobForStream(Blob blob) throws SQLException {
    if (blob == null || blob.length() == 0) {
      return null;
    }
    return EnvSupport.getInstance().copyToFileForStream(blob.getBinaryStream(), blob.length());
  }

  private InputStream copyClobForStream(Clob clob) throws SQLException {
    if (clob == null || clob.length() == 0) {
      return null;
    }
    return EnvSupport.getInstance().copyToFileForStream(clob.getCharacterStream(), clob.length());
  }

}
