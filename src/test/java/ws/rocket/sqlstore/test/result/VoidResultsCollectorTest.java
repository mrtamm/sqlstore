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

package ws.rocket.sqlstore.test.result;

import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptExecuteException;
import ws.rocket.sqlstore.result.VoidResultsCollector;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Tests for the {@link VoidResultsCollector} class.
 */
@Test
public class VoidResultsCollectorTest {

  public void shouldReturnVoidType() {
    VoidResultsCollector collector = new VoidResultsCollector();

    assertSame(collector.getType(), Void.class);
  }

  @Test(expectedExceptions = ScriptExecuteException.class)
  public void shouldFailToWriteValue() {
    VoidResultsCollector collector = new VoidResultsCollector();
    collector.setRowValue(0, "Sample text");
  }

  public void shouldAlwaysReadNullValue() {
    VoidResultsCollector collector = new VoidResultsCollector();

    for (int i = -2; i < 3; i++) {
      assertNull(collector.getRowValue(i));
    }
  }

  public void shouldNeverCompleteRow() {
    VoidResultsCollector collector = new VoidResultsCollector();

    assertTrue(collector.isEmpty(), "Collector must be empty before first row completes.");
    collector.rowCompleted();
    assertTrue(collector.isEmpty(), "Collector must be empty after first row completes.");
  }

  public void shouldReportEmpty() {
    VoidResultsCollector collector = new VoidResultsCollector();
    assertTrue(collector.isEmpty());
  }

  public void shouldReturnNullResults() {
    VoidResultsCollector collector = new VoidResultsCollector();
    collector.rowCompleted();

    assertNull(collector.getResult());
  }

}
