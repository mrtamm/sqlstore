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

package ws.rocket.sqlstore.test.connection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.connection.DataSourceConnectionManager;

/**
 * Tests for {@link DataSourceConnectionManager} class.
 */
@Test
public final class DataSourceConnectionManagerTest {

  @Test(expectedExceptions = NullPointerException.class)
  public void shouldFailWhenNoConnectionOnContstruct() {
    assertNull(new DataSourceConnectionManager(null));
  }

  public void shouldObtainAndReleaseConnection() throws SQLException {
    DataSource ds = mock(DataSource.class);
    Connection con = mock(Connection.class);
    DataSourceConnectionManager manager = new DataSourceConnectionManager(ds);

    when(ds.getConnection()).thenReturn(con);

    try {
      Connection con2 = manager.obtain(true);
      assertSame(con, con2);

      verify(con).setReadOnly(true);

      Connection con3 = manager.obtain(true);
      assertSame(con, con3);
    } finally {
      manager.release();
      verify(con).close();
    }
  }

  public void shouldCommit() throws SQLException {
    DataSource ds = mock(DataSource.class);
    Connection con = mock(Connection.class);
    DataSourceConnectionManager manager = new DataSourceConnectionManager(ds);

    when(ds.getConnection()).thenReturn(con);

    try {
      manager.obtain(false);
      manager.commit();

      verify(con).commit();
    } finally {
      manager.release();
    }
  }

  public void shouldRollback() throws SQLException {
    DataSource ds = mock(DataSource.class);
    Connection con = mock(Connection.class);
    DataSourceConnectionManager manager = new DataSourceConnectionManager(ds);

    when(ds.getConnection()).thenReturn(con);

    try {
      manager.obtain(false);
      manager.rollback();

      verify(con).rollback();
    } finally {
      manager.release();
    }
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void shouldFailCommitWithoutObtain() {
    DataSource ds = mock(DataSource.class);
    DataSourceConnectionManager manager = new DataSourceConnectionManager(ds);

    manager.commit();
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void shouldFailRollbackWithoutObtain() {
    DataSource ds = mock(DataSource.class);
    DataSourceConnectionManager manager = new DataSourceConnectionManager(ds);

    manager.rollback();
  }

}
