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
package org.apache.usergrid.persistence;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.persistence.geo.CollectionGeoSearch;
import org.apache.usergrid.persistence.geo.EntityLocationRef;
import org.apache.usergrid.persistence.geo.model.Point;
import org.apache.usergrid.utils.MapUtils;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Ignore;


@Concurrent()
public class GeoIT extends AbstractCoreIT {
    private static final Logger LOG = LoggerFactory.getLogger( GeoIT.class );


    public GeoIT() {
        super();
    }


    @Ignore // GeoIndexManager and CollectionGeoSearch are no longer needed with Core Persistence
            // TODO: consider rewriting this test to use queries rather than CollectionGeoSearch
            // so we can make it work against Core Persistence
        
    @Test
    public void testGeo() throws Exception {
        LOG.info( "GeoIT.testGeo" );

        UUID applicationId = setup.createApplication( "testOrganization", "testGeo" );
        assertNotNull( applicationId );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        assertNotNull( em );

        Map<String, Object> properties = new LinkedHashMap<String, Object>() {{
            put( "username", "edanuff" );
            put( "email", "ed@anuff.com" );
            put( "location.coordinates", new LinkedHashMap<String, Object>() {{
                put("latitude", 37.776753 );
                put("longitude", -122.407846 );
            }} ); 
        }};

        Entity user = em.create( "user", properties );
        assertNotNull( user );

//        GeoIndexManager geo = em.getGeoIndexManager();
//        geo.storeLocationInCollectionIndex( 
//                em.getApplicationRef(), "users", user.getUuid(), "location.coordinates", loc );

        Point center = new Point( 37.774277, -122.404744 );

        CollectionGeoSearch connSearch = new CollectionGeoSearch( 
                em, setup.getIbl(), setup.getCassSvc(), em.getApplicationRef(), "users" );


        List<EntityLocationRef> listResults = connSearch.proximitySearch( 
                null, null, center, "location.coordinates", 0, 200, 100 ).entityLocations;

        assertEquals( 0, listResults.size() );

        listResults = connSearch.proximitySearch( 
                null, null, center, "location.coordinates", 0, 400, 100 ).entityLocations;


        this.dump( listResults );

        assertEquals( 1, listResults.size() );

//        geo.removeLocationFromCollectionIndex( 
//                em.getApplicationRef(), "users", "location.coordinates", loc );

        properties.remove("location.coordinates");
        em.updateProperties(user, properties);

        listResults = connSearch.proximitySearch( 
                null, null, center, "location.coordinates", 0, 400, 100 ).entityLocations;

        this.dump( listResults );

        assertEquals( 0, listResults.size() );

        updatePos( em, user, 37.426373, -122.14108 );

        center = new Point( 37.774277, -122.404744 );

        listResults = connSearch.proximitySearch( 
                null, null, center, "location.coordinates", 0, 200, 100 ).entityLocations;

        assertEquals( 0, listResults.size() );

        updatePos( em, user, 37.774277, -122.404744 );

        center = new Point( 37.776753, -122.407846 );

        listResults = connSearch.proximitySearch( 
                null, null, center, "location.coordinates", 0, 1000, 100 ).entityLocations;

        assertEquals( 1, listResults.size() );

        // check at globally large distance

        listResults = connSearch.proximitySearch( 
            null, null, center, "location.coordinates", 0, Integer.MAX_VALUE, 100 ).entityLocations;

        assertEquals( 1, listResults.size() );

        // create a new entity so we have 2
        final EntityLocationRef loc2 = new EntityLocationRef( null, 31.1, 121.2 );
        LinkedHashMap<String, Object> properties2 = new LinkedHashMap<String, Object>() {{
            put( "username", "sganyo" );
            put( "email", "sganyo@anuff.com" );
            put( "location.coordinates", new LinkedHashMap<String, Object>() {{
                put("latitude", 31.1 );
                put("longitude", 121.2 );
            }} ); 
        }};
        Entity user2 = em.create( "user", properties2 );
        assertNotNull( user2 );

//        geo.storeLocationInCollectionIndex( 
//                em.getApplicationRef(), "users", user2.getUuid(), "location.coordinates", loc2 );

        listResults = connSearch.proximitySearch( 
                null, null, center, "location.coordinates", 0, 10000, 100 ).entityLocations;

        assertEquals( 1, listResults.size() );

        // check at globally large distance
        listResults = connSearch.proximitySearch( 
            null, null, center, "location.coordinates", 0, Integer.MAX_VALUE, 100 ).entityLocations;

        assertEquals( 2, listResults.size() );

        // check at globally large distance (center point close to other entity)
        center = new Point( 31.14, 121.27 );

        listResults = connSearch.proximitySearch( 
            null, null, center, "location.coordinates", 0, Integer.MAX_VALUE, 100 ).entityLocations;

        assertEquals( 2, listResults.size() );

        Results emSearchResults = em.searchCollection( em.getApplicationRef(), "users",
                Query.fromQL( "location within 1000 of 37.776753, -122.407846" ) );
        assertEquals( 1, emSearchResults.size() );

        updatePos( em, user, 37.776753, -122.407846 );

        center = new Point( 37.428526, -122.140916 );

        listResults = connSearch.proximitySearch( 
                null, null, center, "location.coordinates", 0, 1000, 100 ).entityLocations;


        assertEquals( 0, listResults.size() );

        emSearchResults = em.searchCollection( em.getApplicationRef(), "users",
                Query.fromQL( "location within 1000 of 37.428526, -122.140916" ) );
        assertEquals( 0, emSearchResults.size() );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "name", "Brickhouse" );
        properties.put( "address", "426 Brannan Street" );
        properties.put( "location", getLocation( 37.779632, -122.395131 ) );

