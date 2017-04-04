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

package ws.rocket.sqlstore.test.helper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Types;
import ws.rocket.sqlstore.script.QueryParam;
import ws.rocket.sqlstore.script.params.ParamMode;
import ws.rocket.sqlstore.script.params.TypeNameParam;
import ws.rocket.sqlstore.script.read.StreamReader;

/**
 * Helper class for producing instances of commonly used SqlStore data types. These methods are
 * targeted for tests.
 */
public final class Factory {

  public static StreamReader streamOf(String input) {
    try {
      byte[] bytes = input.getBytes("UTF-8");
      return new StreamReader(new ByteArrayInputStream(bytes));
    } catch (IOException e) {
      throw new RuntimeException("Failed to create sample StreamReader.", e);
    }
  }

  public static TypeNameParam typeParam(Class<?> type, Integer sqlType, String name) {
    return new TypeNameParam(type, sqlType, name);
  }

  public static TypeNameParam typeParam(Class<?> type, String name) {
    return typeParam(type, null, name);
  }

  public static TypeNameParam stringParam(String name) {
    return typeParam(String.class, Types.VARCHAR, name);
  }

  public static QueryParam queryStringParam(String name) {
    return new QueryParam(ParamMode.IN, stringParam(name));
  }

  public static QueryParam queryParam(Class<?> type, String name) {
    return new QueryParam(ParamMode.IN, typeParam(type, name));
  }

  private Factory() {
    throw new AssertionError("Cannot create an instance of this class");
  }

}
