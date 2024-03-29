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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.db.model.Organization;
import ws.rocket.sqlstore.db.model.Person;

/**
 * Tests the {@link BeanUtil} class.
 */
@Test
public final class BeanUtilTest {

  public void shouldEvaluateToMethodForRead() {
    AccessibleObject reader = BeanUtil.requireReader(Person.class, "dateOfBirth");

    assertNotNull(reader);
    assertEquals(reader.getClass(), Method.class);
    assertEquals(((Method) reader).getName(), "getDateOfBirth");
  }

  public void shouldEvaluateToMethodForWrite() {
    AccessibleObject writer = BeanUtil.requireWriter(Person.class, "dateOfBirth");

    assertNotNull(writer);
    assertEquals(writer.getClass(), Method.class);
    assertEquals(((Method) writer).getName(), "setDateOfBirth");
  }

  public void shouldEvaluateToFieldForRead() {
    AccessibleObject reader = BeanUtil.requireReader(Organization.class, "yearFounded");

    assertNotNull(reader);
    assertEquals(reader.getClass(), Field.class);
    assertEquals(((Field) reader).getName(), "yearFounded");
  }

  public void shouldEvaluateToFieldForWrite() {
    AccessibleObject writer = BeanUtil.requireWriter(Organization.class, "yearFounded");

    assertNotNull(writer);
    assertEquals(writer.getClass(), Field.class);
    assertEquals(((Field) writer).getName(), "yearFounded");
  }

  @Test(dependsOnMethods = "shouldEvaluateToMethodForRead")
  public void shouldReadWriteMethod() {
    AccessibleObject reader = BeanUtil.requireReader(Person.class, "dateOfBirth");
    AccessibleObject writer = BeanUtil.requireWriter(Person.class, "dateOfBirth");

    Object obj = BeanUtil.newInstance(Person.class);
    Date value = new Date(1000000);

    BeanUtil.write(obj, writer, value);
    Object valueRead = BeanUtil.read(obj, reader);

    assertSame(valueRead, value);
  }

  @Test(dependsOnMethods = "shouldEvaluateToFieldForRead")
  public void shouldReadWriteField() {
    AccessibleObject field = BeanUtil.requireReader(Organization.class, "yearFounded");
    Object obj = BeanUtil.newInstance(Organization.class);
    Integer value = 64;

    BeanUtil.write(obj, field, value);
    Object valueRead = BeanUtil.read(obj, field);

    assertEquals(valueRead, value);
  }

  @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp
      = "Failed to create an instance of class ws\\.rocket\\.sqlstore\\.script\\."
      + "BeanUtilTest\\$BlockingClass by invoking default constructor\\.")
  public void shouldFailToCreateNewInstance() {
    BeanUtil.newInstance(BlockingClass.class);
  }

  @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp
      = "Could not find method \\[public void getProperty\\(\\)] nor a public "
      + "non-static and non-final field \\[property] in class "
      + "ws\\.rocket\\.sqlstore\\.script\\.BeanUtilTest\\$BlockingClass for writing "
      + "property \\[property]")
  public void shouldFailToFindPropertyReader() {
    BeanUtil.requireReader(BlockingClass.class, "property");
  }

  @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp
      = "Could not find a method \\[public void setProperty\\(AnyType arg\\)] nor a public "
      + "non-static and non-final field \\[property] in class "
      + "ws\\.rocket\\.sqlstore\\.script\\.BeanUtilTest\\$BlockingClass "
      + "for writing property \\[property]")
  public void shouldFailToFindPropertyWriter() {
    BeanUtil.requireWriter(BlockingClass.class, "property");
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "Failed to call \\[null] on \\[some value].")
  public void shouldFailToReadProperty() {
    BeanUtil.read("some value", null);
  }

  @Test(expectedExceptions = ScriptSetupException.class, expectedExceptionsMessageRegExp
      = "Failed to call \\[null] on \\[some value] for storing value \\[new value]\\.")
  public void shouldFailToWriteProperty() {
    BeanUtil.write("some value", null, "new value");
  }

  @Test(expectedExceptions = InvocationTargetException.class)
  public void shouldFailOnInstantiation() throws Exception {
    Constructor<BeanUtil> constructor = BeanUtil.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  private static class BlockingClass {

    BlockingClass() {
      throw new RuntimeException("Cannot create instance of me");
    }

  }

}
