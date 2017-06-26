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

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.script.params.TypeNameParam;

/**
 * Reads and evaluates IN, UPDATE, OUT, HINT parameters of a script definition. An instance of the
 * reader may be used several times within the scope of the same scripts file (bound to one stream
 * reader instance). This reader should be called right after the script name has bean read.
 * <p>
 * Each parameters group begins with keyword "IN", "OUT', "UPDATE", or "HINT" directly followed by
 * an opening parenthesis (no whitespace between). Each group has its own rules for specifying
 * parameters. All groups end with closing parenthesis. The order of these groups is not defined.
 * Each group can be specified only once per script definition.
 * <p>
 * The parsed parameters will be stored in the parameters set, which is not otherwise modified or
 * reset by this reader. Once the script unit is parsed, they will be used for constructing the
 * resulting script unit info object (by the main script reader). Otherwise, this reader does not
 * maintain any state.
 *
 * <h3>IN parameters</h3>
 * <p>
 * These are comma-separated IN-parameters (enclosed in <code>IN(...)</code>) defining what
 * parameters are expected for query input (including their order and corresponding types):
 * <pre>
 * JavaType paramName1, JavaType|SQLTYPE paramName2, ...
 * </pre>
 * <p>
 * Here <code>JavaType</code> is a full class name as <code>org.sample.JavaType</code> or an alias
 * for a full class name. Parameter names are used in the script to inject their values (or values
 * of their properties) at given position within script. The same names can be also be used within
 * UPDATE-parameters group to define the IN-parameter properties to be updated after query
 * execution.
 *
 * <h3>UPDATE parameters</h3>
 * <p>
 * These are comma-separated UPDATE-parameters that need to be updated with values from result-set,
 * usually enclosed in <code>UPDATE(...)</code>:
 * <pre>
 * IN_Param.nested.prop, IN_Param.nested.prop2
 * KEYS(IN_Param.nested.prop, IN_Param.nested.prop2)
 * </pre>
 *
 * <h3>OUT parameters</h3>
 * <p>
 * These are comma-separated OUT-parameters (enclosed in <code>OUT(...)</code>) defining what needs
 * to be extracted from result-set:
 * <pre>
 * JavaType
 * JavaType|SQLTYPE
 * JavaType[prop1, prop2, prop3]
 * JavaType[prop1|SQLTYPE1, prop2|SQLTYPE2, prop3|SQLTYPE3]
 * </pre>
 * <p>
 * Here <code>JavaType</code> is a full class name as <code>org.sample.JavaType</code> or an alias
 * for a full class name.
 * <p>
 * Expressions may be wrapped by <code>KEYS(...)</code> so that it would extract data from generated
 * keys result-set.
 *
 * <h3>HINT parameters</h3>
 * <p>
 * Hints are comma-separated name-value pairs within <code>HINT(...)</code>:
 * <pre>
 * hintName1=hintValue1, hintName2=hintValue2, ...
 * </pre>
 *
 * @see ParamsCategory
 */
public final class ParamsReader {

  private final StreamReader reader;

  private final ParamsSet params;

  /**
   * Initiates the parameters reader instance, which may be used for parsing several scripts as long
   * as they are within one scripts file.
   *
   * @param reader The reader for current scripts file.
   * @param params The shared parameters set to be updated on parsing (its instance is re-used for
   * temporarily storing parameters of several scripts; reset is done by main reader).
   */
  public ParamsReader(StreamReader reader, ParamsSet params) {
    Objects.requireNonNull(reader, "StreamReader is undefined.");
    Objects.requireNonNull(params, "ParamsSet is undefined.");

    this.reader = reader;
    this.params = params;
  }

  /**
   * Parses the script definition parameters, as described in class-level documentation of this
   * class.
   *
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public void parseParams() throws IOException {
    Set<ParamsCategory> parsedParamTypes = new HashSet<>();

    while (!(this.reader.getColumn() == 1 && this.reader.isNext('='))) {
      int column = this.reader.getColumn();
      ParamsCategory category = this.reader.parseParamsType();

      if (parsedParamTypes.contains(category)) {
        throw new ScriptSetupException("Duplicate parameters category on line %d and column %d.",
            this.reader.getLine(), column);
      }
      parsedParamTypes.add(category);

      this.reader.parenthesisOpen();

      if (null != category) {
        switch (category) {
          case IN:
            parseInParams();
            break;
          case OUT:
            parseOutParams();
            break;
          case UPDATE:
            parseUpdateParams();
            break;
          case HINT:
            parseHintParams();
            break;
          default:
            throw new ScriptSetupException("Support for category %s is not yet implemented.",
                category);
        }
      }

      this.reader.requireNext(')');
      this.reader.skipWsp();
    }

    this.params.initInOutUpdateParams();
  }

  /*
   * Parses comma separated IN-parameters (enclosed in <code>IN(...)</code>).
   * <p>
   * Parsing is expected to begin right after opening parenthesis, and should end before a closing
   * parenthesis.
   */
  private void parseInParams() throws IOException {
    do {
      Class<?> javaType = this.reader.parseJavaType();
      Integer sqlType = this.reader.parseSqlType();

      this.reader.skipWsp();

      String paramName = this.reader.parseParamName();

      this.params.addInParam(new TypeNameParam(javaType, sqlType, paramName));
    } while (this.reader.skipIfNext(',') && this.reader.skipWsp() != -1);
  }

