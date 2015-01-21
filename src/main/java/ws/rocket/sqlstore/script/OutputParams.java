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
import java.util.List;
import ws.rocket.sqlstore.script.params.Param;
import ws.rocket.sqlstore.script.params.TypeNameParam;
import ws.rocket.sqlstore.script.params.TypePropParam;
import ws.rocket.sqlstore.result.ListResult;
import ws.rocket.sqlstore.result.MapResult;
import ws.rocket.sqlstore.result.Result;
import ws.rocket.sqlstore.result.VoidResult;

/**
 * A collection of parameters that are expected from the script output after execution. These are
 * <code>OUT(...)</code> category parameters from a script file.
 */
public final class OutputParams {

  /**
   * Constant for "no output parameters".
   */
  public static final OutputParams EMPTY = new OutputParams(new TypeNameParam[0]);

  private final Param[] params;

  /**
   * Creates a new instance of output parameters. The created instance basically wraps given
   * parameters. This constructor does no further validation on parameter constraints, but these
   * parameters should be writable.
   *
   * @param params A not null array of validated out-parameters.
   */
  public OutputParams(Param[] params) {
    if (params == null) {
      throw new NullPointerException("OUT-parameters array must not be null");
    }
    this.params = params;
  }

  /**
   * Performs parameter lookup based on given name. When the parameter with the same name is not
   * found, this method will return null. Otherwise, an instance of the matching named parameter.
   * <p>
   * This method is aware that not all out-parameters have names (e.g. result-set parameters). So it
   * compares a name only to the names of named parameter instances.
   *
   * @param name A parameter name to be used for parameter lookup.
   * @return A matching named parameter instance, or null.
   */
  public TypeNameParam get(String name) {
    TypeNameParam result = null;

    for (Param param : params) {
      if (param instanceof TypeNameParam && ((TypeNameParam) param).getName().equals(name)) {
        result = (TypeNameParam) param;
        break;
      }
    }

    return result;
  }

  /**
   * Informs whether the script supports <code>java.util.List</code> (of query results) as return
   * type.
   *
   * @param type An optional Java type for checking whether the list item type is supported. When
   * null, this check will be skipped.
   * @return A Boolean that is true when the script can return a List of values of given type.
   */
  public boolean supportsList(Class<?> type) {
    boolean supports = false;

    if (this.params.length > 0) {
      List<Param> p = filterParams();

      if (p.size() != 1) {
        supports = false;
      } else if (type == null) {
        supports = true;
      } else if (p.get(0) instanceof TypePropParam) {
        supports = ((TypePropParam) p.get(0)).createsBean(type);
      } else {
        supports = type.isAssignableFrom(p.get(0).getJavaType());
      }
    }

    return supports;
  }

  /**
   * Informs whether the script supports <code>java.util.Map</code> (of query results) as return
   * type.
   *
   * @param typeKey An optional Java type for checking whether it is supported as the type of map
   * keys. When null, this check will be skipped.
   * @param typeValue An optional Java type for checking whether it is supported as the type of map
   * values. When null, this check will be skipped.
   * @return A Boolean that is true when the script can return a List of values of given type.
   */
  public boolean supportsMap(Class<?> typeKey, Class<?> typeValue) {
    boolean supports = false;

    if (this.params.length > 0) {
      List<Param> p = filterParams();
      return p.size() == 2
          && (typeKey == null || typeKey.isAssignableFrom(p.get(0).getJavaType()))
          && (typeValue == null || typeValue.isAssignableFrom(p.get(1).getJavaType()));
    }

    return supports;
  }

  /**
   * Informs whether instance contains no output parameters.
   *
   * @return A boolean that is true when the script does not return any results.
   */
  public boolean isEmpty() {
    return this.params.length == 0;
  }

  /**
   * Creates a result instance for storing the results of a new execution of the script. When the
   * script does not return back any results, this method will provide a results instance that will
   * throw an exception when one attempts to add a value to it.
   *
   * @return A new instance of for storing query results.
   */
  public Result createResultContainer() {
    Result result;

    if (isEmpty()) {
      result = new VoidResult();
    } else {
      List<Param> p = filterParams();

      if (p.size() == 1) {
        result = new ListResult();
      } else if (p.size() == 2) {
        result = new MapResult();
      } else {
        throw new IllegalStateException("A query is not allowed to return more than two "
            + "objects per row");
      }
    }

    return result;
  }

  /**
   * Provides a textual representation of current out-parameters.
   * <p>
   * When there are no output parameters, this method will return an empty string. Otherwise, it
   * will return a comma-separated list of output parameters wrapped in <code>OUT(...)</code>.
   *
   * @return A textual representation of current input parameters.
   */
  @Override
  public String toString() {
    if (this.params.length == 0) {
      return "";
    }

    StringBuilder sb = new StringBuilder(this.params.length * 16);
    sb.append("OUT(");

    boolean first = true;
    for (Param param : this.params) {
      if (!first) {
        sb.append(", ");
      }

      sb.append(param);
      first = false;
    }

    return sb.append(") ").toString();
  }

  // Output parameters may contain TypePropParam's (properties of a type), which evaluate to the
  // properties of the same bean. The bean instance is created when evaluating the first property in
  // the list (TypePropParam method reports 'true'). Other TypePropParam properties need to be
  // ignored when determining the actual amount of returned objects.
  private List<Param> filterParams() {
    List<Param> result = new ArrayList<>(this.params.length);
    for (Param p : this.params) {
      if (!(p instanceof TypePropParam) || ((TypePropParam) p).createsBean()) {
        result.add(p);
      }
    }
    return result;
  }

}
