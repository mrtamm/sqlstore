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

package ws.rocket.sqlstore.test.types;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.script.QueryParam;
import ws.rocket.sqlstore.script.params.ParamMode;
import ws.rocket.sqlstore.script.params.TypeNameParam;
import ws.rocket.sqlstore.types.Bindings;
import ws.rocket.sqlstore.types.ValueMapper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

/**
 * Tests for the {@link IntMapper} class.
 */
@Test
public class BindingsTest {

  @Test(groups = "bindings-init", expectedExceptions = IllegalArgumentException.class)
  public void shouldRejectNullArrayInput() {
    Bindings.register((ValueMapper[]) null);
  }

  @Test(groups = "bindings-init", expectedExceptions = IllegalArgumentException.class)
  public void shouldRejectEmptyArrayInput() {
    Bindings.register(new ValueMapper[0]);
  }

  @Test(groups = "bindings-init", expectedExceptions = IllegalArgumentException.class)
  public void shouldRejectNullHandlerInput() {
    Bindings.register((ValueMapper) null);
  }

  @Test(dependsOnGroups = "bindings-init")
  public void shouldUseSameInstance() {
    assertSame(Bindings.getInstance(), Bindings.getInstance());
  }

  @Test(dependsOnGroups = "bindings-init")
  public void shouldSupportBuiltInTypes() {
    assertEquals(Bindings.getInstance().confirmTypes(String.class, null), Types.VARCHAR);

    assertEquals(Bindings.getInstance().confirmTypes(Date.class, null), Types.TIMESTAMP);

    assertEquals(Bindings.getInstance().confirmTypes(boolean.class, null), Types.BOOLEAN);
    assertEquals(Bindings.getInstance().confirmTypes(Boolean.class, null), Types.BOOLEAN);

    assertEquals(Bindings.getInstance().confirmTypes(int.class, null), Types.NUMERIC);
    assertEquals(Bindings.getInstance().confirmTypes(Integer.class, null), Types.NUMERIC);
    assertEquals(Bindings.getInstance().confirmTypes(long.class, null), Types.NUMERIC);
    assertEquals(Bindings.getInstance().confirmTypes(Long.class, null), Types.NUMERIC);
    assertEquals(Bindings.getInstance().confirmTypes(BigDecimal.class, null), Types.NUMERIC);
  }

  @Test(dependsOnGroups = "bindings-init")
  public void shouldRegisterOutParameter() throws SQLException {
    TypeNameParam var = new TypeNameParam(String.class, Types.VARCHAR, "name");
    QueryParam param = new QueryParam(ParamMode.OUT, var);
    CallableStatement stmt = mock(CallableStatement.class);

    Bindings.getInstance().bindParam(null, param, stmt, 1);

    verify(stmt).registerOutParameter(1, Types.VARCHAR);
  }

  @Test(dependsOnGroups = "bindings-init", enabled = false)
  public void shouldReadStatementOutParameter() throws SQLException {
    TypeNameParam var = new TypeNameParam(String.class, Types.VARCHAR, "name");
    QueryParam param = new QueryParam(ParamMode.INOUT, var);
    CallableStatement stmt = mock(CallableStatement.class);

    Bindings.getInstance().readParam(null, param, stmt, 1);

    verify(stmt).getString(1);
    verify(stmt).getString(1);
  }

}
