/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.usergrid.persistence.core.astyanax;


import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.guice.TestCommonModule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.serializers.LongSerializer;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.util.RangeBuilder;

import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


@RunWith( ITRunner.class )
@UseModules( TestCommonModule.class )
public class MultiRowColumnIteratorTest {

    @Inject
    public CassandraFig cassandraFig;

    protected static Keyspace keyspace;

    protected ApplicationScope scope;

    protected static ColumnFamily<String, Long> COLUMN_FAMILY =
            new ColumnFamily<>( "MultiRowLongTests", StringSerializer.get(), LongSerializer.get() );

    protected static final boolean TRUE = true;


    @Before
    public void setup() throws ConnectionException {


        final CassandraConfig cassandraConfig = new CassandraConfig() {
            @Override
            public ConsistencyLevel getReadCL() {
                return ConsistencyLevel.CL_LOCAL_ONE;
            }

            @Override
            public ConsistencyLevel getConsistentReadCL() {
                return ConsistencyLevel.CL_LOCAL_QUORUM;
            }


            @Override
            public ConsistencyLevel getWriteCL() {
                return ConsistencyLevel.CL_QUORUM;
            }


            @Override
            public int[] getShardSettings() {
                return new int[]{20};
            }
        };


        AstyanaxKeyspaceProvider astyanaxKeyspaceProvider =
                new AstyanaxKeyspaceProvider( cassandraFig, cassandraConfig );

        keyspace = astyanaxKeyspaceProvider.get();

        TestUtils.createKeyspace( keyspace );

        TestUtils.createColumnFamiliy( keyspace, COLUMN_FAMILY, new HashMap<String, Object>() );
    }


    @Test
    public void multiIterator() throws InterruptedException {

        final String rowKey1 = UUIDGenerator.newTimeUUID().toString();

        final String rowKey2 = UUIDGenerator.newTimeUUID().toString();

        final String rowKey3 = UUIDGenerator.newTimeUUID().toString();


        final long maxValue = 10000;

        final CountDownLatch latch = new CountDownLatch( 3 );


        writeData( latch, rowKey1, maxValue, 1 );
        writeData( latch, rowKey2, maxValue, 2 );
        writeData( latch, rowKey3, maxValue, 10 );


        latch.await();


        //create 3 iterators


        final ColumnParser<Long, Long> longParser = new ColumnParser<Long, Long>() {
            @Override
            public Long parseColumn( final Column<Long> column ) {
                return column.getName();
            }
        };


        final ColumnSearch<Long> ascendingSearch = new ColumnSearch<Long>() {
            @Override
            public void buildRange( final RangeBuilder rangeBuilder, final Long value ) {
                rangeBuilder.setStart( value );
            }


            @Override
            public void buildRange( final RangeBuilder rangeBuilder ) {

            }


            @Override
            public boolean skipFirst( final Long first ) {
                return false;
            }
        };


        final Comparator<Long> ascendingComparator = new Comparator<Long>() {

            @Override
            public int compare( final Long o1, final Long o2 ) {
                return Long.compare( o1, o2 );
            }
        };


        final Collection<String> rowKeys = Arrays.asList( rowKey1, rowKey2, rowKey3 );

        MultiRowColumnIterator<String, Long, Long> ascendingItr =
                new MultiRowColumnIterator<>( keyspace, COLUMN_FAMILY, ConsistencyLevel.CL_QUORUM, longParser,
                        ascendingSearch, ascendingComparator, rowKeys, 852 );


        //ensure we have to make several trips, purposefully set to a nonsensical value to ensure we make all the
        // trips required


        for ( long i = 0; i < maxValue; i++ ) {
            assertEquals( i, ascendingItr.next().longValue() );
        }


        final ColumnSearch<Long> descendingSearch = new ColumnSearch<Long>() {
            @Override
            public void buildRange( final RangeBuilder rangeBuilder, final Long value ) {
                rangeBuilder.setStart( value );
                buildRange( rangeBuilder );
            }


            @Override
            public void buildRange( final RangeBuilder rangeBuilder ) {
                rangeBuilder.setReversed( true );
            }


            @Override
            public boolean skipFirst( final Long first ) {
                return false;
            }
        };


        final Comparator<Long> descendingComparator = new Comparator<Long>() {

            @Override
            public int compare( final Long o1, final Long o2 ) {
                return ascendingComparator.compare( o1, o2 ) * -1;
            }
        };


        MultiRowColumnIterator<String, Long, Long> descendingItr =
                new MultiRowColumnIterator<>( keyspace, COLUMN_FAMILY, ConsistencyLevel.CL_QUORUM, longParser,
                        descendingSearch, descendingComparator, rowKeys, 712 );

        for ( long i = maxValue - 1; i > -1; i-- ) {
            assertEquals( i, descendingItr.next().longValue() );
        }
    }