        Entity restaurant = em.create( "restaurant", properties );
        assertNotNull( restaurant );

        em.createConnection( user, "likes", restaurant );

        emSearchResults = em.searchConnectedEntities( user, 
                Query.fromQL( "location within 2000 of 37.776753, -122.407846" ) );
        assertEquals( 1, emSearchResults.size() );

        emSearchResults = em.searchConnectedEntities( user, 
                Query.fromQL( "location within 1000 of 37.776753, -122.407846" ) );
        assertEquals( 0, emSearchResults.size() );
    }


    @Test
    public void testPointPaging() throws Exception {

        UUID applicationId = setup.createApplication( "testOrganization", "testPointPaging" );
        assertNotNull( applicationId );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        assertNotNull( em );

        // save objects in a diagonal line from -90 -180 to 90 180

        int numEntities = 500;

        float minLattitude = -90;
        float maxLattitude = 90;
        float minLongitude = -180;
        float maxLongitude = 180;

        float lattitudeDelta = ( maxLattitude - minLattitude ) / numEntities;

        float longitudeDelta = ( maxLongitude - minLongitude ) / numEntities;

        for ( int i = 0; i < numEntities; i++ ) {
            float lattitude = minLattitude + lattitudeDelta * i;
            float longitude = minLongitude + longitudeDelta * i;

            Map<String, Float> location = MapUtils.hashMap( "latitude", lattitude ).map( "longitude", longitude );

            Map<String, Object> data = new HashMap<String, Object>( 2 );
            data.put( "name", String.valueOf( i ) );
            data.put( "location", location );

            em.create( "store", data );
        }

        em.refreshIndex();

        Query query = new Query();
        // earth's circumference is 40,075 kilometers. Up it to 50,000kilometers
        // just to be save
        query.addFilter( "location within 50000000 of -90, -180" );
        query.setLimit( 100 );

        int count = 0;
        Results results;

        do {
            results = em.searchCollection( em.getApplicationRef(), "stores", query );

            for ( Entity entity : results.getEntities() ) {
                assertEquals( String.valueOf( count ), entity.getName() );
                count++;
            }

            // set for the next "page"
            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        // check we got back all 500 entities
        assertEquals( numEntities, count );
    }


    @Test
    public void testSamePointPaging() throws Exception {

        UUID applicationId = setup.createApplication( "testOrganization", "testSamePointPaging" );
        assertNotNull( applicationId );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        assertNotNull( em );

        // save objects in a diagonal line from -90 -180 to 90 180

        int numEntities = 500;

        for ( int i = 0; i < numEntities; i++ ) {
            Map<String, Object> data = new HashMap<String, Object>( 2 );
            data.put( "name", String.valueOf( i ) );
            setPos( data, 0, 0 );

            em.create( "store", data );
        }

        em.refreshIndex();

        Query query = new Query();
        // earth's circumference is 40,075 kilometers. Up it to 50,000kilometers
        // just to be save
        query.addFilter( "location within 50000000 of 0, 0" );
        query.setLimit( 100 );

        int count = 0;
        Results results;

        do {
            results = em.searchCollection( em.getApplicationRef(), "stores", query );

            for ( Entity entity : results.getEntities() ) {
                assertEquals( String.valueOf( count ), entity.getName() );
                count++;
            }

            // set for the next "page"
            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        // check we got back all 500 entities
        assertEquals( numEntities, count );
    }


    @Test
    public void testDistanceByLimit() throws Exception {

        UUID applicationId = setup.createApplication( "testOrganization", "testDistanceByLimit" );
        assertNotNull( applicationId );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        assertNotNull( em );

        // save objects in a diagonal line from -90 -180 to 90 180

        int numEntities = 100;

        float minLattitude = -90;
        float maxLattitude = 90;
        float minLongitude = -180;
        float maxLongitude = 180;

        float lattitudeDelta = ( maxLattitude - minLattitude ) / numEntities;

        float longitudeDelta = ( maxLongitude - minLongitude ) / numEntities;

        for ( int i = 0; i < numEntities; i++ ) {
            float lattitude = minLattitude + lattitudeDelta * i;
            float longitude = minLongitude + longitudeDelta * i;

            Map<String, Float> location = MapUtils.hashMap( "latitude", lattitude ).map( "longitude", longitude );

            Map<String, Object> data = new HashMap<String, Object>( 2 );
            data.put( "name", String.valueOf( i ) );
            data.put( "location", location );

            em.create( "store", data );
        }

        em.refreshIndex();

        Query query = new Query();
        // earth's circumference is 40,075 kilometers. Up it to 50,000kilometers
        // just to be save
        query.addFilter( "location within 0 of -90, -180" );
        query.setLimit( 100 );

        int count = 0;

        do {
            Results results = em.searchCollection( em.getApplicationRef(), "stores", query );

            for ( Entity entity : results.getEntities() ) {
                assertEquals( String.valueOf( count ), entity.getName() );
                count++;
            }
        }
        while ( query.getCursor() != null );

        // check we got back all 500 entities
        assertEquals( numEntities, count );
    }


    @Test
    public void testGeoWithIntersection() throws Exception {

        UUID applicationId = setup.createApplication( "testOrganization", "testGeoWithIntersection" );
        assertNotNull( applicationId );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        assertNotNull( em );

        int size = 100;
        int min = 50;
        int max = 90;

        List<Entity> created = new ArrayList<Entity>( size );

        for ( int i = 0; i < size; i++ ) {

            // save all entities in the same location
            Map<String, Object> data = new HashMap<String, Object>( 2 );
            data.put( "name", String.valueOf( i ) );
            data.put( "index", i );
            setPos( data, 0, 0 );

            Entity e = em.create( "store", data );

            created.add( e );
        }

        em.refreshIndex();

        int startDelta = size - min;

        //    String queryString = String.format("select * where location within 100 of 0,
        // 0 and index >= %d and index < %d order by index",min, max);

        String queryString = String.format( "select * where index >= %d and index < %d order by index", min, max );

        Query query = Query.fromQL( queryString );

        Results r;
        int count = 0;

        do {

            r = em.searchCollection( em.getApplicationRef(), "stores", query );

            for ( Entity e : r.getEntities() ) {
                assertEquals( created.get( startDelta + count ), e );
                count++;
            }

            query.setCursor( r.getCursor() );
        }
        while ( r.hasCursor() );

        assertEquals( startDelta - ( size - max ), count );
    }


    @Test
    public void testDenseSearch() throws Exception {

        UUID applicationId = setup.createApplication( "testOrganization", "testDenseSearch" );
        assertNotNull( applicationId );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        assertNotNull( em );

        // save objects in a diagonal line from -90 -180 to 90 180

        int numEntities = 500;

        float minLattitude = 48.32455f;
        float maxLattitude = 48.46481f;
        float minLongitude = 9.89561f;
        float maxLongitude = 10.0471f;

        float lattitudeDelta = ( maxLattitude - minLattitude ) / numEntities;

        float longitudeDelta = ( maxLongitude - minLongitude ) / numEntities;

        for ( int i = 0; i < numEntities; i++ ) {
            float lattitude = minLattitude + lattitudeDelta * i;
            float longitude = minLongitude + longitudeDelta * i;

            Map<String, Float> location = MapUtils.hashMap( "latitude", lattitude ).map( "longitude", longitude );

            Map<String, Object> data = new HashMap<String, Object>( 2 );
            data.put( "name", String.valueOf( i ) );
            data.put( "location", location );

            em.create( "store", data );
        }

        em.refreshIndex();

        //do a direct geo iterator test.  We need to make sure that we short circuit on the correct tile.

        float latitude = 48.38626f;
        float longtitude = 9.94175f;
        int distance = 1000;
        int limit = 8;

        {
            // QuerySlice slice = new QuerySlice( "location", 0 );
            // GeoIterator itr = new GeoIterator( new CollectionGeoSearch( 
            //     em, setup.getIbl(), setup.getCassSvc(), em.getApplicationRef(), "stores" ),
            //     limit, slice, "location", new Point( lattitude, longtitude ), distance );
            //
            // // check we got back all 500 entities
            // assertFalse( itr.hasNext() );
            //
            // List<String> cells = itr.getLastCellsSearched();
            // assertEquals( 1, cells.size() );
            // assertEquals( 4, cells.get( 0 ).length() );

            Query query = Query.fromQL( "select * where location within " 
                    + distance + " of " + longtitude + ", " + latitude );
            Results results = em.searchCollection( em.getApplicationRef(), "stores", query);

            assertFalse( results.isEmpty() );
            assertEquals( 1, results.size() );
        }

        {
            long startTime = System.currentTimeMillis();

            //now test at the EM level, there should be 0 results.
            Query query = new Query();

            query.addFilter( "location within 1000 of 48.38626, 9.94175" );
            query.setLimit( limit );

            Results results = em.searchCollection( em.getApplicationRef(), "stores", query );

            assertEquals( 0, results.size() );

            long endTime = System.currentTimeMillis();

            LOG.info( "Runtime took {} milliseconds to search", endTime - startTime );
        }
    }


    public Map<String, Object> getLocation( double latitude, double longitude ) throws Exception {
        Map<String, Object> latlong = new LinkedHashMap<String, Object>();
        latlong.put( "latitude", latitude );
        latlong.put( "longitude", longitude );
        return latlong;
    }


    public void updatePos( EntityManager em, EntityRef entity, double latitude, double longitude ) throws Exception {
        Map<String, Object> latlong = new LinkedHashMap<String, Object>();
        latlong.put( "latitude", latitude );
        latlong.put( "longitude", longitude );

        em.setProperty( entity, "location", latlong );
    }


    public void setPos( Map<String, Object> data, double latitude, double longitude ) {
        Map<String, Object> latlong = new LinkedHashMap<String, Object>();
        latlong.put( "latitude", latitude );
        latlong.put( "longitude", longitude );

        data.put( "location", latlong );
    }
}
