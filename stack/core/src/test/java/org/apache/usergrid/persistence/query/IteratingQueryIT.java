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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.CoreApplication;
import org.apache.usergrid.CoreITSetup;
import org.apache.usergrid.CoreITSetupImpl;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/** @author tnine */
public class IteratingQueryIT {
    private static final Logger LOG = LoggerFactory.getLogger( IteratingQueryIT.class );

    @ClassRule
    public static CoreITSetup setup = new CoreITSetupImpl(  );

    @Rule
    public CoreApplication app = new CoreApplication( setup );


    @Test
    public void allInCollection() throws Exception {
        allIn( new CollectionIoHelper( app ) );
    }


    @Test
    public void allInConnection() throws Exception {
        allIn( new ConnectionHelper( app ) );
    }


    @Test
    public void allInConnectionNoType() throws Exception {
        allIn( new ConnectionNoTypeHelper( app ) );
    }


    @Test
    public void multiOrderByCollection() throws Exception {
        multiOrderBy( new CollectionIoHelper( app ) );
    }


    @Test
    public void multiOrderByComplexUnionCollection() throws Exception {
        multiOrderByComplexUnion( new CollectionIoHelper( app ) );
    }


    @Test
    public void multiOrderByComplexUnionConnection() throws Exception {
        multiOrderByComplexUnion( new CollectionIoHelper( app ) );
    }


    @Test
    public void multOrderByConnection() throws Exception {
        multiOrderBy( new ConnectionHelper( app ) );
    }


    @Test
    public void orderByWithNotCollection() throws Exception {
        notOrderBy( new CollectionIoHelper( app ) );
    }


    @Test
    public void orderByWithNotConnection() throws Exception {
        notOrderBy( new ConnectionHelper( app ) );
    }


    @Test
    public void singleOrderByBoundRangeScanAscCollection() throws Exception {
        singleOrderByBoundRangeScanAsc( new CollectionIoHelper( app ) );

    }


    @Test
    public void singleOrderByBoundRangeScanAscConnection() throws Exception {
        singleOrderByBoundRangeScanAsc( new ConnectionHelper( app ) );
    }


    @Test
    public void singleOrderByBoundRangeScanDescCollection() throws Exception {
        singleOrderByBoundRangeScanDesc( new CollectionIoHelper( app ) );
    }


    @Test
    public void singleOrderByBoundRangeScanDescConnection() throws Exception {
        singleOrderByBoundRangeScanDesc( new ConnectionHelper( app ) );
    }


    @Test
    public void singleOrderByComplexIntersectionCollection() throws Exception {
        singleOrderByComplexIntersection( new CollectionIoHelper( app ) );
    }


    @Test
    public void singleOrderByComplexIntersectionConnection() throws Exception {
        singleOrderByComplexIntersection( new ConnectionHelper( app ) );
    }


    @Test
    public void singleOrderByComplexUnionCollection() throws Exception {
        singleOrderByComplexUnion( new CollectionIoHelper( app ) );
    }


    @Test
    public void singleOrderByComplexUnionConnection() throws Exception {
        singleOrderByComplexUnion( new ConnectionHelper( app ) );
    }


    @Test
    public void singleOrderByIntersectionCollection() throws Exception {
        singleOrderByIntersection( new CollectionIoHelper( app ) );
    }


    @Test
    public void singleOrderByIntersectionConnection() throws Exception {
        singleOrderByIntersection( new ConnectionHelper( app ) );
    }


    @Test
    public void singleOrderByLessThanLimitCollection() throws Exception {
        singleOrderByLessThanLimit( new CollectionIoHelper( app ) );
    }


    @Test
    public void singleOrderByLessThanLimitConnection() throws Exception {
        singleOrderByLessThanLimit( new ConnectionHelper( app ) );
    }


    @Test
    public void singleOrderByMaxLimitCollection() throws Exception {
        singleOrderByMaxLimit( new CollectionIoHelper( app ) );
    }


    @Test
    public void singleOrderByMaxLimitConnection() throws Exception {
        singleOrderByMaxLimit( new ConnectionHelper( app ) );
    }


    @Test
    public void singleOrderByNoIntersectionCollection() throws Exception {
        singleOrderByNoIntersection( new CollectionIoHelper( app ) );
    }


    @Test
    public void singleOrderByNoIntersectionConnection() throws Exception {
        singleOrderByNoIntersection( new CollectionIoHelper( app ) );
    }


