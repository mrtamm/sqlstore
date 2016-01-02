SqlStore Java Library
=====================

SqlStore is a Java library that takes communication with a database over JDBC
API to the next level by

 1. providing a simplified API for invoking queries and mapping results back to
    objects (similar to JdbcTemplate from Spring framework), and
 2. by externalizing SQL code from Java code (similar to WEB4J framework).

SqlStore is small and easy to learn, it does not introduce a fat runtime layer
for managing SQL statements. Understanding and managing SQL code of an
application becomes significantly more convenient.


Documentation
-------------

Documentation and examples are covered in [the manual](MANUAL.md).


Dependencies
------------

SqlStore requires JDK 7 or newer for compiling and Java 7 or newer JVM for
running. It requires SLF4J logging API to be in classpath.

The project uses [Apache Maven](https://maven.apache.org/) for development.

There are database-dependant tests to verify JDBC-driver compatibility with
following databases:
* Derby 10.12: `mvn test -DtestDatabase=derby clean test`
* PostgreSQL 9.4: `mvn test -DtestDatabase=postgresql clean test`
* Oracle 11g: `mvn test -DtestDatabase=oracle clean test`

Some guidelines on how to prepare the environment for executing these tests on
an actual database are outlined in
`src/test/resources/config/_dbname_/README.md`

(Without database-specific configuration in test classpath, and just running
command `mvn test`, the database tests in
`ws.rocket.sqlstore.test.DatabaseTest` will silently skip.)


Additional Information
----------------------

The project is open-sourced under the
[Apache Software License, version 2.0](LICENSE.md).

No official releases yet.

Suggestions, contributions, and issues are welcome through the
[issue tracker](https://github.com/mrtamm/sqlstore/issues).
