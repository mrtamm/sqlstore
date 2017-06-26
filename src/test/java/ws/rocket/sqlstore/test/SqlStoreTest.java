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

package ws.rocket.sqlstore.test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import javax.sql.DataSource;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.Block;
import ws.rocket.sqlstore.SqlStore;
import ws.rocket.sqlstore.connection.SharedConnectionManager;
import ws.rocket.sqlstore.test.script.read.model.ValidTestModel;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link SqlStore} class.
 */
@Test
public final class SqlStoreTest {

  public void shouldCreateSqlStore() {
    assertNotNull(SqlStore.load(ValidTestModel.class));
    assertNotNull(SqlStore.load(ValidTestModel.class, mock(Connection.class)));
    assertNotNull(SqlStore.load(ValidTestModel.class, mock(DataSource.class)));
  }

  public void shouldCreateProxy() {
    ValidTestModel proxy1 = SqlStore.proxy(ValidTestModel.class);
    ValidTestModel proxy2 = SqlStore.proxy(ValidTestModel.class, mock(Connection.class));
    ValidTestModel proxy3 = SqlStore.proxy(ValidTestModel.class, mock(DataSource.class));

    assertNotNull(proxy1);
    assertNotNull(proxy2);
    assertNotNull(proxy3);

    assertEquals(proxy1, proxy1);
    assertNotEquals(proxy1, proxy2);

    assertEquals(proxy1.hashCode(), proxy1.hashCode());
    assertNotEquals(proxy1.hashCode(), proxy2.hashCode());

    assertEquals(proxy1.toString(), proxy2.toString());
  }

  public void shouldReportSize() {
    SqlStore store = SqlStore.load(ValidTestModel.class);
    assertEquals(store.size(), 1);
  }

  public void shouldContainQuery() {
    SqlStore store = SqlStore.load(ValidTestModel.class);
    assertTrue(store.hasQuery("testScript"));
  }

  public void shouldProvideQuery() {
    SqlStore store = SqlStore.load(ValidTestModel.class);
    SharedConnectionManager.register(mock(Connection.class));

    assertNotNull(store.query("testScript"));

    SharedConnectionManager.unregister();
  }

  @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp
      = "Unrecognized script name: noTestScript")
  public void shouldFailAtQueryDueToWrongScriptName() {
    SqlStore store = SqlStore.load(ValidTestModel.class);
    store.query("noTestScript");
  }

  public void shouldExecBlock() {
    SqlStore store = SqlStore.load(ValidTestModel.class);
    SharedConnectionManager.register(mock(Connection.class));

    Block block = mock(Block.class);
    store.execBlock(block);

    SharedConnectionManager.unregister();
    verify(block).execute(isA(SqlStore.class));
  }

  public void shouldExecBlockWithTransactionIsolation() {
    SqlStore store = SqlStore.load(ValidTestModel.class);
    SharedConnectionManager.register(mock(Connection.class));

    Block block = mock(Block.class);
    store.execBlock(block, Connection.TRANSACTION_NONE);

    SharedConnectionManager.unregister();
    verify(block).execute(isA(SqlStore.class));
  }

  public void shouldProvideInfoInToString() {
    SqlStore store = SqlStore.load(ValidTestModel.class);

    assertEquals(store.toString(), "SqlStore (1 scripts):\n"
        + "testScript\n"
        + "====\n"
        + "SELECT COUNT(*) FROM person\n"
        + "====\n"
        + "\n");
  }

  public void shouldPrintState() {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream(512);
    PrintStream out = new PrintStream(bytes);
    SqlStore store = SqlStore.load(ValidTestModel.class);

    store.printState(out);

    assertEquals(bytes.toString(), "testScript\n"
        + "====\n"
        + "SELECT COUNT(*) FROM person\n"
        + "====\n"
        + "\n");
  }

}
