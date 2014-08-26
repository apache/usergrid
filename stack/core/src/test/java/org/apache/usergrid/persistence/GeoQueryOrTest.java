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


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.persistence.geo.model.Point;
import org.apache.usergrid.persistence.index.query.Query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@Concurrent()
public class GeoQueryOrTest extends AbstractCoreIT {
    private static final Logger LOG = LoggerFactory.getLogger( GeoQueryOrTest.class );


    public GeoQueryOrTest() {
        super();
    }


    @Test
    public void testGeoQueryWithOr() throws Exception {

        LOG.info( "GeoIT.testGeoQueryWithOr" );

        UUID applicationId = setup.createApplication( "testOrganization", "testGeoQueryWithOr" );
        assertNotNull( applicationId );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
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

        em.refreshIndex();

        // define center point about 300m from that location
        Point center = new Point( 37.774277, -122.404744 );

        Query query = Query.fromQL( "select * where location within 400 of " 
                                    + center.getLat() + "," + center.getLon());
        Results listResults = em.searchCollection( em.getApplicationRef(), "users", query );
        assertEquals( 2, listResults.size() );

        query = Query.fromQL( "select * where employer='Apigee' or location within 100 of " 
                                    + center.getLat() + "," + center.getLon());
        listResults = em.searchCollection( em.getApplicationRef(), "users", query );

        // no results because geo filter applied after query even in the case or 'or'
        assertEquals( 0, listResults.size() );

        query = Query.fromQL( "select * where employer='Apigee' or location within 400 of " 
                                    + center.getLat() + "," + center.getLon());
        listResults = em.searchCollection( em.getApplicationRef(), "users", query );

        // only one result because geo filter applied after query even in the case or 'or'
        assertEquals( 1, listResults.size() );
    }

}
