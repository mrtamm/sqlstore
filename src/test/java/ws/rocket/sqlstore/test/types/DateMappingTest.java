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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.types.DateMapper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Tests for the {@link DateMapper} class.
 */
@Test
public class DateMappingTest {

  private static final int TIMESTAMP = 1417819819;

  private static final Date UTIL_DATE = new Date(TIMESTAMP);

  private final DateMapper mapper = new DateMapper();

  public void shouldAcceptUtilDate() {
    assertTrue(this.mapper.supports(Date.class));
  }

  @Test(dataProvider = "getDateTimeSqlTypes")
  public void shouldAcceptDateTimeSqlTypes(int sqlType) {
    assertEquals(this.mapper.confirmSqlType(sqlType), sqlType);
  }

  public void shouldDefaultToTimestamp() {
    assertEquals(this.mapper.confirmSqlType(null), Types.TIMESTAMP);
  }

  @Test(dataProvider = "getDateTimeSqlTypesAndSamples")
  public void shouldWriteToStatement(int sqlType, Object sampleValue) throws SQLException {
    PreparedStatement stmt = mock(PreparedStatement.class);

    this.mapper.write(stmt, 1, sampleValue, sqlType);

    if (sampleValue == null) {
      verify(stmt).setNull(1, sqlType);
    } else if (sqlType == Types.TIMESTAMP) {
      verify(stmt).setTimestamp(1, (java.sql.Timestamp) sampleValue);
    } else if (sqlType == Types.DATE) {
      verify(stmt).setDate(1, (java.sql.Date) sampleValue);
    } else if (sqlType == Types.TIME) {
      verify(stmt).setTime(1, (java.sql.Time) sampleValue);
    }
  }

  @Test(dataProvider = "getDateTimeSqlTypesAndSamples")
  public void shouldReadFromStatement(int sqlType, Object sampleValue) throws SQLException {
    CallableStatement stmt = mock(CallableStatement.class);

    if (sqlType == Types.TIMESTAMP) {
      when(stmt.getTimestamp(1)).thenReturn((java.sql.Timestamp) sampleValue);
    } else if (sqlType == Types.DATE) {
      when(stmt.getDate(1)).thenReturn((java.sql.Date) sampleValue);
    } else if (sqlType == Types.TIME) {
      when(stmt.getTime(1)).thenReturn((java.sql.Time) sampleValue);
    }

    Date read = this.mapper.read(stmt, 1, sqlType);

    assertSame(read, sampleValue);
  }

  @Test(dataProvider = "getDateTimeSqlTypesAndSamples")
  public void shouldReadFromResultSet(int sqlType, Object sampleValue) throws SQLException {
    ResultSet rs = mock(ResultSet.class);

    if (sqlType == Types.TIMESTAMP) {
      when(rs.getTimestamp(1)).thenReturn((java.sql.Timestamp) sampleValue);
    } else if (sqlType == Types.DATE) {
      when(rs.getDate(1)).thenReturn((java.sql.Date) sampleValue);
    } else if (sqlType == Types.TIME) {
      when(rs.getTime(1)).thenReturn((java.sql.Time) sampleValue);
    }

    Date read = this.mapper.read(rs, 1, sqlType);

    assertSame(read, sampleValue);
  }

  @DataProvider
  public Object[][] getDateTimeSqlTypes() {
    return new Object[][] {
      { Types.TIMESTAMP },
      { Types.DATE },
      { Types.TIME }
    };
  }

  @DataProvider
  public Object[][] getDateTimeSqlTypesAndSamples() {
    return new Object[][] {
      { Types.TIMESTAMP, new java.sql.Timestamp(TIMESTAMP) },
      { Types.DATE, new java.sql.Date(TIMESTAMP) },
      { Types.TIME, new java.sql.Time(TIMESTAMP) },
      { Types.TIMESTAMP, null },
      { Types.DATE, null },
      { Types.TIME, null },
      { -1, null }
    };
  }

}
