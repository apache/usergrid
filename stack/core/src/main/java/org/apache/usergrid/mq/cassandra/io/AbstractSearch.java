/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.mq.cassandra.io;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.thrift.InvalidRequestException;

import org.apache.usergrid.mq.Message;
import org.apache.usergrid.mq.QueueResults;
import org.apache.usergrid.mq.cassandra.io.NoTransactionSearch.SearchParam;
import org.apache.usergrid.persistence.exceptions.QueueException;
import org.apache.usergrid.persistence.hector.CountingMutator;
import org.apache.usergrid.utils.UUIDUtils;

import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.SliceQuery;

import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static me.prettyprint.hector.api.factory.HFactory.createMultigetSliceQuery;
import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;
import static org.apache.usergrid.mq.Queue.QUEUE_NEWEST;
import static org.apache.usergrid.mq.Queue.QUEUE_OLDEST;
import static org.apache.usergrid.mq.cassandra.CassandraMQUtils.deserializeMessage;
import static org.apache.usergrid.mq.cassandra.CassandraMQUtils.getQueueShardRowKey;
import static org.apache.usergrid.mq.cassandra.QueueManagerImpl.ALL_COUNT;
import static org.apache.usergrid.mq.cassandra.QueueManagerImpl.QUEUE_SHARD_INTERVAL;
import static org.apache.usergrid.mq.cassandra.QueuesCF.CONSUMERS;
import static org.apache.usergrid.mq.cassandra.QueuesCF.MESSAGE_PROPERTIES;
import static org.apache.usergrid.mq.cassandra.QueuesCF.QUEUE_INBOX;
import static org.apache.usergrid.mq.cassandra.QueuesCF.QUEUE_PROPERTIES;
import static org.apache.usergrid.persistence.cassandra.Serializers.be;
import static org.apache.usergrid.persistence.cassandra.Serializers.se;
import static org.apache.usergrid.persistence.cassandra.Serializers.ue;
import static org.apache.usergrid.utils.NumberUtils.roundLong;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMillis;


/** @author tnine */
public abstract class AbstractSearch implements QueueSearch {

    private static final Logger logger = LoggerFactory.getLogger( AbstractSearch.class );

    protected Keyspace ko;


    /**
     *
     */
    public AbstractSearch( Keyspace ko ) {
        this.ko = ko;
    }


    /**
     * Get the position in the queue for the given appId, consumer and queu
     *
     * @param queueId The queueId
     * @param consumerId The consumerId
     */
    public UUID getConsumerQueuePosition( UUID queueId, UUID consumerId ) {
        HColumn<UUID, UUID> result =
                HFactory.createColumnQuery( ko, ue, ue, ue ).setKey( consumerId ).setName( queueId )
                        .setColumnFamily( CONSUMERS.getColumnFamily() ).execute().get();
        if ( result != null ) {
            return result.getValue();
        }

        return null;
    }


    /** Load the messages into an array list */
    protected List<Message> loadMessages( Collection<UUID> messageIds, boolean reversed ) {

        Rows<UUID, String, ByteBuffer> messageResults =
                createMultigetSliceQuery( ko, ue, se, be ).setColumnFamily( MESSAGE_PROPERTIES.getColumnFamily() )
                                                          .setKeys( messageIds )
                                                          .setRange( null, null, false, ALL_COUNT ).execute().get();

        List<Message> messages = new ArrayList<Message>( messageIds.size() );

        for ( Row<UUID, String, ByteBuffer> row : messageResults ) {
            Message message = deserializeMessage( row.getColumnSlice().getColumns() );

            if ( message != null ) {
                messages.add( message );
            }
        }

        Collections.sort( messages, new RequestedOrderComparator( messageIds ) );

        return messages;
    }


