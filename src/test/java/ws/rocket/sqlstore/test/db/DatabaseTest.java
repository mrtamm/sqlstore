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

package ws.rocket.sqlstore.test.db;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import ws.rocket.sqlstore.SqlStore;
import ws.rocket.sqlstore.connection.SharedConnectionManager;
import ws.rocket.sqlstore.test.db.model.Person;

/**
 * Integration tests for checking the overall functionality through SqlStore class.
 */
@Test
public class DatabaseTest {

  private static final Logger LOG = LoggerFactory.getLogger(DatabaseTest.class);

  private ScriptsFacade scripts;

  @Test
  public void executeQueries() {
    final String testDatabase = System.getProperty("testDatabase");

    if (testDatabase == null) {
      LOG.info("Database testing was skipped since system property 'testDatabase' is "
          + "undefined (expected value: 'derby', 'oracle', or 'postgresql').");
      return;
    }

    Connection con;
    try {
      con = initDbConnection(testDatabase);
      SharedConnectionManager.register(con);

    } catch (Exception e) {
      LOG.warn("Database testing was skipped due setup failure: " + e);
      return;
    }

    try {
      System.setProperty("sqlstore.path.suffix", "_" + testDatabase + ".sqls");
      this.scripts = SqlStore.proxy(ScriptsFacade.class, con);

      assertEquals(this.scripts, this.scripts, "The proxy must equal to itself.");

      if (LOG.isDebugEnabled()) {
        LOG.debug("HashCode for the proxy: {}", this.scripts.hashCode());
        LOG.debug(this.scripts.toString());
      }

      boolean tablesCreated = false;

      try {
        createTables();
        tablesCreated = true;

        addRecords();
        queryRecords();
        testProcedure();
        deleteRecords();

      } finally {
        if (tablesCreated) {
          dropTables();
        }
      }

    } finally {
      System.clearProperty("sqlstore.path.suffix");
      SharedConnectionManager.unregister();

      try {
        con.close();
      } catch (SQLException e) {
        LOG.warn("Exception while closing connection.", e);
      }
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
    Date createdAt = this.scripts.calcDateCreated(1L);
    LOG.debug("calcDateCreated() -> {}", createdAt);
  }

  private void dropTables() {
    this.scripts.dropTablePerson();
    this.scripts.dropSampleFunction();
  }

  private static Connection initDbConnection(String dbMode) throws Exception {
    ResourceBundle bundle = ResourceBundle.getBundle("ws.rocket.sqlstore.test.db.test_" + dbMode);

    String derbyPath = bundle.getString("jdbc.dbPath");
    if (!derbyPath.isEmpty()) {
      initDerbyDbPath(derbyPath);
    }

    Class.forName(bundle.getString("jdbc.driver"));

    Connection con = DriverManager.getConnection(
        bundle.getString("jdbc.url"),
        bundle.getString("jdbc.username"),
        bundle.getString("jdbc.password"));
    con.setAutoCommit(false);
    con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

    String schema = bundle.getString("jdbc.schema");
    String testTable = bundle.getString("jdbc.testTable");
    String testSequence = bundle.getString("jdbc.testSequence");

    verifySchemaExists(con, schema);
    removeObjectIfExists(con, schema, testTable, "TABLE");
    removeObjectIfExists(con, schema, testSequence, "SEQUENCE");

    if (!testSequence.isEmpty()) {
      execStmt(con, "CREATE SEQUENCE " + testSequence);
      LOG.info("OK: Sequence '{}' was explicitly created.", schema);
    }

    return con;
  }

  private static void initDerbyDbPath(String derbyPath) throws IOException {
    File dbPath = new File(derbyPath);
    if (!dbPath.exists()) {
      dbPath.mkdirs();
    }

    String canonicalPath = dbPath.getCanonicalPath();
    LOG.debug("DERBY path: {}", canonicalPath);
    System.setProperty("derby.system.home", canonicalPath);
  }

  private static void verifySchemaExists(Connection con, String schema) throws SQLException {
    if (schema == null || schema.isEmpty() || schema.contains(" ")) {
      LOG.info("Skipping: verify schema exists.");
      return;
    }

    try (ResultSet schemas = con.getMetaData().getSchemas()) {
      while (schemas.next()) {
        if (schema.equalsIgnoreCase(schemas.getString(1))) {
          LOG.info("OK: Schema '{}' exists.", schema);
          return;
        }
      }
    }

    execStmt(con, "CREATE SCHEMA " + schema);
    LOG.info("OK: Schema '{}' was explicitly created.", schema);
  }

  private static void removeObjectIfExists(Connection con, String schema, String objName,
      String objType) throws SQLException {

    if (objName == null || objName.isEmpty() || objName.contains(" ")) {
      LOG.info("Skipping: remove {} {}, if exists.", objType, objName);
      return;
    }

    DatabaseMetaData meta = con.getMetaData();

    try (ResultSet rs = meta.getTables(null, schema, objName, new String[] { objType })) {
      if (!rs.next()) {
        LOG.info("OK: {} '{}' does not exist.", objType, objName);
        return;
      }
    }

    execStmt(con, "DROP " + objType + " " + objName);
    LOG.info("OK: {} '{}' was explicitly dropped.", objType, objName);
  }

  private static void execStmt(Connection con, String stmt) throws SQLException {
    try (Statement s = con.createStatement()) {
      s.execute(stmt);
    }
    con.commit();
  }

  private static Date toDate(int year, int month, int day) {
    Calendar cal = Calendar.getInstance();
    cal.set(year, month - 1, day, 0, 0, 0);
    return cal.getTime();
  }

}