    @Test
    public void singleOrderByNotCollection() throws Exception {
        singleOrderByNot( new CollectionIoHelper( app ) );
    }


    @Test
    public void singleOrderByNotConnection() throws Exception {
        singleOrderByNot( new ConnectionHelper( app ) );
    }


    @Test
    public void singleOrderBySameRangeScanGreaterCollection() throws Exception {
        singleOrderBySameRangeScanGreater( new CollectionIoHelper( app ) );
    }


    @Test
    public void singleOrderBySameRangeScanGreaterConnection() throws Exception {
        singleOrderBySameRangeScanGreater( new ConnectionHelper( app ) );
    }


    @Test
    public void singleOrderBySameRangeScanGreaterThanEqualCollection() throws Exception {
        singleOrderBySameRangeScanGreaterThanEqual( new CollectionIoHelper( app ) );
    }


    @Test
    public void singleOrderBySameRangeScanLessCollection() throws Exception {
        singleOrderBySameRangeScanLessEqual( new CollectionIoHelper( app ) );
    }


    @Test
    public void singleOrderBySameRangeScanLessConnection() throws Exception {
        singleOrderBySameRangeScanLessEqual( new ConnectionHelper( app ) );
    }


    @Test
    public void singleOrderBySameRangeScanLessThanEqualCollection() throws Exception {
        singleOrderBySameRangeScanLessThanEqual( new CollectionIoHelper( app ) );
    }


    @Test
    public void singleOrderBySameRangeScanLessThanEqualConnection() throws Exception {
        singleOrderBySameRangeScanLessThanEqual( new ConnectionHelper( app ) );
    }

    class ConnectionNoTypeHelper extends ConnectionHelper {

        public ConnectionNoTypeHelper( final CoreApplication app ) {
            super( app );
        }


        /**
         * (non-Javadoc) @see org.apache.usergrid.persistence.query.SingleOrderByMaxLimitCollection
         * .ConnectionHelper#getResults
         * (org.apache.usergrid.persistence.Query)
         */
        @Override
        public Results getResults( Query query ) throws Exception {
            query.setConnectionType( CONNECTION );
            // don't set it on purpose
            query.setEntityType( null );
            return app.getEntityManager().searchTargetEntities(rootEntity, query);
        }
    }

    public void singleOrderByMaxLimit( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 20;
        int queryLimit = Query.MAX_LIMIT;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entity = new HashMap<String, Object>();
            entity.put("name", String.valueOf(i));

            io.writeEntity(entity);
            //we have to sleep, or we kill embedded cassandra

        }
        app.refreshIndex();
        Thread.sleep(1000);
        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = Query.fromQL("order by  created" );
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

        int size = 70;
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
        app.refreshIndex();

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = Query.fromQL( "select * where intersect = true order by created asc" );
        query.setLimit( queryLimit );

        int count = 0;

        Results results;

