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

/**
 * Classes for storing query results. This package is for internal use.
 *
 * <p>Currently, there are three supported scenarios:
 *
 * <ol>
 * <li><code>VoidResult</code> – the script is not expected to return any results;
 * <li><code>ListResult</code> – the script is expected to return a Java object per row (zero, one,
 * or more instances in total);
 * <li><code>MapResult</code> – the script is expected to return two Java objects per row (zero,
 * one, or more pair instances in total), and the first object will be used as a
 * <code>HashMap</code> key.
 * </ol>
 */
package ws.rocket.sqlstore.result;
