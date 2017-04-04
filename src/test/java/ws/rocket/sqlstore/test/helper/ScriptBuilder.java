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

import java.sql.Types;
import java.util.Collections;
import java.util.List;
import org.mockito.Mockito;
import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.Script;
import ws.rocket.sqlstore.script.params.ParamMode;
import ws.rocket.sqlstore.script.params.TypeNameParam;
import ws.rocket.sqlstore.script.read.ParamsSet;
import ws.rocket.sqlstore.script.sql.SqlScript;
import ws.rocket.sqlstore.test.db.model.Person;

/**
 * Helper class for constructing {@link Script} and {@link ParamsSet} objects for testing purpose.
 */
public final class ScriptBuilder {

  private final ParamsSet params = new ParamsSet();

  private boolean paramsInitiated;

  public ScriptBuilder addInParam(Class<?> paramType, Integer sqlType, String name) {
    params.addInParam(new TypeNameParam(paramType, sqlType, name));
    return this;
  }

  public ScriptBuilder addInParam(Class<?> paramType, String name) {
    return addInParam(paramType, null, name);
  }

  public ScriptBuilder addInPersonParam(String name) {
    return addInParam(Person.class, null, name);
  }

  public ScriptBuilder addInStringParam(String name) {
    return addInParam(String.class, Types.VARCHAR, name);
  }

  public ScriptBuilder addInLongParam(String name) {
    return addInParam(Long.class, Types.NUMERIC, name);
  }

  public ScriptBuilder addOutParam(Class<?> paramType, Integer sqlType, String name) {
    params.addOutParam(paramType, sqlType, name, false);
    return this;
  }

  public ScriptBuilder addOutParam(Class<?> paramType, String name) {
    return addOutParam(paramType, null, name);
  }

  public ScriptBuilder addOutStringParam(String varName) {
    return addOutParam(String.class, Types.VARCHAR, varName);
  }

  public ScriptBuilder addOutLongParam(String varName) {
    return addOutParam(Long.class, Types.NUMERIC, varName);
  }

  public ScriptBuilder addScriptParam(String name) {
    if (!this.paramsInitiated) {
      initParams();
    }

    List<String> emptyList = Collections.emptyList();
    params.addScriptParam(ParamMode.IN, name, emptyList, null);
    return this;
  }

  public ParamsSet initParams() {
    if (!this.paramsInitiated) {
      this.params.initInOutUpdateParams();
      this.paramsInitiated = true;
    }
    return this.params;
  }

  public Script toScript() {
    return new Script("testScript", 1, Mockito.mock(SqlScript.class), initParams());
  }

  public QueryContext toQueryContext(Object... args) {
    return new QueryContext(toScript(), args);
  }

}
