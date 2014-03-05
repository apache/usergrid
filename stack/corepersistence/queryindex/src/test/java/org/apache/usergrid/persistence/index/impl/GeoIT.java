/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

package org.apache.usergrid.persistence.index.impl;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.index.EntityCollectionIndexFactory;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.index.legacy.CoreApplication;
import org.apache.usergrid.persistence.index.legacy.CoreITSetup;
import org.apache.usergrid.persistence.index.legacy.CoreITSetupImpl;
import org.apache.usergrid.persistence.index.legacy.EntityManagerFacade;
import org.apache.usergrid.persistence.index.legacy.Point;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.query.EntityRef;
import org.apache.usergrid.persistence.query.Query;
import org.apache.usergrid.persistence.query.Results;
import org.apache.usergrid.persistence.query.SimpleEntityRef;
import org.apache.usergrid.utils.MapUtils;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(JukitoRunner.class)
@UseModules({ TestIndexModule.class })
public class GeoIT {
    private static final Logger LOG = LoggerFactory.getLogger( GeoIT.class );

    @ClassRule
    public static ElasticSearchRule es = new ElasticSearchRule();

    @ClassRule
    public static CassandraRule cass = new CassandraRule();

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;
    
    @ClassRule
    public static CoreITSetup setup = new CoreITSetupImpl();

    @Rule
    public CoreApplication app = new CoreApplication( setup );

    @Inject
    public EntityCollectionManagerFactory collectionManagerFactory;
    
    @Inject
    public EntityCollectionIndexFactory collectionIndexFactory;


    public GeoIT() {
        super();
    }


