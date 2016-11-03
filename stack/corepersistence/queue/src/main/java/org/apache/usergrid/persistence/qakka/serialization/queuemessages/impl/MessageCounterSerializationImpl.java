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
import org.apache.usergrid.persistence.core.CassandraConfig;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.apache.usergrid.persistence.core.datastax.impl.TableDefinitionStringImpl;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.exceptions.NotFoundException;
import org.apache.usergrid.persistence.qakka.exceptions.QakkaRuntimeException;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.MessageCounterSerialization;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.serialization.sharding.impl.ShardCounterSerializationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


@Singleton
public class MessageCounterSerializationImpl implements MessageCounterSerialization {
    private static final Logger logger = LoggerFactory.getLogger( MessageCounterSerializationImpl.class );

    private final CassandraClient cassandraClient;
    private final CassandraConfig cassandraConfig;
    private final long writeTimeout;

    final static String TABLE_MESSAGE_COUNTERS = "message_counters";
    final static String COLUMN_QUEUE_NAME      = "queue_name";
    final static String COLUMN_COUNTER_VALUE   = "counter_value";
    final static String COLUMN_MESSAGE_TYPE    = "message_type";

    // design note: counters based on DataStax example here:
    // https://docs.datastax.com/en/cql/3.1/cql/cql_using/use_counter_t.html

    static final String CQL =
        "CREATE TABLE IF NOT EXISTS message_counters ( " +
                "counter_value counter, " +
                "queue_name    varchar, " +
                "message_type  varchar, " +
                "PRIMARY KEY (queue_name, message_type) " +
        ");";


    /** number of changes since last save to database */
    final AtomicInteger numChanges = new AtomicInteger( 0 );

    final long maxChangesBeforeSave;

    class InMemoryCount {
        long baseCount;
        final AtomicLong totalInMemoryCount = new AtomicLong( 0L ); // for testing using only in-memory counter
        final AtomicLong increment = new AtomicLong( 0L );
        final AtomicLong decrement = new AtomicLong( 0L );
        long lastWritten = 0L;

        InMemoryCount( long baseCount ) {
            this.baseCount = baseCount;
        }
        public void increment( long inc ) {
            this.increment.addAndGet( inc );
            this.totalInMemoryCount.addAndGet( inc );
        }
        public void decrement( long dec ) {
            this.decrement.addAndGet( dec );
            this.totalInMemoryCount.addAndGet( -dec );
        }
        public long getIncrement() {
            return increment.get();
        }
        public long getDecrement() {
            return decrement.get();
        }
        public void clearDeltas() {
            this.increment.set( 0L );
            this.decrement.set( 0L );
        }
        boolean needsUpdate() {
            return System.currentTimeMillis() - lastWritten > writeTimeout;
        }
        void reset() {
            this.baseCount = 0;
            this.increment.set( 0L );
            this.decrement.set( 0L );
        }
        public long value() {
            // return totalInMemoryCount.get(); // for testing using just in-memory counter:
            return baseCount + increment.get() - decrement.get();
        }
        void setBaseCount( long baseCount ) {
            this.baseCount = baseCount;
            this.lastWritten = System.currentTimeMillis();
        }
    }

    private Map<String, InMemoryCount> inMemoryCounters = new ConcurrentHashMap<>(200);


    @Inject
    public MessageCounterSerializationImpl(
        CassandraConfig cassandraConfig, QakkaFig qakkaFig, CassandraClient cassandraClient ) {

        this.cassandraConfig = cassandraConfig;
        this.maxChangesBeforeSave = qakkaFig.getMessageCounterMaxInMemory();
        this.cassandraClient = cassandraClient;
        this.writeTimeout = qakkaFig.getShardCounterWriteTimeoutMillis();
    }


    private String buildKey( String queueName, DatabaseQueueMessage.Type type ) {
        return queueName + "_" + type;
    }


