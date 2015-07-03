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

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import javax.sql.rowset.serial.SerialClob;

/**
 * Default value mapper for <code>java.lang.String</code>. This mapper does not make any
 * restrictions on SQL type but defaults to  <code>VARCHAR</code> when undefined.
 */
public final class StringMapper implements ValueMapper {

  @Override
  public boolean supports(Class<?> type) {
    return String.class == type;
  }

  @Override
  public int confirmSqlType(Integer providedType) {
    return providedType != null ? providedType : Types.VARCHAR;
  }

  @Override
  public void write(PreparedStatement ps, int index, Object value, int sqlType)
      throws SQLException {
    String str = (String) value;
    if (str == null) {
      ps.setNull(index, sqlType);
    } else if (sqlType == Types.CLOB || sqlType == Types.NCLOB) {
      ps.setClob(index, new SerialClob(str.toCharArray()));
    } else {
      ps.setString(index, str);
    }
  }

  @Override
  public String read(ResultSet rs, int index, int sqlType) throws SQLException {
    if (sqlType == Types.CLOB) {
      return readClob(rs.getClob(index));
    } else if (sqlType == Types.NCLOB) {
      return readClob(rs.getNClob(index));
    }

    return rs.getString(index);
  }

  @Override
  public String read(CallableStatement stmt, int index, int sqlType) throws SQLException {
    if (sqlType == Types.CLOB) {
      return readClob(stmt.getClob(index));
    } else if (sqlType == Types.NCLOB) {
      return readClob(stmt.getNClob(index));
    }

    return stmt.getString(index);
  }

  private String readClob(Clob clob) throws SQLException {
    if (clob == null || clob.length() == 0L) {
      return null;
    }

    int len = EnvSupport.getInstance().getLobSizeForArray(clob.length());
    return len < 0 ? null : clob.getSubString(0L, len);
  }

}
