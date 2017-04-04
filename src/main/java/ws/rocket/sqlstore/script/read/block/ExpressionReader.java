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

package ws.rocket.sqlstore.script.read.block;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ws.rocket.sqlstore.script.params.ParamMode;
import ws.rocket.sqlstore.script.read.ParamsSet;
import ws.rocket.sqlstore.script.read.StreamReader;

import static java.util.Objects.requireNonNull;

/**
 * Reads and registers a script parameter expression from SQL script block, which is usually
 * enclosed in <code>?{...}</code>. (This reader evaluates the content of the expression, beginning
 * right after the opening curly brace and finishing right after the closing curly brace.)
 * <p>
 * It can have following valid syntaxes (within the curly braces):
 * <pre>
 * paramName
 * paramName|TYPE
 * IN(paramName), OUT(paramName), INOUT(paramName)
 * IN(paramName|TYPE), OUT(paramName|TYPE), INOUT(paramName|TYPE)
 * </pre>
 * <p>
 * Example of how to use this class:
 * <pre>expressionReader
 *     .parseExpression()
 *     .register(paramsSet);</pre>
 */
public final class ExpressionReader {

  private final StreamReader reader;

  private ParamMode mode;

  private boolean modeSpecified;

  private String varName;

  private List<String> fields;

  private Integer sqlType;

  /**
   * Initiates the expression reader instance, which may be used for parsing several scripts as long
   * as they are within one scripts file.
   *
   * @param reader The reader for current scripts file.
   */
  public ExpressionReader(StreamReader reader) {
    this.reader = requireNonNull(reader, "StreamReader is undefined.");
  }

  /**
   * Reads and evaluates the expression within <code>${...}</code>.
   * <p>
   * The next step is to register the extracted expression as a query parameter.
   *
   * @return The current reader instance.
   * @throws IOException When a stream-related exception occurs during reading.
   * @see #register(ParamsSet)
   */
  public ExpressionReader parseExpression() throws IOException {
    this.reader.skipWsp();

    parseModeAndVar();
    parseNestedProps();
    parseSqlType();
    parseExpressionEnd();
    return this;
  }

  /**
   * Registers the parsed expression as a JDBC query parameter.
   *
   * @param params The parameters set of the current script.
   */
  public void register(ParamsSet params) {
    params.addScriptParam(this.mode, this.varName, this.fields, this.sqlType);
  }

  private void parseModeAndVar() throws IOException {
    this.mode = ParamMode.IN;
    this.modeSpecified = false;

    this.varName = this.reader.parseParamName();

    if (this.reader.isNext('(')) {
      this.mode = ParamMode.valueOf(this.varName);
      this.modeSpecified = true;
      this.reader.parenthesisOpen();
      this.varName = this.reader.parseParamName();
    }
  }

  private void parseNestedProps() throws IOException {
    this.fields = new ArrayList<>();

    while (this.reader.skipIfNext('.')) {
      this.fields.add(this.reader.parseParamName());
    }
  }

  private void parseSqlType() throws IOException {
    this.reader.skipWsp();
    this.sqlType = this.reader.parseSqlType();
  }

  private void parseExpressionEnd() throws IOException {
    if (this.modeSpecified) {
      this.reader.skipWsp();
      this.reader.requireNext(')');
    }

    this.reader.skipWsp();
    this.reader.requireNext('}');
  }

}
