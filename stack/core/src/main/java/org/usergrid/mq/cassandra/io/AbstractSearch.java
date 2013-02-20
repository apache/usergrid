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
import static me.prettyprint.hector.api.factory.HFactory.createMultigetSliceQuery;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;
import static org.usergrid.mq.Queue.QUEUE_NEWEST;
import static org.usergrid.mq.Queue.QUEUE_OLDEST;
import static org.usergrid.mq.cassandra.CassandraMQUtils.deserializeMessage;
import static org.usergrid.mq.cassandra.CassandraMQUtils.getQueueShardRowKey;
import static org.usergrid.mq.cassandra.QueueManagerImpl.ALL_COUNT;
import static org.usergrid.mq.cassandra.QueueManagerImpl.QUEUE_SHARD_INTERVAL;
import static org.usergrid.mq.cassandra.QueueManagerImpl.se;
import static org.usergrid.mq.cassandra.QueuesCF.CONSUMERS;
import static org.usergrid.mq.cassandra.QueuesCF.MESSAGE_PROPERTIES;
import static org.usergrid.mq.cassandra.QueuesCF.QUEUE_INBOX;
import static org.usergrid.mq.cassandra.QueuesCF.QUEUE_PROPERTIES;
import static org.usergrid.utils.NumberUtils.roundLong;
import static org.usergrid.utils.UUIDUtils.MAX_TIME_UUID;
import static org.usergrid.utils.UUIDUtils.MIN_TIME_UUID;
import static org.usergrid.utils.UUIDUtils.getTimestampInMillis;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.SliceQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.mq.Message;
import org.usergrid.mq.QueueResults;
import org.usergrid.mq.cassandra.QueueManagerImpl.QueueBounds;
import org.usergrid.mq.cassandra.io.NoTransactionSearch.SearchParam;
import org.usergrid.persistence.exceptions.QueueException;

/**
 * @author tnine
 * 
 */
public abstract class AbstractSearch implements QueueSearch {

  private static final Logger logger = LoggerFactory.getLogger(AbstractSearch.class);

  protected static final UUIDSerializer ue = new UUIDSerializer();

  protected static final ByteBufferSerializer be = new ByteBufferSerializer();

  protected Keyspace ko;
  protected long cassTimestamp;

  /**
   * 
   */
  public AbstractSearch(Keyspace ko, long cassTimestamp) {
    this.ko = ko;
    this.cassTimestamp = cassTimestamp;
  }

  /**
   * Get the position in the queue for the given appId, consumer and queu
   * 
   * @param queueId
   *          The queueId
   * @param consumerId
   *          The consumerId
   * 
   * @return
   */
  protected UUID getConsumerQueuePosition(UUID queueId, UUID consumerId) {
    HColumn<UUID, UUID> result = HFactory.createColumnQuery(ko, ue, ue, ue).setKey(consumerId).setName(queueId)
        .setColumnFamily(CONSUMERS.getColumnFamily()).execute().get();
    if (result != null) {
      return result.getValue();
    }

    return null;
  }

  /**
   * Load the messages into an array list
   * 
   * @param messageIds
   * @return
   */
  protected List<Message> loadMessages(List<UUID> messageIds, boolean reversed) {

    Rows<UUID, String, ByteBuffer> messageResults = createMultigetSliceQuery(ko, ue, se, be)
        .setColumnFamily(MESSAGE_PROPERTIES.getColumnFamily()).setKeys(messageIds)
        .setRange(null, null, false, ALL_COUNT).execute().get();

    List<Message> messages = new ArrayList<Message>();
    for (Row<UUID, String, ByteBuffer> row : messageResults) {
      Message message = deserializeMessage(row.getColumnSlice().getColumns());
      if (message != null) {
        messages.add(message);
      }
    }

    //multiget hoses the order, so re-order them
    if(reversed){
      Message.sortReversed(messages);
    }else{
      Message.sort(messages);
    }

    return messages;
  }

  /**
   * Create the results to return from the given messages
   * 
   * @param messages
   * @param queuePath
   * @param queueId
   * @param consumerId
   * @return
   */
  protected QueueResults createResults(List<Message> messages, String queuePath, UUID queueId, UUID consumerId) {

    UUID lastId = null;

    if (messages != null && messages.size() > 0) {
      lastId = messages.get(messages.size() - 1).getUuid();
    }

    return new QueueResults(queuePath, queueId, messages, lastId, consumerId);
  }

