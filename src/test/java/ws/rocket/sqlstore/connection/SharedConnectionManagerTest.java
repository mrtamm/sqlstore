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

package ws.rocket.sqlstore.connection;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.sql.Connection;
import javax.sql.DataSource;
import org.testng.annotations.Test;

/**
 * Tests for {@link SharedConnectionManager} class.
 */
@Test
public class SharedConnectionManagerTest {

  public void shouldReturnNullForInstance() {
    assertNull(SharedConnectionManager.getInstance());
  }

  public void shouldReturnConnectionManagerInstance() {
    assertNull(SharedConnectionManager.getInstance());

    try {
      SharedConnectionManager.register(mock(Connection.class));

      assertNotNull(SharedConnectionManager.getInstance());
      assertEquals(SharedConnectionManager.getInstance().getClass(), SingleConnectionManager.class);
    } finally {
      SharedConnectionManager.unregister();
      assertNull(SharedConnectionManager.getInstance());
    }
  }

  public void shouldReturnDataSourceManagerInstance() {
    assertNull(SharedConnectionManager.getInstance());

    try {
      SharedConnectionManager.register(mock(DataSource.class));

      assertNotNull(SharedConnectionManager.getInstance());
      assertEquals(SharedConnectionManager.getInstance().getClass(),
          DataSourceConnectionManager.class);
    } finally {
      SharedConnectionManager.unregister();
      assertNull(SharedConnectionManager.getInstance());
    }
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void shouldRegisterFailOnNoConnection() {
    assertNull(SharedConnectionManager.getInstance());

    SharedConnectionManager.register((Connection) null);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void shouldRegisterFailOnNoDataSource() {
    assertNull(SharedConnectionManager.getInstance());

    SharedConnectionManager.register((DataSource) null);
  }

}
