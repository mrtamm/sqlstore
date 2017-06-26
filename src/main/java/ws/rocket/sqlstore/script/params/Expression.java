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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.BeanUtil;
import ws.rocket.sqlstore.types.Bindings;

/**
 * Expressions are read-write parameters to a query that refer to declared IN and OUT parameters of
 * the script.
 * <p>
 * An expression always refers to a named variable, can read its (nested) properties (when needed).
 * When a value needs to be stored in an expression, it can alter the variable value (when no
 * properties are specified), or read the properties and alter the last property. The latter works
 * only when the intermediate properties do not return null; however, when the main variable is
 * null, it is attempted to create via default constructor before continuing with properties.
 * <p>
 * The Java type of an expression is the type of the last property. or the type of the main variable
 * when no properties. SQL type will be derived from the Java type unless explicitly provided. As
 * usually, a value converter must exist to support the Java and SQL types combination.
 */
public final class Expression extends Param {

  /**
   * Creates a new instance of a script expression. This method also validates the input data and
   * there are issues that may cause a runtime exception, such as:
   * <ol>
   * <li>the named parameter is not defined;
   * <li>the read method of a provided bean property is not found;
   * <li>the write method of a provided bean property is not found;
   * <li>the provided non-null SQL type may not be supported with the Java type of the property.
   * </ol>
   *
   * @param namedParam The named parameter to bind with (required).
   * @param properties List of property names in the correct order to read the value of the
   * expression, or to set the value of the last property. Also determines Java type of the
   * expression.This may be null or empty when the expression just refers to a named parameter.
   * @param sqlType Optional SQL type for this property to override default.
   * @return A new expression instance.
   */
  public static Expression create(TypeNameParam namedParam, List<String> properties,
      Integer sqlType) {

    Class<?> exprJavaType;
    int exprSqlType;
    String expression;
    AccessibleObject[] readers;
    AccessibleObject writer = null;

    if (properties == null || properties.isEmpty()) {

      if (sqlType == null) {
        sqlType = namedParam.getSqlType();
      }

      exprJavaType = namedParam.getJavaType();
      exprSqlType = Bindings.getInstance().confirmTypes(exprJavaType, sqlType);
      readers = null;
      writer = null;
      expression = namedParam.getName();

    } else {
      StringBuilder exprBuilder = new StringBuilder().append(namedParam.getName());
      Class<?> currentType = namedParam.getJavaType();
      readers = new AccessibleObject[properties.size()];

      for (int i = 0; i < properties.size(); i++) {
        String prop = properties.get(i);

        if (i == properties.size() - 1) {
          writer = BeanUtil.requireWriter(currentType, prop);
        }

        AccessibleObject reader = BeanUtil.requireReader(currentType, prop);
        readers[i] = reader;

        if (reader instanceof Method) {
          currentType = ((Method) reader).getReturnType();
        } else if (reader instanceof Field) {
          currentType = ((Field) reader).getType();
        } else {
          throw new IllegalStateException("Expected the reader to be either method or field. Got: "
              + reader);
        }

        exprBuilder.append('.').append(prop);
      }

      expression = exprBuilder.toString();
      exprJavaType = currentType;
      exprSqlType = Bindings.getInstance().confirmTypes(currentType, sqlType);
    }

    return new Expression(
        namedParam,
        readers,
        writer,
        expression,
        exprJavaType,
        exprSqlType
    );
  }

  private final TypeNameParam typedVariable;

  private final AccessibleObject[] readers;

  private final AccessibleObject writer;

  private final String expression;

  private Expression(TypeNameParam typedVariable, AccessibleObject[] readers,
      AccessibleObject writer, String expression, Class<?> javaType, int sqlType) {
    super(javaType, sqlType);
    this.typedVariable = typedVariable;
    this.readers = readers;
    this.writer = writer;
    this.expression = expression;
  }

  @Override
  public Object read(QueryContext ctx) {
    Object inst = this.typedVariable.read(ctx);

    if (this.readers != null) {
      for (AccessibleObject reader : this.readers) {
        if (inst == null) {
          break;
        }
        inst = BeanUtil.read(inst, reader);
      }
    }

    return inst;
  }

  @Override
  public void write(QueryContext ctx, Object value) {
    if (this.readers == null) {
      this.typedVariable.write(ctx, value);
      return;
    }

    Object inst = this.typedVariable.read(ctx);

    // When the type does not exist, create it.
    // This stores the value when the variable is in output params.
    if (inst == null) {
      inst = BeanUtil.newInstance(this.typedVariable.getJavaType());
      this.typedVariable.write(ctx, inst);
    }

    for (int i = 0; i < this.readers.length - 1; i++) {
      if (inst == null) {
        break;
      }
      inst = BeanUtil.read(inst, this.readers[i]);
    }

    if (inst != null) {
      BeanUtil.write(inst, this.writer, value);
    }
  }

  /**
   * Provides textual representation of this parameter. The returned value has following format:
   * <p>
   * "&lt;simple class name&gt;|&lt;SQL type int value&gt; &lt;expression string&gt;"
   * <p>
   * For example: <code>Date|93 emp.birthDate</code>.
   *
   * @return Textual representation of this parameter instance.
   */
  @Override
  public String toString() {
    return super.toString() + " " + this.expression;
  }

}
