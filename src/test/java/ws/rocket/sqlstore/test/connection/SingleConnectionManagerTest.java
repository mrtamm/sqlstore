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
import org.testng.annotations.Test;
import ws.rocket.sqlstore.connection.SingleConnectionManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

/**
 * Tests for {@link SingleConnectionManager} class.
 */
@Test
public final class SingleConnectionManagerTest {

  @Test(expectedExceptions = NullPointerException.class)
  public void shouldFailWhenNoConnectionOnContstruct() {
    assertNull(new SingleConnectionManager(null));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void shouldFailWhenClosedConnectionOnContstruct() throws SQLException {
    Connection con = mock(Connection.class);
    when(con.isClosed()).thenReturn(true);

    assertNull(new SingleConnectionManager(con));
  }

  public void shouldObtainConnection() throws SQLException {
    Connection con = mock(Connection.class);
    SingleConnectionManager manager = new SingleConnectionManager(con);

    try {
      Connection con2 = manager.obtain(true);
      assertSame(con, con2);

      verify(con).setReadOnly(true);
    } finally {
      manager.release();
    }
  }

  public void shouldCommit() throws SQLException {
    SingleConnectionManager manager = new SingleConnectionManager(mock(Connection.class));

    try {
      Connection con = manager.obtain(false);
      manager.commit();

      verify(con).commit();
    } finally {
      manager.release();
    }
  }

  public void shouldRollback() throws SQLException {
    SingleConnectionManager manager = new SingleConnectionManager(mock(Connection.class));

    try {
      Connection con = manager.obtain(false);
      manager.rollback();

      verify(con).rollback();
    } finally {
      manager.release();
    }
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void shouldFailCommitWithoutObtain() throws SQLException {
    Connection con = mock(Connection.class);
    SingleConnectionManager manager = new SingleConnectionManager(con);

    manager.commit();
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void shouldFailRollbackWithoutObtain() throws SQLException {
    Connection con = mock(Connection.class);
    SingleConnectionManager manager = new SingleConnectionManager(con);

    manager.rollback();
  }

}
