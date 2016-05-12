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
import ws.rocket.sqlstore.ScriptSetupException;
import ws.rocket.sqlstore.script.read.ParamsSet;
import ws.rocket.sqlstore.script.read.StreamReader;
import ws.rocket.sqlstore.script.read.block.SqlBuffer.ParseEvent;
import ws.rocket.sqlstore.script.sql.ConditionAlways;
import ws.rocket.sqlstore.script.sql.SqlPart;
import ws.rocket.sqlstore.script.sql.SqlPartCondition;
import ws.rocket.sqlstore.script.sql.SqlParts;
import ws.rocket.sqlstore.script.sql.SqlScript;

import static java.util.Objects.requireNonNull;

/**
 * Reads and evaluates the entire SQL script block of a script definition. To simplify its work,
 * this class relies on several readers.
 * <p>
 * An SQL block is everything between two marker lines (at least four consecutive equal signs
 * starting from the first column) right after script name and parameters. This reader also includes
 * the marker lines in its parsing process.
 *
 * @see ExpressionReader
 * @see ConditionReader
 * @see SqlBuffer
 */
public final class SqlBlockReader {

  private final StreamReader reader;

  private final ParamsSet params;

  private final ConditionReader condReader;

  private final ExpressionReader exprReader;

  private final SqlBuffer sqlReader = new SqlBuffer();

  /**
   * Initiates the SQL block reader instance, which may be used for parsing several scripts as long
   * as they are within one scripts file.
   *
   * @param reader The reader for current scripts file.
   * @param params The shared parameters set to be updated on parsing.
   */
  public SqlBlockReader(StreamReader reader, ParamsSet params) {
    this.reader = requireNonNull(reader, "StreamReader is undefined.");
    this.params = requireNonNull(params, "ParamsSet is undefined.");
    this.condReader = new ConditionReader(reader);
    this.exprReader = new ExpressionReader(reader);
  }

  /**
   * Reads and evaluates the SQL script block, as described in class-level documentation of this
   * class.
   *
   * @return The parsed SQL script, which does not need further validation.
   * @throws IOException When a stream-related exception occurs during reading.
   */
  public SqlScript parseSql() throws IOException {
    int cp = this.reader.parseSqlSeparator();

    // The whitespace before SQL is omitted.
    if (Character.isWhitespace(cp)) {
      this.reader.skipWsp();
    }

    return parseSqlPart(ConditionAlways.INSTANCE, ParseEvent.END_SCRIPT);
  }

  private SqlScript parseSqlPart(SqlPartCondition condition, ParseEvent untilEvent)
      throws IOException {

    List<SqlScript> innerParts = new ArrayList<>();
    ParseEvent event;

    do {
      this.reader.parseSql(this.sqlReader);
      event = this.sqlReader.getLastEvent();

      if (event == ParseEvent.EXPRESSION) { // Beginning of expression within ?{...}
        this.exprReader.parseExpression().register(this.params);
        continue;
      }

      // A little hack to trim whitespace from the end of the last part.
      if (event == ParseEvent.END_BLOCK) {
        this.sqlReader.removeLastChar();
      } else {
        // The whitespace after SQL is omitted.
        this.sqlReader.trimEnd();
      }

      String script = this.sqlReader.resetSqlContent();

      if (script.trim().length() > 0) {
        innerParts.add(new SqlPart(condition, script, this.params.resetQueryParams()));
      }

      if (event == ParseEvent.CONDITION) { // next inner block condition
        SqlPartCondition cond = this.condReader.parseCondition().resolveParam(this.params).build();
        innerParts.add(parseSqlPart(cond, ParseEvent.END_BLOCK));
      }

    } while (event != untilEvent);

    return finalizeScript(condition, innerParts);
  }

  private SqlScript finalizeScript(SqlPartCondition condition, List<SqlScript> parts) {
    if (parts.isEmpty()) {
      throw new ScriptSetupException("Empty script block on line %d and column %d.",
          this.reader.getLine(), this.reader.getColumn());
    }

    if (parts.size() == 1) {
      return parts.get(0);
    }

    return new SqlParts(condition, parts.toArray(new SqlScript[parts.size()]));
  }

}
