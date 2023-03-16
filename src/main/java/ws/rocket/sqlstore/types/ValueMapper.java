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

/**
 * Contract methods for value mappers, classes that handle setting and reading JDBC statement
 * parameters and results for a specific Java type.
 *
 * <p>Value mappers must be thread-safe and only one instance of a mapper is usually used while the
 * JVM is running.
 *
 * <p>The methods of a value mapper are called in the following order (in a thread scope):
 *
 * <ol>
 * <li>{@link #supports(java.lang.Class)} – whenever the registry is asked to find a value
 * mapper for a Java type of a value;
 * <li>{@link #confirmSqlType(java.lang.Integer)} – during script parsing this method is used
 * to resolve or validate the SQL type of a parameter that has passed the <code>supports()</code>
 * method test;
 * <li>{@link #write(java.sql.PreparedStatement, int, java.lang.Object, int)} – for setting
 * each value in a prepared statement;
 * <li>{@link #read(java.sql.ResultSet, int, int)} – for reading each value from a result set;
 * <li>{@link #read(java.sql.CallableStatement, int, int)} – for reading each value for a
 * previously registered OUT-parameter.
 * </ol>
 */
public interface ValueMapper {

  /**
   * Informs whether the value mapper supports SQL parameters of given Java type.
   *
   * <p>When a mapper supports a type, it must be capable of setting a statement parameter or read a
   * result-set value of that type in corresponding methods of this class.
   *
   * @param type A Java class to be checked against.
   * @return A Boolean that is true when the Java type is supported by this mapper instance.
   */
  boolean supports(Class<?> type);

  /**
   * Confirms the SQL type is supported or provides a default SQL type.
   *
   * <p>The decision to raise an exception or just override it with default type when unsupported
   * SQL type is given is left to implementations. However, runtime exceptions are preferred to
   * avoid unexpected conditions in a JDBC statement and database.
   *
   * <p>This method is called only at script parsing (from an SQLS file) phase.
   *
   * @param providedType An optional SQL type from a Script to verify, or null.
   * @return The SQL type to use.
   */
  int confirmSqlType(Integer providedType);

  /**
   * Sets the given value at given index to the JDBC statement.
   *
   * @param ps The statement to update.
   * @param index The position of the parameter value to write to.
   * @param value The value to apply (may be null).
   * @param sqlType The SQL type of the value.
   * @throws SQLException When an unexpected problem occurs while setting the value.
   */
  void write(PreparedStatement ps, int index, Object value, int sqlType) throws SQLException;

  /**
   * Reads a value at given index from a JDBC result-set row.
   *
   * @param rs The result-set (row) to read.
   * @param index The position of the parameter value to read.
   * @param sqlType The SQL type of the value.
   * @return The extracted value.
   * @throws SQLException When an unexpected problem occurs while reading the value.
   */
  Object read(ResultSet rs, int index, int sqlType) throws SQLException;

  /**
   * Reads a value at given index from a JDBC callable statement.
   *
   * @param stmt The statement to read.
   * @param index The position of the parameter value to read.
   * @param sqlType The SQL type of the value.
   * @return The extracted value.
   * @throws SQLException When an unexpected problem occurs while reading the value.
   */
  Object read(CallableStatement stmt, int index, int sqlType) throws SQLException;

}