  /**
   * Get a list of UUIDs that can be read for the client. This comes directly
   * from the queue inbox, and DOES NOT take into account client messages
   * 
   * @param queueId The queue id to read
   * @param bounds The bounds to use when reading
   * 
   * @return
   */
  protected List<UUID> getQueueRange(UUID queueId, QueueBounds bounds, SearchParam params) {

    if (bounds == null) {
      logger.error("Necessary queue bounds not found");
      throw new QueueException("Neccessary queue bounds not found");
    }

    UUID finish_uuid = params.reversed ?  bounds.getOldest() : bounds.getNewest();

    List<UUID> results = new ArrayList<UUID>(params.limit);

    UUID start = params.startId;
    
    if (start == null) {
      start = params.reversed ? bounds.getNewest() : bounds.getOldest();
    }

    if (start == null) {
      logger.error("No first message in queue");
      return results;
    }

    if (finish_uuid == null) {
      logger.error("No last message in queue");
      return results;
    }

    long start_ts_shard = roundLong(getTimestampInMillis(start), QUEUE_SHARD_INTERVAL);

    long finish_ts_shard = roundLong(getTimestampInMillis(finish_uuid), QUEUE_SHARD_INTERVAL);

    long current_ts_shard = start_ts_shard;
    if (params.reversed) {
      current_ts_shard = finish_ts_shard;
    }

    while ((current_ts_shard >= start_ts_shard) && (current_ts_shard <= finish_ts_shard)) {

      UUID slice_start = MIN_TIME_UUID;
      UUID slice_end = MAX_TIME_UUID;

      if (current_ts_shard == start_ts_shard) {
        slice_start = start;
      }

      if (current_ts_shard == finish_ts_shard) {
        slice_end = finish_uuid;
      }

      SliceQuery<ByteBuffer, UUID, ByteBuffer> q = createSliceQuery(ko, be, ue, be);
      q.setColumnFamily(QUEUE_INBOX.getColumnFamily());
      q.setKey(getQueueShardRowKey(queueId, current_ts_shard));
      q.setRange(slice_start, slice_end, params.reversed, params.limit+1);

      List<HColumn<UUID, ByteBuffer>> cassResults = q.execute().get().getColumns();
     
      for (int i = 0; i < cassResults.size(); i ++)  {
        HColumn<UUID, ByteBuffer> column = cassResults.get(i);
        
        //skip the first one, we've already read it
        if(i == 0 && params.skipFirst && params.startId.equals(column.getName())){
          continue;
        }
        
        results.add(column.getName());

        if (results.size() >= params.limit) {
          return results;
        }
      }

      if (params.reversed) {
        current_ts_shard -= QUEUE_SHARD_INTERVAL;
      } else {
        current_ts_shard += QUEUE_SHARD_INTERVAL;
      }
    }

    return results;
  }

  /**
   * Get the bounds for the queue
   * 
   * @param ko
   * @param queueId
   * @return
   */
  protected QueueBounds getQueueBounds(UUID queueId) {
    try {
      ColumnSlice<String, UUID> result = HFactory.createSliceQuery(ko, ue, se, ue).setKey(queueId)
          .setColumnNames(QUEUE_NEWEST, QUEUE_OLDEST).setColumnFamily(QUEUE_PROPERTIES.getColumnFamily()).execute()
          .get();
      if (result != null) {
        return new QueueBounds(result.getColumnByName(QUEUE_OLDEST).getValue(), result.getColumnByName(QUEUE_NEWEST)
            .getValue());
      }
    } catch (Exception e) {
      logger.error("Error getting oldest queue message ID", e);
    }
    return null;
  }

  /**
   * Write the updated client pointer
   * 
   * @param queueId
   * @param lastReturnedId
   */
  protected void writeClientPointer(UUID queueId, UUID consumerId, UUID lastReturnedId) {
    //nothing to do
    if(lastReturnedId == null){
      return;
    }
    
    Mutator<UUID> mutator = createMutator(ko, ue);

    mutator.addInsertion(consumerId, CONSUMERS.getColumnFamily(),
        createColumn(queueId, lastReturnedId, cassTimestamp, ue, ue));

    mutator.execute();
  }

}
