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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.locking.Lock;
import org.apache.usergrid.locking.LockManager;
import org.apache.usergrid.locking.exception.UGLockException;
import org.apache.usergrid.mq.Message;
import org.apache.usergrid.mq.QueueQuery;
import org.apache.usergrid.mq.QueueResults;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.exceptions.QueueException;
import org.apache.usergrid.persistence.exceptions.TransactionNotFoundException;
import org.apache.usergrid.persistence.hector.CountingMutator;
import org.apache.usergrid.utils.UUIDUtils;

import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.SliceQuery;

import static me.prettyprint.hector.api.factory.HFactory.createColumn;

import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;
import static org.apache.usergrid.mq.cassandra.CassandraMQUtils.getConsumerId;
import static org.apache.usergrid.mq.cassandra.CassandraMQUtils.getQueueClientTransactionKey;
import static org.apache.usergrid.mq.cassandra.CassandraMQUtils.getQueueId;
import static org.apache.usergrid.mq.cassandra.QueuesCF.CONSUMER_QUEUE_TIMEOUTS;
import static org.apache.usergrid.persistence.cassandra.Serializers.*;

/**
 * Reads from the queue and starts a transaction
 *
 * @author tnine
 */
public class ConsumerTransaction extends NoTransactionSearch
{

    private static final Logger logger = LoggerFactory.getLogger( ConsumerTransaction.class );
    private static final int MAX_READ = 10000;
    private final LockManager lockManager;
    private final UUID applicationId;
    protected final CassandraService cass;

    //timeout on reading lock
    private final int lockTimeout;


    /**
     * @param ko
     */
    public ConsumerTransaction( UUID applicationId, Keyspace ko, LockManager lockManager, CassandraService cass, int lockTimeout )
    {
        super( ko );
        this.applicationId = applicationId;
        this.lockManager = lockManager;
        this.cass = cass;
        this.lockTimeout = lockTimeout;
    }


    /**
     * Renew the existing transaction. Does so by deleting the exiting timeout, and replacing it with a new value
     *
     * @param queuePath The queue path
     * @param transactionId The transaction id
     * @param query The query params
     *
     * @return The new transaction uuid
     */
    public UUID renewTransaction( String queuePath, UUID transactionId, QueueQuery query )
            throws TransactionNotFoundException
    {
        long now = System.currentTimeMillis();

        if ( query == null )
        {
            query = new QueueQuery();
        }

        UUID queueId = getQueueId( queuePath );
        UUID consumerId = getConsumerId( queueId, query );
        ByteBuffer key = getQueueClientTransactionKey( queueId, consumerId );

        // read the original transaction, if it's not there, then we can't possibly
        // extend it
        SliceQuery<ByteBuffer, UUID, UUID> q = createSliceQuery( ko, be, ue, ue );
        q.setColumnFamily( CONSUMER_QUEUE_TIMEOUTS.getColumnFamily() );
        q.setKey( key );
        q.setColumnNames( transactionId );

        HColumn<UUID, UUID> col = q.execute().get().getColumnByName( transactionId );

        if ( col == null )
        {
            throw new TransactionNotFoundException(
                    String.format( "No transaction with id %s exists", transactionId ) );
        }

        UUID origTrans = col.getName();
        UUID messageId = col.getValue();

        // Generate a new expiration and insert it
        UUID expirationId = UUIDUtils.newTimeUUID( now + query.getTimeout() );

        logger.debug( "Writing new timeout at '{}' for message '{}'", expirationId, messageId );


        Mutator<ByteBuffer> mutator = CountingMutator.createFlushingMutator( ko, be );

        mutator.addInsertion( key, CONSUMER_QUEUE_TIMEOUTS.getColumnFamily(),
                createColumn( expirationId, messageId, cass.createTimestamp(), ue, ue ) );

        mutator.execute();

        // now delete the old value
        deleteTransaction( queueId, consumerId, origTrans );

        return expirationId;
    }


    /** Delete the specified transaction */
    public void deleteTransaction( String queuePath, UUID transactionId, QueueQuery query )
    {

        if ( query == null )
        {
            query = new QueueQuery();
        }

        UUID queueId = getQueueId( queuePath );
        UUID consumerId = getConsumerId( queueId, query );

        deleteTransaction( queueId, consumerId, transactionId );
    }


