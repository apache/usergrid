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
package org.apache.usergrid.persistence.graph.serialization.impl.shard.count;


import java.beans.PropertyChangeListener;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.safehaus.guicyfig.Bypass;
import org.safehaus.guicyfig.OptionState;
import org.safehaus.guicyfig.Overrides;

import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardApproximation;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.WriteAheadLog;
import com.netflix.astyanax.connectionpool.Host;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.retry.RetryPolicy;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class NodeShardApproximationTest {


    private GraphFig graphFig;

    private NodeShardCounterSerialization nodeShardCounterSerialization;
    private TimeService timeService;

    protected ApplicationScope scope;


    @Before
    public void setup() {
        scope = mock( ApplicationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getApplication() ).thenReturn( orgId );

        graphFig = mock( GraphFig.class );

        when( graphFig.getShardCacheSize() ).thenReturn( 10000l );
        when( graphFig.getShardSize() ).thenReturn( 250000l );

        nodeShardCounterSerialization = mock( NodeShardCounterSerialization.class );

        when(nodeShardCounterSerialization.flush( any(Counter.class) )).thenReturn( mock( MutationBatch.class) );



        timeService = mock( TimeService.class );

        when(timeService.getCurrentTime()).thenReturn( System.currentTimeMillis() );
    }


    @Test
    public void testSingleShard() {


        when(graphFig.getCounterFlushCount()).thenReturn( 100000l );

        NodeShardApproximation approximation =
                new NodeShardApproximationImpl( graphFig, nodeShardCounterSerialization, timeService );


        final Id id = createId( "test" );
        final long shardId = 0l;
        final String type = "type";
        final String type2 = "subType";

        long count = approximation.getCount( scope, id, shardId, type, type2 );

        assertEquals( 0, count );
    }


    @Test
    public void testSingleShardMultipleThreads() throws ExecutionException, InterruptedException {




        NodeShardCounterSerialization serialization = new TestNodeShardCounterSerialization();

        final NodeShardApproximation approximation =
                new NodeShardApproximationImpl( new TestGraphFig(), serialization, new TestTimeService() );


        final int increments = 1000000;
        final int workers = Runtime.getRuntime().availableProcessors() * 2;

        final Id id = createId( "test" );
        final String type = "type";
        final String type2 = "subType";
        final long shardId = 10000;


        ExecutorService executor = Executors.newFixedThreadPool( workers );

        List<Future<Long>> futures = new ArrayList<>( workers );

        for ( int i = 0; i < workers; i++ ) {

            final Future<Long> future = executor.submit( new Callable<Long>() {
                @Override
                public Long call() throws Exception {

                    for ( int i = 0; i < increments; i++ ) {
                        approximation.increment( scope, id, shardId, 1, type, type2 );
                    }

                    return 0l;
                }
            } );

            futures.add( future );
        }



        for ( Future<Long> future : futures ) {
           future.get();
        }


        //get our count.  It should be accurate b/c we only have 1 instance

        final long returnedCount = approximation.getCount( scope, id, shardId, type, type2);
        final long expected = workers * increments;


        assertEquals(expected, returnedCount);





    }



    @Test
    public void testMultipleShardMultipleThreads() throws ExecutionException, InterruptedException {




        NodeShardCounterSerialization serialization = new TestNodeShardCounterSerialization();

        final NodeShardApproximation approximation =
                new NodeShardApproximationImpl( new TestGraphFig(), serialization, new TestTimeService() );


        final int increments = 1000000;
        final int workers = Runtime.getRuntime().availableProcessors() * 2;

        final Id id = createId( "test" );
        final String type = "type";
        final String type2 = "subType";

        final AtomicLong shardIdCounter = new AtomicLong(  );



        ExecutorService executor = Executors.newFixedThreadPool( workers );

        List<Future<Long>> futures = new ArrayList<>( workers );

        for ( int i = 0; i < workers; i++ ) {

            final Future<Long> future = executor.submit( new Callable<Long>() {
                @Override
                public Long call() throws Exception {

                    final long threadShardId = shardIdCounter.incrementAndGet();

                    for ( int i = 0; i < increments; i++ ) {
                        approximation.increment( scope, id, threadShardId, 1, type, type2 );
                    }

                    return threadShardId;
                }
            } );

            futures.add( future );
        }



        for ( Future<Long> future : futures ) {
           final long shardId = future.get();

            final long returnedCount = approximation.getCount( scope, id, shardId, type, type2);

            assertEquals(increments, returnedCount);
        }




    }


    /**
     * These are created b/c we can't use Mockito.  It OOM's with keeping track of all the mock invocations
     */

    private static class TestNodeShardCounterSerialization implements NodeShardCounterSerialization{

        private Counter copy = new Counter();

        @Override
        public MutationBatch flush( final Counter counter ) {
            copy.merge( counter );
            return new TestMutationBatch();
        }


        @Override
        public long getCount( final ShardKey key ) {
            return copy.get( key );
        }


        @Override
        public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }



    /**
     * Simple test mutation to no-op during tests
     */
    private
    static class TestMutationBatch implements MutationBatch {

        @Override
        public <K, C> ColumnListMutation<C> withRow( final ColumnFamily<K, C> columnFamily, final K rowKey ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public <K> void deleteRow( final Iterable<? extends ColumnFamily<K, ?>> columnFamilies, final K rowKey ) {
            //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public void discardMutations() {
            //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public void mergeShallow( final MutationBatch other ) {
            //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public boolean isEmpty() {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public int getRowCount() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public Map<ByteBuffer, Set<String>> getRowKeys() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public MutationBatch pinToHost( final Host host ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public MutationBatch setConsistencyLevel( final ConsistencyLevel consistencyLevel ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public MutationBatch withConsistencyLevel( final ConsistencyLevel consistencyLevel ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public MutationBatch withRetryPolicy( final RetryPolicy retry ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public MutationBatch usingWriteAheadLog( final WriteAheadLog manager ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public MutationBatch lockCurrentTimestamp() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public MutationBatch setTimeout( final long timeout ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public MutationBatch setTimestamp( final long timestamp ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public MutationBatch withTimestamp( final long timestamp ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public MutationBatch withAtomicBatch( final boolean condition ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public ByteBuffer serialize() throws Exception {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public void deserialize( final ByteBuffer data ) throws Exception {
            //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public OperationResult<Void> execute() throws ConnectionException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public ListenableFuture<OperationResult<Void>> executeAsync() throws ConnectionException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }



    private static class TestGraphFig implements GraphFig{

        @Override
        public int getScanPageSize() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public int getWriteTimeout() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public int getReadTimeout() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public int getRepairConcurrentSize() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public long getShardSize() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public long getShardCacheTimeout() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public long getShardCacheSize() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public long getCounterFlushCount() {
            return 100000l;
        }


        @Override
        public long getCounterFlushInterval() {
            return 30000l;
        }


        @Override
        public void addPropertyChangeListener( final PropertyChangeListener propertyChangeListener ) {
            //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public void removePropertyChangeListener( final PropertyChangeListener propertyChangeListener ) {
            //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public OptionState[] getOptions() {
            return new OptionState[0];  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public OptionState getOption( final String s ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public String getKeyByMethod( final String s ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public Object getValueByMethod( final String s ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public Properties filterOptions( final Properties properties ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public Map<String, Object> filterOptions( final Map<String, Object> stringObjectMap ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public void override( final String s, final String s2 ) {
            //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public boolean setOverrides( final Overrides overrides ) {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public Overrides getOverrides() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public void bypass( final String s, final String s2 ) {
            //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public boolean setBypass( final Bypass bypass ) {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public Bypass getBypass() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public Class getFigInterface() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public boolean isSingleton() {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    private static class TestTimeService implements TimeService{

        @Override
        public long getCurrentTime() {
            return System.currentTimeMillis();
        }
    }
}
