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


import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.CoreApplication;
import org.apache.usergrid.CoreITSetup;
import org.apache.usergrid.CoreITSetupImpl;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.index.query.Query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 *
 */
public class IntersectionUnionPagingIT {

    private static final Logger LOG = LoggerFactory.getLogger( IntersectionUnionPagingIT.class );

    private static final String unionScan =
            "select * where (field1Or > '00000000' OR field2Or > '00000000') AND fieldDate = '0000-00-00'";
    private static final String scanUnion =
            "select * where fieldDate = '0000-00-00' AND (field1Or > '00000000' OR field2Or > '00000000') ";

    private static final int PAGE_SIZE = 300;



    @ClassRule
    public static CoreITSetup setup = new CoreITSetupImpl( );

    @Rule
    public CoreApplication app = new CoreApplication( setup );


    @Test
    public void testUnionPagingCollection() throws Exception {


        final CollectionIoHelper collectionIoHelper = new CollectionIoHelper( app );

        Set<String> created = performSetup( collectionIoHelper );


        testUnionPaging( collectionIoHelper, unionScan, created );
        testUnionPaging( collectionIoHelper, scanUnion, created );
    }


    @Test
    public void testUnionPagingConnection() throws Exception {

        final ConnectionHelper connectionHelper = new ConnectionHelper( app );

        Set<String> created = performSetup( connectionHelper );


        testUnionPaging( connectionHelper, unionScan, created );
        testUnionPaging( connectionHelper, scanUnion, created );
    }


    private Set<String> performSetup( final IoHelper io ) throws Exception {
        io.doSetup();

        int size =10;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        final String zeros = String.format( "%08d", 0 );

        Set<String> names = new HashSet<String>( size );

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entity = new HashMap<String, Object>();
            final String name = String.valueOf( i );
            entity.put( "name", name );
            entity.put( "fieldDate", "0000-00-00" );

            String field1 = String.format( "%08d", i + 1 );
            String field2;

            //use a value slightly smaller than page size, since we want to simulate
            //the cursor issues with union queries

            if ( i < size - 10 ) {
                field2 = zeros;
            }
            else {
                field2 = String.format( "%08d", i + 1 );
            }

            names.add( name );

            entity.put( "field1Or", field1 );
            entity.put( "field2Or", field2 );

            Entity saved =  io.writeEntity( entity );

            LOG.debug("Writing entity with id '{}'", saved.getUuid());

        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        return Collections.unmodifiableSet( names );
    }


    private void testUnionPaging( final IoHelper io, final String queryString,
            final Set<String> expectedResults ) throws Exception {

        Set<String> newSets = new HashSet<String>( expectedResults );

        //our field1Or has a result size < our page size, so it shouldn't blow up when the
        // cursor is getting created the leaf iterator should insert it's own "no value left" i
        // not the cursor
        Query query = Query.fromQL( queryString );
        query.setLimit( PAGE_SIZE );

        Results results;

        long start = System.currentTimeMillis();

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                final String name = results.getEntities().get( i ).getName();

                assertTrue( "Value should not be returned twice", newSets.contains( name ) );

                newSets.remove( name );
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        long stop = System.currentTimeMillis();

        LOG.info( "Query took {} ms to return {} entities", stop - start, expectedResults.size() );

        assertEquals( "All names returned", 0, newSets.size() );
    }
}
