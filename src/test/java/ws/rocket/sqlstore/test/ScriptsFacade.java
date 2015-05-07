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

package ws.rocket.sqlstore.test;

import java.util.Date;
import java.util.List;
import ws.rocket.sqlstore.ResultRow;
import ws.rocket.sqlstore.UpdateCount;
import ws.rocket.sqlstore.test.model.Person;

/**
 * This interface acts as a proxy facade for an SqlStore instance. The method names and signatures
 * correspond to scripts in the scripts file in the same package but under resources.
 * <p>
 * This facade will be used to load the scripts: <code>SqlStore.proxy(ScriptsFacade.class)</code>.
 * Existence of corresponding scripts in the file will be checked during the load time.
 */
public interface ScriptsFacade {

  void createTablePerson();

  void createSampleFunction();

  void dropTablePerson();

  void dropSampleFunction();

  @UpdateCount
  int insertPerson(Person p);

  @UpdateCount
  int updatePerson(Person p);

  @UpdateCount
  int deletePerson(Long id);

  @ResultRow(Person.class)
  List<Person> findPersons(boolean onlyActive);

  Person findPersonById(Long id);

  Date calcDateCreated(Long id);

}
