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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Types;
import java.util.Collections;
import java.util.List;
import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.QueryParam;
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

  public ScriptBuilder addKeysOutParam(Class<?> paramType, Integer sqlType, String name) {
    params.addOutParam(paramType, sqlType, name, true);
    return this;
  }

  public ScriptBuilder addKeysOutStringParam(String varName) {
    return addKeysOutParam(String.class, Types.VARCHAR, varName);
  }

  public ScriptBuilder addPersonOutParam(String beanPropName) {
    params.registerBean(Person.class);
    params.addOutParamBeanProp(beanPropName, null, null);
    params.unregisterBean();
    return this;
  }

  public ScriptBuilder addScriptInParam(String name) {
    return addScriptParam(ParamMode.IN, name);
  }

  public ScriptBuilder addScriptOutParam(String name) {
    return addScriptParam(ParamMode.OUT, name);
  }

  private ScriptBuilder addScriptParam(ParamMode mode, String name) {
    if (!this.paramsInitiated) {
      initParams();
    }

    List<String> emptyList = Collections.emptyList();
    params.addScriptParam(mode, name, emptyList, null);
    return this;
  }

  public ParamsSet initParams() {
    if (!this.paramsInitiated) {
      this.params.initInOutUpdateParams();
      this.paramsInitiated = true;
    }
    return this.params;
  }

  private boolean containsScriptOutParam() {
    QueryParam[] queryParams = this.params.resetQueryParams();
    for (QueryParam queryParam : queryParams) {
      if (queryParam.isForOutput()) {
        return true;
      }
    }
    return false;
  }

  public Script toScript() {
    SqlScript sqlScript = mock(SqlScript.class);
    when(sqlScript.containsOutParam()).thenReturn(containsScriptOutParam());

    return new Script("testScript", 1, sqlScript, initParams());
  }

  public QueryContext toQueryContext(Object... args) {
    return new QueryContext(toScript(), args);
  }

}
