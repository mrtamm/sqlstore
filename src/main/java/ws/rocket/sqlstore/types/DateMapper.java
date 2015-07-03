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
import java.util.Date;
import ws.rocket.sqlstore.ScriptSetupException;

/**
 * Default value mapper for <code>java.util.Date</code> and its subclasses in <code>javax.sql</code>
 * package. This mapper does not support other date-related classes, e.g. Calendar.
 * <p>
 * This mapper allows only following SQL types:
 * <ol>
 * <li><code>TIMESTAMP (default)</code>;
 * <li><code>DATE</code>;
 * <li><code>TIME</code>.
 * </ol>
 * <p>
 * Unmentioned SQL types will raise an {@link ScriptSetupException} when encountered.
 */
public final class DateMapper implements ValueMapper {

  @Override
  public boolean supports(Class<?> type) {
    return Date.class.isAssignableFrom(type);
  }

  @Override
  public int confirmSqlType(Integer providedType) {
    int result = providedType != null ? providedType : Types.TIMESTAMP;

    if (result != Types.TIMESTAMP && result != Types.DATE && result != Types.TIME) {
      throw new ScriptSetupException("Cannot use SQL type #%d with java.util.Date.", providedType);
    }

    return result;
  }

  @Override
  public void write(PreparedStatement ps, int index, Object value, int sqlType)
      throws SQLException {

    long time = value == null ? -1 : ((Date) value).getTime();

    if (value == null) {
      ps.setNull(index, sqlType);
    } else if (sqlType == Types.TIMESTAMP) {
      ps.setTimestamp(index, new java.sql.Timestamp(time));
    } else if (sqlType == Types.DATE) {
      ps.setDate(index, new java.sql.Date(time));
    } else if (sqlType == Types.TIME) {
      ps.setTime(index, new java.sql.Time(time));
    }
  }

  @Override
  public Date read(ResultSet rs, int index, int sqlType) throws SQLException {
    Date result = null;

    if (sqlType == Types.TIMESTAMP) {
      result = rs.getTimestamp(index);
    } else if (sqlType == Types.DATE) {
      result = rs.getDate(index);
    } else if (sqlType == Types.TIME) {
      result = rs.getTime(index);
    }

    return result;
  }

  @Override
  public Date read(CallableStatement stmt, int index, int sqlType) throws SQLException {
    Date result = null;

    if (sqlType == Types.TIMESTAMP) {
      result = stmt.getTimestamp(index);
    } else if (sqlType == Types.DATE) {
      result = stmt.getDate(index);
    } else if (sqlType == Types.TIME) {
      result = stmt.getTime(index);
    }

    return result;
  }

}
