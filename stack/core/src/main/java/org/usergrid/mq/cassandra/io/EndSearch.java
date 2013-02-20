/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.mq.cassandra.io;

import static java.lang.Math.max;

import java.util.UUID;

import me.prettyprint.hector.api.Keyspace;

import org.usergrid.mq.QueueQuery;
import org.usergrid.mq.cassandra.io.NoTransactionSearch.SearchParam;

/**
 * Reads from the queue without starting transactions.
 * 
 * @author tnine
 * 
 */
public class EndSearch extends NoTransactionSearch {

  /**
   * @param ko
   * @param cassTimestamp
   */
  public EndSearch(Keyspace ko, long cassTimestamp) {
    super(ko, cassTimestamp);
  } /*
     * (non-Javadoc)
     * 
     * @see org.usergrid.mq.cassandra.io.NoTransaction#getParams(java.util.UUID,
     * java.util.UUID, org.usergrid.mq.QueueQuery)
     */

  @Override
  protected SearchParam getParams(UUID queueId, UUID consumerId, QueueQuery query) {
    UUID lastMessageId = query.getLastMessageId();
    return new SearchParam(lastMessageId, true, lastMessageId != null, max(query.getPreviousCount(), query.getLimit()));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.mq.cassandra.io.FifoSearch#writeClientPointer(java.util.UUID,
   * java.util.UUID, java.util.UUID)
   */
  @Override
  protected void writeClientPointer(UUID queueId, UUID consumerId, UUID lastReturnedId) {
    // no op for searches from the end
  }

}
