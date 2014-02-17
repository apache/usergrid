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
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.mq.Message;
import org.apache.usergrid.mq.QueryProcessor;
import org.apache.usergrid.mq.QueryProcessor.QuerySlice;
import org.apache.usergrid.mq.QueueQuery;
import org.apache.usergrid.mq.QueueResults;

import com.fasterxml.uuid.UUIDComparator;

import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.AbstractComposite.ComponentEquality;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;

import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;
import static org.apache.usergrid.mq.Queue.getQueueId;
import static org.apache.usergrid.mq.cassandra.CassandraMQUtils.getConsumerId;
import static org.apache.usergrid.mq.cassandra.QueueManagerImpl.DEFAULT_SEARCH_COUNT;
import static org.apache.usergrid.mq.cassandra.QueueManagerImpl.QUEUE_SHARD_INTERVAL;
import static org.apache.usergrid.mq.cassandra.QueuesCF.PROPERTY_INDEX;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.apache.usergrid.utils.CompositeUtils.setEqualityFlag;
import static org.apache.usergrid.utils.ConversionUtils.bytebuffer;
import static org.apache.usergrid.utils.NumberUtils.roundLong;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMillis;


/**
 * Searches in the queue without transactions
 *
 * @author tnine
 */
public class FilterSearch extends NoTransactionSearch
{

    private static final Logger logger = LoggerFactory.getLogger( FilterSearch.class );


    /**
     *
     */
    public FilterSearch( Keyspace ko )
    {
        super( ko );
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

        QueryProcessor qp = new QueryProcessor( query );
        List<QuerySlice> slices = qp.getSlices();

        long limit = query.getLimit();

        UUID queueId = getQueueId( queuePath );
        UUID consumerId = getConsumerId( queueId, query );
        QueueBounds bounds = getQueueBounds( queueId );

        UUIDComparator comparator = query.isReversed() ? new ReverseUUIDComparator() : new UUIDComparator();

        SortedSet<UUID> merged = null;

        for ( QuerySlice slice : slices )
        {
            SortedSet<UUID> results =
                    searchQueueRange( ko, queueId, bounds, slice, query.getLastMessageId(), query.isReversed(),
                            comparator );

            if ( merged == null )
            {
                merged = results;
            }
            else
            {
                merged.retainAll( results );
            }
        }

        // now trim. Not efficient, but when indexing is updated, seeking will
        // change, so I'm not worried about this.
        if ( merged.size() > limit )
        {
            Iterator<UUID> current = merged.iterator();
            UUID max = null;


            for ( int i = 0; i <= limit && current.hasNext(); i++ )
            {
                max = current.next();
            }

            merged = merged.headSet( max );
        }

        List<Message> messages = loadMessages( merged, query.isReversed() );

        QueueResults results = createResults( messages, queuePath, queueId, consumerId );

        return results;
    }


    public SortedSet<UUID> searchQueueRange( Keyspace ko, UUID queueId, QueueBounds bounds, QuerySlice slice, UUID last,
                                             boolean reversed, UUIDComparator comparator )
    {

        TreeSet<UUID> uuid_set = new TreeSet<UUID>( comparator );

        if ( bounds == null )
        {
            logger.error( "Necessary queue bounds not found" );
            return uuid_set;
        }

        UUID start_uuid = reversed ? bounds.getNewest() : bounds.getOldest();

        UUID finish_uuid = reversed ? bounds.getOldest() : bounds.getNewest();

        if ( last != null )
        {
            start_uuid = last;
        }


        if ( finish_uuid == null )
        {
            logger.error( "No last message in queue" );
            return uuid_set;
        }

        long start_ts_shard = roundLong( getTimestampInMillis( start_uuid ), QUEUE_SHARD_INTERVAL );

        long finish_ts_shard = roundLong( getTimestampInMillis( finish_uuid ), QUEUE_SHARD_INTERVAL );

        long current_ts_shard = start_ts_shard;

        if ( reversed )
        {
            current_ts_shard = finish_ts_shard;
        }

        ByteBuffer start = null;
        if ( slice.getCursor() != null )
        {
            start = slice.getCursor();
        }
        else if ( slice.getStart() != null )
        {
            DynamicComposite s = new DynamicComposite( slice.getStart().getCode(), slice.getStart().getValue() );
            if ( !slice.getStart().isInclusive() )
            {
                setEqualityFlag( s, ComponentEquality.GREATER_THAN_EQUAL );
            }
            start = s.serialize();
        }

        ByteBuffer finish = null;
        if ( slice.getFinish() != null )
        {
            DynamicComposite f = new DynamicComposite( slice.getFinish().getCode(), slice.getFinish().getValue() );
            if ( slice.getFinish().isInclusive() )
            {
                setEqualityFlag( f, ComponentEquality.GREATER_THAN_EQUAL );
            }
            finish = f.serialize();
        }

        while ( ( current_ts_shard >= start_ts_shard ) && ( current_ts_shard <= finish_ts_shard ) && ( uuid_set.size()
                < DEFAULT_SEARCH_COUNT ) )
        {

            while ( true )
            {
                List<HColumn<ByteBuffer, ByteBuffer>> results =
                        createSliceQuery( ko, be, be, be ).setColumnFamily( PROPERTY_INDEX.getColumnFamily() )
                                .setKey( bytebuffer( key( queueId, current_ts_shard, slice.getPropertyName() ) ) )
                                .setRange( start, finish, false, DEFAULT_SEARCH_COUNT ).execute().get().getColumns();

                for ( HColumn<ByteBuffer, ByteBuffer> column : results )
                {
                    DynamicComposite c = DynamicComposite.fromByteBuffer( column.getName().duplicate() );
                    UUID uuid = c.get( 2, ue );

                    uuid_set.add( uuid );
                }

                if ( results.size() < DEFAULT_SEARCH_COUNT )
                {
                    break;
                }

                start = results.get( results.size() - 1 ).getName().duplicate();
            }

            if ( reversed )
            {
                current_ts_shard -= QUEUE_SHARD_INTERVAL;
            }
            else
            {
                current_ts_shard += QUEUE_SHARD_INTERVAL;
            }
        }

        // trim the results
        return uuid_set.headSet( finish_uuid ).tailSet( start_uuid );
    }


    private static class ReverseUUIDComparator extends UUIDComparator
    {

        /*
         * (non-Javadoc)
         *
         * @see com.fasterxml.uuid.UUIDComparator#compare(java.util.UUID,
         * java.util.UUID)
         */
        @Override
        public int compare( UUID u1, UUID u2 )
        {
            return super.compare( u1, u2 ) * -1;
        }
    }
}
