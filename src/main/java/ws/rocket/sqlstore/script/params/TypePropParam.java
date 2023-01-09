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
import ws.rocket.sqlstore.execute.QueryContext;
import ws.rocket.sqlstore.script.BeanUtil;
import ws.rocket.sqlstore.types.Bindings;

/**
 * A write-only parameter that describes a bean type (with a default constructor) and its property
 * for storing a result-set value. This kind of parameter is used to bind values from a query
 * result-set to bean properties (the order of columns and property names define the mapping). This
 * parameter does not support nested properties, i.e. properties of a property.
 *
 * <p>The SQL type can be defined with the property name. If omitted, SQL value converters are asked
 * to provide default SQL type based on the Java type of the property. The Java type cannot be
 * defined explicitly as it is determined by the type of the property.
 */
public final class TypePropParam extends Param {

  /**
   * Creates a new instance for bean-property-based writable parameter. This method also validates
   * the input data and there are issues that may cause a runtime exception, such as:
   * <ol>
   * <li>the bean is not defined;
   * <li>the write method or field for the provided bean property name is not found;
   * <li>the provided non-null SQL type may not be supported with the Java type of the property.
   * </ol>
   *
   * @param beanType The bean type that the property belongs to (required).
   * @param property The property name of the bean where the value will be stored. Also determines
   *     Java type of this parameter.
   * @param sqlType Optional SQL type for this property to override default.
   * @param initParamIndex The zero-based index value of the results (OUT) parameter where the value
   *     will be stored in a property.
   * @return A new parameter instance.
   */
  public static TypePropParam create(Class<?> beanType, String property, Integer sqlType,
      int initParamIndex) {
    AccessibleObject writer = BeanUtil.requireWriter(beanType, property);

    Class<?> propertyType;
    if (writer instanceof Method) {
      propertyType = ((Method) writer).getParameterTypes()[0];
    } else {
      propertyType = ((Field) writer).getType();
    }
    sqlType = Bindings.getInstance().confirmTypes(propertyType, sqlType);

    return new TypePropParam(beanType, writer, propertyType, sqlType, initParamIndex);
  }

  private final Class<?> beanType;

  private final AccessibleObject writer;

  private final int resultParamIndex;

  private TypePropParam(Class<?> beanType, AccessibleObject writer, Class<?> javaType,
      int sqlType, int resultParamIndex) {
    super(javaType, sqlType);
    this.beanType = beanType;
    this.writer = writer;
    this.resultParamIndex = resultParamIndex;
  }

  @Override
  public Object read(QueryContext ctx) {
    throw new RuntimeException("Detected illegal attempt to read result parameter");
  }

  @Override
  public void write(QueryContext ctx, Object value) {
    if (value != null) {
      Object instance = ctx.getResultsCollector().getRowValue(this.resultParamIndex);

      if (instance == null) {
        instance = BeanUtil.newInstance(this.beanType);
        ctx.getResultsCollector().setRowValue(this.resultParamIndex, instance);
      }

      BeanUtil.write(instance, this.writer, value);
    }
  }

  /**
   * Provides the zero-based index value of the results (OUT) parameter where the value will be
   * stored in a property.
   *
   * @return An integer, normally 0 or 1.
   */
  public int getResultParamIndex() {
    return this.resultParamIndex;
  }

  /**
   * Provides the Java type of the results (OUT) parameter where the value will be stored in a
   * property.
   *
   * @return A Java class.
   */
  public Class<?> getResultBeanType() {
    return this.beanType;
  }

  /**
   * Provides textual representation of this parameter. The returned value has the following format:
   *
   * <p><code>&lt;simple class name of bean&gt;.&lt;property write-method/field name&gt;(&lt;simple
   * class name&gt;|&lt;SQL type int value&gt;)</code>
   *
   * <p>For example: <code>Employee.setBirthDate(Date|93)</code>.
   *
   * @return Textual representation of this parameter instance.
   */
  @Override
  public String toString() {
    String name;
    if (this.writer instanceof Method) {
      name = ((Method) this.writer).getName();
    } else {
      name = ((Field) this.writer).getName();
    }
    return this.beanType.getSimpleName() + "." + name + "(" + super.toString() + ")";
  }

}
