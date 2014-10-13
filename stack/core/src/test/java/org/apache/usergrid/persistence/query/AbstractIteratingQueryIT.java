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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.ClassRule;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.ConcurrentCoreIteratorITSuite;
import org.apache.usergrid.CoreApplication;
import org.apache.usergrid.CoreITSetup;
import org.apache.usergrid.CoreITSetupImpl;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.Results;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/** @author tnine */
public abstract class AbstractIteratingQueryIT {
    private static final Logger LOG = LoggerFactory.getLogger( AbstractIteratingQueryIT.class );

    public static final long WRITE_DELAY = 0; // milliseconds to delay between writes in loop

    @ClassRule
    public static CoreITSetup setup = new CoreITSetupImpl( 
            ConcurrentCoreIteratorITSuite.cassandraResource, ConcurrentCoreIteratorITSuite.elasticSearchResource );

    @Rule
    public CoreApplication app = new CoreApplication( setup );


    public void singleOrderByMaxLimit( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 500;
        int queryLimit = Query.MAX_LIMIT;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entity = new HashMap<String, Object>();
            entity.put( "name", String.valueOf( i ) );

            io.writeEntity( entity );
            //we have to sleep, or we kill embedded cassandra
            Thread.sleep( 10 );

        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = new Query();
        query.addSort( "created" );
        query.setLimit( queryLimit );

        int count = 0;

        Results results;

        start = System.currentTimeMillis();

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                assertEquals( String.valueOf( count ), results.getEntities().get( i ).getName() );
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        stop = System.currentTimeMillis();
        LOG.info( "Query took {} ms to return {} entities", stop - start, count );

        assertEquals( size, count );
    }


    protected void singleOrderByIntersection( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 700;
        int queryLimit = Query.MAX_LIMIT;

        // the number of entities that should be written including an intersection
        int intersectIncrement = 5;

        long start = System.currentTimeMillis();

        List<String> expected = new ArrayList<String>( size / intersectIncrement );

        LOG.info( "Writing {} entities.", size );

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entity = new HashMap<String, Object>();

            String name = String.valueOf( i );

            boolean intersect = i % intersectIncrement == 0;

            entity.put( "name", String.valueOf( i ) );
            // if we hit the increment, set this to true
            entity.put( "intersect", intersect );

            io.writeEntity( entity );

            if ( intersect ) {
                expected.add( name );
            }
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = new Query();
        query.addSort( "created" );
        query.addEqualityFilter( "intersect", true );
        query.setLimit( queryLimit );

        int count = 0;

        Results results;

        start = System.currentTimeMillis();

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                assertEquals( expected.get( count ), results.getEntities().get( i ).getName() );
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        stop = System.currentTimeMillis();

        LOG.info( "Query took {} ms to return {} entities", stop - start, count );

        assertEquals( expected.size(), count );
    }


    protected void singleOrderByComplexIntersection( IoHelper io ) throws Exception {

        int size = 5000;
        int queryLimit = Query.MAX_LIMIT;

        // the number of entities that should be written including an intersection
        int intersectIncrement = 5;
        int secondIncrement = 9;

        long start = System.currentTimeMillis();

        io.doSetup();

        LOG.info( "Writing {} entities.", size );

        List<String> expectedResults = new ArrayList<String>( size / secondIncrement );

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entity = new HashMap<String, Object>();

            String name = String.valueOf( i );
            boolean intersect1 = i % intersectIncrement == 0;
            boolean intersect2 = i % secondIncrement == 0;
            entity.put( "name", name );
            // if we hit the increment, set this to true

            entity.put( "intersect", intersect1 );
            entity.put( "intersect2", intersect2 );
            io.writeEntity( entity );

            if ( intersect1 && intersect2 ) {
                expectedResults.add( name );
            }
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = new Query();
        query.addSort( "created" );
        query.addEqualityFilter( "intersect", true );
        query.addEqualityFilter( "intersect2", true );
        query.setLimit( queryLimit );

        int count = 0;

        Results results;

        start = System.currentTimeMillis();

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                assertEquals( expectedResults.get( count ), results.getEntities().get( i ).getName() );
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        stop = System.currentTimeMillis();

        LOG.info( "Query took {} ms to return {} entities", stop - start, count );

        assertEquals( expectedResults.size(), count );
    }


