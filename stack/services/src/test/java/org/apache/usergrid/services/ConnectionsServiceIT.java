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


import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Query;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.Assert.*;



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

        //TODO: test fails because the connections metadata is null. When before, data would still be returned.
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

    @SuppressWarnings("rawtypes")
    @Test
    public void testUserConnectionsCursor() throws Exception {
        app.put("username", "conn-user1");
        app.put("email", "conn-user1@apigee.com");

        Entity user1 = app.testRequest(ServiceAction.POST, 1, "users").getEntity();
        assertNotNull(user1);

        app.testRequest(ServiceAction.GET, 1, "users", "conn-user1");

        app.put("username", "conn-user2");
        app.put("email", "conn-user2@apigee.com");

        Entity user2 = app.testRequest(ServiceAction.POST, 1, "users").getEntity();
        assertNotNull(user2);


        app.put("username", "conn-user3");
        app.put("email", "conn-user3@apigee.com");

        Entity user3 = app.testRequest(ServiceAction.POST, 1, "users").getEntity();
        assertNotNull(user3);


        //POST users/conn-user2/manages/user2/conn-user1
        app.testRequest(ServiceAction.POST, 1, "users", "conn-user2", "likes", "users", "conn-user1");
        //POST users/conn-user3/reports/users/conn-user1
        app.testRequest(ServiceAction.POST, 1, "users", "conn-user3", "likes", "users", "conn-user1");

        Query query = new Query().fromQLNullSafe("");
        query.setLimit(1);

        //the result should return a valid cursor.
        ServiceResults result = app.testRequest(ServiceAction.GET, 1, "users", "conn-user1", "connecting", "likes",query);
        assertNotNull(result.getCursor());
        String enityName1 = result.getEntity().getProperty("email").toString();

        Query newquery = new Query().fromQLNullSafe("");
        query.setCursor(result.getCursor());
        result = app.testRequest(ServiceAction.GET,1,"users","conn-user1","connecting","likes",query);
        String enityName2 = result.getEntity().getProperty("email").toString();

        //ensure the two entities returned in above requests are different.
        assertNotEquals(enityName1,enityName2);

        newquery = new Query().fromQLNullSafe("");
        query.setCursor(result.getCursor());
        result = app.testRequest(ServiceAction.GET,0,"users","conn-user1","connecting","likes",query);
        //return empty cursor when no more entitites found.
        assertNull(result.getCursor());

        //DELETE users/conn-user1/manages/user2/conn-user2 (qualified by collection type on second entity)
        app.testRequest(ServiceAction.DELETE, 1, "users", "conn-user2", "likes", "users", "conn-user1");

        app.testRequest(ServiceAction.GET,1,"users","conn-user1","connecting","likes");


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

        setup.getEntityIndex().refresh(app.getId());


        //POST users/conn-user1/user2/UUID
      app.testRequest( ServiceAction.POST, 1, "foos", "foo", "bars", bar.getUuid() ); // should succeed

        setup.getEntityIndex().refresh(app.getId());


        try {
        //POST users/conn-user1/user2/bar
        app.testRequest( ServiceAction.POST, 1, "foos", "foo", "bars", "bar" );
        Assert.fail();
      } catch (Exception e) {
        // ok
      }
  }
}
