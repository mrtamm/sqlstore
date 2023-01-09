SqlStore Java Library
=====================

![Build Status](https://github.com/mrtamm/sqlstore/actions/workflows/main.yml/badge.svg)
[![Coverage Status](https://coveralls.io/repos/mrtamm/sqlstore/badge.svg?branch=master&service=github)](https://coveralls.io/github/mrtamm/sqlstore?branch=master)

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


Quick Demo
----------

SqlStore is all about separating Java application and SQL code for the sake of
decoupling and reducing the JDBC boilerplate code.

So one way to use SqlStore is to define just an interface that will be proxied
by SqlStore:

```java
package com.example.repo;

import com.example.data.Document;

public interface DocumentsRepo {

  DocumentInfo findDocument(Long id);

}
```

Now the SQL query is defined in the same package with the same name as the
interface, but with file extension `sqls`:
 _com/example/repo/DocumentsRepo.sqls_.

```
!Document=com.example.data.Document

findDocument IN(Long id)
  OUT(Document[id, sha256Hash, name, mediaType, created, owner])
====
SELECT id, sha256_hash, name, media_type, created_ts, owner_name
  FROM documents WHERE id = ?{id}
====
```

So the SQLS file basically defines the binding between Java and SQL/Database
code. On the first line beginning with exclamation mark (!) it defines an alias
"Document" for Java class `com.example.data.Document`. This is optional, but
helps us avoid writing out the full name of the class (alias name is arbitrary
but we just use the simple class name here).

Next comes a script block with the name "findDocument" (as the interface method
name), which takes a parameter `id` of type `java.lang.Long` (`java.lang`
classes do not require full package name here as well as Java primitive types).
The parameter value is required but may be null. The script definition also
defines an output type (per result-set) of type `com.example.data.Document`
where data from the result-set row will be placed in the properties and/or
fields of the Document object in given order: `id, sha256Hash, name, mediaType,
created, owner`. (The Document class is omitted here, but think of it as a plain
Java object with JavaBean properties or public non-final fields.)

SQL script is within marker lines (at least 4 equal-signs starting from first
column) and will be executed as-is (including line-breaks). Notice that the SQL
script refers to the IN-parameter "id", which will be provided as the parameter
of the script at the position of the question-mark (curly braces block will
vanish from the runtime SQL).

As you may add methods to the interface, you may add corresponding script blocks
to the SQLS file.

Runtime setup is now straight-forward:

```java
DocumentsRepo docs = SqlStore.proxy(DocumentsRepo.class, yourDataSource);
Document doc = docs.findDocument(100L);
```

In the previous code, SqlStore creates a proxy object, which it monitors for
calls. Thus, calling a method of the proxy object, the method signature will be
used for script block lookup and executing it. When the row with given ID value
is not found, the call will return null. When the result-set should contain more
than one row, only the first row object will be returned.

Also note that SqlStore does not keep track nor proxy the returned objects
(here: Document) as JPA solutions do.

With these few lines of code, we delegated JDBC execution task to SqlStore and
reduced the necessary Java code compared to what is usually needed with JDBC.


Dependencies
------------

SqlStore requires JDK 17 or newer for compiling and Java 17 or newer JVM for
running. It requires SLF4J logging API to be in classpath.

The project uses [Apache Maven](https://maven.apache.org/) for development.

There are database-dependant tests to verify JDBC-driver compatibility with
following databases:
* Derby 10.12: `mvn test -DtestDatabase=derby clean test`
* PostgreSQL 9.4: `mvn test -DtestDatabase=postgresql clean test`
* Oracle 11g: `mvn test -DtestDatabase=oracle clean test`

Some guidelines on how to prepare the environment for executing these tests on
an actual database are outlined in [database setup
README](src/test/resources/ws/rocket/sqlstore/test/db/README.md).

(Without database-specific configuration in test classpath, and just running
command `mvn test`, the database tests in
`ws.rocket.sqlstore.test.DatabaseTest` will be skipped.)


Additional Information
----------------------

The project is open-sourced under the
[Apache Software License, version 2.0](LICENSE.md).

No official releases yet.

Suggestions, contributions, and issues are welcome through the
[issue tracker](https://github.com/mrtamm/sqlstore/issues).
