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
package org.apache.usergrid.persistence.query;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.CoreApplication;
import org.apache.usergrid.CoreITSetup;
import org.apache.usergrid.CoreITSetupImpl;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Query;

import static org.junit.Assert.assertEquals;


/**
 *
 */
public class IntersectionTransitivePagingIT{

    private static final Logger LOG = LoggerFactory.getLogger( IntersectionTransitivePagingIT.class );

    private static final String union = "select * where city = 'San Francisco' AND postalCode = 94100";
    private static final String unionReverse = "select * where postalCode = 94100 AND city = 'San Francisco'";

    private static final int PAGE_SIZE = 300;




    @ClassRule
    public static CoreITSetup setup = new CoreITSetupImpl();

    @Rule
    public CoreApplication app = new CoreApplication( setup );

    @Test
    public void testUnionPagingCollection() throws Exception {


        final CollectionIoHelper collectionIoHelper = new CollectionIoHelper( app );

        List<UUID> returned = performSetup( collectionIoHelper );


        testUnionPaging( collectionIoHelper, union, returned );
        testUnionPaging( collectionIoHelper, unionReverse, returned );
    }


    @Test
    public void testUnionPagingConnection() throws Exception {

        final ConnectionHelper connectionHelper = new ConnectionHelper( app );

        List<UUID> returned = performSetup( connectionHelper );


        testUnionPaging( connectionHelper, union, returned );
        testUnionPaging( connectionHelper, unionReverse, returned );
    }


    private List<UUID> performSetup( final IoHelper io ) throws Exception {
        io.doSetup();


        int writeSize = 10;

        List<UUID> expected = new ArrayList<UUID>(writeSize);

        Map<String, Object> entity = new HashMap<String, Object>();

        entity.put( "city", "San Francisco" );





        /**
         * Write a non matching entity
         */
        Map<String, Object> otherEntity = new HashMap<String, Object>();
        otherEntity.put( "city", "Denver" );
        otherEntity.put( "postcalCode", 80211 );

        for(int i = 0; i < writeSize; i ++){


            if( i%2 == 0 ){

                int mod = i%4;

                entity.put( "postalCode", 94100+mod  );

                UUID returned1 = io.writeEntity( entity ).getUuid();

                if(mod == 0){
                    expected.add( returned1 );
                }
            }

            else{
                io.writeEntity( otherEntity );
            }


        }
        this.app.refreshIndex();

        Thread.sleep(1000);
        return expected;
    }


    private void testUnionPaging( final IoHelper io, final String queryString, final List<UUID> expectedResults )
            throws Exception {


        //our field1Or has a result size < our page size, so it shouldn't blow up when the cursor is getting created
        //the leaf iterator should insert it's own "no value left" into the cursor
        Query query = Query.fromQL( queryString );
        query.setLimit( PAGE_SIZE );

        Results results;

        long start = System.currentTimeMillis();

        int currentExpectedIndex = 0;

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++, currentExpectedIndex++ ) {
                final UUID returnedUUID = results.getEntities().get( i ).getUuid();

                assertEquals( "Value should not be returned twice", expectedResults.get( expectedResults.size() - 1 - currentExpectedIndex ),
                        returnedUUID );
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        long stop = System.currentTimeMillis();

        LOG.info( "Query took {} ms to return {} entities", stop - start, expectedResults.size() );

        assertEquals( "All names returned", expectedResults.size(), currentExpectedIndex );
    }
}
