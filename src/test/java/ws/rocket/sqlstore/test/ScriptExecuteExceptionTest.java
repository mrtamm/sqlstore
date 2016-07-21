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

import java.sql.SQLException;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptExecuteException;
import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.Script;
import ws.rocket.sqlstore.script.read.ParamsSet;
import ws.rocket.sqlstore.script.sql.SqlScript;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

/**
 * Tests for {@link ScriptExecuteException} class.
 */
@Test
public final class ScriptExecuteExceptionTest {

  @Test
  public void testSimpleMessage() {
    ScriptExecuteException target = new ScriptExecuteException("%Sample message %.");

    assertNull(target.getCause());
    assertNull(target.getContext());
    assertEquals(target.getMessage(), "%Sample message %.");
    assertEquals(target.toString(),
        "ws.rocket.sqlstore.ScriptExecuteException: %Sample message %.\n");
  }

  @Test
  public void testNoMessage() {
    ScriptExecuteException target = new ScriptExecuteException(null);

    assertNull(target.getCause());
    assertNull(target.getContext());
    assertNotNull(target.getMessage());
    assertEquals(target.toString(),
        "ws.rocket.sqlstore.ScriptExecuteException: [no error description was given]\n");
  }

  @Test
  public void testMessageWithParams() {
    ScriptExecuteException target = new ScriptExecuteException("%s with %d.", "Message", 123);

    assertNull(target.getCause());
    assertNull(target.getContext());
    assertEquals(target.getMessage(), "Message with 123.");
    assertEquals(target.toString(),
        "ws.rocket.sqlstore.ScriptExecuteException: Message with 123.\n");
  }

  @Test
  public void testMessageWithParamsAndParentCause() {
    SQLException other = new SQLException("Other exception", "SQL state info", 1024);
    ScriptExecuteException target = new ScriptExecuteException(other, "%s with %d.", "Message", 12);

    assertSame(target.getCause(), other);
    assertNull(target.getContext());
    assertEquals(target.getMessage(), "Message with 12.");
    assertEquals(target.toString(), "ws.rocket.sqlstore.ScriptExecuteException: Message with 12.\n"
        + "SQL state: SQL state info\n"
        + "SQL error code: 1024\n"
        + "Cause 1. java.sql.SQLException: Other exception");
  }

  @Test
  public void testMessageWithParentCause() {
    SQLException other = new SQLException("Other exception", "SQL state info", 1024);
    ScriptExecuteException target = new ScriptExecuteException(other, "Sample Message.");

    assertSame(target.getCause(), other);
    assertNull(target.getContext());
    assertEquals(target.getMessage(), "Sample Message.");
    assertEquals(target.toString(), "ws.rocket.sqlstore.ScriptExecuteException: Sample Message.\n"
        + "SQL state: SQL state info\n"
        + "SQL error code: 1024\n"
        + "Cause 1. java.sql.SQLException: Other exception");
  }

  @Test
  public void testMessageWithQueryContext() {
    QueryContext context = createQueryContext();
    ScriptExecuteException target = new ScriptExecuteException(context, "Sample Message.");

    assertNull(target.getCause());
    assertSame(target.getContext(), context);
    assertEquals(target.getMessage(), "Sample Message.");
    assertEquals(target.toString(),
        "ws.rocket.sqlstore.ScriptExecuteException: Sample Message.\n");
  }

  @Test
  public void testWithParentCause() {
    SQLException other = new SQLException("Other exception", "SQL state info", 1024);
    ScriptExecuteException target = new ScriptExecuteException(other, (QueryContext) null);

    assertSame(target.getCause(), other);
    assertNull(target.getContext());
    assertEquals(target.getMessage(), "Other exception");
    assertEquals(target.toString(), "ws.rocket.sqlstore.ScriptExecuteException: Other exception\n"
        + "SQL state: SQL state info\n"
        + "SQL error code: 1024\n"
        + "Cause 1. java.sql.SQLException: Other exception");
  }

  @Test
  public void testWithQueryContext() {
    QueryContext context = createQueryContext();
    ScriptExecuteException target = new ScriptExecuteException(null, context);

    assertNull(target.getCause());
    assertSame(target.getContext(), context);
    assertNotNull(target.getMessage());
    assertEquals(target.toString(),
        "ws.rocket.sqlstore.ScriptExecuteException: [no error description was given]\n");
  }

  private static QueryContext createQueryContext() {
    ParamsSet params = new ParamsSet();
    params.initInOutUpdateParams();
    Script script = new Script("sample", 1, mock(SqlScript.class), params);
    return new QueryContext(script, new Object[0]);
  }

}
