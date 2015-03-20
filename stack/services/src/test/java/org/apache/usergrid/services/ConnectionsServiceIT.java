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


import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import org.apache.usergrid.persistence.Entity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class ConnectionsServiceIT extends AbstractServiceIT {
    private static final Logger logger = LoggerFactory.getLogger( ConnectionsServiceIT.class );

    @SuppressWarnings("rawtypes")
    @Test
    public void testUserConnections() throws Exception {
        app.put( "username", "conn-user1" );
        app.put( "email", "conn-user1@apigee.com" );

        Entity user1 = app.testRequest( ServiceAction.POST, 1, "users" ).getEntity();
        assertNotNull( user1 );

        app.testRequest( ServiceAction.GET, 1, "users", "conn-user1" );

        app.put( "username", "conn-user2" );
        app.put( "email", "conn-user2@apigee.com" );

        Entity user2 = app.testRequest( ServiceAction.POST, 1, "users" ).getEntity();
        assertNotNull( user2 );

        //POST users/conn-user1/manages/user2/conn-user2
        app.testRequest( ServiceAction.POST, 1, "users", "conn-user1", "manages", "users", "conn-user2" );
        //POST users/conn-user1/reports/users/conn-user2
        app.testRequest( ServiceAction.POST, 1, "users", "conn-user1", "reports", "users", "conn-user2" );

        app.testRequest( ServiceAction.GET, 1, "users", "conn-user1" );
        app.testRequest( ServiceAction.GET, 1, "users", "conn-user2" );

        //DELETE users/conn-user1/manages/user2/conn-user2 (qualified by collection type on second entity)
        app.testRequest( ServiceAction.DELETE, 1, "users", "conn-user1", "manages", "users", "conn-user2" );

        // "manages" connection removed from both entities
        user1 = app.testRequest( ServiceAction.GET, 1, "users", "conn-user1" ).getEntities().get( 0 );
        assertFalse( ( ( Map ) user1.getMetadata( "connections" ) ).containsKey( "manages" ) );
        user2 = app.testRequest( ServiceAction.GET, 1, "users", "conn-user2" ).getEntities().get( 0 );
        assertFalse( ( ( Map ) user2.getMetadata( "connecting" ) ).containsKey( "manages" ) );


        //DELETE /users/conn-user1/reports/conn-user2 (not qualified by collection type on second entity)
//        app.testRequest( ServiceAction.DELETE, 0, "users", "conn-user1", "reports", "conn-user2" );

        // "reports" connection still exists on both entities
        user1 = app.testRequest( ServiceAction.GET, 1, "users", "conn-user1" ).getEntities().get( 0 );
        assertTrue( ( ( Map ) user1.getMetadata( "connections" ) ).containsKey( "reports" ) );
        user2 = app.testRequest( ServiceAction.GET, 1, "users", "conn-user2" ).getEntities().get( 0 );
        assertTrue( ( ( Map ) user2.getMetadata( "connecting" ) ).containsKey( "reports" ) );

        // POST users/conn-user1/manages/user2/user
        app.put( "username", "conn-user3" );
        app.put( "email", "conn-user3@apigee.com" );
        app.testRequest( ServiceAction.POST, 1, "users", "conn-user1", "manages", "user" );
    }

    @Test
    public void testNonExistentEntity() throws Exception {

      app.put( "name", "foo" );
      Entity foo = app.testRequest( ServiceAction.POST, 1, "foos" ).getEntity();
      assertNotNull( foo );

      app.clear();

      app.put( "name", "bar" );
      Entity bar = app.testRequest( ServiceAction.POST, 1, "bars" ).getEntity();
      assertNotNull( bar );

        setup.getEntityIndex().refresh();


        //POST users/conn-user1/user2/UUID
      app.testRequest( ServiceAction.POST, 1, "foos", "foo", "bars", bar.getUuid() ); // should succeed

        setup.getEntityIndex().refresh();


        try {
        //POST users/conn-user1/user2/bar
        app.testRequest( ServiceAction.POST, 1, "foos", "foo", "bars", "bar" );
        Assert.fail();
      } catch (Exception e) {
        // ok
      }
  }
}
