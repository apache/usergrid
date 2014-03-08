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
package org.apache.usergrid.services;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Query;

import static org.junit.Assert.assertNotNull;


public class ServiceInvocationIT extends AbstractServiceIT {
    private static final Logger LOG = LoggerFactory.getLogger( ServiceInvocationIT.class );


    @Test
    public void testServices() throws Exception {
        LOG.info( "testServices" );

        app.put( "username", "edanuff" );
        app.put( "email", "ed@anuff.com" );

        Entity user = app.testRequest( ServiceAction.POST, 1, "users" ).getEntity();
        assertNotNull( user );

        app.testRequest( ServiceAction.GET, 1, "users" );

        app.testRequest( ServiceAction.GET, 1, "users", user.getUuid() );

        app.testRequest( ServiceAction.GET, 1, "users", Query.fromQL( "select * where username='edanuff'" ) );

        app.put( "foo", "bar" );

        app.testRequest( ServiceAction.PUT, 1, "users", user.getUuid() );

        app.put( "name", "nico" );

        app.testRequest( ServiceAction.POST, 1, "cats" );

        app.testRequest( ServiceAction.GET, 0, "users", user.getUuid(), "messages" );

        app.testRequest( ServiceAction.GET, 0, "users", Query.fromQL( "select * where username='edanuff'" ),
                "messages" );

        Entity cat = app.doCreate( "cat", "dylan" );

        app.testRequest( ServiceAction.GET, 2, "cats" );

        app.testRequest( ServiceAction.GET, 1, "cats", Query.fromQL( "select * where name='dylan'" ) );

        app.testRequest( ServiceAction.POST, 1, null, "users", "edanuff", "likes", cat.getUuid() );

        Entity restaurant = app.doCreate( "restaurant", "Brickhouse" );

        app.createConnection( user, "likes", restaurant );

        restaurant = app.doCreate( "restaurant", "Axis Cafe" );

        app.testRequest( ServiceAction.GET, 2, "restaurants" );

        app.testRequest( ServiceAction.POST, 1, "users", user.getUuid(), "connections", "likes", restaurant.getUuid() );

        app.testRequest( ServiceAction.GET, 1, "users", "edanuff", "likes", "cats" );

        app.testRequest( ServiceAction.GET, 3, "users", "edanuff", "likes" );

        app.testRequest( ServiceAction.GET, 2, "users", "edanuff", "likes", "restaurants" );

        app.testRequest( ServiceAction.GET, 1, "users", "edanuff", "likes", "restaurants",
                Query.fromQL( "select * where name='Brickhouse'" ) );

        app.testRequest( ServiceAction.GET, 1, "users", "edanuff", "likes",
                Query.fromQL( "select * where name='axis*'" ) );

        app.testRequest( ServiceAction.GET, 3, null, "users", "edanuff", "connections" );

        app.put( "color", "blacknwhite" );

        app.testRequest( ServiceAction.PUT, 1, "users", "edanuff", "likes", cat.getUuid() );

        app.put( "eats", "petfood" );

        app.testRequest( ServiceAction.PUT, 1, "users", "edanuff", "likes", "cats", "dylan" );

        app.put( "Todays special", "Coffee" );

        app.testRequest( ServiceAction.PUT, 1, "users", "edanuff", "likes", "restaurants",
                Query.fromQL( "select * where name='Brickhouse'" ) );

        app.testRequest( ServiceAction.DELETE, 1, null, "users", user.getUuid(), "connections", "likes",
                restaurant.getUuid() );

        app.testRequest( ServiceAction.GET, 2, null, "users", "edanuff", "connections" );

        app.testRequest( ServiceAction.GET, 1, null, "users", "edanuff", "likes", "restaurants" );

        UUID uuid = UUID.randomUUID();
        app.put( "visits", 5 );
        app.testRequest( ServiceAction.PUT, 1, "devices", uuid );
    }


    @Test
    public void testBatchCreate() throws Exception {
        List<Map<String, Object>> batch = new ArrayList<Map<String, Object>>();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "test_user_1" );
        properties.put( "email", "user1@test.com" );
        batch.add( properties );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "test_user_2" );
        batch.add( properties );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "test_user_3" );
        batch.add( properties );

        Entity user = app.testBatchRequest( ServiceAction.POST, 3, batch, "users" ).getEntity();
        assertNotNull( user );
    }
}
