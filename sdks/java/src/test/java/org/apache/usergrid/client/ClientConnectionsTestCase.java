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
package org.apache.usergrid.client;

import org.apache.usergrid.java.client.UsergridEnums.UsergridDirection;
import org.apache.usergrid.java.client.Usergrid;
import org.apache.usergrid.java.client.auth.UsergridAppAuth;
import org.apache.usergrid.java.client.model.UsergridEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClientConnectionsTestCase {

    @Before
    public void before() {
        Usergrid.initSharedInstance(SDKTestConfiguration.ORG_NAME, SDKTestConfiguration.APP_NAME, SDKTestConfiguration.USERGRID_URL, SDKTestConfiguration.authFallBack);
        UsergridAppAuth appAuth = new UsergridAppAuth(SDKTestConfiguration.APP_CLIENT_ID, SDKTestConfiguration.APP_CLIENT_SECRET);
        Usergrid.authenticateApp(appAuth);
    }

    @After
    public void after() {
        Usergrid.reset();
    }

    @Test
    public void clientConnect() {
        String collectionName = "testClientConnection" + System.currentTimeMillis();

        UsergridEntity entityOne = new UsergridEntity(collectionName,"john");
        entityOne.putProperty("place","San Jose");
        entityOne.save();
        assertNotNull(entityOne.getUuid());

        UsergridEntity entityTwo = new UsergridEntity(collectionName,"amici");
        entityOne.putProperty("place","San Jose");
        entityTwo.save();
        assertNotNull(entityTwo.getUuid());

        //should connect entities by passing UsergridEntity objects as parameters
        Usergrid.connect(entityOne, "likes", entityTwo);

        UsergridEntity responseEntity = Usergrid.getConnections(UsergridDirection.OUT, entityOne, "likes").first();
        assertNotNull(responseEntity);
        assertEquals("both entities name should be same", entityTwo.getName(),responseEntity.getName());
        assertEquals("both entities uuid should be same", entityTwo.getUuid(),responseEntity.getUuid());

        //should connect entities by passing a source UsergridEntity object and a target uuid.
        Usergrid.connect(entityOne.getType(), entityOne.getUuid(), "visited", entityTwo.getUuid());

        responseEntity = Usergrid.getConnections(UsergridDirection.OUT, entityOne, "visited").first();
        assertNotNull(responseEntity);
        assertEquals("both entities name should be same", entityTwo.getName(),responseEntity.getName());
        assertEquals("both entities uuid should be same", entityTwo.getUuid(),responseEntity.getUuid());

        //should connect entities by passing source type, source uuid, and target uuid as parameters
        Usergrid.connect(entityTwo.getType(), entityTwo.getUuid(), "visitor", entityOne.getUuid());

        responseEntity = Usergrid.getConnections(UsergridDirection.OUT, entityTwo, "visitor").first();
        assertNotNull(responseEntity);
        assertEquals("both entities name should be same", entityOne.getName(),responseEntity.getName());
        assertEquals("both entities uuid should be same", entityOne.getUuid(),responseEntity.getUuid());

        //should connect entities by passing source type, source name, target type, and target name as parameters
        assertNotNull(entityOne.getName());
        assertNotNull(entityTwo.getName());
        Usergrid.connect(entityTwo.getType(), entityTwo.getName(), "welcomed", entityOne.getType(), entityOne.getName());

        responseEntity = Usergrid.getConnections(UsergridDirection.OUT, entityTwo, "welcomed").first();
        assertNotNull(responseEntity);
        assertEquals("both entities name should be same", entityOne.getName(),responseEntity.getName());
        assertEquals("both entities uuid should be same", entityOne.getUuid(),responseEntity.getUuid());

        //should connect entities by passing source type, source name, target type, and target name as parameters
        Usergrid.connect(entityTwo.getType(), entityTwo.getName(), "invalidLink", "invalidName");
        responseEntity = Usergrid.getConnections(UsergridDirection.OUT, entityTwo, "invalidLink").first();
        assertNull("response entity should be null.", responseEntity);
    }

    @Test
    public void clientGetConnect() {
        String collectionName = "testClientGetConnection" + System.currentTimeMillis();

        //should set properties for a given object, overwriting properties that exist and creating those that don\'t
        UsergridEntity entityOne = new UsergridEntity(collectionName, "john");
        entityOne.putProperty("place","San Jose");
        entityOne.save();

        //should set properties for a given object, overwriting properties that exist and creating those that don\'t
        UsergridEntity entityTwo = new UsergridEntity(collectionName, "amici");
        entityTwo.putProperty("place","San Jose");
        entityTwo.save();

        //should connect entities by passing UsergridEntity objects as parameters
        Usergrid.connect(entityOne, "likes", entityTwo);
        Usergrid.connect(entityOne, "visited", entityTwo);

        UsergridEntity responseEntity = Usergrid.getConnections(UsergridDirection.OUT, entityOne, "likes").first();
        assertNotNull(responseEntity);
        assertEquals("both entities name should be same", entityTwo.getName(),responseEntity.getName());
        assertEquals("both entities uuid should be same", entityTwo.getUuid(),responseEntity.getUuid());

        responseEntity = Usergrid.getConnections(UsergridDirection.IN, entityTwo, "visited").first();
        assertNotNull(responseEntity);
        assertEquals("both entities name should be same", entityOne.getName(),responseEntity.getName());
        assertEquals("both entities uuid should be same", entityOne.getUuid(),responseEntity.getUuid());

    }

    @Test
    public void clientDisConnect() {
        String collectionName = "testClientGetConnection" + System.currentTimeMillis();

        //should set properties for a given object, overwriting properties that exist and creating those that don\'t
        UsergridEntity entityOne = new UsergridEntity(collectionName,"john");
        entityOne.putProperty("place","San Jose");
        entityOne.save();
        assertNotNull(entityOne.getName());
        assertNotNull(entityOne.getUuid());

        //should set properties for a given object, overwriting properties that exist and creating those that don\'t
        UsergridEntity entityTwo = new UsergridEntity(collectionName, "amici");
        entityTwo.putProperty("place","San Jose");
        entityTwo.save();
        assertNotNull(entityTwo.getName());
        assertNotNull(entityTwo.getUuid());

        //should connect entities by passing UsergridEntity objects as parameters
        Usergrid.connect(entityOne, "likes", entityTwo);
        Usergrid.connect(entityOne, "visited", entityTwo);
        Usergrid.connect(entityOne, "twice", entityTwo);
        Usergrid.connect(entityOne, "thrice", entityTwo);

        //should disConnect entities by passing UsergridEntity objects as parameters
        Usergrid.disconnect(entityOne, "likes", entityTwo);
        UsergridEntity responseEntity = Usergrid.getConnections(UsergridDirection.IN, entityTwo, "likes").first();
        assertNull("responseEntity should be null", responseEntity);

        //should disConnect entities by passing source type, source uuid, and target uuid as parameters
        Usergrid.disconnect(entityOne.getType(), entityOne.getUuid(), "visited", entityTwo.getUuid());
        responseEntity = Usergrid.getConnections(UsergridDirection.OUT, entityOne, "visited").first();
        assertNull("responseEntity should be null", responseEntity);

        //should disConnect entities by passing source type, source name, target type, and target name as parameters
        Usergrid.disconnect(entityOne.getType(), entityOne.getName(), "twice", entityTwo.getType(), entityTwo.getName());
        responseEntity = Usergrid.getConnections(UsergridDirection.OUT, entityOne, "twice").first();
        assertNull("responseEntity should be null", responseEntity);

        //should fail to disConnect entities when specifying target name without type
        Usergrid.disconnect(entityTwo.getType(), entityTwo.getName(), "thrice", entityOne.getName());
        responseEntity = Usergrid.getConnections(UsergridDirection.OUT, entityTwo, "thrice").first();
        assertNull("both entities name should be same",responseEntity);
    }
}