        start = System.currentTimeMillis();

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );

            for ( int i = 0 ; i< results.size(); i++) {
                assertEquals( expected.get( count++ ), results.getEntities().get( i ).getName() );
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        stop = System.currentTimeMillis();

        LOG.info( "Query took {} ms to return {} entities", stop - start, count );

        assertEquals( expected.size(), count );
    }


    protected void singleOrderByComplexIntersection( IoHelper io ) throws Exception {

        int size = 20;
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

        this.app.refreshIndex();
        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = Query.fromQL( "select * where intersect = true AND intersect2 = true order by  created" );
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

        int size = 20;
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

        Query query = Query.fromQL( "select * where intersect = true AND intersect2 = true order by  created" );
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

        int size = 20;
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
        app.refreshIndex();

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

        int size = 20;
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

        app.refreshIndex();
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


    protected void singleOrderByLessThanLimit( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 10;
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

        app.refreshIndex();
        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = Query.fromQL( "select * where searched = true order by created" );
        query.setLimit( queryLimit );

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


    protected void singleOrderBySameRangeScanLessThanEqual( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 10;
        int queryLimit = 5;
        int startValue = 5;

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
        this.app.refreshIndex();

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = Query.fromQL( "select * where index >= "+ startValue + " order by index desc" );
        query.setLimit( queryLimit );

        int count = 0;
        int delta = size - startValue;

        start = System.currentTimeMillis();

        // now do simple ordering, should be returned in order
        Results results;

        do {

            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                assertEquals( expected.get( size  - count -1 ), results.getEntities().get( i ).getName() );
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.hasCursor() );

        assertEquals( expected.size() - delta, count );

        stop = System.currentTimeMillis();
        LOG.info( "Query took {} ms to return {} entities", stop - start, count );
    }


    protected void singleOrderBySameRangeScanLessEqual( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 20;
        int queryLimit = 5;
        int startValue = 10;

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
        this.app.refreshIndex();

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = Query.fromQL( "select * where index >= "+ startValue + " order by index desc" );
        query.setLimit( queryLimit );

        int count = 0;
        int delta = size - startValue;

        start = System.currentTimeMillis();

        // now do simple ordering, should be returned in order
        Results results;

        do {

            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                assertEquals( expected.get( size - count - 1   ), results.getEntities().get( i ).getName() );
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.hasCursor() );

        assertEquals( expected.size() - delta, count );

        stop = System.currentTimeMillis();
        LOG.info( "Query took {} ms to return {} entities", stop - start, count );
    }


    protected void singleOrderBySameRangeScanGreaterThanEqual( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 20;
        int queryLimit = 10;
        int startValue = 10;

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

        app.refreshIndex();
        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = Query.fromQL( "select * where index >= "+ startValue + " order by index desc" );
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


    protected void singleOrderBySameRangeScanGreater( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 20;
        int queryLimit = 10;
        int startValue = 9;

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
        app.refreshIndex();

        Thread.sleep(500);
        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = Query.fromQL( "select * where index >= "+ startValue + " order by index desc" );
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

        assertEquals( expected.size() - startValue , count );

        stop = System.currentTimeMillis();
        LOG.info( "Query took {} ms to return {} entities", stop - start, count );
    }


    protected void singleOrderByBoundRangeScanDesc( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 20;
        int queryLimit = 10;
        int startValue = 5;
        int endValue = 15;

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

        app.refreshIndex();
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

        assertEquals( expected.size() - startValue - delta +1 , count );

        stop = System.currentTimeMillis();
        LOG.info( "Query took {} ms to return {} entities", stop - start, count );
    }


    protected void singleOrderByBoundRangeScanAsc( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 20;
        int queryLimit = 10;
        int startValue = 5;
        int endValue = 15;

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
        app.refreshIndex();

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
    protected void allIn( IoHelper io ) throws Exception {

        io.doSetup();

        int size = 30;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entity = new HashMap<String, Object>();
            entity.put( "name", String.valueOf( i ) );
            io.writeEntity( entity );
        }

        this.app.refreshIndex();

        long stop = System.currentTimeMillis();

        LOG.info("Writes took {} ms", stop - start );


        Query query = new Query();
        query.setLimit( 10 );

        int count = 0;

        Results results;

        start = System.currentTimeMillis();

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                assertEquals( String.valueOf( size - count -1 ), results.getEntities().get( i ).getName() );
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

        int size = 20;
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

        app.refreshIndex();

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

        int size = 20;
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

        app.refreshIndex();

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
    protected void notOrderBy( IoHelper io ) throws Exception {

        io.doSetup();

        /**
         * Leave this as a large size.  We have to write over 1k to reproduce this issue
         */
        int size = 20;

        long start = System.currentTimeMillis();

        LOG.info( "Writing {} entities.", size );

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entity = new HashMap<String, Object>();
            entity.put( "name", String.valueOf( i ) );
            entity.put( "boolean", (i % 2 == 0));
            entity.put( "index", i);

            io.writeEntity( entity );
        }
        this.app.refreshIndex();

        long stop = System.currentTimeMillis();

        LOG.info( "Writes took {} ms", stop - start );

        Query query = Query.fromQL("select * where NOT boolean = false order by index asc");
        query.setLimit( 2 );

        int index = 0;
        int count = 0;

        Results results;

        start = System.currentTimeMillis();

        do {

            // now do simple ordering, should be returned in order
            results = io.getResults( query );

            for ( int i = 0; i < results.size(); i++ ) {
                assertEquals( String.valueOf( index ), results.getEntities().get( i ).getName() );
                index +=2;
                count++;
            }

            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        stop = System.currentTimeMillis();
        LOG.info( "Query took {} ms to return {} entities", stop - start, count );

        assertEquals( size/2, count );
    }


}
