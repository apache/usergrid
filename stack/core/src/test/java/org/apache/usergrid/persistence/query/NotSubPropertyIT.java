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
 * Tests sub entites in full results
 */
public class NotSubPropertyIT {


    private static final Logger LOG = LoggerFactory.getLogger( IntersectionUnionPagingIT.class );

    private static final String notQuery = "select * where NOT subArray.usageType = 'true' order by created asc";

    private static final int PAGE_SIZE = 300;


    @ClassRule
    public static CoreITSetup setup = new CoreITSetupImpl(  );

    @Rule
    public CoreApplication app = new CoreApplication( setup );


    @Test
    public void testNotPagingCollection() throws Exception {


        final CollectionIoHelper collectionIoHelper = new CollectionIoHelper( app );

        List<UUID> expected = performSetup( collectionIoHelper );


        testSubPropertySearching( collectionIoHelper, notQuery, expected );
    }


    @Test
    public void testNotPagingConnection() throws Exception {

        final ConnectionHelper connectionHelper = new ConnectionHelper( app );

        List<UUID> expected = performSetup( connectionHelper );


        testSubPropertySearching( connectionHelper, notQuery, expected );
    }


    /**
     * Perform the writes
     */
    private List<UUID> performSetup( final IoHelper io ) throws Exception {
        io.doSetup();

        int size = 200;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );


        List<UUID> expected = new ArrayList<UUID>( size );

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entity = new HashMap<String, Object>();


            final boolean usageTypeBool = i % 2 == 0;
            final String usageType = String.valueOf( usageTypeBool );


            List<Map<String, Object>> subArray = new ArrayList<Map<String, Object>>();

            for ( int j = 0; j < 2; j++ ) {

                Map<String, Object> subFields = new HashMap<String, Object>();
                subFields.put( "startDate", 10000 );
                subFields.put( "endDate", 20000 );
                subFields.put( "usageType", usageType );

                subArray.add( subFields );
            }


            entity.put( "subArray", subArray );

            UUID entityId = io.writeEntity( entity ).getUuid();

            if ( !usageTypeBool ) {
                expected.add( entityId );
            }
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        app.refreshIndex();

        return expected;
    }


    private void testSubPropertySearching( final IoHelper io, final String queryString,
                                           final List<UUID> expectedResults ) throws Exception {


        //our field1Or has a result size < our page size, so it shouldn't blow up when the cursor is getting created
        //the leaf iterator should insert it's own "no value left" into the cursor
        Query query = Query.fromQL( queryString );
        query.setLimit( PAGE_SIZE );

        Results results;

        long start = System.currentTimeMillis();
        int expectedIndex = 0;

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );


            for ( int i = 0; i < results.size(); i++, expectedIndex++ ) {
                final UUID returned = results.getEntities().get( i ).getUuid();
                final UUID expected = expectedResults.get( expectedIndex );

                assertEquals( "Not returned as excpected", expected, returned );
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        long stop = System.currentTimeMillis();

        LOG.info( "Query took {} ms to return {} entities", stop - start, expectedResults.size() );

        assertEquals( "All names returned", expectedResults.size(), expectedIndex );
    }
}
