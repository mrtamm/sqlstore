SqlStore Reference Manual
=========================

SqlStore is a small Java library for executing SQL scripts over JDBC. SqlStore
takes care of mapping parameters and reading results back to Java objects. The
library also demands SQL scripts to be placed outside of Java code into separate
SQL scripts file (.sqls) identified by class name that associates with the
scripts. The Java class needs to identify the script to be executed by name, and
pass the required parameters.


Tabel of Contents
-----------------

1. [Why SqlStore?](#why-sqlstore)
2. [How Does It Work?](#how-does-it-work)
3. [The Java API of SqlStore](#the-java-api-of-sqlstore)
  1. [Working With SqlStore Instance](#working-with-sqlstore-instance)
  2. [Working With SqlStore Through Proxy Interface](#working-with-sqlstore-through-proxy-interface)
  3. [Statement Parameter Value Binding](#statement-parameter-value-binding)
  4. [Exception Handling](#exception-handling)
  5. [Inspection](#inspection)
4. [The SQL Scripts File](#the-sql-scripts-file)
5. [Java Type Aliases](#java-type-aliases)
6. [SQL Script Declaration](#sql-script-declaration)
7. [SQL Script Parameters](#sql-script-parameters)
  1. [Script Input Parameters](#script-input-parameters)
  2. [Script Output Parameters](#script-output-parameters)
  3. [Updated Script Input Parameters](#updated-script-input-parameters)
  4. [Specifying Result-Set Type](#specifying-result-set-type)
  5. [Statement Hints](#statement-hints)
10. [SQL statement](#sql-statement)
  1. [Dynamically Included SQL Statement Parts](#dynamically-included-sql-statement-parts)
  2. [Binding for Script Parameters](#binding-for-script-parameters)
  3. [Support for Stored Procedures](#support-for-stored-procedures)
  4. [Escaping Special Characters](#escaping-special-characters)
11. [Logging](#logging)
12. [Additional Information](#additional-information)


Why SqlStore?
-------------

The library was designed and created to address these problems:

1. Too few lightweight and framework-independent libraries to simplify
   execution of SQL scripts.
2. Using JDBC API directly provides faster execution but the code becomes
   repetitive and verbose, accidental typos too easy to make.
3. SQL scripts do not fit well into Java code. Short scripts are fine, of
   course, but in practice a script (as a Java String) usually covers multiple
   lines, needs to be presented as readable as possible but is also subject to
   unwanted modifications by code formatters.
4. Placing SQL scripts into external resources has been talked about but no good
   mechanism exists. Java property files tend to be not that good.

SqlStore supports the most commonly used part of the JDBC API: plain statements,
prepared statements, call statements (but users of SqlStore don't have to think
about them). In addition, dynamically including or excluding parts of the SQL
script is also possible when defining scripts. SqlStore itself does not
initiate or close connections but just uses are previously initiated JDBC
connection or data-source.

SqlStore does not (yet) support dynamically generated _WHERE x IN (...)_
clauses for a list of values, and batch execution of statements. However, these
are considered and might be included in future versions when technically
feasible.


How Does It Work?
-----------------

Here is a general overview of how SqlStore is used to execute SQL scripts.

1. Developer creates a class that is used by other classes making queries to
   database. This class would be a typical Data Access Object (DAO).
2. Developer creates a file in the same package as the DAO class, with the same
   name as the DAO class but with extension ".sqls" for placing SQL scripts. The
   file may be empty at first. The file will contain script definitions
   declaring the script name, its parameters, results handling and the SQL
   script itself.
3. On construction, the object calls `SqlStore.load()`, optionally also
   providing the JDBC connection or data-source to use (SqlStore supports
   late-connection providing, therefore connection/data-source does not have to
   be provided immediately on loading).
4. In the Java class, when a script needs to be executed, the loaded `SqlStore`
   object is used for pre-defined query lookup, parameter binding, execution,
   and for extracting results. For example:
   `sqlStore.query("queryName", param1, param2).forValue(int.class);`

Usage of SqlStore Java API consists of two steps: query lookup with parameters,
and invoking execution. There are several Java methods for the second part
depending on whether the script is expected to return any results and how many
is to be returned.

Here are few examples for class _com.sample.db.AuthenticationProvider_. First
is the scripts file in class-path.

_com/sample/db/AuthenticationProvider.sqls_:

```
isAuthenticationValid IN(String username, String passwordHash) OUT(boolean) {
SELECT COUNT(*)
  FROM users
 WHERE username = ${username}
   AND passhash = ${passowrdHash}
   AND active = 'Y'
}

addLoginAttempt IN(com.sample.db.login.LoginAttempt login) {
INSERT INTO login_attempt (id, username, ip_address, auth_time, success)
VALUES (login_attempt_seq.nextval, ${login.username}, ${login.ipAddress}, SYSDATE, ${login.success})
}
```

_com/sample/db/AuthenticationProvider.java_:

```java
package com.sample.db;

import com.sample.db.login.LoginAttempt;
import ws.rocket.sqlstore.SqlStore;

public class AuthenticationProvider {

  private final SqlStore sqlStore = SqlStore.load(AuthenticationProvider.class);

  public boolean authenticate(LoginAttempt loginAttempt) {
    boolean valid = sqlStore.query("isAuthenticationValid",
        loginAttempt.getUsername(),
        loginAttempt.getPasswordHash()).forValue(boolean.class);

    loginAttempt.setSuccess(valid);

    sqlStore.query("addLoginAttempt", loginAttempt).execute();

    return valid;
  }
}
```

Regarding the previous sample, notice how in the scripts file the binding
between parameter values and the SQL script is done. The places, where the
parameter values are substituted, are actually replaced with question marks
(?), as usually with JDBC statements. At execution time, parameter values are
evaluated based on given expressions and are bound to the JDBC statement.

SqlStore also supports transactions as long as the block of statements is
defined together:

```java
sqlStore.block(new Block() {

  public void execute(SqlStore sql) {
    Integer v = sql.query("queryName1", param1).forValue(Integer.class);
    sql.query("queryName2", v).execute();
  }

});
```

The SqlStore object commits the transaction when the block completes without any
kind of exeption. Otherwise, it rolls back. It is important that the block would
use the provided SqlStore instance instead of the one stored in the DAO class!



The Java API of SqlStore
------------------------

In general, users of SqlStore only need to use the `SqlStore` class from the
library. This class is used for loading scripts into memory, calling execution
of the scripts (including with transactions) and for printing out the loaded
scripts. Please refer to the documentation of the `SqlStore` class for details.

### Working With SqlStore Instance

To begin with, an `SqlStore` instance must be created by loading scripts from a
scripts file, which is identified by a class:

```java
SqlStore sql = SqlStore.load(MyClass.class);
```

The load process also validates the scripts file contents. The file is assumed
to be located at the same folder and have the same name as the class, only with
a different extension: `sqls`. This way the scripts are always tied to some Java
class, and can be identified where they are used.

The `SqlStore.load` method also accepts `javax.sql.DataSource` or
`java.sql.Connection` instance as its second parameter. The recommended way is
to provide database access method directly to the `SqlStore` instace.

Alternative approach is shown in the previous code sample where the
database connection information is omitted. In that case, the connection to use
must be registered separately (before executing any scripts), and all `SqlStore`
instances, that have no explicit connection reference, will default to that
_shared_ one instead:

```java
import ws.​rocket.​sqlstore.​connection.SharedConnectionManager;

// ...

// Define global javax.sql.DataSource:
SharedConnectionManager.register(createDataSource());
// or java.sql.Connection:
SharedConnectionManager.register(createConnection());
```

Note that `SharedConnectionManager` can be used to redefine the connection to a
database without restarting or reloading scripts.

The next logical step is to call the loaded SQL scripts stored in the file. This
is done by providing the script name and values for its IN-parameters, when
present.

```java
Map<String, Integer> counts =
    SqlStore.query("scriptName", "param1", "param2")
            .forMap(String.class, Integer.class);
```

The previous example shows how executing a script consists of two parts:

1. loading the script by name and optional parameters (`query()`), and
2. executing it by defining the output (`forMap()`).

Since various output types are supported, the second part varies. However,
looking up the script as the first step is always the same (the object returned
by the `query()` method cannot be reused for executing multiple times).

Supported execution methods and return types are following:

* `void execute()` – just execute without reading results;
* `int updateCount()` – execute and read the count of updated rows;
* `List<V> forValues(Class<V>)` – read the values (with type `V`) of a single
   column result (empty result returns empty list);
* `V forValue(Class<V>)` – read the first value (with type `V`) of a single
  column result (empty result returns `null`);
* `Object[][] forRows(Class<?>[])` – read the values (where provided classes
  match the types of columns) of query result with any number of columns and
  rows (inner `Object[]` represents a row, while outer `Object[][]` is all rows);
* `Object[] forRow(Class<?>[])` – read the values (where provided classes
  match the types of columns) of the first row of the query result with any
  number of columns (empty result returns `null`);
* `Map<K, V> forMap(Class<K>, Class<V>)` – read key-value pairs from the query
  result, which is commonly with two columns.

Please refer to the documentation of the `ws.rocket.sqlstore.Query` class for
more details.


### Working With SqlStore Through Proxy Interface

Invoking an SQL script is similar to executing a method of a Java class: it has
a name, input parameters, and a return type. So to make it even easier, it is
also  possible to hide the usage of an SqlStore instance behind an interface.
The same rules as using the instance directly still apply, however, it becomes
more convenient.

When using the proxy interface, the loading step becomes as following:

```java
MyQueries queries = SqlStore.proxy(MyQueries.class);
```

As with the `SqlStore.load()` method, the `proxy()` method also accepts a
`javax.sql.DataSource` or `java.sql.Connection` as the second parameter.

To invoke a script, SqlStore expects that the name of the called method is the
name of the script, and its parameters are the IN-parameters for the script. The
return type of the method is used for evaluating which execution mode is
selected. Based on the previous example, it can look following:

```java
Map<String, Integer> counts = queries.scriptName("param1", "param2");
```

Annotations must be used in certain cases:

1. To return the number of updated rows, the method must return an `int` and
   must be annotated with `@UpdateCount`;
2. To specify column types (List or Map value types) when the method returns a
   `List`, `Map`, or `Object[]`, the method must be annotated with `@ResultRow`;

So the interface method from the previous example must be annotated as:

```java
@ResultRow({ String.class, Integer.class })
Map<String, Integer> scriptName(String p1, String p2);
```

Using the SqlStore proxy solution supports most of the SqlStore features, except
block queries.


### Statement Parameter Value Binding

Another important aspect of executing an SQL script is value binding. To begin
with, let's mention that it is possible to provide a Java bean object as a query
parameter because SqlStore can read its properties for setting SQL parameters
(the script definition description below covers how it's done). Now binding a
value instance to a parameter depends on the type of the value.

As it turns out, SqlStore is very keen to know about the Java types of all the
parameters. The Java type must be declared if it cannot be determined (e.g. the
property type of a Java Bean can be determined). SqlStore internally uses a list
of `ValueMapper`s in order to set parameters or read values. The list is
overridable and customizable but without them SqlStore would not know how to
handle parameters and results. Therefore, support for a parameter/result value
type is determined by internally used `ValueMapper` instances, and when a type
is not supported, SqlStore will inform about this at scripts loading time with a
`ScriptSetupException`.

The registry of `ValueMapper`s is maintained by the `Bindings` class, which also
provides methods to update the list of mappers.


### Exception Handling

Regarding possible exceptions, there are two specific ones that SqlStore uses.

Loading time failures, regarding the targeted scripts file, are often thrown as
`ScriptSetupException` where the message often also indicates the exact place
where the problem was discovered.

All query-related exceptions are thrown as `ScriptExecutionException`, which is
a runtime exception and therefore does not have to be caught. The exception may
also contain an `SQLException` for further details. For printing out an
exception, it is enough to print out `ScriptExecutionException`, which also
includes inner exception in its output.


### Inspection

The `SqlStore` class provides a few methods for inspecting its contents.

The number of scripts within a store instance can be resolved with the `size()`
method.

Although all the details about the scripts within the store can be printed with
the `toString()` method, the more preferred solution is to use the
`printState(PrintStream)` method, which writes directly to the stream and thus
is more memory-efficient.


The SQL Scripts File
--------------------

A scripts file used by a Java class must be in the same package, with the same
name, and with extension ".sqls". This makes it easier to understand where the
scripts belong to in the sense of Java code. The file is expected to contain
zero or more type aliases and script declarations (in the given order).


Java Type Aliases
-----------------

This is an optional feature so that typing Java class names would be easier. To
refer to a Java class or interface, its name must be given together with package
name, and using the same class name in several places is unconvenient for
developers and readers. Therefore, using an alias makes it better. It has simple
syntax:

		!aliasName=java.type.Name

Important details:

1. usage of white-space is not allowed in an alias declaration.
2. aliasName is treate case-sensitive
3. java.type.Name is a fully qualifed name of class and case-sensitive, as well.



SQL Script Declaration
----------------------

This is not just a name assignment to an SQL script but also declaration of what
kind of input parameters are expected and what script execution results are to
be returned back to the calling method. All this information is needed to avoid
misunderstandings between the Java code and the SQL stored outside of the code.
SqlStore has the role to make sure that both sides are not miscommunicating with
each other before actually executing the script.

The general outline of a single declaration follows. A scripts file may contain
several of these as long as script names are unique per file. Whitespace around
declaration is ignored; where-ever a whitespace character is allowed, it
includes any kind of white-space character and no matter how many.

		script-name optional-parameters { SQL Script here }

A script name may contain any character that is allowed for Java identifiers
(variable/method name).


SQL Script Parameters
---------------------

It is possible to define four categories of parameters in a script declaration:
input, output, update, and hint parameters. When any of them is not expected, it
must be omitted. Each category of parameters can only be declared at most once
while they can be presented in any order.

### Script Input Parameters

When an SQL script needs parameter values from Java code, the script needs to
declare the types and names for these parameters.

		IN(ParamType name[, ParamType2|SQL_TYPE name2, ...])

Notes:

1. The category begins with "IN(", ends with ")", and declares at least one
   input parameter.
2. An input parameter declaration begins with a Java type, which is either a
   previously declared type alias, a fully qualified class name, a primitive
   type name, or a class name from `java.lang` package.
3. A parameter also gets an SQL type, which is a constant value from the
   `java.sql.Types` class. When omitted from declaration, the SQL type will be
   decided by the `ValueMapper` that reports to handle the Java type of the
   parameter.
4. The parameter name must be unique among all the parameters in the same script
   declaration. It must be a valid Java identifier.
5. Multiple input parameters are separated by comma, which may be preceded or
   followed by whitespace.
6. The order and types of the parameters must match when they are passed
   together with script name for query lookup.


### Script Output Parameters

When execution of the SQL script should produce a JDBC result-set containing
execution results, SqlStore can extract values and return them to the SqlStore
client. The only requirement is that a row must be mapped to a single object
(returned as List) or to a pair of objects (returned as a `HashMap`). However,
a result-set may contain more than two columns, since SqlStore can map multiple
columns to different properties of the same object.

		OUT(JavaType|SQL_TYPE, com.other.JavaType2[prop1|SQL_TYPE, prop2|SQL_TYPE])

Notes:

1. The category begins with "OUT(", ends with ")", and declares either one or
   two Java types to be returned.
2. An output parameter declaration begins with a Java type, which is either a
   previously declared type alias, a fully qualified class name, a primitive type
   name, or a class name from `java.lang` package.
3. A parameter also gets an SQL type, which is a constant value from the
   `java.sql.Types` class. When omitted from declaration, the SQL type will be
   decided by the `ValueMapper` that reports to handle the Java type of the
   parameter.
4. The place where the SQL type is declared depends on whether the column values
   are stored in the position where type is declared or in the properties of the
   [Java Bean] type. In the former case, the SQL type is declared right after
   the Java type. In case of properties, the SQL type is declared right after a
   property name (separated by pipe character). However, remember that SQL types
   are commonly needed when the default SQL type (assigned by a ValueMapper)
   needs to be overridden.
5. When column values must be stored in bean properties, the property names must
   be declared inside square brackets right after the Java type. Nested
   properties are allowed, too.
6. The order of JDBC result-set columns and the types (and/or their properties)
   must match. Reading output parameters should give an idea how a row will be
   stored in Java types and how will it be returned (`Map` vs. `List`). To be
   clear, the number of columns and types/properties must be equal.
7. Multiple output parameters are separated by comma, which may be preceded or
   followed by whitespace.

There are also SQL statements that return data but not using result-sets: stored
procedures and functions. To support them, there's a bit different syntax for
output parameters. As described below in the SQL script syntax, SqlStore
supports binding parameters by name, possibly binding to a property of a
parameter. Therefore, the output parameters must be declared as they are with
input parameters, except either one or two. SqlStore supports binding to the
properties of the parameters, so that multiple values could be stored. The
syntax description follows:

		OUT(ParamType name[, ParamType2|SQL_TYPE name2])

The requirements are basically the same as for input parameters but to be clear
the parameter names must be unique among all the input and output parameter
names of that particular script.



### Updated Script Input Parameters

There are situations where it is more preferred to store the result-set value in
an object that was provided for input. For example, when an object represents a
row to be inserted, the generated primary key should be stored in a property of
the same object. To support this, a script can have UPDATE-parameters, which are
like OUT-parameters but are one or more expressions where values will be stored.

		UPDATE(name.prop, name2.prop2|SQL_TYPE, ...)

The names must refer to the input parameters. Expressions must contain at least
one property (separated by dot). As with OUT-parameters, the amount of columns
of the result-set and the properties to update must be equal.


### Specifying Result-Set Type

JDBC statement returns result-sets for accessing query results and generated
keys. The former case is supported without explicitly specifying anything.
However, to extract values from a keys result-set, the columns must be wrapped
with "KEYS(" and ")" in either OUT- or UPDATE-parameters list.

For example, `UPDATE(KEYS(p.id))` tells to look for generated keys and stored
the value in the property "id" of an input parameter "p". Of course, when the
KEYS-clause is used in a script definition, SqlStore will inform the database
driver to return generated keys.

Now that two methods for returning query results have been covered, it is
important to remember that they cannot be together on the same result-set! A
list of output parameters is closely tied with a result-set row columns
definition. However, UPDATE- and OUT-parameters can be used in the same script
definition if one handles values from generated keys result-set, and the other
handles query results.

Here is an example to the previous paragraph:

		IN(Person p) UPDATE(KEYS(p.id)) OUT(String)

In the example, the query takes an object of type `Person` as an input, updates
the property "id" from the generated keys result-set after executing the script,
and also returns a String value (or more as a List) from the first column of
query results. OUT-category also implies that if there are query results, each
row must have exactly one column. Otherwise, it's a faulty script declaration
and a runtime exception will be thrown when executing the script.



### Statement Hints

JDBC supports SQL execution hints at the `Statement` level. Usually they are
not needed but they can be useful some optimizations are needed. SqlStore
supports specifying the hints at script declaration next to other parameters:

		HINT(hintName=hintValue, ...)

where following hints are supported:

1. `queryTimeout=`_int in milliseconds_
2. `fetchSize=`_int_
3. `maxRows=`_int_
4. `maxFieldSize=`_int_
5. `poolable=`_true or false_
6. `escapeProcessing=`_true or false_
7. `readOnly=`_true or false_

These are the properties of JDBC Statement class and documented there.



SQL statement
-------------

A script declaration requires an SQL statement right after script name and
parameters, and within curly braces. SqlStore removes whitespace before and
after the script but keeps them within the statement (this may prove helpful for
debugging, as well). So here is a simple example:

```
{
SELECT *
  FROM person
 WHERE active=1
}
```

For SqlStore, this SQL looks like `SELECT *\n  FROM person\n WHERE active=1`.
SqlStore also sees that this is a simple statement and does not take any
parameters.



### Dynamically Included SQL Statement Parts

Although it is possible to compose SQL statements that apply some filters or
not depending on whether a parameter value is defined, it would be more
efficient if the statement would just omit a filter clause when its parameter
is undefined. Of course, describing such scripts, where parts may be omitted,
can become complicated to comprehend, it's still often more convenient than
creating multiple SQL statements for different scenarios.

SqlStore supports dynamic SQL statement parts by describing a condition (when
to use that part) and the block to include. The condition is an input parameter
expression that must not evaluate to null nor empty (string/array/collection)
to include the block.

Conditional blocks are expressed within SQL script by placing the condition on
a separate line (starting with an exclamation mark on the first column):

```
!(condition){ SQL-script-part }
```

Note that there must be no whitespace until the SQL script part block (i.e.
from the exclamation mark to the opening curly brace, included). The block
itself may contain several lines and its whitespace will be preserved.

The `condition` may be one of the following:

1. `inParam.expression` -- an expression based on an input parameter, e.g. `f`
   or `f.prop1.prop2`. This evaluates to true when the expression value is not
   null and not an empty string/array/collection. For example, Boolean `false`
   still evaluates to true.
2. `empty(inParam.expression)` -- similar to previous, however the opposite:
   the expression must evaluate to null or an empty value.
3. `true(inParam.expression)` -- the expression must evaluate to a value that
   is equal to Boolean `true`.

To support more complicated expressions, the preferred method is to define the
condition in a method of an IN-parameter that returns `true` or `false`
depending on whether to include the block or not.

Here is an example with four conditional blocks using the default condition
expression (the first one of the three condition types):

```
findUsers
  IN(UserSearchFilter f)
  OUT(UsersListRow[id, username, name, active, updated]) {
SELECT id, username, name, active, date_updated
  FROM users
 WHERE active=${f.active}
!(f.name){ AND name LIKE ${f.name} }
!(f.username){ AND name LIKE ${f.username} }
!(f.updatedBegin){ AND date_updated <= ${f.updatedBegin} }
!(f.updatedEnd){ AND date_updated >= ${f.updatedEnd} }
}
```

### Binding for Script Parameters

However, an SQL can declare that it takes parameter values from the
IN-parameters clause by wrapping the expression inside "${" and "}":


```
addProduct IN(Product p) UPDATE(KEYS(p.id)) {
INSERT INTO products (
	name,
	description,
	category_id,
	producer_id,
	provider_id,
	created_at,
	created_by
) VALUES (
	${p.name},
	${p.description},
	${p.categoryId},
	${p.producerId},
	${p.providerId},
	CURRENT_TIMESTAMP,
	${p.createdBy}
)
}
```

The places, where `${...}` expressions are used, will be replaced with question
marks "?", which is sign used by JDBC to bind a parameter value. The value will
be read from where the expression suggests. For example, `${p.name}` means that
the value is to be read from the property "name" of the IN-parameter "p".
Nested properties are supported, too.


### Support for Stored Procedures

Like JDBC, SqlStore supports calling stored procedures from Java code. More
specifically, SqlStore supports parameter type binding for IN, OUT, and INOUT
parameters as well as escape syntax for stored procedures.

SqlStore determines parameter mode IN or OUT depending on where the referred
script parameter is declared. It chooses IN-mode when the script parameter
originates from IN-parameters, otherwise, OUT-mode when it originates from
OUT-parameters list (has no initial value). SqlStore also supports storing the
value of an INOUT-mode parameter in an expression. Here is a quick demo:


```
updatePrice IN(Product p) OUT(BigDecimal total) {
  {${total} = call update_price(
                     ${p.productId},
	                   ${INOUT(p.price)},
                     ${p.vatPercent}
                   )}
}
```

The previous sample uses JDBC escape syntax (additional curly braces around
function call) and INOUT-mode for expression `p.price` (expecting the value of
the expression to be updated by the stored procedure).


### Escaping Special Characters

As with UPDATE-parameters, it is possible to override the default SQL type from
`ValueMapper`s by providing type name after the expression separated by pipe
character: `${p.availableSince|DATE}` (default type would TIMESTAMP, for
example).

It is possible to escape expressions when they are not to be interpreted by
using a backward-slash, e.g. `\${escapeMe}`. However, this does not have to be
escaped since it does not begin with "${": `$IWillBePreserved`.

Regarding curly braces within SQL statement, SqlStore parser keeps track of them
and keeps them as part of the SQL string when they are in sequence "{" before
"}". In this case, they do not have to be escaped. Otherwise, the parser may get
confused and it's inevitable to escape them: `\} \{`.

It is possible to escape the escape-character as well: `\\{` will result `\{`.


Logging
-------

SqlStore uses the SLF4J logging facade API for exposing its activity details. The logger names have
prefix `ws.rocket.sqlstore`. Usually the INFO-level should be sufficient, as it informs which script
files are parsed and how much time the loading process took. DEBUG-level adds details about some
steps involved in executing scripts.

There are some additional TRACE-level loggers when time performance needs to be analyzed:

1. `ws.rocket.sqlstore.timer.EXEC` - logs the time spent for executing a script (`Query.execute()`).
2. `ws.rocket.sqlstore.timer.DB` - logs the time spent for just executing a prepared JDBC statement.

Both loggers report both script name and duration in milliseconds.


Additional Information
----------------------

It is possible that some of the information regarding the usage of SqlStore
library was not documented precisely or was forgotten to add here. In addition,
additional concerns or questions may arise when using this library. In such
cases feel free to create a topic at
[the official issue tracker](https://github.com/mrtamm/sqlstore/issues) of this
project.