    @Test
    public void multiIteratorPageBoundary() throws InterruptedException {

        final String rowKey1 = UUIDGenerator.newTimeUUID().toString();

        final String rowKey2 = UUIDGenerator.newTimeUUID().toString();

        final String rowKey3 = UUIDGenerator.newTimeUUID().toString();


        final long maxValue = 200;

        final CountDownLatch latch = new CountDownLatch( 3 );


        //only write with 1 row key to simulate ending on a page break the last iteration
        writeData( latch, rowKey1, maxValue, 1 );
        writeData( latch, rowKey2, maxValue, 2 );
        writeData( latch, rowKey3, maxValue, 10 );


        latch.await();


        //create 3 iterators


        final ColumnParser<Long, Long> longParser = new ColumnParser<Long, Long>() {
            @Override
            public Long parseColumn( final Column<Long> column ) {
                return column.getName();
            }
        };


        final ColumnSearch<Long> ascendingSearch = new ColumnSearch<Long>() {
            @Override
            public void buildRange( final RangeBuilder rangeBuilder, final Long value ) {
                rangeBuilder.setStart( value );
            }


            @Override
            public void buildRange( final RangeBuilder rangeBuilder ) {

            }


            @Override
            public boolean skipFirst( final Long first ) {
                return false;
            }
        };


        final Comparator<Long> ascendingComparator = new Comparator<Long>() {

            @Override
            public int compare( final Long o1, final Long o2 ) {
                return Long.compare( o1, o2 );
            }
        };


        final Collection<String> rowKeys = Arrays.asList( rowKey1, rowKey2, rowKey3 );

        MultiRowColumnIterator<String, Long, Long> ascendingItr =
                new MultiRowColumnIterator<>( keyspace, COLUMN_FAMILY, ConsistencyLevel.CL_QUORUM, longParser,
                        ascendingSearch, ascendingComparator, rowKeys, ( int ) maxValue / 2 );


        //ensure we have to make several trips, purposefully set to a nonsensical value to ensure we make all the
        // trips required


        for ( long i = 0; i < maxValue; i++ ) {
            assertEquals( i, ascendingItr.next().longValue() );
        }

        //now advance one more time. There should be no values

        assertFalse( "Should not throw exception", ascendingItr.hasNext() );


        final ColumnSearch<Long> descendingSearch = new ColumnSearch<Long>() {
            @Override
            public void buildRange( final RangeBuilder rangeBuilder, final Long value ) {
                rangeBuilder.setStart( value );
                buildRange( rangeBuilder );
            }


            @Override
            public void buildRange( final RangeBuilder rangeBuilder ) {
                rangeBuilder.setReversed( true );
            }


            @Override
            public boolean skipFirst( final Long first ) {
                return false;
            }
        };


        final Comparator<Long> descendingComparator = new Comparator<Long>() {

            @Override
            public int compare( final Long o1, final Long o2 ) {
                return ascendingComparator.compare( o1, o2 ) * -1;
            }
        };


        MultiRowColumnIterator<String, Long, Long> descendingItr =
                new MultiRowColumnIterator<>( keyspace, COLUMN_FAMILY, ConsistencyLevel.CL_QUORUM, longParser,
                        descendingSearch, descendingComparator, rowKeys, 712 );

        for ( long i = maxValue - 1; i > -1; i-- ) {
            assertEquals( i, descendingItr.next().longValue() );
        }
        //now advance one more time. There should be no values

        assertFalse( "Should not throw exception", ascendingItr.hasNext() );
    }