    /** Delete the specified transaction */
    private void deleteTransaction( UUID queueId, UUID consumerId, UUID transactionId )
    {

        Mutator<ByteBuffer> mutator = CountingMutator.createFlushingMutator( ko, be );
        ByteBuffer key = getQueueClientTransactionKey( queueId, consumerId );

        mutator.addDeletion( key, CONSUMER_QUEUE_TIMEOUTS.getColumnFamily(), transactionId, ue,
                cass.createTimestamp() );

        mutator.execute();
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.mq.cassandra.io.QueueSearch#getResults(java.lang.String,
     * org.apache.usergrid.mq.QueueQuery)
     */
    @Override
    public QueueResults getResults( String queuePath, QueueQuery query )
    {

        UUID queueId = getQueueId( queuePath );
        UUID consumerId = getConsumerId( queueId, query );

        if ( query.getLimit() > MAX_READ )
        {
            throw new IllegalArgumentException( String.format(
                    "You specified a size of %d, you cannot specify a size larger than %d when using transations",
                    query.getLimit( DEFAULT_READ ), MAX_READ ) );
        }

        QueueResults results = null;

        Lock lock = lockManager.createLock( applicationId, queueId.toString(), consumerId.toString() );

        try
        {

            //only try to get a lock with a timeout, if we can't bail
            if(!lock.tryLock(lockTimeout, TimeUnit.SECONDS)){
                throw new QueueException( "Unable to obtain a lock on queue '" + queuePath + "' after '" + lockTimeout + "'seconds" );
            }

            long startTime = System.currentTimeMillis();

            UUID startTimeUUID = UUIDUtils.newTimeUUID( startTime, 0 );   //this exact moment in time + clockseq + node

            QueueBounds bounds = getQueueBounds( queueId );  //first write in time, most write in time

            //queue has never been written to
            if ( bounds == null )
            {
                return createResults( new ArrayList<Message>( 0 ), queuePath, queueId, consumerId );
            }

            // with transactional reads, we can't read into the future, set the bounds
            // to be now
            bounds = new QueueBounds( bounds.getOldest(), startTimeUUID );

            SearchParam params = getParams( queueId, consumerId, query );

            //if startId is greater than our max, we disregard it and reset to now because we've advanced beyond
            //"now"
            if( params.startId != null && UUIDUtils.compare( params.startId, startTimeUUID ) > 0){
                logger.warn( "Our cursor has advanced beyond the end of the queue due to transactions.  Was {}, resetting to {}", params.startId, startTimeUUID );
                params = new SearchParam( startTimeUUID, params.reversed, false, params.limit );
            }


            List<UUID> ids = getQueueRange( queueId, bounds, params );

            // get a list of ids from the consumer.

            List<TransactionPointer> pointers = getConsumerIds( queueId, consumerId, params, startTimeUUID );

            TransactionPointer pointer;

            int lastTransactionIndex = -1;

            for ( int i = 0; i < pointers.size(); i++ )
            {

                pointer = pointers.get( i );

                int insertIndex = Collections.binarySearch( ids, pointer.expiration );

                // we're done, this message goes at the end, no point in continuing
                // since
                // we have our full result set
                if ( insertIndex <= params.limit * -1 - 1 )
                {
                    break;
                }

                // get the insertion index into the set
                insertIndex = ( insertIndex + 1 ) * -1;

                ids.add( insertIndex, pointer.targetMessage );

                lastTransactionIndex = i;
            }

            // now we've merge the results, trim them to size;
            if ( ids.size() > params.limit )
            {
                ids = ids.subList( 0, params.limit );
            }

            // load the messages
            List<Message> messages = loadMessages( ids, params.reversed );

            // write our future timeouts for all these messages
            writeTransactions( messages, query.getTimeout() + startTime, queueId, consumerId );

            // remove all read transaction pointers
            deleteTransactionPointers( pointers, lastTransactionIndex + 1, queueId, consumerId );

            // return the results
            results = createResults( messages, queuePath, queueId, consumerId );

            UUID lastReadTransactionPointer =
                    lastTransactionIndex == -1 ? null : pointers.get( lastTransactionIndex ).expiration;

            UUID lastId = messages.size() == 0 ? null : messages.get( messages.size() - 1 ).getUuid();

            // our last read id will either be the last read transaction pointer, or
            // the
            // last read messages uuid, whichever is greater
            UUID lastReadId = UUIDUtils.max( lastReadTransactionPointer, lastId );

            //we can only store the min of the queue Id, beyond that we'll cause errors
            lastReadId = UUIDUtils.min( lastReadId, bounds.getNewest() );

            writeClientPointer( queueId, consumerId, lastReadId );
        }
        catch ( UGLockException e )
        {
            logger.debug( "Unable to acquire lock", e );
            throw new QueueException( "Unable to acquire lock", e );
        }
        finally
        {
            try
            {
                lock.unlock();
            }
            catch ( UGLockException e )
            {
                logger.debug( "Unable to release lock", e );
                throw new QueueException( "Unable to release lock", e );
            }
        }

        return results;
    }