    /** Create the results to return from the given messages */
    protected QueueResults createResults( List<Message> messages, String queuePath, UUID queueId, UUID consumerId ) {

        UUID lastId = null;

        if ( messages != null && messages.size() > 0 ) {
            lastId = messages.get( messages.size() - 1 ).getUuid();
        }

        return new QueueResults( queuePath, queueId, messages, lastId, consumerId );
    }


    /**
     * Get a list of UUIDs that can be read for the client. This comes directly from the queue inbox, and DOES NOT take
     * into account client messages
     *
     * @param queueId The queue id to read
     * @param bounds The bounds to use when reading
     */
    protected List<UUID> getQueueRange( UUID queueId, QueueBounds bounds, SearchParam params ) {

        if ( bounds == null ) {
            logger.error( "Necessary queue bounds not found" );
            throw new QueueException( "Neccessary queue bounds not found" );
        }

        UUID finish_uuid = params.reversed ? bounds.getOldest() : bounds.getNewest();

        List<UUID> results = new ArrayList<>( params.limit );

        UUID start = params.startId;

        if ( start == null ) {
            start = params.reversed ? bounds.getNewest() : bounds.getOldest();
        }

        if ( start == null ) {
            logger.error( "No first message in queue" );
            return results;
        }

        if ( finish_uuid == null ) {
            logger.error( "No last message in queue" );
            return results;
        }

        long start_ts_shard = roundLong( getTimestampInMillis( start ), QUEUE_SHARD_INTERVAL );

        long finish_ts_shard = roundLong( getTimestampInMillis( finish_uuid ), QUEUE_SHARD_INTERVAL );

        long current_ts_shard = start_ts_shard;

        if ( params.reversed ) {
            current_ts_shard = finish_ts_shard;
        }

        final MessageIdComparator comparator = new MessageIdComparator( params.reversed );


        //should be start < finish
        if ( comparator.compare( start, finish_uuid ) > 0 ) {
            logger.warn( "Tried to perform a slice with start UUID {} after finish UUID {}.", start, finish_uuid );
            throw new IllegalArgumentException(
                    String.format( "You cannot specify a start value of %s after finish value of %s", start,
                            finish_uuid ) );
        }


        UUID lastValue = start;
        boolean firstPage = true;

        while ( ( current_ts_shard >= start_ts_shard ) && ( current_ts_shard <= finish_ts_shard )
                && comparator.compare( start, finish_uuid ) < 1 ) {

            logger.info( "Starting search with start UUID {}, finish UUID {}, and reversed {}",
                    new Object[] { lastValue, finish_uuid, params.reversed } );


            SliceQuery<ByteBuffer, UUID, ByteBuffer> q = createSliceQuery( ko, be, ue, be );
            q.setColumnFamily( QUEUE_INBOX.getColumnFamily() );
            q.setKey( getQueueShardRowKey( queueId, current_ts_shard ) );
            q.setRange( lastValue, finish_uuid, params.reversed, params.limit + 1 );

            final List<HColumn<UUID, ByteBuffer>> cassResults = swallowOrderedExecution(q);


            for ( int i = 0; i < cassResults.size(); i++ ) {
                HColumn<UUID, ByteBuffer> column = cassResults.get( i );

                final UUID columnName = column.getName();

                // skip the first one, we've already read it
                if ( i == 0 && ( firstPage && params.skipFirst && params.startId.equals( columnName ) ) || ( !firstPage
                        && lastValue != null && lastValue.equals( columnName ) ) ) {
                    continue;
                }


                lastValue = columnName;

                results.add( columnName );

                logger.debug( "Added id '{}' to result set for queue id '{}'", start, queueId );

                if ( results.size() >= params.limit ) {
                    return results;
                }

                firstPage = false;
            }

            if ( params.reversed ) {
                current_ts_shard -= QUEUE_SHARD_INTERVAL;
            }
            else {
                current_ts_shard += QUEUE_SHARD_INTERVAL;
            }
        }

        return results;
    }