  /*
   * Parses comma separated OUT-parameters (enclosed in <code>OUT(...)</code>).
   * <p>
   * Parsing is expected to begin right after opening parenthesis, and should end before a closing
   * parenthesis.
   */
  private void parseOutParams() throws IOException {
    boolean evalKeys = true;
    boolean keysMode = false;
    String keysColName = null;
    Class<?> javaType = null;

    do {
      if (evalKeys) {
        javaType = this.reader.parseKeysOrJavaType();

        if (javaType == null) {
          evalKeys = false;
          keysMode = true;

          Object typeOrColumn = this.reader.parseKeyColumnNameOrJavaType();
          if (typeOrColumn instanceof Class) {
            javaType = (Class<?>) typeOrColumn;
          } else {
            keysColName = (String) typeOrColumn;
          }
        }
      }

      if (this.reader.skipIfNext('[')) {
        this.params.registerBean(javaType);

        do {
          this.reader.skipWsp();

          if (keysMode) {
            keysColName = this.reader.parseKeyColumnName();
          }

          String fieldName = this.reader.parseName("field");
          Integer sqlType = this.reader.parseSqlType();

          this.params.addOutParamBeanProp(fieldName, sqlType, keysColName);
          this.reader.skipWsp();
        } while (this.reader.skipIfNext(','));

        this.params.unregisterBean();
        this.reader.requireNext(']');
      } else {
        if (keysMode) {
          if (keysColName == null) {
            keysColName = this.reader.parseKeyColumnName();
          }
          javaType = this.reader.parseJavaType();
        }

        Integer sqlType = this.reader.parseSqlType();
        String paramName = null;

        if (keysMode) {
          paramName = keysColName;
          keysColName = null;
        } else {
          int cp = this.reader.skipWsp();

          if (cp != ',' && cp != ')' && cp != -1) {
            paramName = this.reader.parseParamName();
          }
        }

        this.params.addOutParam(javaType, sqlType, paramName, keysMode);
      }

    } while (this.reader.skipIfNext(',') && this.reader.skipWsp() != -1);

    if (keysMode && Character.isWhitespace(this.reader.requireNext(')'))) {
      this.reader.skipWsp();
    }
  }

  /*
   * Parses comma separated UPDATE-parameters (enclosed in <code>UPDATE(...)</code>).
   * <p>
   * Parsing is expected to begin right after opening parenthesis, and should end before a closing
   * parenthesis.
   */
  private void parseUpdateParams() throws IOException {
    boolean evalKeys = true;
    boolean keysMode = false;
    String keysColName = null;
    String paramName;

    do {
      paramName = null;

      if (evalKeys) {
        paramName = this.reader.parseKeysOrParamName();
        keysMode = paramName == null;
        evalKeys = !keysMode;
      }

      if (keysMode) {
        keysColName = this.reader.parseKeyColumnName();
      }

      if (paramName == null) {
        paramName = this.reader.parseParamName();
      }

      this.reader.requireNext('.');
      String property = this.reader.parseParamPropName();
      this.params.addUpdateParam(paramName, property, keysColName);

      if (keysMode && this.reader.skipWsp() == ')') {
        this.reader.skipNext();
        keysMode = false;
        keysColName = null;
      }

    } while (this.reader.skipWsp() == ',');
  }

  /*
   * Parses comma separated HINT-parameters (enclosed in <code>HINT(...)</code>).
   * <p>
   * Parsing is expected to begin right after opening parenthesis, and should end before a closing
   * parenthesis.
   */
  private void parseHintParams() throws IOException {
    do {
      this.reader.skipWsp();

      String hintName = this.reader.parseName("hint");
      this.reader.requireNext('=');
      String hintValue = this.reader.parseAlphaNum();

      this.params.setQueryHint(hintName, hintValue);

      this.reader.skipWsp();
    } while (this.reader.skipIfNext(','));
  }

}