    /**
     * Get all pending transactions that have timed out
     *
     * @param queueId The queue id
     * @param consumerId The consumer id
     * @param params The server params
     * @param startTimeUUID The start time
     */
    protected List<TransactionPointer> getConsumerIds( UUID queueId, UUID consumerId, SearchParam params,
                                                       UUID startTimeUUID )
    {

        SliceQuery<ByteBuffer, UUID, UUID> q = createSliceQuery( ko, be, ue, ue );
        q.setColumnFamily( CONSUMER_QUEUE_TIMEOUTS.getColumnFamily() );
        q.setKey( getQueueClientTransactionKey( queueId, consumerId ) );
        q.setRange( params.startId, startTimeUUID, false, params.limit + 1 );

        List<HColumn<UUID, UUID>> cassResults = swallowOrderedExecution(q);

        List<TransactionPointer> results = new ArrayList<TransactionPointer>( params.limit );

        for ( HColumn<UUID, UUID> column : cassResults )
        {

            if ( logger.isDebugEnabled() )
            {
                logger.debug( "Adding uuid '{}' for original message '{}' to results for queue '{}' and consumer '{}'",
                        new Object[] { column.getName(), column.getValue(), queueId, consumerId } );
                logger.debug( "Max timeuuid : '{}', Current timeuuid : '{}', comparison '{}'", new Object[] {
                        startTimeUUID, column.getName(), UUIDUtils.compare( startTimeUUID, column.getName() )
                } );
            }

            results.add( new TransactionPointer( column.getName(), column.getValue() ) );
        }

        return results;
    }


    public boolean hasOutstandingTransactions( UUID queueId, UUID consumerId )
    {
        SliceQuery<ByteBuffer, UUID, UUID> q = createSliceQuery( ko, be, ue, ue );
        q.setColumnFamily( CONSUMER_QUEUE_TIMEOUTS.getColumnFamily() );
        q.setKey( getQueueClientTransactionKey( queueId, consumerId ) );
        q.setRange( null, null, false, 1 );
        return q.execute().get().getColumns().size() > 0;
    }


    /**
     * Delete all re-read transaction pointers
     *
     * @param pointers The list of transaction pointers
     * @param maxIndex The index to stop at (exclusive)
     * @param queueId The queue id
     * @param consumerId The consumer id
     */
    protected void deleteTransactionPointers( List<TransactionPointer> pointers, int maxIndex, UUID queueId,
                                              UUID consumerId )
    {

        if ( maxIndex == 0 || pointers.size() == 0 )
        {
            return;
        }

        Mutator<ByteBuffer> mutator = CountingMutator.createFlushingMutator( ko, be );
        ByteBuffer key = getQueueClientTransactionKey( queueId, consumerId );

        for ( int i = 0; i < maxIndex && i < pointers.size(); i++ )
        {
            UUID pointer = pointers.get( i ).expiration;

            if ( logger.isDebugEnabled() )
            {
                logger.debug( "Removing transaction pointer '{}' for queue '{}' and consumer '{}'", new Object[] {
                        pointer, queueId, consumerId
                } );
            }

            mutator.addDeletion( key, CONSUMER_QUEUE_TIMEOUTS.getColumnFamily(), pointer, ue, cass.createTimestamp() );
        }

        mutator.execute();
    }


    /**
     * Write the transaction timeouts
     *
     * @param messages The messages to load
     * @param futureTimeout The time these message should expire
     * @param queueId The queue UUId
     * @param consumerId The consumer Id
     */
    protected void writeTransactions( List<Message> messages, final long futureTimeout, UUID queueId, UUID consumerId )
    {

        Mutator<ByteBuffer> mutator = CountingMutator.createFlushingMutator( ko, be );

        ByteBuffer key = getQueueClientTransactionKey( queueId, consumerId );

        int counter = 0;

        long time = cass.createTimestamp();

        for ( Message message : messages )
        {
            // note we're not incrementing futureSnapshot on purpose. The uuid
            // generation should give us a sequenced unique ID for each response, even
            // if the
            // time is the same since we increment the counter. If we read more than
            // 10k messages in a single transaction, our millisecond will roll to the
            // next due to 10k being the max amount of 1/10 microsecond headroom. Not
            // possible to avoid this given the way time uuids are encoded.
            UUID expirationId = UUIDUtils.newTimeUUID( futureTimeout, counter );
            UUID messageId = message.getUuid();

            logger.debug( "Writing new timeout at '{}' for message '{}'", expirationId, messageId );

            mutator.addInsertion( key, CONSUMER_QUEUE_TIMEOUTS.getColumnFamily(),
                    createColumn( expirationId, messageId, time, ue, ue ) );

            // add the transactionid to the message
            message.setTransaction( expirationId );
            counter++;
        }

        mutator.execute();
    }


    private static class TransactionPointer
    {
        private UUID expiration;
        private UUID targetMessage;


        /**
         * @param expiration
         * @param targetMessage
         */
        public TransactionPointer( UUID expiration, UUID targetMessage )
        {
            super();
            this.expiration = expiration;
            this.targetMessage = targetMessage;
        }


        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            return "TransactionPointer [expiration=" + expiration + ", targetMessage=" + targetMessage + "]";
        }
    }
}
