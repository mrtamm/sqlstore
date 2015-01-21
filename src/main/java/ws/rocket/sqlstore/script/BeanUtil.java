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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import ws.rocket.sqlstore.ScriptSetupException;

/**
 * Helper methods for working with Java bean types and bean objects. All Java beans related code
 * ought to be gathered in this class.
 * <p>
 * Users of the library ought to not use this class as it is designed for the needs of the SqlStore
 * library and is open for non-compatible modifications.
 */
public final class BeanUtil {

  private static final Class<?>[] PRIMITIVES = {
    byte.class, char.class, short.class, int.class, long.class, float.class, double.class,
    boolean.class
  };

  /**
   * Attempts to resolve a class of a primitive by the given name.
   *
   * @param name The Java primitive name.
   * @return The class of the primitive with the same name, or null.
   */
  public static Class<?> getPrimitiveClass(String name) {
    Class<?> result = null;
    for (Class<?> p : PRIMITIVES) {
      if (p.getName().equals(name)) {
        result = p;
      }
    }
    return result;
  }

  /**
   * Attempts to load the class with given complete class name. When the class is not found, null
   * value will be returned. This method does not consider primitives, therefore primitive names
   * will also result with null.
   *
   * @param fullName A class name.
   * @return The Class instance, or null.
   * @see #getPrimitiveClass(java.lang.String)
   */
  public static Class<?> getClass(String fullName) {
    try {
      return Class.forName(fullName);
    } catch (ClassNotFoundException ex) {
      return null;
    }
  }

  /**
   * Attempts to create and return a new instance of given class by invoking default constructor.
   * Checked exceptions will be converted into runtime exceptions.
   *
   * @param clazz The class to be instantiated.
   * @return A new instance of the class.
   */
  public static Object newInstance(Class<?> clazz) {
    try {
      return clazz.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(String.format("Failed to create an instance of %s by invoking "
          + "default constructor.", clazz), e);
    }
  }

  /**
   * Attempts to resolve the method for reading the given property of the bean class.
   * <p>
   * The method must be public, non-static, with a non-void return type, and not expecting any
   * parameters. The method name is the property name with first letter in upper-case and with
   * prefix "is" (when return type is 'boolean') or "get".
   * <p>
   * When the read-method is not found, this method will throw a runtime exception.
   *
   * @param clazz The class from which the method for reading the property is searched.
   * @param property The name of the property (not null, valid minimum length is 1).
   * @return The method for reading the property.
   */
  public static Method requireReader(Class<?> clazz, String property) {
    String suffix = propToTitleCase(property);
    String methodNameGet = "get" + suffix;
    String methodNameIs = "is" + suffix;
    Method reader = null;

    for (Method classMethod : clazz.getMethods()) {
      boolean isMethod = methodNameIs.equals(classMethod.getName())
          && Boolean.TYPE.equals(classMethod.getReturnType());
      boolean getMethod = !isMethod
          && methodNameGet.equals(classMethod.getName())
          && !Void.TYPE.equals(classMethod.getReturnType());

      if ((isMethod || getMethod)
          && classMethod.getParameterTypes().length == 0
          && Modifier.isPublic(classMethod.getModifiers())
          && !Modifier.isStatic(classMethod.getModifiers())) {
        reader = classMethod;
        break;
      }
    }

    if (reader == null) {
      throw new RuntimeException(String.format("Could not find method "
          + "[public void %s(AnyType arg)] in %s for writing property [%s]",
          methodNameGet, clazz, property));
    }

    return reader;
  }

  /**
   * Attempts to resolve the method for writing the given property of the bean class.
   * <p>
   * The method must be public, non-static, with a <code>void</code> return type, and expecting a
   * single parameter . The method name is the property name with first letter in upper-case and
   * with prefix "set".
   * <p>
   * When the write-method is not found, this method will throw a runtime exception.
   *
   * @param clazz The class from which the method for reading the property is searched.
   * @param property The name of the property (not null, valid minimum length is 1).
   * @return The method for writing the property.
   */
  public static Method requireWriter(Class<?> clazz, String property) {
    String methodName = "set" + propToTitleCase(property);
    Method writer = null;

    for (Method classMethod : clazz.getMethods()) {
      if (methodName.equals(classMethod.getName())
          && classMethod.getParameterTypes().length == 1
          && Void.TYPE.equals(classMethod.getReturnType())
          && Modifier.isPublic(classMethod.getModifiers())
          && !Modifier.isStatic(classMethod.getModifiers())) {
        writer = classMethod;
        break;
      }
    }

    if (writer == null) {
      throw new RuntimeException(String.format("Could not find method "
          + "[public void %s(AnyType arg)] in %s for writing property [%s]",
          methodName, clazz, property));
    }

    return writer;
  }

  /**
   * Attempts to read a value by calling the Java bean read-method on given instance.
   * <p>
   * Thrown checked exceptions will be wrapped into runtime exceptions.
   *
   * @param inst An object instance.
   * @param reader The read-method for reading the property.
   * @return The return-value of the method.
   */
  public static Object read(Object inst, Method reader) {
    try {
      return reader.invoke(inst);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new ScriptSetupException(msgFailed(inst, reader), e);
    }
  }

  /**
   * Attempts to write a value by calling the Java bean write-method on given instance.
   * <p>
   * Thrown checked exceptions will be wrapped into runtime exceptions.
   *
   * @param inst An object instance.
   * @param writer The write-method for writing the value to a property.
   * @param value The value to write.
   */
  public static void write(Object inst, Method writer, Object value) {
    try {
      writer.invoke(inst, value);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new ScriptSetupException(msgFailed(inst, writer, value), e);
    }
  }

  private static String propToTitleCase(String property) {
    return Character.toUpperCase(property.charAt(0)) + property.substring(1);
  }

  private static String msgFailed(Object inst, Method method) {
    return String.format("Failed to call [%s] on [%s].", method, inst);
  }

  private static String msgFailed(Object inst, Method method, Object value) {
    return String.format("Failed to call [%s] on [%s] for storing value [%s].",
        method, inst, value);
  }

  private BeanUtil() {
    throw new AssertionError("Cannot create an instance of this class");
  }

}
