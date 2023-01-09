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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptSetupException;

/**
 * Tests for {@link ScriptSetupException} class.
 */
@Test
public final class ScriptSetupExceptionTest {

  @Test
  public void testSimpleMessage() {
    ScriptSetupException target = new ScriptSetupException("%Sample message %.");

    assertEquals(target.getMessage(), "%Sample message %.");
    assertNull(target.getCause());
  }

  @Test
  public void testNoMessage() {
    ScriptSetupException target = new ScriptSetupException(null);

    assertNotNull(target.getMessage());
    assertNull(target.getCause());
  }

  @Test
  public void testMessageWithParams() {
    ScriptSetupException target = new ScriptSetupException("%s with %d.", "Message", 123);

    assertEquals(target.getMessage(), "Message with 123.");
    assertNull(target.getCause());
  }

  @Test
  public void testMessageWithParamsAndParentCause() {
    RuntimeException other = new RuntimeException("Other exception");
    ScriptSetupException target = new ScriptSetupException(other, "%s with %d.", "Message", 123);

    assertEquals(target.getMessage(), "Message with 123.");
    assertSame(target.getCause(), other);
  }

}
