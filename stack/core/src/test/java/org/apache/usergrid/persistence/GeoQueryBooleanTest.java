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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.apache.usergrid.AbstractCoreIT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;



public class GeoQueryBooleanTest extends AbstractCoreIT {
    private static final Logger log = LoggerFactory.getLogger( GeoQueryBooleanTest.class );


    public GeoQueryBooleanTest() {
        super();
    }


    @Test
    public void testGeoQueryWithOr() throws Exception {

        log.info( "GeoQueryBooleanTest.testGeoQueryWithOr" );


        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        // create two users at a location

        Map<String, Object> properties = new LinkedHashMap<String, Object>() {{
            put( "username", "ed" );
            put( "employer", "Apigee" );
            put( "email", "ed@example.com" );
            put( "location", new LinkedHashMap<String, Object>() {{
                put("latitude", 37.776753 );
                put("longitude", -122.407846 );
            }} );
        }};

        Entity user1 = em.create( "user", properties );
        assertNotNull( user1 );

        properties = new LinkedHashMap<String, Object>() {{
            put( "username", "fred" );
            put( "employer", "Microsoft" );
            put( "email", "fred@example.com" );
            put( "location", new LinkedHashMap<String, Object>() {{
                put("latitude", 37.776753 );
                put("longitude", -122.407846 );
            }} );
        }};

        Entity user2 = em.create( "user", properties );
        assertNotNull( user2 );

        app.refreshIndex();

        // define center point about 300m from that location
        final double lat = 37.774277;
        final double lon = -122.404744 ;

        Query query = Query.fromQL( "select * where location within 400 of "
                                    + lat + "," + lon);
        Results listResults = em.searchCollection( em.getApplicationRef(), "users", query );
        assertEquals( 2, listResults.size() );

        query = Query.fromQL( "select * where employer='Apigee' or location within 100 of "
                                    + lat + "," + lon);
        listResults = em.searchCollection( em.getApplicationRef(), "users", query );

        // no results because geo filter applied after query even in the case or 'or'
        assertEquals( 1, listResults.size() );

        query = Query.fromQL( "select * where employer='Apigee' or location within 400 of "
                                    + lat + "," + lon);
        listResults = em.searchCollection( em.getApplicationRef(), "users", query );

        // only one result because geo filter applied after query even in the case or 'or'
        assertEquals( 2, listResults.size() );
    }


    @Test
    //@Ignore // work in progress
    public void testGeoQueryWithNot() throws Exception {

        log.info( "GeoQueryBooleanTest.testGeoQueryWithOr" );

        EntityManager em = app.getEntityManager();

        assertNotNull( em );

        // define two users at a location

        Map<String, Object> properties = new LinkedHashMap<String, Object>() {{
            put( "username", "bart" );
            put( "email", "bart@example.com" );
            put( "block", new ArrayList<Object>() {{
                add( new LinkedHashMap<String, Object>() {{ put("name", "fred"); }});
                add( new LinkedHashMap<String, Object>() {{ put("name", "gertrude"); }});
                add( new LinkedHashMap<String, Object>() {{ put("name", "mina"); }});
            }});
            put( "blockedBy", new ArrayList<Object>() {{
                add( new LinkedHashMap<String, Object>() {{ put("name", "gertrude"); }});
                add( new LinkedHashMap<String, Object>() {{ put("name", "isabell"); }});
            }});
            put( "location", new LinkedHashMap<String, Object>() {{
                put("latitude", 37.776753 );
                put("longitude", -122.407846 );
            }});
        }};

        Entity userBart = em.create( "user", properties );
        assertNotNull( userBart );

        properties = new LinkedHashMap<String, Object>() {{
            put( "username", "fred" );
            put( "email", "fred@example.com" );
            put( "block", new ArrayList<Object>() {{
                add( new LinkedHashMap<String, Object>() {{ put("name", "steve"); }});
                add( new LinkedHashMap<String, Object>() {{ put("name", "mina"); }});
            }});
            put( "blockedBy", new ArrayList<Object>() {{
                add( new LinkedHashMap<String, Object>() {{ put("name", "bart"); }});
                add( new LinkedHashMap<String, Object>() {{ put("name", "beth"); }});
            }});
            put( "location", new LinkedHashMap<String, Object>() {{
                put("latitude", 37.776753 );
                put("longitude", -122.407846 );
            }} );
        }};

        Entity userFred = em.create( "user", properties );
        assertNotNull( userFred );

        app.refreshIndex();

        // define center point about 300m from that location
        final double lat = 37.774277;
        final double lon =  -122.404744 ;

        // one user within 400 meters IS NOT blocked by bart
        Query query = Query.fromQL(
            "select * where NOT blockedBy.name='bart' and location within 400 of "
               + lat + "," + lon);
        Results listResults = em.searchCollection( em.getApplicationRef(), "users", query );
        assertEquals( 1, listResults.size() );

        // one user within 400 meters IS blocked by bart
        query = Query.fromQL(
            "select * where blockedBy.name='bart' and location within 400 of "
               + lat + "," + lon);
        listResults = em.searchCollection( em.getApplicationRef(), "users", query );
        assertEquals( 1, listResults.size() );

     }

}
