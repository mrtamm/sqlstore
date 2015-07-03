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

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.rocket.sqlstore.ScriptSetupException;

/**
 * A mapper that converts BLOB, CLOB and NBLOB content into an {@link File}. To achieve this, the
 * LOB content will be temporarily stored on the disk. Unless the file is manually deleted by some
 * external code or user/system action, it will be removed from disk when the JVM exits. When the
 * LOB is empty, this mapper will return null.
 *
 * @see EnvSupport#copyToFile(InputStream, long)
 * @see EnvSupport#copyToFile(Reader, long)
 */
public final class FileMapper implements ValueMapper {

  private static final Logger LOG = LoggerFactory.getLogger(FileMapper.class);

  @Override
  public boolean supports(Class<?> type) {
    return File.class == type;
  }

  @Override
  public int confirmSqlType(Integer providedType) {
    if (providedType == null) {
      providedType = Types.BLOB;

    } else if (providedType != Types.BLOB
        && providedType != Types.CLOB
        && providedType != Types.NCLOB) {
      throw new ScriptSetupException("java.io.File value binding for SQL type #%d not supported.",
          providedType);
    }

    return providedType;
  }

  @Override
  public void write(PreparedStatement ps, int index, Object value, int sqlType)
      throws SQLException {
    File file = (File) value;

    try {
      if (!EnvSupport.isFileAvailable(file)) {
        ps.setNull(index, sqlType);
      } else if (sqlType == Types.BLOB) {
        ps.setBlob(index, EnvSupport.fileInStream(file));
      } else if (sqlType == Types.CLOB) {
        ps.setClob(index, EnvSupport.fileReader(file));
      } else if (sqlType == Types.NCLOB) {
        ps.setNClob(index, EnvSupport.fileReader(file));
      }
    } catch (FileNotFoundException e) {
      LOG.warn("File was unexpectectedly lost while attempting to read it.", e);
      throw new RuntimeException("File was unexpectectedly lost while attempting to read it.", e);
    }
  }

  @Override
  public File read(ResultSet rs, int index, int sqlType) throws SQLException {
    File result = null;

    if (sqlType == Types.BLOB) {
      result = copyBlob(rs.getBlob(index));
    } else if (sqlType == Types.CLOB) {
      result = copyClob(rs.getClob(index));
    } else if (sqlType == Types.NCLOB) {
      result = copyClob(rs.getNClob(index));
    }

    return result;
  }

  @Override
  public File read(CallableStatement stmt, int index, int sqlType) throws SQLException {
    File result = null;

    if (sqlType == Types.BLOB) {
      result = copyBlob(stmt.getBlob(index));
    } else if (sqlType == Types.CLOB) {
      result = copyClob(stmt.getClob(index));
    } else if (sqlType == Types.NCLOB) {
      result = copyClob(stmt.getNClob(index));
    }

    return result;
  }

  private File copyBlob(Blob blob) throws SQLException {
    if (blob == null || blob.length() == 0) {
      return null;
    }
    return EnvSupport.getInstance().copyToFile(blob.getBinaryStream(), blob.length());
  }

  private File copyClob(Clob clob) throws SQLException {
    if (clob == null || clob.length() == 0) {
      return null;
    }
    return EnvSupport.getInstance().copyToFile(clob.getCharacterStream(), clob.length());
  }

}
