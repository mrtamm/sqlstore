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
 * <p>
 * The SQL type can be defined with the property name. If omitted, SQL value converters are asked to
 * provide default SQL type based on the Java type of the property. The Java type cannot be defined
 * explicitly as it is determined by the type of the property.
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
   * Java type of this parameter.
   * @param sqlType Optional SQL type for this property to override default.
   * @param firstParam Is this parameter the first of the bean properties. Determines if this
   * parameter also creates the bean when the value is to be stored.
   * @return A new parameter instance.
   */
  public static TypePropParam create(Class<?> beanType, String property, Integer sqlType,
      boolean firstParam) {
    AccessibleObject writer = BeanUtil.requireWriter(beanType, property);

    Class<?> propertyType;
    if (writer instanceof Method) {
      propertyType = ((Method) writer).getParameterTypes()[0];
    } else {
      propertyType = ((Field) writer).getType();
    }
    sqlType = Bindings.getInstance().confirmTypes(propertyType, sqlType);

    return new TypePropParam(beanType, writer, propertyType, sqlType, firstParam);
  }

  private final Class<?> beanType;

  private final AccessibleObject writer;

  private final boolean createBean;

  private TypePropParam(Class<?> beanType, AccessibleObject writer, Class<?> javaType,
      int sqlType, boolean firstProp) {
    super(javaType, sqlType);
    this.beanType = beanType;
    this.writer = writer;
    this.createBean = firstProp;
  }

  @Override
  public Object read(QueryContext ctx) {
    throw new RuntimeException("Detected illegal attempt to read result parameter");
  }

  @Override
  public void write(QueryContext ctx, Object value) {
    Object instance;

    if (this.createBean) {
      instance = BeanUtil.newInstance(this.beanType);
      ctx.pushResultItem(instance);
    } else {
      instance = ctx.getLastResultItem();
    }

    BeanUtil.write(instance, this.writer, value);
  }

  /**
   * Informs whether parameter also creates the bean instance and stores it in result values. This
   * normally happens when the property if the first among the list of properties for storing
   * result-set values.
   *
   * @return A Boolean that is true when this parameter creates a new instance of bean.
   */
  public boolean createsBean() {
    return this.createBean;
  }

  /**
   * Informs whether parameter also creates the bean instance of given type and stores it in result
   * values. This normally happens when the property if the first among the list of properties for
   * storing result-set values.
   *
   * @param expectedType The expected type of created bean.
   * @return A Boolean that is true when this parameter creates a new instance of bean given type.
   */
  public boolean createsBean(Class<?> expectedType) {
    return this.createBean && expectedType != null && expectedType.isAssignableFrom(this.beanType);
  }

  /**
   * Provides textual representation of this parameter. The returned value has following format:
   * <p>
   * "&lt;simple class name of bean&gt;.&lt;property write-method/field name&gt;(&lt;simple class
   * name&gt;|&lt;SQL type int value&gt;)"
   * <p>
   * For example: <code>Employee.setBirthDate(Date|93)</code>.
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
