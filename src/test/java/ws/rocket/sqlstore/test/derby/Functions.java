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

package ws.rocket.sqlstore.test.derby;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Implementation for database functions used within Derby database testing.
 */
public final class Functions {

  /**
   * Returns the DATE_CREATED field value of a PERSON row with given ID.
   *
   * @param personId The ID of the person.
   * @return The timestamp of the DATE_CREATED field, or null when not found.
   * @throws SQLException While communicating with the database.
   */
  public static Timestamp findDateCreated(int personId) throws SQLException {
    Timestamp result = null;

    try (
        Connection con = DriverManager.getConnection("jdbc:default:connection");
        PreparedStatement ps = con.prepareStatement("SELECT date_created FROM person WHERE id=?")) {
      ps.setInt(1, personId);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          result = rs.getTimestamp(1);
        }
      }

    }

    return result;
  }

  private Functions() {
    throw new AssertionError("Cannot create an instance of this class");
  }

}
