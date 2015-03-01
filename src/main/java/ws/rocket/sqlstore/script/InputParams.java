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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import ws.rocket.sqlstore.ScriptExecuteException;
import ws.rocket.sqlstore.script.params.TypeNameParam;

/**
 * A collection of parameters that are expected for script input in order to execute it. These are
 * <code>IN(...)</code> category parameters from a script file.
 */
public final class InputParams {

  /**
   * Constant for "no input parameters".
   */
  public static final InputParams EMPTY = new InputParams(new TypeNameParam[0]);

  private final TypeNameParam[] params;

  /**
   * Creates a new instance of input parameters. The created instance basically wraps given named
   * parameters. This constructor does no further validation on parameter constraints.
   *
   * @param params A not null array of validated named parameters.
   */
  public InputParams(TypeNameParam[] params) {
    if (params == null) {
      throw new NullPointerException("Named parameters array must not be null");
    }
    this.params = params;
  }

  /**
   * Performs parameter lookup based on given name. When the parameter with the same name is not
   * found, this method will return null. Otherwise, an instance of the matching parameter.
   *
   * @param name A parameter name to be used for parameter lookup.
   * @return A matching parameter instance, or null.
   */
  public TypeNameParam get(String name) {
    TypeNameParam result = null;

    for (TypeNameParam var : this.params) {
      if (var.getName().equals(name)) {
        result = var;
        break;
      }
    }

    return result;
  }

  /**
   * Creates a map where given values can be looked up by corresponding parameter names. The map is
   * used in the query context to maintain values accessible to expressions.
   * <p>
   * This method also validates the input values:
   * <ol>
   * <li>the amount of defined parameters and provided values must match;
   * <li>the Java types of parameters and provided values must match.
   * </ol>
   * <p>
   * A validation error produces an <code>ScriptExecuteException</code>.
   *
   * @param args The values provided to the script being executed. The array must not be null.
   * @return A map where the values can be looked up by their parameter names.
   */
  public Map<String, Object> bind(Object[] args) {
    if (args.length != this.params.length) {
      throw new ScriptExecuteException("Script input arguments amount mismatch: "
          + "expected %d, got %d", this.params.length, args.length);
    }

    Map<String, Object> context;

    if (args.length == 0) {
      context = Collections.emptyMap();
    } else {
      context = new HashMap<>(args.length, 1f);

      for (int i = 0; i < args.length; i++) {
        this.params[i].validate(args[i], i);
        context.put(this.params[i].getName(), args[i]);
      }
    }

    return context;
  }

  /**
   * Informs whether instance contains no input parameters.
   *
   * @return A boolean that is true when the script does not accept any parameters.
   */
  public boolean isEmpty() {
    return this.params.length == 0;
  }

  /**
   * Provides a textual representation of current input parameters.
   * <p>
   * When there are no input parameters, this method will return an empty string. Otherwise, it will
   * return a comma-separated list of input parameters wrapped in <code>IN(...)</code>.
   *
   * @return A textual representation of current input parameters.
   */
  @Override
  public String toString() {
    if (this.params.length == 0) {
      return "";
    }

    StringBuilder sb = new StringBuilder(this.params.length * 20);
    sb.append("IN(");

    boolean first = true;
    for (TypeNameParam param : this.params) {
      if (!first) {
        sb.append(", ");
      }

      sb.append(param);
      first = false;
    }

    return sb.append(") ").toString();
  }

}
