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
package ws.rocket.sqlstore;

/**
 * Interface for custom query blocks in order to execute multiple SQL statements in one transaction.
 * Upon failure, all data changes are going to roll back to the state before the block was executed.
 * <p>
 * This interface is usually implemented when calling <code>SqlStore.execBlock(...)</code> method.
 *
 * @see SqlStore#execBlock(ws.rocket.sqlstore.Block)
 * @see SqlStore#execBlock(ws.rocket.sqlstore.Block, int)
 */
public interface Block {

  /**
   * Invokes one or more SQL scripts from current <code>SqlStore</code>. When any of these scripts
   * should fail, the transaction will be rolled back to the state before the execution of this
   * block.
   *
   * @param sql The (current) SQL store to use.
   */
  void execute(SqlStore sql);

}
