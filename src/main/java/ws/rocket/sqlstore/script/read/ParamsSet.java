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

package ws.rocket.sqlstore.script.read;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.script.InputParams;
import ws.rocket.sqlstore.script.OutputParams;
import ws.rocket.sqlstore.script.QueryHints;
import ws.rocket.sqlstore.script.QueryParam;
import ws.rocket.sqlstore.script.Script;
import ws.rocket.sqlstore.script.params.Expression;
import ws.rocket.sqlstore.script.params.Param;
import ws.rocket.sqlstore.script.params.ParamMode;
import ws.rocket.sqlstore.script.params.TypeNameParam;
import ws.rocket.sqlstore.script.params.TypeParam;
import ws.rocket.sqlstore.script.params.TypePropParam;
import ws.rocket.sqlstore.types.Bindings;

/**
 * Helper class for evaluating different sets of parameters as they are parsed from an SQLS file.
 * <p>
 * This class is designed to be reused script-after-script by calling
 * {@link #cleanup(ws.rocket.sqlstore.script.Script)} after the parameters have been read by the
 * <code>Script</code> constructor. After that, the instance will be ready to evaluate all the
 * parameters of the next script.
 */
public final class ParamsSet {

  private final List<TypeNameParam> inVarParams = new ArrayList<>();

  private final List<TypeNameParam> outVarParams = new ArrayList<>();

  private final List<Param> outTypeParams = new ArrayList<>();

  private final List<QueryParam> queryParams = new ArrayList<>();

  private final List<Param> keysResults = new ArrayList<>();

  private final List<Param> rowsResults = new ArrayList<>();

  private final List<String> keysColumns = new ArrayList<>();

  private Class<?> propParamBeanType;

  private boolean beanFirstProp;

  private InputParams inputParams;

  private OutputParams outputParams;

  private int outParamIndex;

  private QueryHints hints;

  /**
   * Validates that all parameters of the script were actually used, and resets all internally
   * contained data so that this class instance could be reused for handling the parameters of next
   * script.
   *
   * @param script The script instance for which the current parameters set instance/state was just
   * used for providing the parameters. (Script is used for providing better error messages.) It is
   * valid to leave it null (e.g. on parse error) in which case parameters usage check is skipped.
   */
  public void cleanup(Script script) {
    if (script != null && !this.inVarParams.isEmpty()) {
      throw new ScriptSetupException(
          "Script [%s] (line %d): following IN-parameters were not used: %s",
          script.getName(), script.getLine(), this.inVarParams.toString());

    } else if (script != null && !this.outVarParams.isEmpty()) {
      throw new ScriptSetupException(
          "Script [%s] (line %d): following OUT-parameters were not used: %s",
          script.getName(), script.getLine(), this.outVarParams.toString());
    }

    this.inVarParams.clear();
    this.outVarParams.clear();
    this.outTypeParams.clear();
    this.queryParams.clear();
    this.keysResults.clear();
    this.rowsResults.clear();
    this.keysColumns.clear();
    this.inputParams = null;
    this.outputParams = null;
    this.outParamIndex = 0;
    this.hints = null;
  }

  /**
   * Registers a new IN-parameter. Also checks that the name of the parameter is not already used.
   *
   * @param param The parameter to be added.
   */
  public void addInParam(TypeNameParam param) {
    checkParamName(param.getName());
    this.inVarParams.add(param);
  }