    @Test
    public void testGeo() throws Exception {
        LOG.info( "GeoIT.testGeo" );

        Id appId = new SimpleId("testGeo");
        Id orgId = new SimpleId("testOrganization");
        EntityManagerFacade em = new EntityManagerFacade( orgId, appId, 
            collectionManagerFactory, collectionIndexFactory );
        assertNotNull( em );

		// Two intersections two blocks apart
        final Point folsomAnd7th =    new Point( 37.774277, -122.404744 );
        final Point folsomAndBryant = new Point( 37.776753, -122.407846 );

		// and about 50 kilometers away...
		// 260 Sheridan Ave, Palo Alto, CA
	 	final Point paloalto = new Point( 37.426373, -122.14108 ); 

        Map<String, Object> properties = new LinkedHashMap<String, Object>() {{
            put( "username", "edanuff" );
            put( "email", "ed@anuff.com" );
            put( "location", new HashMap<String, Object>() {{
                put("latitude", folsomAndBryant.getLat() );
                put("longitude", folsomAndBryant.getLon() );
            }});
        }};
	
        Entity user = em.create( "user", properties );
        assertNotNull( user );
		LOG.info( user.toString() );

		// Folsom and 7th more than 100 meters from Folson and Bryant
		Query q = Query.fromQL("location within 100 of " 
			+ folsomAnd7th.getLat() + "," + folsomAnd7th.getLon() + " limit 100");
		Results results = em.searchCollection(null, "users", q);
        assertEquals( 0, results.size() );

		// but within 400 meters
		q = Query.fromQL("location within 400 of " 
			+ folsomAnd7th.getLat() + "," + folsomAnd7th.getLon() + " limit 100");
		results = em.searchCollection(null, "users", q);
        assertEquals( 1, results.size() );

		// move user to Palo Alto, 50km away 
        updatePos( em, new SimpleEntityRef( user.getId(), user.getVersion() ), 
			paloalto.getLat(), paloalto.getLon() );

		// user no longer within 200m of that San Francico intersection  
		q = Query.fromQL("location within 200 of " + folsomAndBryant.getLat() 
				+ "," + folsomAndBryant.getLon() + " limit 100");
		results = em.searchCollection(null, "users", q);
        assertEquals( 0, results.size() );

		// move user to the other SF intersection
        updatePos( em, user, folsomAnd7th.getLat(), folsomAnd7th.getLon() );

		// now they are close to Folsom and Bryant
		q = Query.fromQL("location within 1000 of " 
				+ folsomAndBryant.getLat() + "," + folsomAndBryant.getLon() + " limit 100");
		results = em.searchCollection(null, "users", q);
        assertEquals( 1, results.size() );

        // check at globally large distance
		q = Query.fromQL("location within " + Integer.MAX_VALUE + " of " 
				+ folsomAndBryant.getLat() + "," + folsomAndBryant.getLon() + " limit 100");
		results = em.searchCollection(null, "users", q);
        assertEquals( 1, results.size() );

        // create a new entity so we have 2
        LinkedHashMap<String, Object> properties2 = new LinkedHashMap<String, Object>() {{
        	put( "username", "sganyo" );
        	put( "email", "sganyo@anuff.com" );
			put( "location", new HashMap<String, Object>() {{
				put( "latitude", 31.1 ); // 31.1, 121.2 is shanghai
				put( "longitude", 121.2 ); 
			}});
		}};
        Entity user2 = em.create( "user", properties2 );
        assertNotNull( user2 );
        
		q = Query.fromQL("location within 10000 of " 
				+ folsomAndBryant.getLat() + "," + folsomAndBryant.getLon() + " limit 100");
		results = em.searchCollection(null, "users", q);

        assertEquals( 1, results.size() );

        // check at globally large distance
		q = Query.fromQL("location within " + Integer.MAX_VALUE + " of " 
				+ folsomAndBryant.getLat() + "," + folsomAndBryant.getLon() + " limit 100");
		results = em.searchCollection(null, "users", q);
		
        assertEquals( 2, results.size() );

        // check at globally large distance (center point close to other entity)
        Point shanghai = new Point( 31.14, 121.27 );

		q = Query.fromQL("location within " + Integer.MAX_VALUE + " of " 
				+ shanghai.getLat() + "," + shanghai.getLon() + " limit 100");
		results = em.searchCollection(null, "users", q);

        assertEquals( 2, results.size() );

        Results emSearchResults = em.searchCollection( em.getApplicationRef(), "users",
            Query.fromQL( "location within 1000 of 37.776753, -122.407846" ) );

        assertEquals( 1, emSearchResults.size() );

        updatePos( em, user, folsomAndBryant.getLat(), folsomAndBryant.getLon());

        Point paloaltoCaltran = new Point( 37.428526, -122.140916 );

		q = Query.fromQL("location within 1000 of " 
				+ paloaltoCaltran.getLat() + "," + paloaltoCaltran.getLon() + " limit 100");
		results = em.searchCollection(null, "users", q);

        assertEquals( 0, results.size() );

        emSearchResults = em.searchCollection( em.getApplicationRef(), "users",
                Query.fromQL( "location within 1000 of 37.428526, -122.140916" ) );
        assertEquals( 0, emSearchResults.size() );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "name", "Brickhouse" );
        properties.put( "address", "426 Brannan Street" );
        properties.put( "location", getLocation( 37.779632, -122.395131 ) );

        Entity restaurant = em.create( "restaurant", properties );
        assertNotNull( restaurant );

