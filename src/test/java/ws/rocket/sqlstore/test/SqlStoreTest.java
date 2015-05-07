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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import org.apache.commons.dbcp2.BasicDataSource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.SqlStore;
import ws.rocket.sqlstore.connection.SharedConnectionManager;
import ws.rocket.sqlstore.test.model.Person;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Integration tests for checking the overall functionality through SqlStore class.
 */
public class SqlStoreTest {

  private BasicDataSource dataSource;

  private ScriptsFacade scripts;

  @BeforeClass
  public void setUp() throws SQLException, IOException {
    ResourceBundle bundle = ResourceBundle.getBundle("ws.rocket.sqlstore.test.test");

    File dbPath = new File(bundle.getString("jdbc.dbPath"));
    if (!dbPath.exists()) {
      dbPath.mkdirs();
    }

    System.setProperty("derby.system.home", dbPath.getCanonicalPath());

    BasicDataSource ds = new BasicDataSource();
    ds.setDriverClassName(bundle.getString("jdbc.driver"));
    ds.setUrl(bundle.getString("jdbc.url"));
    ds.setDefaultReadOnly(true);
    ds.setDefaultAutoCommit(false);
    ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    ds.setInitialSize(Integer.parseInt(bundle.getString("jdbc.initialConnections")));

    this.dataSource = ds;

    boolean tableExists;
    try (Connection c = this.dataSource.getConnection()) {
      DatabaseMetaData meta = c.getMetaData();
      try (ResultSet rs = meta.getTables(null, "SQLSTORE", "PERSON", new String[] { "TABLE" })) {
        tableExists = rs.next();
      }
    }

    if (tableExists) {
      dropTables();
    }
  }

  @AfterClass
  public void tearDown() {
    try {
      this.dataSource.close();
    } catch (SQLException e) {
      throw new RuntimeException("Unexpected problem when shutting down the data source.", e);
    }
  }

  @Test
  public void executeQueries() throws SQLException {
    this.scripts = SqlStore.proxy(ScriptsFacade.class);

    assertTrue(this.scripts.equals(this.scripts), "The proxy must equal to itself.");
    System.out.println("HashCode for the proxy: " + this.scripts.hashCode());
    System.out.println(this.scripts.toString());

    SharedConnectionManager.register(this.dataSource);
    boolean tablesCreated = false;

    try {
      createTables();
      tablesCreated = true;
      addRecords();
      queryRecords();
      deleteRecords();
      testProcedure();
    } finally {
      if (tablesCreated) {
        dropTables();
      }
      SharedConnectionManager.unregister();
    }
  }

  private void createTables() {
    this.scripts.createTablePerson();
    this.scripts.createSampleFunction();
  }

  private void addRecords() {
    Person person = new Person();
    person.setCreatedBy(1L);
    person.setActive(true);

    insertPerson(person, 1L, "God", toDate(1976, 1, 5));
    insertPerson(person, 2L, "Harry from the Valley", toDate(1977, 2, 3));
    insertPerson(person, 3L, "Candide Berdide", toDate(1978, 4, 11));
    insertPerson(person, 4L, "Dennis Dormis", toDate(1979, 5, 14));
  }

  private void insertPerson(Person person, Long expectId, String name, Date birthDate) {
    person.setName(name);
    person.setDateOfBirth(birthDate);

    int updateCount = this.scripts.insertPerson(person);

    assertEquals(updateCount, 1, "checking the number of rows updated");
    assertEquals(person.getId(), expectId, "checking the generated ID");
  }

  private void queryRecords() {
    // Explicit checks by ID:
    selectPerson(1L, true);
    selectPerson(2L, true);
    selectPerson(3L, true);
    selectPerson(4L, true);
    selectPerson(5L, false);

    // Checking that both queries would return equal records.
    List<Person> persons = this.scripts.findPersons(true);
    assertEquals(persons.size(), 4, "checking Persons count");

    for (Person person : persons) {
      Person p = this.scripts.findPersonById(person.getId());
      assertNotNull(p, "query should return a Person object");
      assertEquals(p, person, "'findPersons' and 'findPersonById' should return equal Persons");
    }
  }

  private void selectPerson(Long id, boolean nonEmpty) {
    Person p = this.scripts.findPersonById(id);

    if (nonEmpty) {
      assertNotNull(p, "query should return a Person object");
      assertEquals(p.getId(), id, "checking ID on the returned Person object");
    } else {
      assertNull(p, "query should return null because record was not found");
    }
  }

  private void deleteRecords() {
    for (long id = 4; id > 0; id--) {
      int count = this.scripts.deletePerson(id);
      assertEquals(count, 1, "checking the number of rows updated (expecting one)");
    }

    int count = this.scripts.deletePerson(4L);
    assertEquals(count, 0, "checking the number of rows updated (none expected)");
  }

  private void testProcedure() {
    // To be done.
//    Date createdAt = this.sqlStore.query("calcDateCreated", 1L).forValue(Date.class);
  }

  private void dropTables() {
    this.scripts.dropTablePerson();
    this.scripts.dropSampleFunction();
  }

  private static Date toDate(int year, int month, int day) {
    Calendar cal = Calendar.getInstance();
    cal.set(year, month - 1, day, 0, 0, 0);
    return cal.getTime();
  }

}
