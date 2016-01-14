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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.connection.ConnectionManager;
import ws.rocket.sqlstore.connection.ScopedConnectionManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

/**
 * Tests for {@link ScopedConnectionManager} class.
 */
@Test
public class ScopedConnectionManagerTest {

  @Test(expectedExceptions = NullPointerException.class)
  public void shouldFailWhenNoConnectionManagerOnContstruct() {
    assertNull(new ScopedConnectionManager(null));
  }

  public void shouldObtainAndReleaseConnection() throws SQLException {
    Connection con = mock(Connection.class);
    ConnectionManager inner = mock(ConnectionManager.class);
    ScopedConnectionManager manager = new ScopedConnectionManager(inner);

    when(inner.obtain(false)).thenReturn(con);

    try {
      Connection con2 = manager.obtain(true);
      assertSame(con, con2);

      verify(con).setAutoCommit(false);

      Connection con3 = manager.obtain(true);
      assertSame(con, con3);
    } finally {
      manager.releaseFinally();
      verify(inner).release();
    }
  }

  public void shouldCommit() throws SQLException {
    Connection con = mock(Connection.class);
    Savepoint savepoint = mock(Savepoint.class);
    ConnectionManager inner = mock(ConnectionManager.class);

    when(con.setSavepoint()).thenReturn(savepoint);
    when(inner.obtain(false)).thenReturn(con);

    ScopedConnectionManager manager = new ScopedConnectionManager(inner);

    try {
      manager.obtain(false);
      manager.commit();

      verify(con).releaseSavepoint(savepoint);
      verify(con, times(2)).setSavepoint();
    } finally {
      manager.release();
      verify(con, times(2)).releaseSavepoint(savepoint);
    }
  }

  public void shouldRollback() throws SQLException {
    Connection con = mock(Connection.class);
    Savepoint savepoint = mock(Savepoint.class);
    ConnectionManager inner = mock(ConnectionManager.class);

    when(con.setSavepoint()).thenReturn(savepoint);
    when(inner.obtain(false)).thenReturn(con);

    ScopedConnectionManager manager = new ScopedConnectionManager(inner);

    try {
      manager.obtain(false);
      manager.rollback();

      verify(con).rollback(savepoint);
    } finally {
      manager.releaseFinally();
    }
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void shouldFailCommitWithoutObtain() throws SQLException {
    ConnectionManager inner = mock(ConnectionManager.class);
    ScopedConnectionManager manager = new ScopedConnectionManager(inner);

    manager.commit();
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void shouldFailRollbackWithoutObtain() throws SQLException {
    ConnectionManager inner = mock(ConnectionManager.class);
    ScopedConnectionManager manager = new ScopedConnectionManager(inner);

    manager.rollback();
  }

}
