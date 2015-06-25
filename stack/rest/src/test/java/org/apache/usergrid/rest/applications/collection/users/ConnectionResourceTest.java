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


import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.endpoints.CollectionEndpoint;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class ConnectionResourceTest extends AbstractRestIT {
    private static Logger log = LoggerFactory.getLogger(ConnectionResourceTest.class);

    @Test
    public void connectionsQueryTest() throws IOException {

        //create a peep
        Entity peep = new Entity();
        peep.put("type", "chicken");

        peep = this.app().collection("peeps").post(peep);


        Entity todd = new Entity();
        todd.put("username", "todd");
        todd = this.app().collection("users").post(todd);

        Entity scott = new Entity();
        scott.put("username", "scott");
        scott = this.app().collection("users").post(scott);

        Entity objectOfDesire = new Entity();
        objectOfDesire.put("codingmunchies", "doritoes");
        objectOfDesire = this.app().collection("snacks").post(objectOfDesire);
        refreshIndex();

        Entity toddWant = this.app().collection("users").entity(todd).collection("likes").collection("snacks").entity(objectOfDesire).post();
        assertNotNull(toddWant);

        try {

            this.app().collection("users").entity(scott).collection("likes").collection("peeps").entity(peep).get();
            fail("This should throw an exception");
        } catch (UniformInterfaceException uie) {
            // Should return a 404 Not Found
            assertEquals(404, uie.getResponse().getStatus());
        }


    }


    @Test
    public void connectionsLoopbackTest() throws IOException {

        // create entities thing1 and thing2
        Entity thing1 = new Entity();
        thing1.put("name", "thing1");
        thing1 = this.app().collection("things").post(thing1);

        Entity thing2 = new Entity();
        thing2.put("name", "thing2");
        thing2 = this.app().collection("things").post(thing2);

        refreshIndex();
        //create the connection: thing1 likes thing2
        this.app().collection("things").entity(thing1).connection("likes").collection("things").entity(thing2).post();
        refreshIndex();

        //test we have the "likes" in our connection meta data response
        thing1 = this.app().collection("things").entity(thing1).get();
        //TODO this is ugly. revisit.
        String url = (String) ((Map<String, Object>) ((Map<String, Object>) thing1.get("metadata")).get("connections")).get("likes");
        assertNotNull("Connection url returned with entity", url);

        //now that we know the URl is correct, follow it
        CollectionEndpoint likesEndpoint = new CollectionEndpoint(url, this.context(), this.app());
        Collection likes = likesEndpoint.get();
        assertNotNull(likes);
        Entity likedEntity = likes.next();
        assertNotNull(likedEntity);

        //make sure the returned entity is thing2
        assertEquals(thing2.getUuid(), likedEntity.getUuid());


        //now follow the loopback, which should be pointers to the other entity
        thing2 = this.app().collection("things").entity(thing2).get();
        //TODO this is ugly. revisit.
        url = (String) ((Map<String, Object>) ((Map<String, Object>) thing2.get("metadata")).get("connecting")).get("likes");
        assertNotNull("Connecting url returned with entity", url);

        CollectionEndpoint likedByEndpoint = new CollectionEndpoint(url, this.context(), this.app());
        Collection likedBy = likedByEndpoint.get();
        assertNotNull(likedBy);
        Entity likedByEntity = likedBy.next();
        assertNotNull(likedByEntity);

        //make sure the returned entity is thing1
        assertEquals(thing1.getUuid(), likedByEntity.getUuid());

    }


    /**
     * Ensure that the connected entity can be deleted
     * properly after it has been connected to another entity
     *
     * @throws IOException
     */
    @Test //USERGRID-3011
    public void connectionsDeleteSecondEntityInConnectionTest() throws IOException {

        //Create 2 entities, thing1 and thing2
        Entity thing1 = new Entity();
        thing1.put("name", "thing1");
        thing1 = this.app().collection("things").post(thing1);

        Entity thing2 = new Entity();
        thing2.put("name", "thing2");
        thing2 = this.app().collection("things").post(thing2);

        refreshIndex();
        //create the connection: thing1 likes thing2
        this.app().collection("things").entity(thing1).connection("likes").collection("things").entity(thing2).post();
        //delete thing2
        this.app().collection("things").entity(thing2).delete();

        refreshIndex();

        try {
            //attempt to retrieve thing1
            thing2 = this.app().collection("things").entity(thing2).get();
            fail("This should throw an exception");
        } catch (UniformInterfaceException uie) {
            // Should return a 404 Not Found
            assertEquals(404, uie.getResponse().getStatus());
        }
    }

    /**
     * Ensure that the connecting entity can be deleted
     * properly after a connection has been added
     *
     * @throws IOException
     */
    @Test //USERGRID-3011
    public void connectionsDeleteFirstEntityInConnectionTest() throws IOException {

        //Create 2 entities, thing1 and thing2
        Entity thing1 = new Entity();
        thing1.put("name", "thing1");
        thing1 = this.app().collection("things").post(thing1);

        Entity thing2 = new Entity();
        thing2.put("name", "thing2");
        thing2 = this.app().collection("things").post(thing2);

        refreshIndex();
        //create the connection: thing1 likes thing2
        this.app().collection("things").entity(thing1).connection("likes").collection("things").entity(thing2).post();
        //delete thing1
        this.app().collection("things").entity(thing1).delete();

        refreshIndex();

        try {
            //attempt to retrieve thing1
            thing1 = this.app().collection("things").entity(thing1).get();
            fail("This should throw an exception");
        } catch (UniformInterfaceException uie) {
            // Should return a 404 Not Found
            assertEquals(404, uie.getResponse().getStatus());
        }

    }


}