    @Override
    public void incrementCounter(String queueName, DatabaseQueueMessage.Type type, long increment ) {

        String key = buildKey( queueName, type );

        synchronized ( inMemoryCounters ) {

            if (inMemoryCounters.get( key ) == null) {

                Long value = retrieveCounterFromStorage( queueName, type );

                if (value == null) {
                    incrementCounterInStorage( queueName, type, 0L );
                    inMemoryCounters.put( key, new InMemoryCount( 0L ) );
                } else {
                    inMemoryCounters.put( key, new InMemoryCount( value ) );
                }
            }
        }

        InMemoryCount inMemoryCount = inMemoryCounters.get( key );

        synchronized ( inMemoryCount ) {
            inMemoryCount.increment( increment );
            saveIfNeeded( queueName, type );
        }

        if ( logger.isDebugEnabled() ) {
            long value = inMemoryCounters.get( key ).value();
            if (value <= 0) {
                logger.debug( "Queue {} type {} decremented count = {}", queueName, type, value );
            }
        }
    }


    @Override
    public void decrementCounter(String queueName, DatabaseQueueMessage.Type type, long decrement) {

        String key = buildKey( queueName, type );

        synchronized ( inMemoryCounters ) {

            if (inMemoryCounters.get( key ) == null) {

                Long value = retrieveCounterFromStorage( queueName, type );

                if (value == null) {
                    decrementCounterInStorage( queueName, type, 0L );
                    inMemoryCounters.put( key, new InMemoryCount( 0L ) );
                } else {
                    inMemoryCounters.put( key, new InMemoryCount( value ) );
                }
            }
        }

        InMemoryCount inMemoryCount = inMemoryCounters.get( key );

        synchronized ( inMemoryCount ) {
            inMemoryCount.decrement( decrement );
            saveIfNeeded( queueName, type );
        }

        if ( logger.isDebugEnabled() ) {
            long value = inMemoryCounters.get( key ).value();
            if (value <= 0) {
                logger.debug( "Queue {} type {} incremented count = {}", queueName, type, value );
            }
        }
    }


    @Override
    public long getCounterValue( String queueName, DatabaseQueueMessage.Type type ) {

        String key = buildKey( queueName, type );

        if ( inMemoryCounters.get( key ) == null ) {

            Long value = retrieveCounterFromStorage( queueName, type );

            if ( value == null ) {
                throw new NotFoundException(
                        MessageFormat.format( "No counter found for queue {0} type {1}", queueName, type ));
            } else {
                inMemoryCounters.put( key, new InMemoryCount( value ));
            }
        }

        InMemoryCount inMemoryCount = inMemoryCounters.get( key );

        synchronized ( inMemoryCount ) {

            if ( inMemoryCount.needsUpdate() ) {
                long totalIncrement = inMemoryCount.getIncrement();
                incrementCounterInStorage( queueName, type, totalIncrement );
                inMemoryCount.setBaseCount( retrieveCounterFromStorage( queueName, type ) );
                inMemoryCount.clearDeltas();
            }
        }

        return inMemoryCounters.get( key ).value();
    }


    @Override
    public void resetCounter(String queueName, DatabaseQueueMessage.Type type) {

        // this sucks: "You cannot index, delete, or re-add a counter column"
        // https://docs.datastax.com/en/cql/3.1/cql/cql_using/use_counter_t.html
        // so instead we decrement or increment the counter to zero

        // get value first, before resetting in memory counter
        long value = getCounterValue( queueName, type );

        String key = buildKey( queueName, type );
        InMemoryCount inMemoryCount = inMemoryCounters.get( key );
        if ( inMemoryCount != null ) {
            inMemoryCount.reset();
        }

        if ( value < 0 ) {

            Statement update = QueryBuilder.update( TABLE_MESSAGE_COUNTERS )
                .where( QueryBuilder.eq(   COLUMN_QUEUE_NAME, queueName ) )
                .and(   QueryBuilder.eq(   COLUMN_MESSAGE_TYPE, type.toString() ) )
                .with(  QueryBuilder.incr( COLUMN_COUNTER_VALUE, -1 * value ) ); // incr must be positive
            cassandraClient.getQueueMessageSession().execute( update );

        } else {

            Statement update = QueryBuilder.update( TABLE_MESSAGE_COUNTERS )
                .where( QueryBuilder.eq(   COLUMN_QUEUE_NAME, queueName ) )
                .and(   QueryBuilder.eq(   COLUMN_MESSAGE_TYPE, type.toString() ) )
                .with(  QueryBuilder.decr( COLUMN_COUNTER_VALUE, value ) );
            cassandraClient.getQueueMessageSession().execute( update );
        }

    }


