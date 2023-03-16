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

package ws.rocket.sqlstore.script.read.block;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;
import ws.rocket.sqlstore.script.read.block.SqlBuffer.ParseEvent;

/**
 * Tests the {@link SqlBuffer} class.
 */
@Test
public class SqlBufferTest {

  private static boolean applyChars(SqlBuffer buffer, String text) {
    boolean canContinue = true;

    for (int i = 0; canContinue && i < text.length(); i++) {
      canContinue = buffer.next(text.codePointAt(i), i + 1);
    }

    return canContinue;
  }

  public void testSimpleMode() {
    SqlBuffer buffer = new SqlBuffer();
    boolean canContinue = applyChars(buffer, "SELECT 1");

    assertTrue(canContinue);
    assertEquals(buffer.getLastEvent(), ParseEvent.NONE);
    assertEquals(buffer.resetSqlContent(), "SELECT 1");

    canContinue = buffer.next(-1, 1);
    assertFalse(canContinue);
  }

  public void testEscapeModeQuertionMark() {
    SqlBuffer buffer = new SqlBuffer();
    boolean canContinue = applyChars(buffer, "\\?");
    expectContent(buffer, canContinue, "?");
  }

  public void testEscapeModeCurlyBraceOpen() {
    SqlBuffer buffer = new SqlBuffer();
    boolean canContinue = applyChars(buffer, "\\{");
    expectContent(buffer, canContinue, "{");
  }

  public void testEscapeModeCurlyBraceClose() {
    SqlBuffer buffer = new SqlBuffer();
    boolean canContinue = applyChars(buffer, "\\}");
    expectContent(buffer, canContinue, "}");
  }

  public void testEscapeModeNotSupported() {
    SqlBuffer buffer = new SqlBuffer();
    boolean canContinue = applyChars(buffer, "\\(");
    expectContent(buffer, canContinue, "\\(");
  }

  public void testExpressionModeFalse() {
    SqlBuffer buffer = new SqlBuffer();
    boolean canContinue = applyChars(buffer, "?(");
    expectContent(buffer, canContinue, "?(");
  }

  public void testExpressionModeTrue() {
    SqlBuffer buffer = new SqlBuffer();
    boolean canContinue = applyChars(buffer, "?{");

    assertFalse(canContinue);
    assertEquals(buffer.getLastEvent(), ParseEvent.EXPRESSION);
    assertEquals(buffer.resetSqlContent(), "?");
  }

  public void testConditionModeFalse() {
    SqlBuffer buffer = new SqlBuffer();
    boolean canContinue = applyChars(buffer, "! ");
    expectContent(buffer, canContinue, "! ");
  }

  public void testConditionModeTrue() {
    SqlBuffer buffer = new SqlBuffer();
    boolean canContinue = applyChars(buffer, "!(");

    assertFalse(canContinue);
    assertEquals(buffer.getLastEvent(), ParseEvent.CONDITION);
    assertEquals(buffer.resetSqlContent(), "");

    canContinue = applyChars(buffer, "}");

    assertFalse(canContinue);
    assertEquals(buffer.getLastEvent(), ParseEvent.END_BLOCK);
    assertEquals(buffer.resetSqlContent(), "");
  }

  public void testEndModeFalse() {
    SqlBuffer buffer = new SqlBuffer();
    boolean canContinue = applyChars(buffer, "=== ");
    expectContent(buffer, canContinue, "=== ");
  }

  public void testEndModeTrue() {
    SqlBuffer buffer = new SqlBuffer();
    boolean canContinue = applyChars(buffer, "====ABC\n");

    assertFalse(canContinue);
    assertEquals(buffer.getLastEvent(), ParseEvent.END_SCRIPT);
    assertEquals(buffer.resetSqlContent(), "");
  }

  private void expectContent(SqlBuffer buffer, boolean canContinue, String content) {
    assertTrue(canContinue);
    assertEquals(buffer.getLastEvent(), ParseEvent.NONE);
    assertEquals(buffer.resetSqlContent(), content);
  }

}
