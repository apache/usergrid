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

import org.apache.usergrid.java.client.Usergrid;
import org.apache.usergrid.java.client.auth.UsergridAppAuth;
import org.apache.usergrid.java.client.model.UsergridEntity;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class ClientRestTestCase {

    final String collectionName = "testClientConnection" + System.currentTimeMillis();

    @Before
    public void before()  {
        Usergrid.initSharedInstance(SDKTestConfiguration.ORG_NAME, SDKTestConfiguration.APP_NAME, SDKTestConfiguration.USERGRID_URL, SDKTestConfiguration.authFallBack);
        UsergridAppAuth appAuth = new UsergridAppAuth(SDKTestConfiguration.APP_CLIENT_ID, SDKTestConfiguration.APP_CLIENT_SECRET);
        Usergrid.authenticateApp(appAuth);
        createCollectionAndEntity();
    }

    @After
    public void after() {
        Usergrid.reset();
    }

    public void createCollectionAndEntity()  {
        UsergridEntity entityOne = new UsergridEntity(collectionName,"john");
        entityOne.putProperty("place", "San Jose");
        entityOne.save();

        UsergridEntity entityTwo = new UsergridEntity(collectionName,"amici");
        entityTwo.putProperty("place", "San Jose");
        entityTwo.save();

        assertNotNull(entityOne.getUuid());
        assertNotNull(entityTwo.getUuid());

        Usergrid.connect(entityOne, "likes", entityTwo);
        Usergrid.connect(entityOne.getType(), entityOne.getUuid(), "visited", entityTwo.getUuid());
    }

    @Test
    public void clientGET() {
        // Retrieve the response.
        UsergridResponse response = Usergrid.GET(collectionName, "john");
        assertTrue("response should be ok", response.ok());
        assertNull("no error thrown", response.getResponseError());

        assertNotNull(response.getEntities());
        assertTrue("response entities is an Array", response.getEntities().getClass() == ArrayList.class);

        // response.first should exist and have a valid uuid
        UsergridEntity firstEntity = response.first();
        assertNotNull(firstEntity);
        assertNotNull("first entity is not null and has uuid", firstEntity.getUuid());

        // response.entity should exist, equals the first entity, and have a valid uuid
        UsergridEntity responseEntity = response.entity();
        assertNotNull(responseEntity);
        assertEquals(firstEntity, responseEntity);
        assertNotNull("entity is not null and has uuid", responseEntity.getUuid());

        // response.last should exist and have a valid uuid
        UsergridEntity lastEntity = response.last();
        assertNotNull(lastEntity);
        assertNotNull("last entity is not null and has uuid", lastEntity.getUuid());
    }
}
