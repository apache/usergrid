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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.usergrid.persistence.index.query.Query;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.cassandra.QueryProcessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 *
 */
public class OrderByDiscardPagesPagingIT extends IteratingQueryIT {

    private static final Logger LOG = LoggerFactory.getLogger( OrderByDiscardPagesPagingIT.class );

    private static final String search = "select * where  field1 = true AND field2 = true order by ordinal";

    private static final int PAGE_SIZE = 300;


    @Test
    public void testUnionPagingCollection() throws Exception {


        final CollectionIoHelper collectionIoHelper = new CollectionIoHelper( app );

        Set<UUID> created = performSetup( collectionIoHelper );


        testUnionPaging( collectionIoHelper, search, created );
    }


    @Test
    public void testUnionPagingConnection() throws Exception {

        final ConnectionHelper connectionHelper = new ConnectionHelper( app );

        Set<UUID> created = performSetup( connectionHelper );


        testUnionPaging( connectionHelper, search, created );
    }


    private Set<UUID> performSetup( final IoHelper io ) throws Exception {
        io.doSetup();

        int size = ( int ) ( QueryProcessor.PAGE_SIZE * 2.5 );

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        Set<UUID> entites = new HashSet<UUID>( size );

        for ( int i = 0; i < size; i++ ) {

            Map<String, Object> entity = new HashMap<String, Object>();

            entity.put( "ordinal", i );

            int segment = i / PAGE_SIZE;

            boolean shouldBeReturned = segment % 2 != 0;

            if ( shouldBeReturned ) {

                entity.put( "field1", true );
                entity.put( "field2", true );
            }
            else {
                entity.put( "field1", false );
                entity.put( "field2", false );
            }


            Entity saved = io.writeEntity( entity );

            LOG.info( "Writing entity with id '{}'", saved.getUuid() );

            if ( shouldBeReturned ) {
                entites.add( saved.getUuid() );
            }
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        return entites ;
    }


    private void testUnionPaging( final IoHelper io, final String queryString, final Set<UUID> expectedResults )
            throws Exception {

        //our field1Or has a result size < our page size, so it shouldn't blow up when the cursor is getting created
        //the leaf iterator should insert it's own "no value left" into the cursor
        Query query = Query.fromQL( queryString );
        query.setLimit( PAGE_SIZE );

        Results results;

        long start = System.currentTimeMillis();

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );

            final List<Entity> entities = results.getEntities();

            for ( int i = 0; i < entities.size(); i++ ) {
                final UUID uuid = entities.get( i ).getUuid();

                assertTrue( "Value should not be returned twice", expectedResults.contains( uuid ) );

                expectedResults.remove( uuid );
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        long stop = System.currentTimeMillis();

        LOG.info( "Query took {} ms to return {} entities", stop - start, expectedResults.size() );

        assertEquals( "All entities returned", 0, expectedResults.size() );
    }
}
