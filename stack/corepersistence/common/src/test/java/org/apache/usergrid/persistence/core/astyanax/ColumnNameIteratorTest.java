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

import static org.junit.Assert.assertEquals;



@RunWith( ITRunner.class )
@UseModules( TestCommonModule.class )
public class ColumnNameIteratorTest {


    @Inject
    public CassandraFig cassandraFig;

    protected static Keyspace keyspace;

    protected ApplicationScope scope;

    protected static ColumnFamily<String, Long> COLUMN_FAMILY =
            new ColumnFamily<>( "LongTests", StringSerializer.get(), LongSerializer.get() );

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
    public void testSingleIterator() {

        String rowKey1 = UUIDGenerator.newTimeUUID().toString();


        final long maxValue = 10000;


        /**
         * Write to both rows in parallel
         */


        final MutationBatch batch = keyspace.prepareMutationBatch();

        for ( long i = 0; i < maxValue; i++ ) {
            batch.withRow( COLUMN_FAMILY, rowKey1 ).putColumn( i, TRUE );

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


        //now read from them, we should get an iterator that repeats from 0 to 9999 2 x for every entry

        final ColumnParser<Long, Long> longParser = new ColumnParser<Long, Long>() {
            @Override
            public Long parseColumn( final Column<Long> column ) {
                return column.getName();
            }
        };


        //ensure we have to make several trips, purposefully set to a nonsensical value to ensure we make all the
        // trips required
        final RangeBuilder forwardRange = new RangeBuilder().setLimit( 720 );


        final RowQuery<String, Long> forwardQuery =
                keyspace.prepareQuery( COLUMN_FAMILY ).getKey( rowKey1 ).withColumnRange( forwardRange.build() );


        ColumnNameIterator<Long, Long> itr = new ColumnNameIterator<>( forwardQuery, longParser, false );

        for ( long i = 0; i < maxValue; i++ ) {
            assertEquals( i, itr.next().longValue() );
        }

        //now test it in reverse


        final RangeBuilder reverseRange = new RangeBuilder().setLimit( 720 ).setReversed( true );


        final RowQuery<String, Long> reverseQuery =
                keyspace.prepareQuery( COLUMN_FAMILY ).getKey( rowKey1 ).withColumnRange( reverseRange.build() );


        ColumnNameIterator<Long, Long> reverseItr = new ColumnNameIterator<>( reverseQuery, longParser, false );

        for ( long i = maxValue - 1; i > -1; i-- ) {
            assertEquals( i, reverseItr.next().longValue() );
        }
    }


    //    /**
    //             * Write to both rows in parallel
    //             */
    //            Observable.from( new String[]{rowKey1, rowKey2} ).parallel( new Func1<Observable<String>,
    // Observable<String>>() {
    //                @Override
    //                public Observable<String> call( final Observable<String> stringObservable ) {
    //                   return stringObservable.doOnNext( new Action1<String>() {
    //                       @Override
    //                       public void call( final String key ) {
    //
    //                           final MutationBatch batch = keyspace.prepareMutationBatch();
    //
    //                           for(long i = 0; i < maxValue; i ++){
    //                               batch.withRow( COLUMN_FAMILY, key).putColumn( i, TRUE );
    //
    //                               if(i % 1000 == 0){
    //                                   try {
    //                                       batch.execute();
    //                                   }
    //                                   catch ( ConnectionException e ) {
    //                                       throw new RuntimeException(e);
    //                                   }
    //                               }
    //
    //                           }
    //
    //                       }
    //                   } );
    //                }
    //            } ).toBlocking().last();
}
