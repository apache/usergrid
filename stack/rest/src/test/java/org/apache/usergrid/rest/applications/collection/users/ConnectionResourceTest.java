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
package org.apache.usergrid.rest.applications.collection.users;


import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.endpoints.CollectionEndpoint;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * TODO: Document this
 */
public class ConnectionResourceTest extends AbstractRestIT {
    private static Logger log = LoggerFactory.getLogger( ConnectionResourceTest.class );


    @Test
    public void connectionsQueryTest() throws IOException {

        //create a peep
        Entity peep = new Entity();
        peep.put( "type", "chicken" );

        peep = this.app().collection( "peeps" ).post( peep );


        Entity todd = new Entity();
        todd.put( "username", "todd" );
        todd = this.app().collection( "users" ).post( todd );

        Entity scott = new Entity();
        scott.put( "username", "scott" );
        scott = this.app().collection( "users" ).post( scott );

        Entity objectOfDesire = new Entity();
        objectOfDesire.put( "codingmunchies", "doritoes" );
        objectOfDesire = this.app().collection( "snacks" ).post( objectOfDesire );
        refreshIndex();

        Entity toddWant = this.app().collection( "users" ).entity( todd ).collection( "likes" ).collection( "snacks" )
                              .entity( objectOfDesire ).post();
        assertNotNull( toddWant );

        try {

            this.app().collection( "users" ).entity( scott )
                .collection("likes").collection( "peeps" ).entity( peep ).get();
            fail( "This should throw an exception" );
        }
        catch ( NotFoundException uie ) {
            // Should return a 404 Not Found
        }
    }


    @Test
    public void connectionsLoopbackTest() throws IOException {

        // create entities thing1 and thing2
        Entity thing1 = new Entity();
        thing1.put( "name", "thing1" );
        thing1 = this.app().collection( "things" ).post( thing1 );

        Entity thing2 = new Entity();
        thing2.put( "name", "thing2" );
        thing2 = this.app().collection( "things" ).post( thing2 );

        refreshIndex();
        //create the connection: thing1 likes thing2
        this.app().collection( "things" ).entity( thing1 )
            .connection("likes").collection( "things" ).entity( thing2 ).post();
        refreshIndex();

        //test we have the "likes" in our connection meta data response
        thing1 = this.app().collection( "things" ).entity( thing1 ).get();
        //TODO this is ugly. revisit.
        String url = ( String ) ( ( Map<String, Object> ) ( ( Map<String, Object> ) thing1.get( "metadata" ) )
            .get( "connections" ) ).get( "likes" );
        assertNotNull( "Connection url returned with entity", url );

        //now that we know the URl is correct, follow it
        CollectionEndpoint likesEndpoint = new CollectionEndpoint( url, this.context(), this.app() );
        Collection likes = likesEndpoint.get();
        assertNotNull( likes );
        Entity likedEntity = likes.next();
        assertNotNull( likedEntity );

        //make sure the returned entity is thing2
        assertEquals( thing2.getUuid(), likedEntity.getUuid() );


        //now follow the loopback, which should be pointers to the other entity
        thing2 = this.app().collection( "things" ).entity( thing2 ).get();
        //TODO this is ugly. revisit.
        url = ( String ) ( ( Map<String, Object> ) ( ( Map<String, Object> ) thing2.get( "metadata" ) )
            .get( "connecting" ) ).get( "likes" );
        assertNotNull( "Connecting url returned with entity", url );

        CollectionEndpoint likedByEndpoint = new CollectionEndpoint( url, this.context(), this.app() );
        Collection likedBy = likedByEndpoint.get();
        assertNotNull( likedBy );
        Entity likedByEntity = likedBy.next();
        assertNotNull( likedByEntity );

        //make sure the returned entity is thing1
        assertEquals( thing1.getUuid(), likedByEntity.getUuid() );
    }


    /**
     * Ensure that the connected entity can be deleted properly after it has been connected to another entity
     */
    @Test //USERGRID-3011
    public void connectionsDeleteSecondEntityInConnectionTest() throws IOException {

        //Create 2 entities, thing1 and thing2
        Entity thing1 = new Entity();
        thing1.put( "name", "thing1" );
        thing1 = this.app().collection( "things" ).post( thing1 );

        Entity thing2 = new Entity();
        thing2.put( "name", "thing2" );
        thing2 = this.app().collection( "things" ).post( thing2 );

        refreshIndex();
        //create the connection: thing1 likes thing2
        this.app().collection( "things" ).entity( thing1 )
            .connection("likes").collection( "things" ).entity( thing2 ).post();
        //delete thing2
        this.app().collection( "things" ).entity( thing2 ).delete();

        refreshIndex();

        try {
            //attempt to retrieve thing1
            thing2 = this.app().collection( "things" ).entity( thing2 ).get();
            fail( "This should throw an exception" );
        }
        catch ( NotFoundException uie ) {
            // Should return a 404 Not Found
        }
    }


