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

package ws.rocket.sqlstore;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation describing the row value type(s) when the method return type is collection or an
 * Object array. This annotation is read when a class acts as a proxy to SqlStore instance, and its
 * abstract method is being translated into a script call while the method returns a collection or
 * array type. In that case, the query row types cannot be inferred from the method signature and
 * this annotation can help bring clarification to the problem.
 *
 * <p>Value expectations depending on the method return type:
 *
 * <ul>
 * <li><code>java.util.List</code> -- the expected type of the list items.
 * <li><code>java.util.Map</code> -- the expected types for the map key and value (two classes!).
 * <li><code>java.lang.Object[]</code> -- the expected type of the array items.
 * <li><code>java.lang.Object[][]</code> -- the expected type of the inner array items.
 * <li><em>other cases</em> -- this annotation will be ignored.
 * </ul>
 *
 * <p>As always, the type(s) provided with this annotation must match the script result types. This
 * will be checked before executing the script.
 *
 * @see SqlStore#proxy(java.lang.Class)
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResultRow {

  /**
   * Defines the Java types of the row values returned by script execution. These types are
   * validated against the script definition (look for the OUT-parameter).
   *
   * @return Array of Java types that are expected to be extracted from a result-set row in given
   *     order.
   */
  Class<?>[] value();

}
