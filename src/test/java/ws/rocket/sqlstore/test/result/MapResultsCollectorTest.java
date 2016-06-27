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

import java.util.Map;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.result.MapResultsCollector;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Tests for the {@link MapResultsCollector} class.
 */
@Test
public class MapResultsCollectorTest {

  public void shouldReturnMapType() {
    MapResultsCollector collector = new MapResultsCollector();

    assertSame(collector.getType(), Map.class);
  }

  public void shouldWriteAndReadSameValue() {
    final String testValue1 = "Sample text";
    final Long testValue2 = 123L;

    MapResultsCollector collector = new MapResultsCollector();
    collector.setRowValue(1, testValue1);
    collector.setRowValue(0, testValue2);

    assertSame(collector.getRowValue(0), testValue2);
    assertSame(collector.getRowValue(1), testValue1);
  }

  public void shouldCompleteRow() {
    MapResultsCollector collector = new MapResultsCollector();
    collector.setRowValue(0, "Sample Text");

    assertTrue(collector.isEmpty(), "Collector must be empty before first row completes.");
    collector.rowCompleted();
    assertFalse(collector.isEmpty(), "Collector must not be empty after first row completes.");
  }

  public void shouldReturnInsertedData() {
    MapResultsCollector collector = new MapResultsCollector();
    collector.setRowValue(0, "Sample Text");
    collector.rowCompleted();
    collector.setRowValue(1, 123L);
    collector.rowCompleted();

    Map<Object, Object> results = collector.getResult();

    assertEquals(results.size(), 2, "The number of rows must match.");

    assertTrue(results.containsKey("Sample Text"), "Checking the key of the 1st row.");
    assertNull(results.get("Sample Text"), "Checking the value of the 1st row.");

    assertTrue(results.containsKey(null), "Checking the key of the 2nd row.");
    assertEquals(results.get(null), 123L, "Checking the value of the 2nd row.");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void shouldFailWhenWritingToBelowZeroIndex() {
    MapResultsCollector collector = new MapResultsCollector();
    collector.setRowValue(-1, "TEXT");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void shouldFailWhenWritingToGreaterThanOneIndex() {
    MapResultsCollector collector = new MapResultsCollector();
    collector.setRowValue(2, "TEXT");
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void shouldFailWhenOverWritingKey() {
    MapResultsCollector collector = new MapResultsCollector();
    collector.setRowValue(0, "TEXT1");
    collector.setRowValue(0, "TEXT2");
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void shouldFailWhenOverWritingValue() {
    MapResultsCollector collector = new MapResultsCollector();
    collector.setRowValue(1, "TEXT1");
    collector.setRowValue(1, "TEXT2");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void shouldFailWhenReadingFromBelowZeroIndex() {
    MapResultsCollector collector = new MapResultsCollector();
    collector.getRowValue(-1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void shouldFailWhenReadingFromGreaterThanOneIndex() {
    MapResultsCollector collector = new MapResultsCollector();
    collector.getRowValue(2);
  }

}
