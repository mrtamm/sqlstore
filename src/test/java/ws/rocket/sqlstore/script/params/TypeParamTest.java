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

import java.sql.Types;
import java.util.List;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.QueryContext;
import ws.rocket.sqlstore.helper.ScriptBuilder;

/**
 * Tests the {@link TypeParam} class.
 */
@Test
public final class TypeParamTest extends ParamTest {

  public void shouldStoreParamProperties() {
    TypeParam param = new TypeParam(String.class, Types.VARCHAR, 0);
    checkTypes(param, String.class, Types.VARCHAR);
  }

  @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp
      = "Detected illegal attempt to read result parameter")
  public void shouldFailToRead() {
    TypeParam param = new TypeParam(String.class, Types.VARCHAR, 0);
    param.read(null);
  }

  public void shouldWriteParamValue() {
    QueryContext queryCtx = new ScriptBuilder()
        .addOutParam(String.class, null)
        .toQueryContext();
    queryCtx.initResultsContainer(List.class, String.class);

    TypeParam param = new TypeParam(String.class, Types.VARCHAR, 0);
    param.write(queryCtx, "some value");

    assertEquals(queryCtx.getResultsCollector().getRowValue(0), "some value");
  }

  public void shouldProvideToString() {
    TypeParam param = new TypeParam(String.class, Types.VARCHAR, 0);
    assertEquals(param.toString(), "String|12");
  }

}
