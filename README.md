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

Documentation and examples are covered on a [separate page](MANUAL.md).


Additional information
----------------------

SqlStore requires JDK 7 or newer for compiling and Java 7 or newer JVM for
running. It requires SLF4J logging API to be in classpath.

The project uses [Gradle build tool](http://www.gradle.org/) for development.

The project is open-sourced under the
[Apache Software License, version 2.0](LICENSE.md).

No official releases yet.

Suggestions, contributions, and issues are welcome through the
[issue tracker](https://github.com/mrtamm/sqlstore/issues).
