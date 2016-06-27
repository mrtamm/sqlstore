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
import ws.rocket.sqlstore.result.ArrayResultsCollector;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Tests for the {@link ArrayResultsCollector} class.
 */
@Test
public class ArrayResultsCollectorTest {

  public void shouldReturnObjectArrayType() {
    ArrayResultsCollector collector = new ArrayResultsCollector(0);

    assertSame(collector.getType(), Object[][].class);
  }

  public void shouldWriteAndReadSameValue() {
    final String testValue1 = "Sample text";
    final Long testValue2 = 1024L;

    ArrayResultsCollector collector = new ArrayResultsCollector(2);

    collector.setRowValue(1, testValue1);
    collector.setRowValue(0, testValue2);

    assertSame(collector.getRowValue(0), testValue2);
    assertSame(collector.getRowValue(1), testValue1);
  }

  public void shouldCompleteRow() {
    ArrayResultsCollector collector = new ArrayResultsCollector(2);
    collector.setRowValue(1, "Sample Text");

    assertTrue(collector.isEmpty(), "Collector must be empty before first row completes.");
    collector.rowCompleted();
    assertFalse(collector.isEmpty(), "Collector must not be empty after first row completes.");
  }

  public void shouldReturnInsertedData() {
    ArrayResultsCollector collector = new ArrayResultsCollector(2);
    collector.setRowValue(1, "Sample Text");
    collector.rowCompleted();
    collector.setRowValue(0, 1L);
    collector.rowCompleted();

    Object[][] results = collector.getResult();

    assertEquals(results.length, 2, "The number of rows must match.");
    assertEquals(results[0].length, 2, "The number of columns must match.");
    assertEquals(results[1].length, 2, "The number of columns must match.");

    assertNull(results[0][0], "Checking the 1st column of the 1st row.");
    assertEquals(results[0][1], "Sample Text", "Checking the 2nd column of the 1st row.");

    assertEquals(results[1][0], 1L, "Checking the 1st column of the 2nd row.");
    assertNull(results[1][1], "Checking the 2nd column of the 2nd row.");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void shouldFailWhenWritingToBelowZeroIndex() {
    ArrayResultsCollector collector = new ArrayResultsCollector(2);
    collector.setRowValue(-1, "TEXT");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void shouldFailWhenWritingToGreaterThanDefinedIndex() {
    ArrayResultsCollector collector = new ArrayResultsCollector(2);
    collector.setRowValue(2, "TEXT");
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void shouldFailWhenOverWritingValue() {
    ArrayResultsCollector collector = new ArrayResultsCollector(2);
    collector.setRowValue(1, "TEXT1");
    collector.setRowValue(1, "TEXT2");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void shouldFailWhenReadingFromBelowZeroIndex() {
    ArrayResultsCollector collector = new ArrayResultsCollector(2);
    collector.getRowValue(-1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void shouldFailWhenReadingFromGreaterThanDefinedIndex() {
    ArrayResultsCollector collector = new ArrayResultsCollector(2);
    collector.getRowValue(2);
  }

}
