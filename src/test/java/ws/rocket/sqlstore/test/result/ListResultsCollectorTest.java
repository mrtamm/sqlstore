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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.List;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.result.ListResultsCollector;

/**
 * Tests for the {@link ListResultsCollector} class.
 */
@Test
public final class ListResultsCollectorTest {

  public void shouldReturnObjectArrayType() {
    ListResultsCollector collector = new ListResultsCollector();

    assertSame(collector.getType(), List.class);
  }

  public void shouldWriteAndReadSameValue() {
    final String testValue = "Sample text";

    ListResultsCollector collector = new ListResultsCollector();
    collector.setRowValue(0, testValue);

    assertSame(collector.getRowValue(0), testValue);
  }

  public void shouldCompleteRow() {
    ListResultsCollector collector = new ListResultsCollector();
    collector.setRowValue(0, "Sample Text");

    assertTrue(collector.isEmpty(), "Collector must be empty before first row completes.");
    collector.rowCompleted();
    assertFalse(collector.isEmpty(), "Collector must not be empty after first row completes.");
  }

  public void shouldReturnInsertedData() {
    ListResultsCollector collector = new ListResultsCollector();
    collector.setRowValue(0, "Sample Text");
    collector.rowCompleted();
    collector.setRowValue(0, 1L);
    collector.rowCompleted();

    List<Object> results = collector.getResult();

    assertEquals(results.size(), 2, "The number of rows must match.");

    assertEquals(results.get(0), "Sample Text", "Checking the 1st row.");
    assertEquals(results.get(1), 1L, "Checking the 2nd row.");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void shouldFailWhenWritingToBelowZeroIndex() {
    ListResultsCollector collector = new ListResultsCollector();
    collector.setRowValue(-1, "TEXT");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void shouldFailWhenWritingToGreaterThanZeroIndex() {
    ListResultsCollector collector = new ListResultsCollector();
    collector.setRowValue(1, "TEXT");
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void shouldFailWhenOverWritingValue() {
    ListResultsCollector collector = new ListResultsCollector();
    collector.setRowValue(0, "TEXT1");
    collector.setRowValue(0, "TEXT2");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void shouldFailWhenReadingFromBelowZeroIndex() {
    ListResultsCollector collector = new ListResultsCollector();
    collector.getRowValue(-1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void shouldFailWhenReadingFromGreaterThanZeroIndex() {
    ListResultsCollector collector = new ListResultsCollector();
    collector.getRowValue(1);
  }

}
