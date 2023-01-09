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
 * Scripts parsing from a textual (UTF-8) stream functionality. Normally users of SqlStore do not
 * use the functionality directly but through the static factory method of
 * {@link ws.rocket.sqlstore.SqlStore#load(java.lang.Class, java.sql.Connection)}.
 *
 * <p>The functionality of parsing is divided depending on the level of interpretation being
 * handled:
 *
 * <ol>
 * <li><code>ScriptsReader</code> &ndash; the most generic level, the scope is the entire scripts
 * file;
 * <li><code>ScriptReader</code> &ndash; reusable, but parses just one script from the file;
 * <li><code>ParamsSet</code> &ndash; reusable helper, but handles the parameters setup of a script;
 * <li><code>StreamReader</code> &ndash; the most lower level, works with symbols from the file;
 * </ol>
 *
 * <h2>The SQLS File</h2>
 *
 * <p><code>SqlStore</code> introduces a custom file format describing multiple SQL scripts, their
 * names and parameters. The file must be bound to a class (by having same path and file name as the
 * class) where it is used to be clear, and it must have an extension ".sqls". The scripts from the
 * file can be executed via an <code>SqlStore</code> instance by specifying the script name and its
 * parameters. The library validates the name and parameters (amount and types), then executes the
 * script and possibly returns results as specified in the scripts file. Note that
 * <code>SqlStore</code> aims to provide the same functionality that developers would have when they
 * write code to execute SQL scripts through JDBC. However, <code>SqlStore</code> helps avoid
 * writing SQL in Java code and helps avoid coding errors when using JDBC API.
 *
 * <h3>File Structure</h3>
 *
 * <p>The structure of this file is simple. At the beginning, there are optional custom Java type
 * mappings (a string that matches to a full class name) to simplify referring to these classes in
 * SQL scripts. Each mapping is on a separate line and starts with an exclamation mark. For example:
 *
 * <pre>!Employee=org.company.data.Employee
 * !E=org.company.data.Employee</pre>
 *
 * <p>The previous example specifies that Java types "Employee" and "E" actually refer to class
 * <code>org.company.data.Employee</code>.
 *
 * <p>After optional Java type mappings there are stored SQL scripts. Each script begins with a
 * name, optional parameters and query hints, and an SQL script in curly braces. Whitespace
 * (including newlines) is allowed between these elements but all this information can also be
 * contained in one line, as well.
 *
 * <p>The overall script syntax is following and a file can contain multiple such scripts:
 *
 * <pre><em>script-name</em> <em>parameters-hints</em> {
 * <em>SQL script</em>
 * }</pre>
 *
 * <h4>Script Name</h4>
 *
 * <p>The name is used for enabling a Java code to refer to a specific script to be executed in an
 * <code>SqlStore</code> instance. Therefore, each script name must be unique within an SQLS file.
 * The name should be simple and descriptive so that a Java code reader (developer) could better
 * assume what is going to be executed in the database without opening up the SQLS file. The parser
 * restricts naming by requiring the same rules that apply to Java methods. Some good examples of
 * understandable names: <code>findEmployee</code>, <code>updateEmployee</code>,
 * <code>getCurrentEmployees</code>.
 *
 * <p>The script name can be summarized as following:
 *
 * <pre>script-name := &lt;a valid Java identifier&gt;</pre>
 *
 * <h4>Parameters And Hints</h4>
 *
 * <p>There are three types of possible parameters: <code>IN</code>, <code>OUT</code>, and
 * <code>UPDATE</code>. The order they are specified for a script is not relevant since the
 * parameters are grouped with the parameters type name and parenthesis. It is also possible to
 * specify query hints, which correspond to properties of JDBC {@link java.sql.Statement}.
 * Therefore, <code><em>parameters-hints</em></code> clause has the following syntax:
 *
 * <pre>parameters-hints := {
 *   IN(<em>named-params</em>)
 *   |
 *   OUT({ <em>named-params</em> | <em>type-params</em> })
 *   |
 *   UPDATE(<em>expressions</em>)
 *   |
 *   HINTS(<em>hint-clauses</em>)
 * }*</pre>
 *
 * <p><strong>NOTE:</strong> no whitespace is allowed before opening parenthesis! For example:
 *
 * <ol>
 * <li>Wrong: <code>IN (...)</code>
 * <li>Correct: <code>IN(...)</code>
 * </ol>
 *
 * <h4>Parameter Value Types</h4>
 *
 * <p>When talking about parameters, it is important to note that all parameters must have a Java
 * type, and an optional SQL type (a constant name from {@link java.sql.Types}). The Java type must
 * be one of the following:
 *
 * <ol>
 * <li>an alias of a type from the beginning of the SQLS file;
 * <li>a full class name;
 * <li>a Java primitive name;
 * <li>a class name from <code>java.lang</code> package;
 * </ol>
 *
 * <p>The Java type is used for choosing the most approach to setting or reading a statement
 * parameter. It can also be influenced by the SQL type of the parameter.
 *
 * <p>When SQL type is present, it is separated with a pipe (<code>|</code>) character from the Java
 * type. For example: <code>int|DECIMAL</code>. The SQL type is usually omitted and assigned by a
 * runtime value converter based on the Java type. For example, <code>java.util.Date</code> may
 * represent a timestamp, a date, or a time. Value converter for <code>java.util.Date</code> may
 * assign <code>Types.TIMESTAMP</code> as the default SQL type. It can be overridden per use-case by
 * specifying <code>java.util.Date|DATE</code> in a parameter declaration in an SQLS file.
 *
 * <h4>Named IN/OUT Parameters</h4>
 *
 * <p>When input parameters are required, they are represented as a comma separated list of named
 * parameters. The name is used to refer to the value of the parameter. When the named parameter is
 * an output parameter, the name is used to set the output parameter value from the script (usually
 * when a function or procedure is called in the script).
 *
 * <p>The rules for the parameter name are the same as for the script name: it must be valid Java
 * identifier, and it must be unique within the context of the script where it is used.
 *
 * <p>Therefore, nothing complicated as shown in these examples:
 *
 * <pre>IN(Long id, java.math.BigDecimal amount)
 * IN(Long id, java.util.Date|DATE birthDate)
 * OUT(Long id)</pre>
 *
 * <h4>Type Parameters</h4>
 *
 * <p>When the script returns a <em>result-set</em> after execution and each row must be converted
 * to a bean, the type parameter is just for the case. When the value from the result-set must be
 * set as it is, just use the type name. Otherwise, when there are several columns to be converted
 * into bean, also specify the properties for each column. Here are examples for both scenarios:
 *
 * <pre># Returns a map with Long values as keys, and String values as values.
 * # The query: SELECT id, name FROM employees
 * OUT(Long, String)
 *
 * # Returns a map with Long values as keys, and Employee beans as values.
 * # The query: SELECT id, id, name, birth_date FROM employees
 * OUT(Long, Employee[id, name, birthDate])</pre>
 *
 * <h4>Hints for SQL Script</h4>
 *
 * <p>SqlStore does not do some guessing to optimize a script execution. It is so to avoid bad
 * surprises. However, it provides a way to specify query hints that are normally set on a JDBC
 * <code>Statement</code>:
 *
 * <ol>
 * <li><code>queryTimeout</code> &ndash; {@link java.sql.Statement#setQueryTimeout(int)};
 * <li><code>fetchSize</code> &ndash; {@link java.sql.Statement#setFetchSize(int)};
 * <li><code>maxRows</code> &ndash; {@link java.sql.Statement#setMaxRows(int)};
 * <li><code>maxFieldSize</code> &ndash; {@link java.sql.Statement#setMaxFieldSize(int)};
 * <li><code>poolable</code> &ndash; {@link java.sql.Statement#setPoolable(boolean)};
 * <li><code>escapeProcessing</code> &ndash; {@link java.sql.Statement#setEscapeProcessing(boolean)}
 * <li><code>readOnly</code> {@link java.sql.Connection#setReadOnly(boolean)}
 * </ol>
 *
 * <h4>Expressions</h4>
 *
 * <p>An SQL script can make use of parameters by referring to them using expressions. These are
 * marked within symbols <code>${...}</code> in an SQL script, and are replaced with question marks,
 * which are already familiar to JDBC API users. An expression typically begins with a reference to
 * a parameter name and can refer to any number of (nested) properties using dot-notation as
 * ".property1.property2" for reading a value and injecting it where the expression is used. The
 * value to be injected is escaped by JDBC API.
 *
 * <p>In addition, expressions also are aware of Java and SQL type of the expression value. The Java
 * type is inherited from the named parameter type (when no properties) or from the last property
 * (when properties are used). The SQL type is also inherited or derived unless explicitly provided
 * after the parameter name or the last property separated with the pipe-character.
 *
 * <p>Here are examples:
 *
 * <pre># Injects input parameters "id", "name", and "birthDate":
 * INSERT INTO employee(id, name, birth_date)
 * VALUES (${id}, ${name}, ${birthDate|DATE});
 *
 * # Injects the properties of an input parameter "e":
 * INSERT INTO employee(id, name, birth_date)
 * VALUES (${e.id}, ${e.name}, ${e.birthDate|DATE});</pre>
 *
 * <h4>OUT-Expressions</h4>
 *
 * <p>In more advanced scenarios, mostly related to calling database procedures, it may be necessary
 * to read back the values of the script (after being modified by a procedure call). An expression
 * in the script may be wrapped into <code>OUT(...)</code> or <code>INOUT(...)</code>. The former
 * informs that the expression does not set a value before execution but reads the value after
 * execution. The second form also sets the value before execution.
 *
 * <h4>Generated Keys</h4>
 *
 * <p>JDBC supports two kinds of result-sets: one for reading query results, other for reading
 * generated keys. By default, when type-parameters are used, they refer to query results. To inform
 * that the values are to be read from generated keys result-set, wrap the types in
 * <code>KEYS(...)</code>. This also sends a signal to the statement to be executed that the query
 * expects auto-generated keys to be returned.
 *
 * <h4>UPDATE-parameters</h4>
 *
 * <p>These parameters are more like expressions to be evaluated after query, especially when the
 * expression is not directly used in a query. Here is an example to illustrate a use case:
 *
 * <pre>UPDATE(KEYS(e.id))
 * # for SQL script
 * INSERT INTO employee(id, name, birth_date)
 * VALUES (emp_seq.nextval, ${e.name}, ${e.birthDate|DATE});</pre>
 *
 * <p>In the previous example, the use of <code>KEYS(...)</code> signals that the auto-generated
 * keys are expected after query. The auto-generated key is from sequence named
 * <code>emp_seq</code>. The key will be stored in the property <code>id</code> of the input
 * parameter named <code>e</code>. This script most likely does not have any output parameters,
 * since the generated ID will be stored in the input parameter right after the execution of the
 * script.
 *
 * <h4>Script Validation</h4>
 *
 * <p>During the parsing process most of the script information will be validated. Only the SQL
 * script itself cannot be validated for correctness as it can be verified only by database. An
 * error will abort the loading process by throwing {@link ws.rocket.sqlstore.ScriptSetupException}.
 *
 * <p>Examples of items that are checked during script parsing:
 *
 * <ol>
 * <li>uniqueness of a name (script name &ndash; per file; parameter name &ndash; per script);
 * <li>Java type exists;
 * <li>SQL type exists or can be determined by Java type;
 * <li>references to bean properties (for reading or writing values)
 * <li>a bean can be constructed when necessary;
 * </ol>
 */
package ws.rocket.sqlstore.script.read;
