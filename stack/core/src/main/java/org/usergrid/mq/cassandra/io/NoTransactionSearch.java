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

import static org.usergrid.mq.cassandra.CassandraMQUtils.getConsumerId;
import static org.usergrid.mq.cassandra.CassandraMQUtils.getQueueId;

import java.util.List;
import java.util.UUID;

import me.prettyprint.hector.api.Keyspace;

import org.usergrid.mq.Message;
import org.usergrid.mq.QueueQuery;
import org.usergrid.mq.QueueResults;

/**
 * Reads from the queue without starting transactions.
 * 
 * @author tnine
 * 
 */
public class NoTransactionSearch extends AbstractSearch {

  /**
   * @param ko
   * @param cassTimestamp
   */
  public NoTransactionSearch(Keyspace ko, long cassTimestamp) {
    super(ko, cassTimestamp);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.mq.cassandra.io.QueueSearch#getResults(java.lang.String,
   * org.usergrid.mq.QueueQuery)
   */
  @Override
  public QueueResults getResults(String queuePath, QueueQuery query) {

    UUID queueId = getQueueId(queuePath);
    UUID consumerId = getConsumerId(queueId, query);
    QueueBounds bounds = getQueueBounds(queueId);
    SearchParam params =  getParams(queueId, consumerId, query);

    List<UUID> ids = getIds(queueId, consumerId, bounds, params);

    List<Message> messages = loadMessages(ids, params.reversed);

    QueueResults results = createResults(messages, queuePath, queueId, consumerId);

    writeClientPointer(queueId, consumerId, results.getLast());

    return results;
  }

  /**
   * Get information for reading no transactionally from the query. Subclasses
   * can override this behavior
   * 
   * @param queueId
   * @param consumerId
   * @param query
   * @return
   */
  protected SearchParam getParams(UUID queueId, UUID consumerId, QueueQuery query) {
    UUID lastReadMessageId = getConsumerQueuePosition(queueId, consumerId);

    return new SearchParam(lastReadMessageId, false, lastReadMessageId != null, query.getNextCount());
  }

  /**
   * Get the list of ids we should load and return to the client. Message ids
   * should be in order on return
   * 
   * @param queueId
   * @param consumerId
   * @param bounds
   * @param params
   * @return
   */
  protected List<UUID> getIds(UUID queueId, UUID consumerId, QueueBounds bounds, SearchParam params) {
    return getQueueRange(queueId, bounds, params);
  }

  protected static class SearchParam {

    /**
     * The uuid to start seeking from
     */
    protected final UUID startId;

    /**
     * true if we should seek from high to low
     */
    protected final boolean reversed;
    /**
     * The number of results to include
     */
    protected final int limit;
    /**
     * true if the first result should be skipped. Useful for paging from a
     * previous result
     */
    protected final boolean skipFirst;

    /**
     * @param startId
     * @param reversed
     * @param count
     */
    public SearchParam(UUID startId, boolean reversed, boolean skipFirst, int count) {
      this.startId = startId;
      this.reversed = reversed;
      this.skipFirst = skipFirst;
      this.limit = count;
    }
  }
  
  

}
