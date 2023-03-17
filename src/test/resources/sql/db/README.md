Testing with Actual Databases
=============================

It is possible to run a full integration test using an actual database.
Currently, Derby, PostgreSQL and Oracle SE are covered. More details below.



Environment Setup for Testing SqlStore with Derby 10.16
-------------------------------------------------------

There are no preparation steps for running SqlStore database tests.

The path, where the database folder will be created, is defined in the
`test_derby.properties` file. By default, the folder will be below the
target-folder, so that `mvn clean` command always deletes the folder with its
content.

The database will run in embedded mode. SqlStore database will be prepared by
the test class.



Environment Setup for Testing SqlStore with Oracle 11g
------------------------------------------------------

Preparation consists of two phases:

1. Install the JDBC driver in your local Maven repository.
2. Prepare the Oracle 11g database


### Installation of the JDBC Driver

Download the driver from here:
http://www.oracle.com/technetwork/apps-tech/jdbc-112010-090769.html

Add the downloaded JAR-file to your Maven repository:

```
mvn install:install-file \
    -Dfile=path/to/ojdbc6.jar \
    -DgroupId=oracle.jdbc \
    -DartifactId=ojdbc6 \
    -Dversion=11.2.0.4 \
    -Dpackaging=jar
```
Add Oracle driver as dependency manually to the pom.xml:

    <dependency>
      <groupId>oracle.jdbc</groupId>
      <artifactId>ojdbc6</artifactId>
      <version>11.2.0.4</version>
      <scope>test</scope>
    </dependency>

(Please don't commit that dependency to Git repository as that dependency is not
publicly available due to some restrictions.)


### Preparation of the Oracle 11g Database

Database installation procedure is not covered here. However, once the database
is running, an administrative user (like SYSTEM) should execute following
statements in order to prepare SQLSTORE user-schema that the tests depend on.

```sql
-- USER SQL
CREATE USER sqlstore
  IDENTIFIED BY 'sqlstore'
  DEFAULT    TABLESPACE "USERS"
  TEMPORARY  TABLESPACE "TEMP";

-- SYSTEM PRIVILEGES
GRANT CONNECT,
      CREATE SESSION,
      CREATE TABLE,
      CREATE SEQUENCE,
      CREATE TRIGGER,
      CREATE PROCEDURE,
      UNLIMITED TABLESPACE
   TO sqlstore
```

The SQLSTORE user may be dropped when it is not needed anymore for testing.



Environment Setup for Testing SqlStore with PostgreSQL 9.4
----------------------------------------------------------

Preparation consists of just adding an SQLSTORE database/schema/user in the
database.

It can be done on command-line. For example, on Ubuntu:

```
sudo -u postgres createuser sqlstore
sudo -u postgres --owner=sqlstore --encoding=UTF-8 createdb sqlstore
```
Or as an administrative user on the database console (`psql`):

```
CREATE ROLE sqlstore;
CREATE DATABASE sqlstore WITH OWNER = sqlstore ENCODING = 'UTF8'
CREATE SCHEMA sqlstore AUTHORIZATION sqlstore;
```

The SQLSTORE database may be dropped when it is not needed anymore for testing.
