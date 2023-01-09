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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import ws.rocket.sqlstore.ScriptSetupException;

/**
 * Helper methods for working with Java bean types and bean objects. All Java beans related code
 * ought to be gathered in this class.
 *
 * <p>Users of the library ought to not use this class as it is designed for the needs of the
 * SqlStore library and is open for non-compatible modifications.
 */
public final class BeanUtil {

  private static final Class<?>[] PRIMITIVES = {
      byte.class, char.class, short.class, int.class, long.class, float.class, double.class,
      boolean.class
  };

  private static final Class<?>[] PRIMITIVE_WRAPPERS = {
      Byte.class, Character.class, Short.class, Integer.class, Long.class, Float.class,
      Double.class, Boolean.class
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
        break;
      }
    }
    return result;
  }

  /**
   * Attempts to resolve the boxing class for given primitive type.
   *
   * @param primitiveClass A Java primitive class.
   * @return The class of the primitive wrapper, or null.
   */
  public static Class<?> getPrimitiveWrapperClass(Class<?> primitiveClass) {
    Class<?> result = null;
    for (int i = 0; i < PRIMITIVES.length; i++) {
      if (primitiveClass == PRIMITIVES[i]) {
        result = PRIMITIVE_WRAPPERS[i];
        break;
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
   *
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
      return clazz.getConstructor().newInstance();
    } catch (InstantiationException | InvocationTargetException
             | IllegalAccessException | NoSuchMethodException e) {
      throw new RuntimeException(String.format("Failed to create an instance of %s by invoking "
          + "default constructor.", clazz), e);
    }
  }

  /**
   * Attempts to resolve the method or field for reading the given property of the bean class.
   *
   * <p>The method must be public, non-static, with a non-void return type, and not expecting any
   * parameters. The method name is the property name with first letter in upper-case and with
   * prefix "is" (when return type is 'boolean') or "get".
   *
   * <p>When the read-method is not found, this method attempts to find public and non-static field
   * with the exactly same name as the property.
   *
   * <p>When both read-method and field are not found, this method will throw a runtime exception.
   *
   * @param clazz The class from which the method for reading the property is searched.
   * @param property The name of the property (not null, valid minimum length is 1).
   * @return The method or field for reading the property.
   */
  public static AccessibleObject requireReader(Class<?> clazz, String property) {
    String suffix = propToTitleCase(property);
    String methodNameGet = "get" + suffix;
    String methodNameIs = "is" + suffix;
    AccessibleObject reader = null;

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
      for (Field f : clazz.getFields()) {
        if (f.getName().equals(property)
            && Modifier.isPublic(f.getModifiers())
            && !Modifier.isStatic(f.getModifiers())) {
          reader = f;
          break;
        }
      }
    }

    if (reader == null) {
      throw new RuntimeException(String.format("Could not find method "
          + "[public void %s()] nor a public non-static and non-final field [%s] in "
          + "%s for writing property [%s]", methodNameGet, property, clazz, property));
    }

    return reader;
  }

  /**
   * Attempts to resolve the method or field for writing the given property of the bean class.
   *
   * <p>The method must be public, non-static, with a <code>void</code> return type, and expecting a
   * single parameter . The method name is the property name with first letter in upper-case and
   * with prefix "set".
   *
   * <p>When the write-method is not found, this method attempts to find public non-final and
   * non-static field with the exactly same name as the property.
   *
   * <p>When both write-method and field are not found, this method will throw a runtime exception.
   *
   * @param clazz The class from which the method or field for writing the property is searched.
   * @param property The name of the property (not null, valid minimum length is 1).
   * @return The method or field for writing the property.
   */
  public static AccessibleObject requireWriter(Class<?> clazz, String property) {
    String methodName = "set" + propToTitleCase(property);
    AccessibleObject writer = null;

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
      for (Field f : clazz.getFields()) {
        if (f.getName().equals(property)
            && Modifier.isPublic(f.getModifiers())
            && !Modifier.isFinal(f.getModifiers())
            && !Modifier.isStatic(f.getModifiers())) {
          writer = f;
          break;
        }
      }
    }

    if (writer == null) {
      throw new RuntimeException(String.format("Could not find a method "
              + "[public void %s(AnyType arg)] nor a public non-static and non-final field [%s] in "
              + "%s for writing property [%s]",
          methodName, property, clazz, property));
    }

    return writer;
  }

  /**
   * Attempts to read a value by calling the Java bean read-method on given instance.
   *
   * <p>Thrown checked exceptions will be wrapped into runtime exceptions.
   *
   * @param inst An object instance.
   * @param reader The read-method or public field for reading the value of the property.
   * @return The return-value of the property from the given reader.
   */
  public static Object read(Object inst, AccessibleObject reader) {
    try {
      Object result;

      if (reader instanceof Method) {
        result = ((Method) reader).invoke(inst);
      } else if (reader instanceof Field) {
        result = ((Field) reader).get(inst);
      } else {
        throw new IllegalArgumentException("Either method or field expected for reading the "
            + "property value. Got: " + reader);
      }

      return result;
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new ScriptSetupException(e, "Failed to call [%s] on [%s].", reader, inst);
    }
  }

  /**
   * Attempts to write a value by calling the Java bean write-method on given instance.
   *
   * <p>Thrown checked exceptions will be wrapped into runtime exceptions.
   *
   * @param inst An object instance.
   * @param writer The write-method for writing the value to a property.
   * @param value The value to write.
   */
  public static void write(Object inst, AccessibleObject writer, Object value) {
    try {
      if (writer instanceof Method) {
        ((Method) writer).invoke(inst, value);
      } else if (writer instanceof Field) {
        ((Field) writer).set(inst, value);
      } else {
        throw new IllegalArgumentException("Either method or field expected for writing the "
            + "property value. Got: " + writer);
      }
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new ScriptSetupException(e, "Failed to call [%s] on [%s] for storing value [%s].",
          writer, inst, value);
    }
  }

  private static String propToTitleCase(String property) {
    return Character.toUpperCase(property.charAt(0)) + property.substring(1);
  }

  private BeanUtil() {
    throw new AssertionError("Cannot create an instance of this class");
  }

}