		// TODO: update with new Core Persistence graph API

//        em.createConnection( user, "likes", restaurant );
//
//        emSearchResults =
//                em.searchConnectedEntities( user, Query.fromQL( "location within 2000 of 37.776753, -122.407846" ) );
//        assertEquals( 1, emSearchResults.size() );
//
//        emSearchResults =
//                em.searchConnectedEntities( user, Query.fromQL( "location within 1000 of 37.776753, -122.407846" ) );
//        assertEquals( 0, emSearchResults.size() );
    }


    @Test
    public void testPointPaging() throws Exception {

        Id appId = new SimpleId("testGeo");
        Id orgId = new SimpleId("testOrganization");
        EntityManagerFacade em = new EntityManagerFacade( orgId, appId, 
            collectionManagerFactory, collectionIndexFactory );
        assertNotNull( em );

        // save objects in a diagonal line from -90 -180 to 90 180

        int numEntities = 10;

        float minLattitude = -90;
        float maxLattitude = 90;
        float minLongitude = -180;
        float maxLongitude = 180;

        float lattitudeDelta = ( maxLattitude - minLattitude ) / numEntities;

        float longitudeDelta = ( maxLongitude - minLongitude ) / numEntities;

        for ( int i = 0; i < numEntities; i++ ) {
            float lattitude = minLattitude + lattitudeDelta * i;
            float longitude = minLongitude + longitudeDelta * i;

            Map<String, Float> location = MapUtils.hashMap( 
                    "latitude", lattitude ).map( "longitude", longitude );

            Map<String, Object> data = new HashMap<String, Object>( 2 );
            data.put( "name", String.valueOf( i ) );
            data.put( "location", location );

            em.create( "dog", data );
        }

        // earth's circumference is 40075km; up it to 50000km, to be safe
        Query query = new Query();
        query.addFilter( "location within 50000000 of -90, -180" );
        query.setLimit( 100 );

        int count = 0;
        Results results;

        do {
            results = em.searchCollection( em.getApplicationRef(), "dogs", query );

            for ( Entity entity : results.getEntities() ) {
                count++;
            }

            // set for the next "page"
            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        // check we got back all entities
        assertEquals( numEntities, count );
    }


    @Test
    public void testSamePointPaging() throws Exception {

        Id appId = new SimpleId("testGeo");
        Id orgId = new SimpleId("testOrganization");
        EntityManagerFacade em = new EntityManagerFacade( orgId, appId, 
            collectionManagerFactory, collectionIndexFactory );
        assertNotNull( em );

        // save objects in a diagonal line from -90 -180 to 90 180

        // TODO: use a larger count here
        int numEntities = 10;

        for ( int i = 0; i < numEntities; i++ ) {
            Map<String, Object> data = new HashMap<String, Object>( 2 );
            data.put( "name", String.valueOf( i ) );
            setPos( data, 0, 0 );

            em.create( "store", data );
        }

        Query query = new Query();
        // earth's circumference is 40,075 kilometers. Up it to 50,000kilometers
        // just to be save
        query.addFilter( "location within 50000000 of 0, 0" );
        query.setLimit( 10 );

        int count = 0;
        Results results;

        do {
            results = em.searchCollection( em.getApplicationRef(), "stores", query );
            for ( Entity entity : results.getEntities() ) {
                count++;
            }

            // set for the next "page"
            query.setCursor( results.getCursor() );
        }
        while ( results.getCursor() != null );

        // check we got back all entities
        assertEquals( numEntities, count );
    }


    @Test
    public void testDistanceByLimit() throws Exception {

        Id appId = new SimpleId("testGeo");
        Id orgId = new SimpleId("testOrganization");
        EntityManagerFacade em = new EntityManagerFacade( orgId, appId, 
            collectionManagerFactory, collectionIndexFactory );
        assertNotNull( em );

        // save objects in a diagonal line from -90 -180 to 90 180

        // TODO: use a larger count here
        int numEntities = 10;

        float minLattitude = -90;
        float maxLattitude = 90;
        float minLongitude = -180;
        float maxLongitude = 180;

        float lattitudeDelta = ( maxLattitude - minLattitude ) / numEntities;
        float longitudeDelta = ( maxLongitude - minLongitude ) / numEntities;

        for ( int i = 0; i < numEntities; i++ ) {
            final float latitude = minLattitude + lattitudeDelta * i;
            final float longitude = minLongitude + longitudeDelta * i;

            Map<String, Object> location = new HashMap<String, Object>() {{
                put("latitude", latitude);
                put("longitude", longitude);
            }}; 

            Map<String, Object> data = new HashMap<String, Object>( 2 );
            data.put( "name", String.valueOf( i ) );
            data.put( "location", location );

            em.create( "car", data );
        }

        // earth's circumference is 40075km; up it to 50,000km, just to be safe.
        Query query = new Query();
        query.addFilter( "location within " + (50000 * 1000) + " of -90, -180" );
        query.setLimit( 100 );

        int count = 0;
        do {
            Results results = em.searchCollection( em.getApplicationRef(), "cars", query );
            for ( Entity entity : results.getEntities() ) {
                count++;
            }
        }
        while ( query.getCursor() != null );

        // check we got back all entities
        assertEquals( numEntities, count );
    }


    @Test
    public void testGeoWithIntersection() throws Exception {

        Id appId = new SimpleId("testGeo");
        Id orgId = new SimpleId("testOrganization");
        EntityManagerFacade em = new EntityManagerFacade( orgId, appId, 
            collectionManagerFactory, collectionIndexFactory );
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

            Entity e = em.create( "puppy", data );

            created.add( e );
        }

        int startDelta = size - min;

        //    String queryString = String.format("select * where location within 100 of 0,
        // 0 and index >= %d and index < %d order by index",min, max);

        String queryString = String.format( 
                "select * where index >= %d and index < %d order by index", min, max );

        Query query = Query.fromQL( queryString );

        Results r;
        int count = 0;

        do {

            r = em.searchCollection( em.getApplicationRef(), "puppies", query );

            for ( Entity e : r.getEntities() ) {
                assertEquals( created.get( startDelta + count ), e );
                count++;
            }

            query.setCursor( r.getCursor() );
        }
        while ( r.hasCursor() );

        assertEquals( startDelta - ( size - max ), count );
    }