  /**
   * Registers a new OUT-parameter.
   * <p>
   * This method also
   * <ol>
   * <li>checks that the name of the parameter, if present, is not already used;
   * <li>resolves the SQL type when necessary;
   * <li>validates that OUT-parameters set would remain correct.
   * </ol>
   * <p>
   * There are a bit complex rules but here's a list of what kind of OUT-parameters list is not
   * allowed:
   * <ol>
   * <li>named parameters within <code>KEYS(...)</code> clause (the clause is for result-set, named
   * parameters are registered in statement);
   * <li>named and unnamed parameters mixed (however, this can be solved by using UPDATE-parameters
   * to update a property on an input parameter value, and using OUT-parameters to return data from
   * result-set).
   * </ol>
   *
   * @param javaType The Java type of the parameter (required).
   * @param sqlType The SQL type of the parameter (optional).
   * @param name A name for the parameter or generated key column (required).
   * @param key When true, the previous parameter is a column name for a generated key to return.
   * Otherwise, a name for the parameter.
   *
   * @see #addOutParamBeanProp(String, Integer, String)
   */
  public void addOutParam(Class<?> javaType, Integer sqlType, String name, boolean key) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("The 'name' parameter must not be empty");
    }

    if (!key) {
      if (!this.outTypeParams.isEmpty()) {
        throw new ScriptSetupException("OUT-params with names and without names cannot be mixed");
      }

      checkParamName(name);

      this.outVarParams.add(new TypeNameParam(javaType, sqlType, name, this.outParamIndex));

    } else {
      if (!this.outVarParams.isEmpty()) {
        throw new ScriptSetupException("OUT-params with names and without names cannot be mixed");
      }

      TypeParam param = new TypeParam(javaType, getSqlType(javaType, sqlType), this.outParamIndex);
      this.outTypeParams.add(param);
      addResultParam(param, true);
      this.keysColumns.add(name);
    }

    this.outParamIndex++;
  }

  /**
   * Adds a result-set parameter where the value is to be stored in the property of previously
   * registered bean.
   *
   * @param fieldName The property name (will be validated).
   * @param sqlType Optional SQL type when provided in the SQL file next to the property name.
   * @param keyColumn If value is from a generated key then its column name. Otherwise null.
   */
  public void addOutParamBeanProp(String fieldName, Integer sqlType, String keyColumn) {
    TypePropParam param = TypePropParam.create(this.propParamBeanType, fieldName, sqlType,
        this.outParamIndex);

    if (this.beanFirstProp) {
      if (!this.outVarParams.isEmpty()) {
        throw new ScriptSetupException("OUT-params with names and without names cannot be mixed");
      }

      this.outTypeParams.add(param);
    }

    addResultParam(param, keyColumn != null);

    if (keyColumn != null) {
      this.keysColumns.add(keyColumn);
    }

    this.beanFirstProp = false;
  }

  /**
   * Registers a new UPDATE-parameter. Currently, this implementation does not support nested
   * properties to be updated with read value.
   *
   * @param paramName The IN or OUT parameter name this expression refers to.
   * @param property The property of the IN/OUT-parameter to update with value.
   * @param keyColumn If value is from a generated key then its column name. Otherwise null.
   */
  public void addUpdateParam(String paramName, String property, String keyColumn) {
    TypeNameParam param = getParamByName(paramName, this.inVarParams);
    if (param == null) {
      throw new ScriptSetupException("There is no parameter with name '%s' defined on this script",
          paramName);
    }

    Expression expression = Expression.create(param, Collections.singletonList(property), null);
    addResultParam(expression, keyColumn != null);

    if (keyColumn != null) {
      this.keysColumns.add(keyColumn);
    }
  }

  /**
   * Registers a new SQL script parameter as an {@link Expression}. Be sure that
   * {@link #initInOutUpdateParams()} is called before adding SQL script parameters.
   *
   * @param mode The script parameter mode as defined in the expression in SQLS. Null when not
   * defined.
   * @param varName The IN/OUT parameter name being referred.
   * @param fields The properties of the referred parameter to traverse for reading/writing the
   * value.
   * @param sqlType Optional SQL type for the expression when explicitly set next to the expression.
   */
  public void addScriptParam(ParamMode mode, String varName, List<String> fields, Integer sqlType) {
    TypeNameParam param = this.inputParams.get(varName);

    if (param == null) {
      if (mode == ParamMode.IN || mode == ParamMode.INOUT) {
        throw new ScriptSetupException("Expression referring to parameter '%s' was "
            + "specified as %s-parameter but the parameter is not among IN-parameters.",
            varName, mode.name());
      }

      param = this.outputParams.get(varName);
      mode = ParamMode.OUT;
      removeParam(this.outVarParams, varName);
    } else {
      mode = mode == null ? ParamMode.IN : mode;
      markInParamAsUsed(varName);
    }

    if (param == null) {
      throw new ScriptSetupException("There is no parameter with name '%s' defined on this script",
          varName);
    }

    QueryParam queryParam;

    if (fields.isEmpty()) {
      if (sqlType == null) {
        sqlType = param.getSqlType();
      }

      if (sqlType == null) {
        sqlType = getSqlType(param.getJavaType(), null);
      }

      if (!sqlType.equals(param.getSqlType())) {
        param = new TypeNameParam(param.getJavaType(), sqlType, param.getName());
      }

      queryParam = new QueryParam(mode, param);
    } else {
      Expression expression = Expression.create(param, fields, sqlType);
      queryParam = new QueryParam(mode, expression);
    }

    this.queryParams.add(queryParam);
  }

  /**
   * Informs that following OUT-parameters are the properties of this bean. Definitely unregister
   * the bean type after the properties have been registered.
   *
   * @param beanType The bean type to register.
   *
   * @see #addOutParamBeanProp(String, Integer, String)
   * @see #unregisterBean()
   */
  public void registerBean(Class<?> beanType) {
    this.propParamBeanType = beanType;
    this.beanFirstProp = true;
  }

  /**
   * Marks the end of bean properties of previously registered bean.
   *
   * @see #registerBean(Class)
   */
  public void unregisterBean() {
    this.propParamBeanType = null;
    this.beanFirstProp = false;
    this.outParamIndex++;
  }

  /**
   * Prepares {@link InputParams} and {@link OutputParams} internally. This method must be called
   * right before evaluating the SQL script so that encountered SQL script parameters would be
   * handled correctly (the referred input/output parameters would be found).
   *
   * @see #addScriptParam(ParamMode, String, List, Integer)
   * @see #getInputParams()
   * @see #getOutputParams()
   */
  public void initInOutUpdateParams() {
    this.inputParams = InputParams.EMPTY;
    this.outputParams = OutputParams.EMPTY;

    if (!this.inVarParams.isEmpty()) {
      TypeNameParam[] params = this.inVarParams.toArray(new TypeNameParam[this.inVarParams.size()]);
      this.inputParams = new InputParams(params);
    }

    if (!this.outTypeParams.isEmpty()) {
      Param[] params = this.outTypeParams.toArray(new Param[this.outTypeParams.size()]);
      this.outputParams = new OutputParams(params);
    } else if (!this.outVarParams.isEmpty()) {
      Param[] params = this.outVarParams.toArray(new Param[this.outVarParams.size()]);
      this.outputParams = new OutputParams(params);
    }
  }

  /**
   * Registers a hint for the current script.
   *
   * @param hintName The hint name.
   * @param hintValue The hint value.
   * @see QueryHints#setHint(String, String)
   */
  public void setQueryHint(String hintName, String hintValue) {
    if (this.hints == null) {
      this.hints = new QueryHints();
    }
    this.hints.setHint(hintName, hintValue);
  }

  /**
   * Provides the validated input parameters set.
   *
   * @return The input parameters for current script.
   * @see #initInOutUpdateParams()
   */
  public InputParams getInputParams() {
    return this.inputParams;
  }

  /**
   * Provides the validated output parameters set.
   *
   * @return The output parameters for current script.
   * @see #initInOutUpdateParams()
   */
  public OutputParams getOutputParams() {
    return this.outputParams;
  }

  /**
   * Provides the validated parameters to be used with the JDBC statement.
   *
   * @return An array of query parameters. May be empty but never null.
   */
  public QueryParam[] resetQueryParams() {
    QueryParam[] result = QueryParam.NO_PARAMS;

    if (!this.queryParams.isEmpty()) {
      result = this.queryParams.toArray(new QueryParam[this.queryParams.size()]);
      this.queryParams.clear();
    }

    return result;
  }

  /**
   * Provides the column names for the generated keys.
   *
   * @return An array of generated key column names, or null when script does not use them.
   */
  public String[] getGenerateKeyColumns() {
    if (this.keysColumns.isEmpty()) {
      return null;
    }
    return this.keysColumns.toArray(new String[this.keysColumns.size()]);
  }

  /**
   * Provides the validated parameters to be used for reading the generated keys result-set.
   *
   * @return An array of generated keys result-set parameters. May be empty but never null.
   */
  public Param[] getKeysParams() {
    Param[] result = Param.NO_PARAMS;

    if (!this.keysResults.isEmpty()) {
      result = this.keysResults.toArray(new Param[this.keysResults.size()]);
    }

    return result;
  }

  /**
   * Provides the query hints for the current script.
   *
   * @return The query hints object, or null, which stands for "no hints defined".
   */
  public QueryHints getQueryHints() {
    return this.hints;
  }

  /**
   * Provides the validated parameters to be used for reading the query results result-set.
   *
   * @return An array of query results result-set parameters. May be empty but never null.
   */
  public Param[] getResultsParams() {
    Param[] result = Param.NO_PARAMS;

    if (!this.rowsResults.isEmpty()) {
      result = this.rowsResults.toArray(new Param[this.rowsResults.size()]);
    }

    return result;
  }

  /**
   * Marks an IN-parameter by name as used so that this parameter would not trigger unused parameter
   * exception. It is safe to call this method multiple times with the same name, i.e. does not
   * cause unknown parameter exception after it has already been removed.
   * <p>
   * This method is intended to be called when handling the SQL part of the script definition to
   * notify a parameter usage once it is detected in the script. The script parameters are copied
   * into their own structures by that moment.
   *
   * @param paramName The name of the IN-parameter to mark as used.
   */
  public void markInParamAsUsed(String paramName) {
    removeParam(this.inVarParams, paramName);
  }

  private TypeNameParam getParamByName(String name, List<TypeNameParam> params) {
    TypeNameParam result = null;
    for (TypeNameParam param : params) {
      if (name.equals(param.getName())) {
        result = param;
        break;
      }
    }
    return result;
  }

  private void checkParamName(String name) {
    if (getParamByName(name, this.inVarParams) != null
        || getParamByName(name, this.outVarParams) != null) {
      throw new ScriptSetupException("Another parameter with name '%s' is already defined", name);
    }
  }

  private void addResultParam(Param param, boolean keys) {
    if (keys) {
      this.keysResults.add(param);
    } else {
      this.rowsResults.add(param);
    }
  }

  private void removeParam(List<TypeNameParam> params, String varName) {
    for (Iterator<TypeNameParam> it = params.iterator(); it.hasNext();) {
      if (it.next().getName().equals(varName)) {
        it.remove();
        break;
      }
    }
  }

  private static int getSqlType(Class<?> javaType, Integer providedSqlType) {
    return Bindings.getInstance().confirmTypes(javaType, providedSqlType);
  }

}
