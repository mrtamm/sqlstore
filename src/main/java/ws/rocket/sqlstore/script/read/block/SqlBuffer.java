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

/**
 * A class devoted to storing and updating the SQL, while being parsed, and reflecting back when a
 * different reader needs to be used to continue parsing.
 * <p>
 * An instance of this class is safe to use while parsing several scripts from one or even more
 * files. However, this class is not thread-safe.
 *
 * @see ws.rocket.sqlstore.script.read.StreamReader#parseSql(SqlBuffer)
 */
public final class SqlBuffer {

  /**
   * Events that trigger pause while filling the SQL buffer.
   */
  public enum ParseEvent {

    /**
     * Initial state, meaning that no event has occurred yet.
     */
    NONE,
    /**
     * Reached SQL script parameter expression. Expression reader should continue from the current
     * stream position. After that, may continue filling in the SQL buffer.
     */
    EXPRESSION,
    /**
     * Reached SQL script part condition. When the SQL buffer is not empty, it should be used to
     * form a script part, and then reset. The condition Condition reader should continue from the
     * current stream position. After that, may continue filling in the SQL buffer for the new part.
     */
    CONDITION,
    /**
     * Reached the end of a conditional block. SQL buffer content can be retrieved and reset to form
     * the SQL part object.
     */
    END_BLOCK,
    /**
     * Reached the end of a script block. SQL buffer content can be retrieved and reset to form the
     * SQL part object (when content is not empty). When there are multiple parts for one script
     * block, these must be grouped into one parent SQL parts object.
     */
    END_SCRIPT

  };

  /**
   * A mode identifies the internal input character handling mode of this buffer. The SIMPLE mode
   * adds characters to the buffer, unless the next character is a symbol that triggers a different
   * mode. When the specific mode completes, it reverts back to SIMPLE mode.
   */
  private enum ParseMode {

    SIMPLE, ESCAPE, EXPRESSION, CONDITION, END

  };

  private ParseMode mode = ParseMode.SIMPLE;

  private ParseEvent event = ParseEvent.NONE;

  private final StringBuilder sql = new StringBuilder(1024);

  private final int[] nestedBraces = new int[4];

  private int nestedBracesIdx;

  /**
   * Provides the last parsing event. Initially, it returns NONE. Every time that
   * {@link #next(int, int, int)} method returns false, this event information will be updated to
   * inform why the SQL block now needs special reader or evaluation.
   *
   * @return The last event.
   * @see ParseEvent
   */
  public ParseEvent getLastEvent() {
    return this.event;
  }

  /**
   * Resets and returns the current buffer (when reading a script block is completed).
   *
   * @return The content of the buffer right before reset.
   */
  public String resetSqlContent() {
    String content = this.sql.toString();
    this.sql.setLength(0);
    return content;
  }

  /**
   * Trims whitespace and semi-commas off the end of the buffer content.
   *
   * @return The current SQL buffer instance.
   */
  public SqlBuffer trimEnd() {
    while (this.sql.length() > 0) {
      int cp = this.sql.codePointAt(sql.length() - 1);

      if (cp == ';' || Character.isWhitespace(cp)) {
        this.sql.setLength(this.sql.length() - 1);
      } else {
        break;
      }
    }
    return this;
  }

  /**
   * Just removes the last character off the end of the buffer content when it's not empty.
   *
   * @return The current SQL buffer instance.
   */
  public SqlBuffer removeLastChar() {
    if (this.sql.length() > 0) {
      this.sql.setLength(this.sql.length() - 1);
    }
    return this;
  }

  /**
   * Updates the buffer content with the next character from stream.
   *
   * @param cp The character code-point.
   * @param line The line number (1-based) where the character is located.
   * @param column The column number (1-based) where the character is located.
   * @return Whether to continue reading SQL (true) or another reader is required (false).
   * @see #getLastEvent()
   */
  public boolean next(int cp, int line, int column) {
    if (cp == -1) {
      return false;
    } else if (this.mode == ParseMode.SIMPLE) {
      return nextSimpleChar(cp, column);
    } else if (this.mode == ParseMode.ESCAPE) {
      return nextEscapeChar(cp);
    } else if (this.mode == ParseMode.EXPRESSION) {
      return nextExpressionChar(cp);
    } else if (this.mode == ParseMode.CONDITION) {
      return nextConditionChar(cp, column);
    } else if (this.mode == ParseMode.END) {
      return nextEndChar(cp, column);
    }
    throw new IllegalStateException("Parse mode not covered");
  }

  private boolean nextSimpleChar(int cp, int column) {
    if (cp == '{') {
      incrNestedBraces();
    } else if (cp == '}') {
      if (!decrNestedBraces()) {
        this.event = ParseEvent.END_BLOCK;
        return false;
      }
    } else if (cp == '\\') {
      this.mode = ParseMode.ESCAPE;
    } else if (cp == '?') {
      this.mode = ParseMode.EXPRESSION;
    } else if (column == 1 && cp == '!') {
      this.mode = ParseMode.CONDITION;
    } else if (column == 1 && cp == '=') {
      this.mode = ParseMode.END;
    }

    if (this.mode == ParseMode.SIMPLE) {
      this.sql.appendCodePoint(cp);
    }

    return true;
  }

  private boolean nextEscapeChar(int cp) {
    this.mode = ParseMode.SIMPLE;
    if (cp != '?' && cp != '{' && cp != '}' && cp != '\\') {
      this.sql.append('\\');
    }
    this.sql.appendCodePoint(cp);
    return true;
  }

  private boolean nextExpressionChar(int cp) {
    this.mode = ParseMode.SIMPLE;
    this.sql.append('?'); // JDBC parameter placeholder

    if (cp == '{') {
      this.event = ParseEvent.EXPRESSION;
      return false; // Break to evaluate the expression for the JDBC parameter
    }

    this.sql.appendCodePoint(cp);
    return true;
  }

  private boolean nextConditionChar(int cp, int column) {
    this.mode = ParseMode.SIMPLE;

    if (column == 2) {
      if (cp == '(') {
        pushNestedBraces();
        this.event = ParseEvent.CONDITION;
        return false; // Break to parse the condition.
      }
      this.sql.append('!');
    }

    this.sql.appendCodePoint(cp);
    return true;
  }

  private boolean nextEndChar(int cp, int column) {
    if (column <= 4 && cp == '=') {
      return true;
    } else if (column > 4) {
      boolean cont = cp != '\n';
      if (!cont) {
        this.mode = ParseMode.SIMPLE;
        this.event = ParseEvent.END_SCRIPT;
      }
      return cont;
    }

    this.mode = ParseMode.SIMPLE;

    // Was not end-line; append omitted chars; and continue.
    for (int i = 1; i < column; i++) {
      this.sql.append('=');
    }

    this.sql.appendCodePoint(cp);
    return true;
  }

  private void incrNestedBraces() {
    this.nestedBraces[this.nestedBracesIdx]++;
  }

  private boolean decrNestedBraces() {
    this.nestedBraces[this.nestedBracesIdx]--;
    int val = this.nestedBraces[this.nestedBracesIdx];

    boolean endOfInnerBlock = val < 0;

    if (endOfInnerBlock) {
      this.nestedBraces[this.nestedBracesIdx] = 0;
      this.nestedBracesIdx--;
    }

    return !endOfInnerBlock;
  }

  private void pushNestedBraces() {
    this.nestedBracesIdx++;
  }

}
