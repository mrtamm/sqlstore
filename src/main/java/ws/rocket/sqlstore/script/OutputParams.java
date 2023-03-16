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

package ws.rocket.sqlstore.script;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import ws.rocket.sqlstore.ResultsCollector;
import ws.rocket.sqlstore.ScriptExecuteException;
import ws.rocket.sqlstore.result.ArrayResultsCollector;
import ws.rocket.sqlstore.result.ListResultsCollector;
import ws.rocket.sqlstore.result.MapResultsCollector;
import ws.rocket.sqlstore.result.VoidResultsCollector;
import ws.rocket.sqlstore.script.params.Param;
import ws.rocket.sqlstore.script.params.TypeNameParam;
import ws.rocket.sqlstore.script.params.TypePropParam;

/**
 * A collection of parameters that are expected from the script output after execution. These are
 * <code>OUT(...)</code> category parameters from a script file.
 */
public final class OutputParams {

  /**
   * Constant for "no output parameters".
   */
  public static final OutputParams EMPTY = new OutputParams(new TypeNameParam[0]);

  private final Class<?>[] types;

  private final String description;

  private final Map<String, TypeNameParam> namedParams;

  /**
   * Creates a new instance of output parameters. The created instance basically wraps given
   * parameters. This constructor does no further validation on parameter constraints, but these
   * parameters should be writable.
   *
   * @param params A not null array of validated out-parameters.
   */
  public OutputParams(Param[] params) {
    Objects.requireNonNull(params, "OUT-parameters array OutputParams.EMPTYis undefined");
    this.description = composeDescription(params);
    this.namedParams = initNamedParams(params);
    this.types = getResultTypes(params);
  }

  /**
   * Performs parameter lookup based on given name. When the parameter with the same name is not
   * found, this method will return null. Otherwise, an instance of the matching named parameter.
   *
   * <p>This method is aware that not all out-parameters have names (e.g. result-set parameters).
   * So it compares a name only to the names of named parameter instances.
   *
   * @param name A parameter name to be used for parameter lookup.
   * @return A matching named parameter instance, or null.
   */
  public TypeNameParam get(String name) {
    return this.namedParams != null ? this.namedParams.get(name) : null;
  }

  /**
   * Informs whether instance contains no output parameters.
   *
   * @return A boolean that is true when the script does not return any results.
   */
  public boolean isEmpty() {
    return this.types.length == 0;
  }

  /**
   * Creates a result instance for storing the results of a new execution of the script.
   *
   * <p>This method also triggers results container validation to make sure that the query can
   * actually return collected results in given container and that the query actually has as many
   * return column types (OUT-params) as provided here (also checking that the types match).
   *
   * <p>When the script does not return back any results (Void), this method will provide a results
   * instance that will throw an exception when one attempts to add a value to it.
   *
   * @param resultContainerType The class of the container type, such as Void, Map, List, Object[].
   * @param columnTypes An array of classes that must match Java types of the OUT-params in the same
   *     order. When a type is null, it will not raise an exception but the corresponding type check
   *     will be just skipped.
   * @return A new instance of results collector for storing query results.
   */
  public ResultsCollector createResultsCollector(Class<?> resultContainerType,
      Class<?>... columnTypes) {
    int colTypesLength = columnTypes == null ? 0 : columnTypes.length;
    ResultsCollector result = null;

    if (supportsColumns(columnTypes)) {
      if (colTypesLength == 0 && Void.class.equals(resultContainerType)) {
        result = VoidResultsCollector.INSTANCE;
      } else if (colTypesLength == 1 && List.class.equals(resultContainerType)) {
        result = new ListResultsCollector();
      } else if (colTypesLength == 2 && Map.class.equals(resultContainerType)) {
        result = new MapResultsCollector();
      } else if (Object[][].class.equals(resultContainerType)) {
        result = new ArrayResultsCollector(this.types.length);
      }
    }

    if (result == null) {
      throw new ScriptExecuteException("Query does not support return type %s with column types %s",
          resultContainerType.getName(), Arrays.toString(columnTypes));
    }

    return result;
  }

  /**
   * Provides a textual representation of current out-parameters.
   *
   * <p>When there are no output parameters, this method will return an empty string. Otherwise, it
   * will return a comma-separated list of output parameters wrapped in <code>OUT(...)</code>.
   *
   * @return A textual representation of current input parameters.
   */
  @Override
  public String toString() {
    return this.description;
  }

  // Output parameters may contain TypePropParam's (properties of a type), which evaluate to the
  // properties of the same bean. The bean instance is created when evaluating the first property in
  // the list (TypePropParam method reports 'true'). Other TypePropParam properties need to be
  // ignored when determining the actual amount of returned objects.
  private Class<?>[] getResultTypes(Param[] params) {
    List<Class<?>> result = new ArrayList<>(2);
    int resultIndex = -1;

    for (Param p : params) {
      if (p instanceof TypePropParam) {
        int paramResultIndex = ((TypePropParam) p).getResultParamIndex();
        if (paramResultIndex == resultIndex) {
          continue;
        }

        resultIndex = paramResultIndex;
        result.add(((TypePropParam) p).getResultBeanType());
      } else {
        resultIndex++;
        result.add(p.getJavaType());
      }
    }

    return result.toArray(new Class<?>[resultIndex + 1]);
  }

  private static String composeDescription(Param[] params) {
    if (params.length == 0) {
      return "";
    }

    StringBuilder sb = new StringBuilder(params.length * 16);
    sb.append("OUT(");

    boolean first = true;
    for (Param param : params) {
      if (!first) {
        sb.append(", ");
      }

      sb.append(param);
      first = false;
    }

    return sb.append(')').toString();
  }

  private static Map<String, TypeNameParam> initNamedParams(Param[] params) {
    Map<String, TypeNameParam> result = null;

    for (Param param : params) {
      if (param instanceof TypeNameParam) {
        if (result == null) {
          result = new HashMap<>(params.length);
        }

        result.put(((TypeNameParam) param).getName(), (TypeNameParam) param);
      }
    }

    return result;
  }

  private boolean supportsColumns(Class<?>... columnTypes) {
    int colTypesLength = columnTypes == null ? 0 : columnTypes.length;
    boolean supports = false;

    if (this.types.length == colTypesLength) {
      supports = true;

      for (int i = 0; i < colTypesLength; i++) {
        if (columnTypes[i] == null || !columnTypes[i].isAssignableFrom(this.types[i])) {
          supports = false;
          break;
        }
      }
    }

    return supports;
  }

}
