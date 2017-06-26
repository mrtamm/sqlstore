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

package ws.rocket.sqlstore.test.script.params;

import java.sql.Types;
import java.util.List;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.params.TypePropParam;
import ws.rocket.sqlstore.test.db.model.Person;
import ws.rocket.sqlstore.test.helper.ScriptBuilder;

import static org.testng.Assert.assertEquals;

/**
 * Tests the {@link TypePropParam} class.
 */
@Test
public final class TypePropParamTest extends ParamTest {

  public void shouldStoreParamProperties() {
    TypePropParam param = TypePropParam.create(Person.class, "name", Types.VARCHAR, 0);

    checkTypes(param, String.class, Types.VARCHAR);
    assertEquals(param.getResultBeanType(), Person.class);
    assertEquals(param.getResultParamIndex(), 0);
  }

  @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp
      = "Detected illegal attempt to read result parameter")
  public void shouldFailToRead() {
    TypePropParam param = TypePropParam.create(Person.class, "name", Types.VARCHAR, 0);
    param.read(null);
  }

  public void shouldWriteParamValue() {
    QueryContext queryCtx = new ScriptBuilder()
        .addPersonOutParam("name")
        .toQueryContext();
    queryCtx.initResultsContainer(List.class, Person.class);

    TypePropParam param = TypePropParam.create(Person.class, "name", Types.VARCHAR, 0);

    param.write(queryCtx, "person name");

    Person rowValue = (Person) queryCtx.getResultsCollector().getRowValue(0);
    assertEquals(rowValue.getName(), "person name");
  }

  public void shouldProvideToString() {
    TypePropParam param = TypePropParam.create(Person.class, "name", Types.VARCHAR, 0);
    assertEquals(param.toString(), "Person.setName(String|12)");
  }

}
