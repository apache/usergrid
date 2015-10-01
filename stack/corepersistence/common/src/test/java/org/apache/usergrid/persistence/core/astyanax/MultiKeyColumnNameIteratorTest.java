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
import java.util.Comparator;
import java.util.HashMap;

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
import com.netflix.astyanax.query.RowQuery;
import com.netflix.astyanax.serializers.LongSerializer;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.util.RangeBuilder;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;


@RunWith( ITRunner.class )
@UseModules( TestCommonModule.class )
public class MultiKeyColumnNameIteratorTest {

    @Inject
    public CassandraFig cassandraFig;

    protected static Keyspace keyspace;

    protected ApplicationScope scope;

    protected static ColumnFamily<String, Long> COLUMN_FAMILY =
            new ColumnFamily<>( "MultiKeyLongTests", StringSerializer.get(), LongSerializer.get() );

    protected static final boolean TRUE = true;


    @Before
    public  void setup() throws ConnectionException {


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
    public void multiIterator() {

        final String rowKey1 = UUIDGenerator.newTimeUUID().toString();

        final String rowKey2 = UUIDGenerator.newTimeUUID().toString();

        final String rowKey3 = UUIDGenerator.newTimeUUID().toString();


        final long maxValue = 10000;


        /**
         * Write to both rows in parallel
         */
        Observable.from( new String[] { rowKey1, rowKey2, rowKey3 } )
            //perform a flatmap
                  .flatMap( stringObservable -> Observable.just( stringObservable ).doOnNext( key -> {
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

            ColumnNameIterator<Long, Long> row1Iterator = createIterator( rowKey1, false );
            ColumnNameIterator<Long, Long> row2Iterator = createIterator( rowKey2, false );
            ColumnNameIterator<Long, Long> row3Iterator = createIterator( rowKey3, false );

            final Comparator<Long> ascendingComparator = new Comparator<Long>() {

                @Override
                public int compare( final Long o1, final Long o2 ) {
                    return Long.compare( o1, o2 );
                }
            };

            /**
             * Again, arbitrary buffer size to attempt we buffer at some point
             */
            final MultiKeyColumnNameIterator<Long, Long> ascendingItr =
                new MultiKeyColumnNameIterator<>( Arrays.asList( row1Iterator, row2Iterator, row3Iterator ),
                    ascendingComparator, 900 );


            //ensure we have to make several trips, purposefully set to a nonsensical value to ensure we make all the
            // trips required


            for ( long i = 0; i < maxValue; i++ ) {
                assertEquals( i, ascendingItr.next().longValue() );
            }

            //now test it in reverse

            ColumnNameIterator<Long, Long> row1IteratorDesc = createIterator( rowKey1, true );
            ColumnNameIterator<Long, Long> row2IteratorDesc = createIterator( rowKey2, true );
            ColumnNameIterator<Long, Long> row3IteratorDesc = createIterator( rowKey3, true );

            final Comparator<Long> descendingComparator = new Comparator<Long>() {

                @Override
                public int compare( final Long o1, final Long o2 ) {
                    return ascendingComparator.compare( o1, o2 ) * -1;
                }
            };

            /**
             * Again, arbitrary buffer size to attempt we buffer at some point
             */
            final MultiKeyColumnNameIterator<Long, Long> descendingItr =
                new MultiKeyColumnNameIterator<>( Arrays.asList( row1IteratorDesc, row2IteratorDesc, row3IteratorDesc ),
                    descendingComparator, 900 );


            for ( long i = maxValue - 1; i > -1; i-- ) {
                assertEquals( i, descendingItr.next().longValue() );
            }
        }


    @Test
       public void singleIterator() {

           final String rowKey1 = UUIDGenerator.newTimeUUID().toString();



           final long maxValue = 10000;

           /**
            * Write to both rows in parallel
            */
           Observable.just( rowKey1  ).flatMap( rowKey -> Observable.just( rowKey ).doOnNext( key -> {
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
               }} ).subscribeOn( Schedulers.io() ) ).toBlocking().last();


           //create 3 iterators

           ColumnNameIterator<Long, Long> row1Iterator = createIterator( rowKey1, false );

           final Comparator<Long> ascendingComparator = new Comparator<Long>() {

               @Override
               public int compare( final Long o1, final Long o2 ) {
                   return Long.compare( o1, o2 );
               }
           };

           /**
            * Again, arbitrary buffer size to attempt we buffer at some point
            */
           final MultiKeyColumnNameIterator<Long, Long> ascendingItr =
                   new MultiKeyColumnNameIterator<>( Arrays.asList( row1Iterator ),
                           ascendingComparator, 900 );


           //ensure we have to make several trips, purposefully set to a nonsensical value to ensure we make all the
           // trips required


           for ( long i = 0; i < maxValue; i++ ) {
               //we have 3 iterators, so we should get each value 3 times in the aggregation
               assertEquals( i, ascendingItr.next().longValue() );
           }

           //now test it in reverse

           ColumnNameIterator<Long, Long> row1IteratorDesc = createIterator( rowKey1, true );

           final Comparator<Long> descendingComparator = new Comparator<Long>() {

               @Override
               public int compare( final Long o1, final Long o2 ) {
                   return ascendingComparator.compare( o1, o2 ) * -1;
               }
           };

           /**
            * Again, arbitrary buffer size to attempt we buffer at some point
            */
           final MultiKeyColumnNameIterator<Long, Long> descendingItr =
                   new MultiKeyColumnNameIterator<>( Arrays.asList( row1IteratorDesc),
                           descendingComparator, 900 );


           for ( long i = maxValue - 1; i > -1; i-- ) {
               assertEquals( i, descendingItr.next().longValue() );
           }
       }


    private static ColumnNameIterator<Long, Long> createIterator( final String rowKey, final boolean reversed ) {


        final ColumnParser<Long, Long> longParser = new ColumnParser<Long, Long>() {
            @Override
            public Long parseColumn( final Column<Long> column ) {
                return column.getName();
            }
        };

        final RangeBuilder forwardRange = new RangeBuilder().setLimit( 720 ).setReversed( reversed );


        final RowQuery<String, Long> forwardQuery =
                keyspace.prepareQuery( COLUMN_FAMILY ).getKey( rowKey ).withColumnRange( forwardRange.build() );


        ColumnNameIterator<Long, Long> itr = new ColumnNameIterator<>( forwardQuery, longParser, false );

        return itr;
    }
}
