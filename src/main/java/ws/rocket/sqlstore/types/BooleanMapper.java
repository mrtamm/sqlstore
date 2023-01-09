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
import ws.rocket.sqlstore.ScriptSetupException;

/**
 * Default value mapper for primitive <code>boolean</code> and object
 * <code>java.lang.Boolean</code>.
 *
 * <p>This mapper makes restrictions on SQL types:
 * <ol>
 * <li><code>BOOLEAN</code> (default) &ndash; the value will be stored and read as it is;
 * <li><code>CHAR</code>, <code>VARCHAR</code> &ndash; the value will be stored as 'Y' for true and
 * 'N' for false, and the read value will be true when it is 'Y', otherwise false;
 * <li><code>NUMERIC</code>, <code>DECIMAL</code>, <code>INTEGER</code>, <code>SMALLINT</code>,
 * <code>TINYINT</code> &ndash; the value will be stored as 1 for true and 0 for false, and the read
 * value will be true when it is positive, otherwise false;
 * <li><code>null</code> for boolean values will always be read and stored as NULL;
 * <li>unmentioned SQL types will raise an {@link ScriptSetupException} when encountered.
 * </ol>
 */
public final class BooleanMapper implements ValueMapper {

  @Override
  public boolean supports(Class<?> type) {
    return boolean.class == type || Boolean.class == type;
  }

  @Override
  public int confirmSqlType(Integer providedType) {
    if (providedType == null) {
      providedType = Types.BOOLEAN;
    } else if (providedType != Types.VARCHAR && providedType != Types.CHAR
        && providedType != Types.DECIMAL && providedType != Types.NUMERIC
        && providedType != Types.INTEGER && providedType != Types.SMALLINT
        && providedType != Types.TINYINT && providedType != Types.BIT) {
      throw new ScriptSetupException("Boolean value binding for SQL type #%d is not supported.",
          providedType);
    }

    return providedType;
  }

  @Override
  public void write(PreparedStatement ps, int index, Object value, int sqlType)
      throws SQLException {

    Boolean b = (Boolean) value;

    if (b == null) {
      ps.setNull(index, sqlType);
    } else if (sqlType == Types.BOOLEAN) {
      ps.setBoolean(index, b);
    } else if (sqlType == Types.VARCHAR || sqlType == Types.CHAR) {
      ps.setString(index, b ? "Y" : "N");
    } else if (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC || sqlType == Types.INTEGER
        || sqlType == Types.SMALLINT || sqlType == Types.TINYINT || sqlType == Types.BIT) {
      ps.setInt(index, b ? 1 : 0);
    }
  }

  @Override
  public Boolean read(ResultSet rs, int index, int sqlType) throws SQLException {
    Boolean b = null;

    if (sqlType == Types.BOOLEAN) {
      b = rs.getBoolean(index);
      b = rs.wasNull() ? null : b;

    } else if (sqlType == Types.VARCHAR || sqlType == Types.CHAR) {
      b = "Y".equals(rs.getString(index));
      b = rs.wasNull() ? null : b;

    } else if (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC || sqlType == Types.INTEGER
        || sqlType == Types.SMALLINT || sqlType == Types.TINYINT || sqlType == Types.BIT) {
      int value = rs.getInt(index);
      b = rs.wasNull() ? null : (value > 0);
    }

    return b;
  }

  @Override
  public Boolean read(CallableStatement stmt, int index, int sqlType) throws SQLException {
    Boolean b = null;

    if (sqlType == Types.BOOLEAN) {
      b = stmt.getBoolean(index);
      b = stmt.wasNull() ? null : b;

    } else if (sqlType == Types.VARCHAR || sqlType == Types.CHAR) {
      b = "Y".equals(stmt.getString(index));

    } else if (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC || sqlType == Types.INTEGER
        || sqlType == Types.SMALLINT || sqlType == Types.TINYINT || sqlType == Types.BIT) {
      int value = stmt.getInt(index);
      b = stmt.wasNull() ? null : (value > 0);
    }

    return b;
  }

}
