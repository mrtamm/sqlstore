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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Default value mapper for primitive <code>int</code> and objects of
 * <code>java.lang.Integer</code>. This mapper does not make any restrictions on SQL type but
 * defaults to <code>NUMERIC</code> when undefined.
 */
public final class IntMapper implements ValueMapper {

  @Override
  public boolean supports(Class<?> type) {
    return int.class == type || Integer.class == type;
  }

  @Override
  public int confirmSqlType(Integer providedType) {
    return providedType != null ? providedType : Types.NUMERIC;
  }

  @Override
  public void write(PreparedStatement ps, int index, Object value, int sqlType)
      throws SQLException {

    if (value == null) {
      ps.setNull(index, sqlType);
    } else {
      ps.setInt(index, (Integer) value);
    }
  }

  @Override
  public Integer read(ResultSet rs, int index, int sqlType) throws SQLException {
    int value = rs.getInt(index);
    return rs.wasNull() ? null : value;
  }

  @Override
  public Integer read(CallableStatement stmt, int index, int sqlType) throws SQLException {
    int value = stmt.getInt(index);
    return stmt.wasNull() ? null : value;
  }

}