    void incrementCounterInStorage( String queueName, DatabaseQueueMessage.Type type, long increment ) {

        Statement update = QueryBuilder.update( TABLE_MESSAGE_COUNTERS )
                .where( QueryBuilder.eq(   COLUMN_QUEUE_NAME, queueName ) )
                .and(   QueryBuilder.eq(   COLUMN_MESSAGE_TYPE, type.toString() ) )
                .with(  QueryBuilder.incr( COLUMN_COUNTER_VALUE, increment ) );
        cassandraClient.getQueueMessageSession().execute( update );
    }


    void decrementCounterInStorage( String queueName, DatabaseQueueMessage.Type type, long decrement ) {

        Statement update = QueryBuilder.update( TABLE_MESSAGE_COUNTERS )
            .where( QueryBuilder.eq(   COLUMN_QUEUE_NAME, queueName ) )
            .and(   QueryBuilder.eq(   COLUMN_MESSAGE_TYPE, type.toString() ) )
            .with(  QueryBuilder.decr( COLUMN_COUNTER_VALUE, decrement ) );
        cassandraClient.getQueueMessageSession().execute( update );
    }


    Long retrieveCounterFromStorage( String queueName, DatabaseQueueMessage.Type type ) {

        Statement query = QueryBuilder.select().from( TABLE_MESSAGE_COUNTERS )
                .where( QueryBuilder.eq( COLUMN_QUEUE_NAME, queueName ) )
                .and( QueryBuilder.eq( COLUMN_MESSAGE_TYPE, type.toString()) );

        ResultSet resultSet = cassandraClient.getQueueMessageSession().execute( query );
        List<Row> all = resultSet.all();

        if ( all.size() > 1 ) {
            throw new QakkaRuntimeException(
                    "Multiple rows for counter " + queueName + " type " + type );
        }
        if ( all.isEmpty() ) {
            return null;
        }
        return all.get(0).getLong( COLUMN_COUNTER_VALUE );
    }


    private void saveIfNeeded( String queueName, DatabaseQueueMessage.Type type ) {

        String key = buildKey( queueName, type );

        InMemoryCount inMemoryCount = inMemoryCounters.get( key );

        if ( numChanges.incrementAndGet() > maxChangesBeforeSave ) {

            long totalIncrement = inMemoryCount.getIncrement();
            incrementCounterInStorage( queueName, type, totalIncrement );

            long totalDecrement = inMemoryCount.getDecrement();
            decrementCounterInStorage( queueName, type, totalDecrement );

            long baseCount = retrieveCounterFromStorage( queueName, type );

            logger.debug("Writing queue counter {} type {} to storage count = {}", queueName, type, baseCount );

            inMemoryCount.setBaseCount( baseCount );
            inMemoryCount.clearDeltas();

            numChanges.set( 0 );
        }
    }


    @Override
    public Collection<MultiTenantColumnFamilyDefinition> getColumnFamilies() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<TableDefinition> getTables() {
        return Collections.singletonList( new TableDefinitionStringImpl(
            cassandraConfig.getApplicationLocalKeyspace(), TABLE_MESSAGE_COUNTERS, CQL ) );
    }

}
