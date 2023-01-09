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
 * Classes for handling Java and SQL value conversions before and after statement execution.
 *
 * <p>All handlers must implement {@link ws.rocket.sqlstore.types.ValueMapper} interface, and must
 * be registered at {@link ws.rocket.sqlstore.types.Bindings} registry to be effective.
 *
 * <p>The registry is initialized by registering custom value mappers via
 * {@link ws.rocket.sqlstore.types.Bindings#register(ws.rocket.sqlstore.types.ValueMapper...)} or on
 * first call to {@link ws.rocket.sqlstore.types.Bindings#getInstance()}. Initialization can happen
 * only once, and it will remain effective until JVM terminates.
 */
package ws.rocket.sqlstore.types;
