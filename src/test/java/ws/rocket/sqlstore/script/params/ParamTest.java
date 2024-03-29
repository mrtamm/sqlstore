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

package ws.rocket.sqlstore.script.params;

import static org.testng.Assert.assertEquals;

/**
 * Common code for verifying the properties of abstract class {@link Param}.
 */
abstract class ParamTest {

  void checkTypes(Param param, Class<?> javaType, Integer sqlType) {
    assertEquals(param.getJavaType(), javaType, "Expected parameter Java type to match");
    assertEquals(param.getSqlType(), sqlType, "Expected parameter SQL type to match");
  }

}