//    @Test
//    public void testDenseSearch() throws Exception {
//
//        Id appId = new SimpleId("testGeo");
//        Id orgId = new SimpleId("testOrganization");
//        EntityManagerFacade em = new EntityManagerFacade( orgId, appId, 
//            collectionManagerFactory, collectionIndexFactory );
//        assertNotNull( em );
//
//        // save objects in a diagonal line from -90 -180 to 90 180
//
//        int numEntities = 500;
//
//        float minLattitude = 48.32455f;
//        float maxLattitude = 48.46481f;
//        float minLongitude = 9.89561f;
//        float maxLongitude = 10.0471f;
//
//        float lattitudeDelta = ( maxLattitude - minLattitude ) / numEntities;
//
//        float longitudeDelta = ( maxLongitude - minLongitude ) / numEntities;
//
//        for ( int i = 0; i < numEntities; i++ ) {
//            float lattitude = minLattitude + lattitudeDelta * i;
//            float longitude = minLongitude + longitudeDelta * i;
//
//            Map<String, Float> location = MapUtils.hashMap( "latitude", lattitude ).map( "longitude", longitude );
//
//            Map<String, Object> data = new HashMap<String, Object>( 2 );
//            data.put( "name", String.valueOf( i ) );
//            data.put( "location", location );
//
//            em.create( "store", data );
//        }
//
//        //do a direct geo iterator test.  We need to make sure that we short circuit on the correct tile.
//
//        float lattitude = 48.38626f;
//        float longtitude = 9.94175f;
//        int distance = 1000;
//        int limit = 8;
//
//
//        QuerySlice slice = new QuerySlice( "location", 0 );
//
//        GeoIterator itr = new GeoIterator(
//                new CollectionGeoSearch( em, setup.getIbl(), setup.getCassSvc(), em.getApplicationRef(), "stores" ),
//                limit, slice, "location", new Point( lattitude, longtitude ), distance );
//
//
//        // check we got back all 500 entities
//        assertFalse( itr.hasNext() );
//
//        List<String> cells = itr.getLastCellsSearched();
//
//        assertEquals( 1, cells.size() );
//
//        assertEquals( 4, cells.get( 0 ).length() );
//
//
//        long startTime = System.currentTimeMillis();
//
//        //now test at the EM level, there should be 0 results.
//        Query query = new Query();
//
//        query.addFilter( "location within 1000 of 48.38626, 9.94175" );
//        query.setLimit( 8 );
//
//
//        Results results = em.searchCollection( em.getApplicationRef(), "stores", query );
//
//        assertEquals( 0, results.size() );
//
//        long endTime = System.currentTimeMillis();
//
//        LOG.info( "Runtime took {} milliseconds to search", endTime - startTime );
//    }


    public Map<String, Object> getLocation( double latitude, double longitude ) throws Exception {
        Map<String, Object> latlong = new LinkedHashMap<String, Object>();
        latlong.put( "latitude", latitude );
        latlong.put( "longitude", longitude );
        return latlong;
    }


    public void updatePos( EntityManagerFacade em, EntityRef ref, double lat, double lon) throws Exception {
        em.setProperty( ref, "location", lat, lon );
	}

    public void updatePos( EntityManagerFacade em, Entity e, double lat, double lon) throws Exception {
        em.setProperty( new SimpleEntityRef( e.getId(), e.getVersion()), "location", lat, lon );
    }


    public void setPos( Map<String, Object> data, double latitude, double longitude ) {
        Map<String, Object> latlong = new LinkedHashMap<String, Object>();
        latlong.put( "latitude", latitude );
        latlong.put( "longitude", longitude );
        data.put( "location", latlong );
    }
}
