/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.qakka.serialization.queuemessages.impl;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.apache.usergrid.persistence.core.datastax.impl.TableDefinitionStringImpl;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.exceptions.NotFoundException;
import org.apache.usergrid.persistence.qakka.exceptions.QakkaRuntimeException;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardCounterSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


@Singleton
public class MessageCounterSerializationImpl implements ShardCounterSerialization {
    private static final Logger logger = LoggerFactory.getLogger( MessageCounterSerializationImpl.class );

    private final CassandraClient cassandraClient;

    final static String TABLE_SHARD_COUNTERS       = "counters";
    final static String COLUMN_QUEUE_NAME    = "queue_name";
    final static String COLUMN_SHARD_ID      = "shard_id";
    final static String COLUMN_COUNTER_VALUE = "counter_value";
    final static String COLUMN_SHARD_TYPE    = "shard_type";

    // design note: counters based on DataStax example here:
    // https://docs.datastax.com/en/cql/3.1/cql/cql_using/use_counter_t.html

    static final String CQL =
        "CREATE TABLE IF NOT EXISTS shard_counters ( " +
                "counter_value counter, " +
                "queue_name    varchar, " +
                "shard_type    varchar, " +
                "shard_id      bigint, " +
                "PRIMARY KEY (queue_name, shard_type, shard_id) " +
        ");";


    final long maxInMemoryIncrement;

    class InMemoryCount {
        long baseCount;
        final AtomicLong increment = new AtomicLong( 0L );
        InMemoryCount( long baseCount ) {
            this.baseCount = baseCount;
        }
        public long value() {
            return baseCount + increment.get();
        }
        public AtomicLong getIncrement() {
            return increment;
        }
        void setBaseCount( long baseCount ) {
            this.baseCount = baseCount;
        }
    }

    private Map<String, InMemoryCount> inMemoryCounters = new ConcurrentHashMap<>(200);


    @Inject
    public MessageCounterSerializationImpl(QakkaFig qakkaFig, CassandraClient cassandraClient ) {
        this.maxInMemoryIncrement = qakkaFig.getMaxInMemoryShardCounter();
        this.cassandraClient = cassandraClient;
    }


    @Override
    public void incrementCounter(String queueName, Shard.Type type, long shardId, long increment ) {

        String key = queueName + type + shardId;
        synchronized ( inMemoryCounters ) {

            if ( inMemoryCounters.get( key ) == null ) {

                Long value = retrieveCounterFromStorage( queueName, type, shardId );

                if ( value == null ) {
                    incrementCounterInStorage( queueName, type, shardId, 0L );
                    inMemoryCounters.put( key, new InMemoryCount( 0L ));
                } else {
                    inMemoryCounters.put( key, new InMemoryCount( value ));
                }
                inMemoryCounters.get( key ).getIncrement().addAndGet( increment );
                return;
            }
        }

        InMemoryCount inMemoryCount = inMemoryCounters.get( key );

        synchronized ( inMemoryCount ) {
            long totalIncrement = inMemoryCount.getIncrement().addAndGet( increment );

            if (totalIncrement > maxInMemoryIncrement) {
                incrementCounterInStorage( queueName, type, shardId, totalIncrement );
                inMemoryCount.setBaseCount( retrieveCounterFromStorage( queueName, type, shardId ) );
                inMemoryCount.getIncrement().set( 0L );
            }
        }

    }


    @Override
    public long getCounterValue( String queueName, Shard.Type type, long shardId ) {

        String key = queueName + type + shardId;

        synchronized ( inMemoryCounters ) {

            if ( inMemoryCounters.get( key ) == null ) {

                Long value = retrieveCounterFromStorage( queueName, type, shardId );

                if ( value == null ) {
                    throw new NotFoundException(
                            MessageFormat.format( "No counter found for queue {0} type {1} shardId {2}",
                                    queueName, type, shardId ));
                } else {
                    inMemoryCounters.put( key, new InMemoryCount( value ));
                }
            }
        }

        return inMemoryCounters.get( key ).value();
    }

    void incrementCounterInStorage( String queueName, Shard.Type type, long shardId, long increment ) {

        Statement update = QueryBuilder.update( TABLE_SHARD_COUNTERS )
                .where( QueryBuilder.eq(   COLUMN_QUEUE_NAME, queueName ) )
                .and(   QueryBuilder.eq(   COLUMN_SHARD_TYPE, type.toString() ) )
                .and(   QueryBuilder.eq(   COLUMN_SHARD_ID, shardId ) )
                .with(  QueryBuilder.incr( COLUMN_COUNTER_VALUE, increment ) );
        cassandraClient.getSession().execute( update );
    }


    Long retrieveCounterFromStorage( String queueName, Shard.Type type, long shardId ) {

        Statement query = QueryBuilder.select().from( TABLE_SHARD_COUNTERS )
                .where( QueryBuilder.eq( COLUMN_QUEUE_NAME, queueName ) )
                .and( QueryBuilder.eq( COLUMN_SHARD_TYPE, type.toString()) )
                .and( QueryBuilder.eq( COLUMN_SHARD_ID, shardId ) );

        ResultSet resultSet = cassandraClient.getSession().execute( query );
        List<Row> all = resultSet.all();

        if ( all.size() > 1 ) {
            throw new QakkaRuntimeException(
                    "Multiple rows for counter " + queueName + " type " + type + " shardId " + shardId );
        }
        if ( all.isEmpty() ) {
            return null;
        }
        return all.get(0).getLong( COLUMN_COUNTER_VALUE );
    }


    @Override
    public Collection<MultiTenantColumnFamilyDefinition> getColumnFamilies() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<TableDefinition> getTables() {
        return Collections.singletonList( new TableDefinitionStringImpl( TABLE_SHARD_COUNTERS, CQL ) );
    }

}