    protected void singleOrderByNoIntersection( IoHelper io ) throws Exception {
        io.doSetup();

        int size = 2000;
        int queryLimit = Query.MAX_LIMIT;

        // the number of entities that should be written including an intersection
        int secondIncrement = 9;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entity = new HashMap<String, Object>();
            entity.put( "name", String.valueOf( i ) );
            // if we hit the increment, set this to true
            entity.put( "intersect", false );
            entity.put( "intersect2", i % secondIncrement == 0 );
            io.writeEntity( entity );
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = new Query();
        query.addSort( "created" );
        // nothing will ever match this, the search should short circuit
        query.addEqualityFilter( "intersect", true );
        query.addEqualityFilter( "intersect2", true );
        query.setLimit( queryLimit );

        start = System.currentTimeMillis();

        Results results = io.getResults( query );

        // now do simple ordering, should be returned in order

        stop = System.currentTimeMillis();

        LOG.info( "Query took {} ms to return {} entities", stop - start, 0 );

        assertEquals( 0, results.size() );
    }


    protected void singleOrderByComplexUnion( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 2000;
        int queryLimit = Query.MAX_LIMIT;

        // the number of entities that should be written including an intersection
        int intersectIncrement = 5;
        int secondIncrement = 9;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        List<String> expectedResults = new ArrayList<String>( size / secondIncrement );

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entity = new HashMap<String, Object>();

            String name = String.valueOf( i );
            boolean intersect1 = i % intersectIncrement == 0;
            boolean intersect2 = i % secondIncrement == 0;
            entity.put( "name", name );
            // if we hit the increment, set this to true

            entity.put( "intersect", intersect1 );
            entity.put( "intersect2", intersect2 );
            io.writeEntity( entity );

            if ( intersect1 || intersect2 ) {
                expectedResults.add( name );
            }
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = Query.fromQL( "select * where intersect = true OR intersect2 = true order by created" );
        query.setLimit( queryLimit );

        int count = 0;

        Results results;

        start = System.currentTimeMillis();

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                assertEquals( expectedResults.get( count ), results.getEntities().get( i ).getName() );
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        stop = System.currentTimeMillis();

        LOG.info( "Query took {} ms to return {} entities", stop - start, count );

        assertEquals( expectedResults.size(), count );
    }


    protected void singleOrderByNot( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 2000;
        int queryLimit = Query.MAX_LIMIT;

        // the number of entities that should be written including an intersection
        int intersectIncrement = 5;
        int secondIncrement = 9;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        List<String> expectedResults = new ArrayList<String>( size / secondIncrement );

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entity = new HashMap<String, Object>();

            String name = String.valueOf( i );
            boolean intersect1 = i % intersectIncrement == 0;
            boolean intersect2 = i % secondIncrement == 0;
            entity.put( "name", name );
            // if we hit the increment, set this to true

            entity.put( "intersect", intersect1 );
            entity.put( "intersect2", intersect2 );
            io.writeEntity( entity );

            if ( !( intersect1 && intersect2 ) ) {
                expectedResults.add( name );
            }
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = Query.fromQL( "select * where NOT (intersect = true AND intersect2 = true) order by created" );
        query.setLimit( queryLimit );

        int count = 0;

        Results results;

        start = System.currentTimeMillis();

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                assertEquals( expectedResults.get( count ), results.getEntities().get( i ).getName() );
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        stop = System.currentTimeMillis();

        LOG.info( "Query took {} ms to return {} entities", stop - start, count );

        assertEquals( expectedResults.size(), count );
    }