    /**
     * Ensure that the connecting entity can be deleted properly after a connection has been added
     */
    @Test //USERGRID-3011
    public void connectionsDeleteFirstEntityInConnectionTest() throws IOException {

        //Create 2 entities, thing1 and thing2
        Entity thing1 = new Entity();
        thing1.put( "name", "thing1" );
        thing1 = this.app().collection( "things" ).post( thing1 );

        Entity thing2 = new Entity();
        thing2.put( "name", "thing2" );
        thing2 = this.app().collection( "things" ).post( thing2 );

        refreshIndex();
        //create the connection: thing1 likes thing2
        this.app().collection( "things" ).entity( thing1 )
            .connection("likes").collection( "things" ).entity( thing2 ).post();
        //delete thing1
        this.app().collection( "things" ).entity( thing1 ).delete();

        refreshIndex();

        try {
            //attempt to retrieve thing1
            thing1 = this.app().collection( "things" ).entity( thing1 ).get();
            fail( "This should throw an exception" );
        }
        catch ( NotFoundException uie ) {
            // Should return a 404 Not Found
        }
    }


    /**
     * UERGRID-1018
     */
    @Test
    public void testRePostOrder() {

        Entity thing1 = new Entity();
        thing1.put( "name", "thing1" );

        final CollectionEndpoint collection = this.app().collection( "things" );

        thing1 = collection.post( thing1 );

        Entity thing2 = new Entity();
        thing2.put( "name", "thing2" );
        thing2 = collection.post( thing2 );

        Entity thing3 = new Entity();
        thing3.put( "name", "thing3" );
        thing3 = collection.post( thing3 );

        //now connect them

        //connect thing1 -> thing2

        final CollectionEndpoint connectionEndpoint = collection.entity( thing1 ).connection( "connectorder" );
        connectionEndpoint.entity( thing2 ).post();

        //connect thing1 -> thing3
        connectionEndpoint.entity( thing3 ).post();

        refreshIndex();

        //now do a GET, we should see thing2 then thing3

        final ApiResponse order1 = connectionEndpoint.get().getResponse();

        //now verify order
        verifyOrder( order1, thing3, thing2 );

        final QueryParameters queryParameters = new QueryParameters();
        queryParameters.setQuery( "select * order by modified desc" );

        final ApiResponse order1Query = connectionEndpoint.get( queryParameters ).getResponse();

        //now verify order
        verifyOrder( order1Query, thing3, thing2 );


        //now re-post thing 2 it should appear second
        connectionEndpoint.entity( thing2 ).post();

        refreshIndex();


        final ApiResponse order2 = connectionEndpoint.get().getResponse();

        //now verify order
        verifyOrder( order2, thing2, thing3 );

        final ApiResponse order2Query = connectionEndpoint.get( queryParameters ).getResponse();

        //now verify order
        verifyOrder( order2Query, thing3, thing2 );
    }


    /**
     * UERGRID-1018
     */
    @Test
    public void testRePutOrder() {

        Entity thing1 = new Entity();
        thing1.put( "name", "thing1" );

        final CollectionEndpoint collection = this.app().collection( "things" );

        thing1 = collection.post( thing1 );

        Entity thing2 = new Entity();
        thing2.put( "name", "thing2" );
        thing2 = collection.post( thing2 );


        Entity thing3 = new Entity();
        thing3.put( "name", "thing3" );
        thing3 = collection.post( thing3 );

        //now connect them

        //connect thing1 -> thing2

        final CollectionEndpoint connectionEndpoint = collection.entity( thing1 ).connection( "connectorder" );
        connectionEndpoint.entity( thing2 ).put( thing2 );

        //connect thing1 -> thing3
        connectionEndpoint.entity( thing3 ).put( thing3 );

        refreshIndex();

        //now do a GET, we should see thing2 then thing3

        final ApiResponse order1 = connectionEndpoint.get().getResponse();

        //now verify order
        verifyOrder( order1, thing3, thing2 );

        final QueryParameters queryParameters = new QueryParameters();
        queryParameters.setQuery( "select * order by modified desc" );

        final ApiResponse order1Query = connectionEndpoint.get( queryParameters ).getResponse();

        //now verify order
        verifyOrder( order1Query, thing3, thing2 );


        //now re-post thing 2 it should appear second
        connectionEndpoint.entity( thing2 ).put( thing2 );

        refreshIndex();

        final ApiResponse order2 = connectionEndpoint.get().getResponse();


        //now verify order
        verifyOrder( order2, thing2, thing3 );
    }


    /**
     * Verify our response
     */
    private void verifyOrder( final ApiResponse apiResponse, final Entity... verifyOrder ) {

        final List<Entity> responseEntities = apiResponse.getEntities();

        assertEquals( "Size should be equals", verifyOrder.length, responseEntities.size() );


        for ( int i = 0; i < verifyOrder.length; i++ ) {

            final Entity verifyEntity = verifyOrder[i];

            final Entity returned = responseEntities.get( i );

            assertEquals( "Should be the same entity", verifyEntity.getUuid(), returned.getUuid() );
        }
    }
}
