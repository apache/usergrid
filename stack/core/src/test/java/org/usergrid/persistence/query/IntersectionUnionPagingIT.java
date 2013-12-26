/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence.query;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author tnine
 */
public class IntersectionUnionPagingIT extends AbstractIteratingQueryIT {

    private static final Logger LOG = LoggerFactory.getLogger( IntersectionUnionPagingIT.class );


    @Test
    public void testUnionPagingCollection() throws Exception {

        testUnionPaging( new CollectionIoHelper( app ) );
    }


    @Test
    public void testUnionPagingConnection() throws Exception {

        testUnionPaging( new ConnectionHelper( app ) );
    }


    private void testUnionPaging( IoHelper io ) throws Exception {
        io.doSetup();

        int size = 500;
        int pageSize = 100;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        final String zeros = String.format( "%08d", 0 );

        Set<String> names = new HashSet<String>( size );

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entity = new HashMap<String, Object>();
            final String name = String.valueOf( i );
            entity.put( "name", name );
            entity.put( "fieldDate", "0000-00-00" );

            String field1;
            String field2;

            if ( i < pageSize - 10 ) {
                field1 = String.format( "%08d", (int)(100 * Math.random()) );
                field2 = zeros;


            }
            else {
                field1 = zeros;
                field2 = String.format( "%08d", (int)(100 * Math.random()) );
            }

            names.add( name );

            entity.put( "field1Or", field1);
            entity.put( "field2Or", field2 );

            io.writeEntity( entity );

        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        //our field1Or has a result size < our page size, so it shouldn't blow up when the cursor is getting created
        //the leaf iterator should insert it's own "no value left" into the cursor
        Query query = Query.fromQL(
                "select * where (field1Or > '00000000' OR field2Or > '00000000') AND fieldDate = '0000-00-00'" );
        query.setLimit( pageSize );

        Results results;

        start = System.currentTimeMillis();

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                final String name = results.getEntities().get( i ).getName();

                assertTrue( names.contains( name ) );

                names.remove( name );
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        stop = System.currentTimeMillis();

        LOG.info( "Query took {} ms to return {} entities", stop - start, size );

        assertEquals( "All names returned", 0, names.size() );
    }
}