    public void singleOrderByLessThanLimit( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 500;
        int queryLimit = Query.MAX_LIMIT;

        int matchMax = queryLimit - 1;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        List<String> expected = new ArrayList<String>( matchMax );

        for ( int i = 0; i < size; i++ ) {
            String name = String.valueOf( i );
            boolean searched = i < matchMax;

            Map<String, Object> entity = new HashMap<String, Object>();

            entity.put( "name", name );
            entity.put( "searched", searched );
            io.writeEntity( entity );

            if ( searched ) {
                expected.add( name );
            }
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = new Query();
        query.addSort( "created" );
        query.setLimit( queryLimit );
        query.addEqualityFilter( "searched", true );

        int count = 0;

        start = System.currentTimeMillis();

        // now do simple ordering, should be returned in order
        Results results = io.getResults( query );

        for ( int i = 0; i < results.size(); i++ ) {
            assertEquals( expected.get( count ), results.getEntities().get( i ).getName() );
            count++;
        }

        assertTrue( results.getCursor() == null );

        stop = System.currentTimeMillis();
        LOG.info( "Query took {} ms to return {} entities", stop - start, count );

        assertEquals( expected.size(), count );
    }


    public void singleOrderBySameRangeScanLessThanEqual( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 500;
        int queryLimit = 100;
        int startValue = 400;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        List<String> expected = new ArrayList<String>( size );

        for ( int i = 0; i < size; i++ ) {
            String name = String.valueOf( i );

            Map<String, Object> entity = new HashMap<String, Object>();

            entity.put( "name", name );
            entity.put( "index", i );
            io.writeEntity( entity );
            expected.add( name );
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = new Query();
        query.addSort( "index desc" );
        query.addLessThanEqualFilter( "index", startValue );
        query.setLimit( queryLimit );

        int count = 0;
        int delta = size - startValue;

        start = System.currentTimeMillis();

        // now do simple ordering, should be returned in order
        Results results;

        do {

            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                assertEquals( expected.get( size - delta - count ), results.getEntities().get( i ).getName() );
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.hasCursor() );

        assertEquals( expected.size() - delta + 1, count );

        stop = System.currentTimeMillis();
        LOG.info( "Query took {} ms to return {} entities", stop - start, count );
    }


    public void singleOrderBySameRangeScanLessEqual( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 500;
        int queryLimit = 100;
        int startValue = 400;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        List<String> expected = new ArrayList<String>( size );

        for ( int i = 0; i < size; i++ ) {
            String name = String.valueOf( i );

            Map<String, Object> entity = new HashMap<String, Object>();

            entity.put( "name", name );
            entity.put( "index", i );
            io.writeEntity( entity );
            expected.add( name );
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = new Query();
        query.addSort( "index desc" );
        query.addLessThanFilter( "index", startValue );
        query.setLimit( queryLimit );

        int count = 0;
        int delta = size - startValue;

        start = System.currentTimeMillis();

        // now do simple ordering, should be returned in order
        Results results;

        do {

            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                assertEquals( expected.get( size - delta - count - 1 ), results.getEntities().get( i ).getName() );
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.hasCursor() );

        assertEquals( expected.size() - delta, count );

        stop = System.currentTimeMillis();
        LOG.info( "Query took {} ms to return {} entities", stop - start, count );
    }


    public void singleOrderBySameRangeScanGreaterThanEqual( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 500;
        int queryLimit = 100;
        int startValue = 100;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        List<String> expected = new ArrayList<String>( size );

        for ( int i = 0; i < size; i++ ) {
            String name = String.valueOf( i );

            Map<String, Object> entity = new HashMap<String, Object>();

            entity.put( "name", name );
            entity.put( "index", i );
            io.writeEntity( entity );
            expected.add( name );
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = new Query();
        query.addSort( "index desc" );
        query.addGreaterThanEqualFilter( "index", startValue );
        query.setLimit( queryLimit );

        int count = 0;

        start = System.currentTimeMillis();

        // now do simple ordering, should be returned in order
        Results results;

        do {

            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                assertEquals( expected.get( size - count - 1 ), results.getEntities().get( i ).getName() );
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.hasCursor() );

        assertEquals( expected.size() - startValue, count );

        stop = System.currentTimeMillis();
        LOG.info( "Query took {} ms to return {} entities", stop - start, count );
    }


    public void singleOrderBySameRangeScanGreater( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 500;
        int queryLimit = 100;
        int startValue = 99;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        List<String> expected = new ArrayList<String>( size );

        for ( int i = 0; i < size; i++ ) {
            String name = String.valueOf( i );

            Map<String, Object> entity = new HashMap<String, Object>();

            entity.put( "name", name );
            entity.put( "index", i );
            io.writeEntity( entity );
            expected.add( name );
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = new Query();
        query.addSort( "index desc" );
        query.addGreaterThanFilter( "index", startValue );
        query.setLimit( queryLimit );

        int count = 0;

        start = System.currentTimeMillis();

        // now do simple ordering, should be returned in order
        Results results;

        do {

            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                assertEquals( expected.get( size - count - 1 ), results.getEntities().get( i ).getName() );
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.hasCursor() );

        assertEquals( expected.size() - startValue - 1, count );

        stop = System.currentTimeMillis();
        LOG.info( "Query took {} ms to return {} entities", stop - start, count );
    }


    public void singleOrderByBoundRangeScanDesc( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 500;
        int queryLimit = 100;
        int startValue = 100;
        int endValue = 400;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        List<String> expected = new ArrayList<String>( size );

        for ( int i = 0; i < size; i++ ) {
            String name = String.valueOf( i );

            Map<String, Object> entity = new HashMap<String, Object>();

            entity.put( "name", name );
            entity.put( "index", i );
            io.writeEntity( entity );
            expected.add( name );
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = Query.fromQL(
                String.format( "select * where index >= %d AND index <= %d order by index desc", startValue,
                        endValue ) );
        query.setLimit( queryLimit );

        int count = 0;
        int delta = size - endValue;

        start = System.currentTimeMillis();

        // now do simple ordering, should be returned in order
        Results results;

        do {

            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                assertEquals( expected.get( size - count - delta ), results.getEntities().get( i ).getName() );
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.hasCursor() );

        assertEquals( expected.size() - startValue - delta + 1, count );

        stop = System.currentTimeMillis();
        LOG.info( "Query took {} ms to return {} entities", stop - start, count );
    }


    public void singleOrderByBoundRangeScanAsc( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 500;
        int queryLimit = 100;
        int startValue = 100;
        int endValue = 400;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        List<String> expected = new ArrayList<String>( size );

        for ( int i = 0; i < size; i++ ) {
            String name = String.valueOf( i );

            Map<String, Object> entity = new HashMap<String, Object>();

            entity.put( "name", name );
            entity.put( "index", i );
            io.writeEntity( entity );
            expected.add( name );
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = Query.fromQL(
                String.format( "select * where index >= %d AND index <= %d order by index asc", startValue,
                        endValue ) );
        query.setLimit( queryLimit );

        int count = 0;
        int delta = size - endValue;

        start = System.currentTimeMillis();

        // now do simple ordering, should be returned in order
        Results results;

        do {

            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                assertEquals( expected.get( delta + count ), results.getEntities().get( i ).getName() );
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.hasCursor() );

        assertEquals( expected.size() - startValue - delta + 1, count );

        stop = System.currentTimeMillis();
        LOG.info( "Query took {} ms to return {} entities", stop - start, count );
    }


    /**
     * Tests that when an empty query is issued, we page through all entities correctly
     *
     * @param io the io helper
     */
    public void allIn( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 300;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entity = new HashMap<String, Object>();
            entity.put( "name", String.valueOf( i ) );

            io.writeEntity( entity );
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        app.getEm().refreshIndex();

        Query query = new Query();
        query.setLimit( 100 );

        int count = 0;

        Results results;

        start = System.currentTimeMillis();

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                assertEquals( String.valueOf( count ), results.getEntities().get( i ).getName() );
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        stop = System.currentTimeMillis();
        LOG.info( "Query took {} ms to return {} entities", stop - start, count );

        assertEquals( size, count );
    }


    protected void multiOrderBy( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 2000;
        int queryLimit = Query.MAX_LIMIT;

        // the number of entities that should be written including an intersection

        Set<Entity> sortedResults = new TreeSet<Entity>( new Comparator<Entity>() {

            @Override
            public int compare( Entity o1, Entity o2 ) {
                boolean o1Boolean = ( Boolean ) o1.getProperty( "boolean" );
                boolean o2Boolean = ( Boolean ) o2.getProperty( "boolean" );

                if ( o1Boolean != o2Boolean ) {
                    if ( o1Boolean ) {
                        return -1;
                    }

                    return 1;
                }

                int o1Index = ( Integer ) o1.getProperty( "index" );
                int o2Index = ( Integer ) o2.getProperty( "index" );

                if ( o1Index > o2Index ) {
                    return 1;
                }
                else if ( o2Index > o1Index ) {
                    return -1;
                }

                return 0;
            }
        } );


        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entity = new HashMap<String, Object>();

            String name = String.valueOf( i );
            boolean bool = i % 2 == 0;
            entity.put( "name", name );
            entity.put( "boolean", bool );

            /**
             * we want them to be ordered from the "newest" time uuid to the oldec since we
             * have a low cardinality value as the first second clause.  This way the test
             *won't accidentally pass b/c the UUID ordering matches the index ordering.  If we were
             *to reverse the value of index (size-i) the test would pass incorrectly
             */

            entity.put( "index", i );

            Entity saved = io.writeEntity( entity );

            sortedResults.add( saved );
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        app.getEm().refreshIndex();

        Query query = Query.fromQL( "select * order by boolean desc, index asc" );
        query.setLimit( queryLimit );

        int count = 0;

        Results results;

        start = System.currentTimeMillis();

        Iterator<Entity> itr = sortedResults.iterator();

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                Entity expected = itr.next();
                Entity returned = results.getEntities().get( i );

                assertEquals( "Order incorrect", expected.getName(), returned.getName() );
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        stop = System.currentTimeMillis();

        LOG.info( "Query took {} ms to return {} entities", stop - start, count );

        assertEquals( sortedResults.size(), count );
    }


    protected void multiOrderByComplexUnion( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 2000;
        int queryLimit = Query.MAX_LIMIT;

        // the number of entities that should be written including an intersection
        int intersectIncrement = 5;
        int secondIncrement = 9;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        Set<Entity> sortedResults = new TreeSet<Entity>( new Comparator<Entity>() {

            @Override
            public int compare( Entity o1, Entity o2 ) {
                long o1Index = ( Long ) o1.getProperty( "created" );
                long o2Index = ( Long ) o2.getProperty( "created" );

                if ( o1Index > o2Index ) {
                    return 1;
                }
                else if ( o2Index > o1Index ) {
                    return -1;
                }


                boolean o1Boolean = ( Boolean ) o1.getProperty( "intersect" );
                boolean o2Boolean = ( Boolean ) o2.getProperty( "intersect" );

                if ( o1Boolean != o2Boolean ) {
                    if ( o1Boolean ) {
                        return -1;
                    }

                    return 1;
                }


                return 0;
            }
        } );

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entity = new HashMap<String, Object>();

            String name = String.valueOf( i );
            boolean intersect1 = i % intersectIncrement == 0;
            boolean intersect2 = i % secondIncrement == 0;
            entity.put( "name", name );
            // if we hit the increment, set this to true

            entity.put( "intersect", intersect1 );
            entity.put( "intersect2", intersect2 );
            Entity e = io.writeEntity( entity );

            if ( intersect1 || intersect2 ) {
                sortedResults.add( e );
            }
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        app.getEm().refreshIndex();

        Query query =
                Query.fromQL( "select * where intersect = true OR intersect2 = true order by created, intersect desc" );
        query.setLimit( queryLimit );

        int count = 0;

        Results results;

        start = System.currentTimeMillis();

        Iterator<Entity> expected = sortedResults.iterator();

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );

            for ( Entity result : results.getEntities() ) {
                assertEquals( expected.next(), result );
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        stop = System.currentTimeMillis();

        LOG.info( "Query took {} ms to return {} entities", stop - start, count );

        assertEquals( sortedResults.size(), count );
    }


    /**
     * Tests that when an empty query is issued, we page through all entities correctly
     *
     * @param io the io helper
     */
    public void notOrderBy( IoHelper io ) throws Exception {

        io.doSetup();

        /**
         * Leave this as a large size.  We have to write over 1k to reproduce this issue
         */
        int size = 2000;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entity = new HashMap<String, Object>();
            entity.put( "name", String.valueOf( i ) );
            entity.put( "boolean", !(i % 2 == 0));
            entity.put( "index", i);

            io.writeEntity( entity );
        }

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = Query.fromQL("select * where NOT boolean = false order by index asc");
        query.setLimit( 20 );

        int index = 0;
        int count = 0;

        Results results;

        start = System.currentTimeMillis();

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
//                assertEquals( String.valueOf( index ), results.getEntities().get( i ).getName() );
//                index +=2;
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        stop = System.currentTimeMillis();
        LOG.info( "Query took {} ms to return {} entities", stop - start, count );

        assertEquals( size/2, count );
    }


    /**
     * Interface to abstract actually doing I/O targets. The same test logic can be applied to both collections and
     * connections
     *
     * @author tnine
     */
    public static interface IoHelper {
        /** Perform any setup required */
        public void doSetup() throws Exception;

        /**
         * Write the entity to the data store
         *
         * @param entity the entity
         */
        public Entity writeEntity( Map<String, Object> entity ) throws Exception;

        /**
         * Get the results for the query
         *
         * @param query the query to get results for
         *
         * @return the results of the query
         */
        public Results getResults( Query query ) throws Exception;
    }


    public static  class CollectionIoHelper implements IoHelper {

        protected final CoreApplication app;


        public CollectionIoHelper( final CoreApplication app ) {
            this.app = app;
        }


        @Override
        public void doSetup() throws Exception {
        }


        @Override
        public Entity writeEntity( Map<String, Object> entity ) throws Exception {

            Entity e = app.getEm().create( "test", entity );

            if ( WRITE_DELAY > 0 ) {
                Thread.sleep( WRITE_DELAY );
            }

            return e;
        }


        @Override
        public Results getResults( Query query ) throws Exception {
            app.getEm().refreshIndex();
            return app.getEm().searchCollection( app.getEm().getApplicationRef(), "tests", query );
        }
    }


    public static class ConnectionHelper extends CollectionIoHelper {

        /**
         *
         */
        protected static final String CONNECTION = "connection";
        protected Entity rootEntity;


        public ConnectionHelper( final CoreApplication app) {
            super( app );
        }


        @Override
        public void doSetup() throws Exception {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put( "name", "rootentity" );
            rootEntity = app.getEm().create( "root", data );
        }


        @Override
        public Entity writeEntity( Map<String, Object> entity ) throws Exception {

            // write to the collection
            Entity created = super.writeEntity( entity );
            app.getEm().createConnection( rootEntity, CONNECTION, created );

            if ( WRITE_DELAY > 0 ) {
                Thread.sleep( WRITE_DELAY );
            }

            return created;
        }


        /*
         * (non-Javadoc)
         *
         * @see
         * org.apache.usergrid.persistence.query.SingleOrderByMaxLimitCollection.CollectionIoHelper#
         * getResults(org.apache.usergrid.persistence.Query)
         */
        @Override
        public Results getResults( Query query ) throws Exception {

            app.getEm().refreshIndex();
            query.setConnectionType( CONNECTION );
            query.setEntityType( "test" );

            return app.getEm().searchConnectedEntities( rootEntity, query );
        }
    }
}