    @Test
    public void singleIterator() {

        final String rowKey1 = UUIDGenerator.newTimeUUID().toString();


        final long maxValue = 10000;

        /**
         * Write to both rows in parallel
         */
        Observable.just( rowKey1 ).flatMap( rowKey -> Observable.just( rowKey ).doOnNext( key -> {
            final MutationBatch batch = keyspace.prepareMutationBatch();

            for ( long i = 0; i < maxValue; i++ ) {
                batch.withRow( COLUMN_FAMILY, key ).putColumn( i, TRUE );

                if ( i % 1000 == 0 ) {
                    try {
                        batch.execute();
                    }
                    catch ( ConnectionException e ) {
                        throw new RuntimeException( e );
                    }
                }
            }

            try {
                batch.execute();
            }
            catch ( ConnectionException e ) {
                throw new RuntimeException( e );
            }
        } ).subscribeOn( Schedulers.io() ) ).toBlocking().last();


        //create 3 iterators

        final ColumnParser<Long, Long> longParser = new ColumnParser<Long, Long>() {
            @Override
            public Long parseColumn( final Column<Long> column ) {
                return column.getName();
            }
        };


        final ColumnSearch<Long> ascendingSearch = new ColumnSearch<Long>() {
            @Override
            public void buildRange( final RangeBuilder rangeBuilder, final Long value ) {
                rangeBuilder.setStart( value );
            }


            @Override
            public void buildRange( final RangeBuilder rangeBuilder ) {

            }


            @Override
            public boolean skipFirst( final Long first ) {
                return false;
            }
        };


        final Comparator<Long> ascendingComparator = new Comparator<Long>() {

            @Override
            public int compare( final Long o1, final Long o2 ) {
                return Long.compare( o1, o2 );
            }
        };


        final Collection<String> rowKeys = Arrays.asList( rowKey1 );

        MultiRowColumnIterator<String, Long, Long> ascendingItr =
                new MultiRowColumnIterator<>( keyspace, COLUMN_FAMILY, ConsistencyLevel.CL_QUORUM, longParser,
                        ascendingSearch, ascendingComparator, rowKeys, 712 );


        //ensure we have to make several trips, purposefully set to a nonsensical value to ensure we make all the
        // trips required


        for ( long i = 0; i < maxValue; i++ ) {
            assertEquals( i, ascendingItr.next().longValue() );
        }


        final ColumnSearch<Long> descendingSearch = new ColumnSearch<Long>() {
            @Override
            public void buildRange( final RangeBuilder rangeBuilder, final Long value ) {
                rangeBuilder.setStart( value );
                buildRange( rangeBuilder );
            }


            @Override
            public void buildRange( final RangeBuilder rangeBuilder ) {
                rangeBuilder.setReversed( true );
            }


            @Override
            public boolean skipFirst( final Long first ) {
                return false;
            }
        };


        final Comparator<Long> descendingComparator = new Comparator<Long>() {

            @Override
            public int compare( final Long o1, final Long o2 ) {
                return ascendingComparator.compare( o1, o2 ) * -1;
            }
        };


        MultiRowColumnIterator<String, Long, Long> descendingItr =
                new MultiRowColumnIterator<>( keyspace, COLUMN_FAMILY, ConsistencyLevel.CL_QUORUM, longParser,
                        descendingSearch, descendingComparator, rowKeys, 712 );

        for ( long i = maxValue - 1; i > -1; i-- ) {
            assertEquals( i, descendingItr.next().longValue() );
        }
    }


    private void writeData( final CountDownLatch latch, final String rowKey, final long maxValue, final long mod ) {

        Observable.just( rowKey ).doOnNext( new Action1<String>() {
            @Override
            public void call( final String key ) {

                final MutationBatch batch = keyspace.prepareMutationBatch();

                for ( long i = 0; i < maxValue; i++ ) {

                    if ( i % mod == 0 ) {
                        batch.withRow( COLUMN_FAMILY, key ).putColumn( i, TRUE );
                    }

                    if ( i % 1000 == 0 ) {
                        try {
                            batch.execute();
                        }
                        catch ( ConnectionException e ) {
                            throw new RuntimeException( e );
                        }
                    }
                }

                try {
                    batch.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( e );
                }
            }
        } ).subscribe( new Observer<String>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }


            @Override
            public void onError( final Throwable e ) {

            }


            @Override
            public void onNext( final String s ) {

            }
        } );
    }
}
