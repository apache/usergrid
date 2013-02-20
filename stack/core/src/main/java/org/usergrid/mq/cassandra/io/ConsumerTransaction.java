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

import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;
import static org.usergrid.mq.cassandra.CassandraMQUtils.getConsumerId;
import static org.usergrid.mq.cassandra.CassandraMQUtils.getQueueClientTransactionKey;
import static org.usergrid.mq.cassandra.CassandraMQUtils.getQueueId;
import static org.usergrid.mq.cassandra.QueuesCF.CONSUMER_QUEUE_TIMEOUTS;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.SliceQuery;

import org.usergrid.mq.Message;
import org.usergrid.mq.QueueQuery;
import org.usergrid.mq.QueueResults;
import org.usergrid.mq.cassandra.QueueManagerImpl.QueueBounds;
import org.usergrid.utils.UUIDUtils;

/**
 * Reads from the queue and starts a transaction
 * 
 * @author tnine
 * 
 */
public class ConsumerTransaction extends NoTransactionSearch {

  /**
   * @param ko
   * @param cassTimestamp
   */
  public ConsumerTransaction(Keyspace ko, long cassTimestamp) {
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

    SearchParam params = getParams(queueId, consumerId, query);

    List<UUID> ids = getQueueRange(queueId, bounds, params);

    // get a list of ids from the consumer.
    List<TransactionPointer> pointers = getConsumerIds(queueId, consumerId, params);

    TransactionPointer pointer = null;

    int lastTransactionIndex = 0;

    for (lastTransactionIndex = 0; lastTransactionIndex < pointers.size(); lastTransactionIndex++) {

      pointer = pointers.get(lastTransactionIndex);

      int insertIndex = Collections.binarySearch(ids, pointer.expiration);

      // we're done, this message goes at the end, not point in continuing since
      // we have our full result set
      if (insertIndex == params.limit * -1 - 1) {
        break;
      }

      // get the insertion index into the set
      insertIndex = (insertIndex + 1) * -1;

      ids.add(pointer.targetMessage);

    }

    // now we've merge the results, trim them to size;
    ids = ids.subList(0, params.limit);

    // load the messages
    List<Message> messages = loadMessages(ids, params.reversed);

    // write our future timeouts for all these messages
    writeTransactions(ids, query.getTimeout(), queueId, consumerId);

    // remove all read transaction pointers
    deleteTransactionPointers(pointers, lastTransactionIndex, queueId, consumerId);

    // return the results
    QueueResults results = createResults(messages, queuePath, queueId, consumerId);

    writeClientPointer(queueId, consumerId, results.getLast());

    return results;
  }

  /**
   * Get all pending transactions that have timed out
   * 
   * @param queueId
   * @param consumerId
   * @param startId
   *          The time to start seeking from
   * @param lastId
   *          The
   * @return
   */
  protected List<TransactionPointer> getConsumerIds(UUID queueId, UUID consumerId, SearchParam params) {
    SliceQuery<ByteBuffer, UUID, UUID> q = createSliceQuery(ko, be, ue, ue);
    q.setColumnFamily(CONSUMER_QUEUE_TIMEOUTS.getColumnFamily());
    q.setKey(getQueueClientTransactionKey(queueId, consumerId));
    q.setRange(params.startId, null, false, params.limit);

    List<HColumn<UUID, UUID>> cassResults = q.execute().get().getColumns();

    List<TransactionPointer> results = new ArrayList<TransactionPointer>(params.limit);

    for (HColumn<UUID, UUID> column : cassResults) {
      results.add(new TransactionPointer(column.getName(), column.getValue()));
    }

    return results;
  }

  /**
   * Delete all re-read transaction pointers
   * 
   * @param pointers
   * @param maxIndex
   * @param queueId
   * @param consumerId
   */
  protected void deleteTransactionPointers(List<TransactionPointer> pointers, int maxIndex, UUID queueId,
      UUID consumerId) {
    Mutator<ByteBuffer> mutator = createMutator(ko, be);
    ByteBuffer key = getQueueClientTransactionKey(queueId, consumerId);

    for (int i = 0; i < maxIndex && i < pointers.size(); i++) {
      mutator.addDeletion(key, CONSUMER_QUEUE_TIMEOUTS.getColumnFamily(), pointers.get(i).expiration, ue);
    }

    mutator.execute();
  }

  /**
   * Write the transaction timeouts
   * 
   * @param messageIds
   * @param futureTimeout
   */
  protected void writeTransactions(List<UUID> messageIds, final long futureTimeout, UUID queueId, UUID consumerId) {

    Mutator<ByteBuffer> mutator = createMutator(ko, be);

    long futureSnapshot = System.currentTimeMillis() + futureTimeout;

    ByteBuffer key = getQueueClientTransactionKey(queueId, consumerId);

    for (UUID messageId : messageIds) {

      // note we're not incrementing futureSnapshot on purpose. The uuid
      // generation should give us a unique ID for each response, even if the
      // time is the same
      UUID expirationId = UUIDUtils.newTimeUUID(futureSnapshot);
      mutator.addInsertion(key, CONSUMER_QUEUE_TIMEOUTS.getColumnFamily(), createColumn(expirationId, messageId));
    }

    mutator.execute();
  }

  private static class TransactionPointer {
    private UUID expiration;
    private UUID targetMessage;

    /**
     * @param expiration
     * @param targetMessage
     */
    public TransactionPointer(UUID expiration, UUID targetMessage) {
      super();
      this.expiration = expiration;
      this.targetMessage = targetMessage;
    }

  }
}