    /**
     * Get the bounds for the queue
     *
     * @return The bounds for the queue
     */
    public QueueBounds getQueueBounds( UUID queueId ) {
        try {
            ColumnSlice<String, UUID> result = HFactory.createSliceQuery( ko, ue, se, ue ).setKey( queueId )
                                                       .setColumnNames( QUEUE_NEWEST, QUEUE_OLDEST )
                                                       .setColumnFamily( QUEUE_PROPERTIES.getColumnFamily() ).execute()
                                                       .get();
            if ( result != null && result.getColumnByName( QUEUE_OLDEST ) != null
                    && result.getColumnByName( QUEUE_NEWEST ) != null ) {
                return new QueueBounds( result.getColumnByName( QUEUE_OLDEST ).getValue(),
                        result.getColumnByName( QUEUE_NEWEST ).getValue() );
            }
        }
        catch ( Exception e ) {
            logger.error( "Error getting oldest queue message ID", e );
        }
        return null;
    }


    /**
     * Write the updated client pointer
     *
     * @param lastReturnedId This is a null safe parameter. If it's null, this won't be written since it means we didn't
     * read any messages
     */
    protected void writeClientPointer( UUID queueId, UUID consumerId, UUID lastReturnedId ) {
        // nothing to do
        if ( lastReturnedId == null ) {
            return;
        }

        // we want to set the timestamp to the value from the time uuid. If this is
        // not the max time uuid to ever be written
        // for this consumer, we want this to be discarded to avoid internode race
        // conditions with clock drift.
        long colTimestamp = UUIDUtils.getTimestampInMicros( lastReturnedId );

        Mutator<UUID> mutator = CountingMutator.createFlushingMutator( ko, ue );

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Writing last client id pointer of '{}' for queue '{}' and consumer '{}' with timestamp '{}",
                    new Object[] {
                            lastReturnedId, queueId, consumerId, colTimestamp
                    } );
        }

        mutator.addInsertion( consumerId, CONSUMERS.getColumnFamily(),
                createColumn( queueId, lastReturnedId, colTimestamp, ue, ue ) );

        mutator.execute();
    }


    private class RequestedOrderComparator implements Comparator<Message> {

        private Map<UUID, Integer> indexCache = new HashMap<UUID, Integer>();


        private RequestedOrderComparator( Collection<UUID> ids ) {
            int i = 0;

            for ( UUID id : ids ) {
                indexCache.put( id, i );
                i++;
            }
        }


        /*
         * (non-Javadoc)
         *
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare( Message o1, Message o2 ) {
            int o1Idx = indexCache.get( o1.getUuid() );

            int o2Idx = indexCache.get( o2.getUuid() );

            return o1Idx - o2Idx;
        }
    }


    protected static final class MessageIdComparator implements Comparator<UUID> {

        private final int comparator;


        protected MessageIdComparator( final boolean reversed ) {

            this.comparator = reversed ? -1 : 1;
        }


        @Override
        public int compare( final UUID o1, final UUID o2 ) {
            return UUIDUtils.compare( o1, o2 ) * comparator;
        }
    }


    /**
     * This method intentionally swallows ordered execution issues.  For some reason, our Time UUID ordering does
     * not agree with the cassandra comparator as our micros get very close
     * @param query
     * @param <K>
     * @param <UUID>
     * @param <V>
     * @return
     */
    protected static <K, UUID, V> List<HColumn<UUID, V>> swallowOrderedExecution( final SliceQuery<K, UUID, V> query ) {
        try {

            return query.execute().get().getColumns();
        }
        catch ( HInvalidRequestException e ) {
            //invalid request.  Occasionally we get order issues when there shouldn't be, disregard them.

            final Throwable invalidRequestException = e.getCause();

            if ( invalidRequestException instanceof InvalidRequestException
                    //we had a range error
                    && ( ( InvalidRequestException ) invalidRequestException ).getWhy().contains(
                    "range finish must come after start in the order of traversal" )) {
                return Collections.emptyList();
            }

            throw e;
        }
    }


}
