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

package ws.rocket.sqlstore.script.sql;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

/**
 * Tests the {@link ConditionAlways} class.
 */
@Test
public class ConditionAlwaysTest {

  public void shouldAlwaysApply() {
    assertTrue(ConditionAlways.INSTANCE.isApplicable(null));
  }

  public void shouldProvideCustomToString() {
    assertEquals(ConditionAlways.INSTANCE.toString(), "always");
  }

}
