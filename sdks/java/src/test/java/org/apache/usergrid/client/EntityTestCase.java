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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.usergrid.java.client.UsergridEnums.UsergridDirection;
import org.apache.usergrid.java.client.Usergrid;
import org.apache.usergrid.java.client.auth.UsergridAppAuth;
import org.apache.usergrid.java.client.model.UsergridEntity;
import org.apache.usergrid.java.client.query.UsergridQuery;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class EntityTestCase {

    @Before
    public void before() {
        Usergrid.initSharedInstance(SDKTestConfiguration.ORG_NAME, SDKTestConfiguration.APP_NAME, SDKTestConfiguration.USERGRID_URL, SDKTestConfiguration.authFallBack);
        Usergrid.authenticateApp(new UsergridAppAuth(SDKTestConfiguration.APP_CLIENT_ID, SDKTestConfiguration.APP_CLIENT_SECRET));
    }

    @After
    public void after() {
        Usergrid.reset();
    }

    @Test
    public void testEntityCreationSuccess() {
        String collectionName = "ect" + System.currentTimeMillis();
        String entityName = "testEntity1";

        HashMap<String,JsonNode> map = new HashMap<>();
        map.put("name",new TextNode(entityName));
        map.put("color",new TextNode("red"));
        map.put("shape",new TextNode("square"));

        UsergridEntity entity = new UsergridEntity(collectionName,null,map);
        UsergridResponse response = entity.save();
        assertNull(response.getResponseError());

        UsergridEntity eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The returned entity is null!", eLookUp);
        assertEquals("entities has the correct type", eLookUp.getType(),collectionName);
        assertEquals("entities has the correct name", eLookUp.getName(),entityName);
        assertEquals("entities has the correct color", eLookUp.getStringProperty("color"),"red");
        assertEquals("entities has the correct shape", eLookUp.getStringProperty("shape"),"square");
    }

    @Test
    public void testDuplicateEntityNameFailure() {
        String collectionName = "testDuplicateEntityNameFailure" + System.currentTimeMillis();

        UsergridEntity entity = new UsergridEntity(collectionName,"test3");
        UsergridResponse response = Usergrid.POST(entity);
        assertNull("First entity create should have succeeded.", response.getResponseError());

        response = Usergrid.POST(entity);
        assertNotNull("Second entity create should not succeed!", response.getResponseError());
    }

    @Test
    public void testEntityLookupByName() {
        String collectionName = "testEntityLookupByName" + System.currentTimeMillis();
        String entityName = "testEntity4";

        UsergridEntity entity = new UsergridEntity(collectionName,entityName);
        entity.save();

        UsergridEntity eLookup = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The returned entity is null!", eLookup);
        assertEquals("The returned entity does not have the same UUID", entity.getUuid(),eLookup.getUuid());
    }

    @Test
    public void testEntityLookupByUUID() {
        String collectionName = "testEntityLookupByUUID" + System.currentTimeMillis();
        String entityName = "testEntity5";

        UsergridEntity entity = new UsergridEntity(collectionName,entityName);
        entity.save();
        assertNotNull(entity.getUuid());

        UsergridEntity eLookup = Usergrid.GET(collectionName, entity.getUuid()).first();
        assertNotNull("The returned entity is null!", eLookup);
        assertEquals("The returned entity does not have the same UUID", entity.getUuid(),eLookup.getUuid());
    }

    @Test
    public void testEntityLookupByQuery() {
        String collectionName = "testEntityLookupByQuery" + System.currentTimeMillis();
        String entityName = "testEntity6";

        UsergridEntity entity = new UsergridEntity(collectionName,entityName);
        entity.putProperty("color","red");
        entity.putProperty("shape","square");
        entity.save();

        SDKTestUtils.indexSleep();

        UsergridQuery query = new UsergridQuery(collectionName).eq("color", "red");
        UsergridEntity eLookup = Usergrid.GET(query).first();

        assertNotNull("The entity was not returned on lookup", eLookup);
        assertEquals("The returned entity does not have the same UUID", entity.getUuid(),eLookup.getUuid());

        query = new UsergridQuery(collectionName).eq("name", entityName);
        eLookup = Usergrid.GET(query).first();

        assertNotNull("The entity was not returned on lookup", eLookup);
        assertEquals("The returned entity does not have the same UUID", entity.getUuid(),eLookup.getUuid());

        query = new UsergridQuery(collectionName).eq("shape", "square");
        eLookup = Usergrid.GET(query).first();

        assertNotNull("The entity was not returned on lookup", eLookup);
        assertEquals("The returned entity does not have the same UUID", entity.getUuid(),eLookup.getUuid());

        query = new UsergridQuery(collectionName).eq("shape", "circle");
        eLookup = Usergrid.GET(query).first();

        assertNull("The entity was not expected to be returned on lookup", eLookup);
    }

    @Test
    public void testEntityUpdate() {
        String collectionName = "testEntityLookupByUUID" + System.currentTimeMillis();
        String entityName = "testEntity7";

        UsergridEntity entity = new UsergridEntity(collectionName,entityName);
        entity.putProperty("color","red");
        entity.putProperty("shape","square");
        entity.putProperty("orientation","up");
        entity.save();

        SDKTestUtils.sleep(1000);

        UsergridQuery query = new UsergridQuery(collectionName).eq("orientation", "up");
        UsergridEntity eLookup = Usergrid.GET(query).first();
        assertNotNull(eLookup);

        assertEquals("The returned entity does not have the same UUID when querying by field", entity.getUuid(),eLookup.getUuid());

        entity.putProperty("orientation", "down");
        entity.save();
        assertNotNull(entity.getUuid());

        eLookup = Usergrid.GET(collectionName, entity.getUuid()).first();
        assertNotNull(eLookup);

        assertEquals("The returned entity does not have the same UUID", entity.getUuid(),eLookup.getUuid());
        assertEquals("The field was not updated!", eLookup.getStringProperty("orientation"),"down");

        SDKTestUtils.sleep(1000);

        query = new UsergridQuery(collectionName).eq("orientation", "up");
        eLookup = Usergrid.GET(query).first();

        assertNull("The entity was returned for old value!", eLookup);
    }

    @Test
    public void testEntityDelete() {
        String collectionName = "testEntityDelete" + System.currentTimeMillis();
        String entityName = "testEntity8";

        UsergridEntity entity = new UsergridEntity(collectionName,entityName);
        entity.putProperty("color","red");
        entity.putProperty("shape","square");
        entity.putProperty("orientation","up");
        entity.save();

        SDKTestUtils.indexSleep();

        assertNotNull(entity.getUuid());
        assertNotNull(entity.getName());

        UsergridQuery query = new UsergridQuery(collectionName).eq("orientation", "up");
        UsergridEntity eLookup = Usergrid.GET(query).first();

        assertNotNull("The returned entity was null!", eLookup);
        assertEquals("The returned entity does not have the same UUID when querying by field", entity.getUuid(),eLookup.getUuid());

        Usergrid.DELETE(entity);

        eLookup = Usergrid.GET(collectionName, entity.getUuid()).first();
        assertNull("The entity was not expected to be returned by UUID", eLookup);

        eLookup = Usergrid.GET(collectionName, entity.getName()).first();
        assertNull("The entity was not expected to be returned by getName", eLookup);

        query = new UsergridQuery(collectionName).eq("color", "red");
        eLookup = Usergrid.GET(query).first();
        assertNull("The entity was not expected to be returned", eLookup);

        query = new UsergridQuery(collectionName).eq("shape", "square");
        eLookup = Usergrid.GET(query).first();
        assertNull("The entity was not expected to be returned", eLookup);

        query = new UsergridQuery(collectionName).eq("orientation", "up");
        eLookup = Usergrid.GET(query).first();
        assertNull("The entity was not expected to be returned", eLookup);
    }

    @Test
    public void testEntityPutPropertyAndSave() {
        String collectionName = "testEntityPutProperty" + System.currentTimeMillis();
        String entityName = "testEntity9";

        UsergridEntity entity = new UsergridEntity(collectionName,entityName);
        entity.putProperty("color","red");
        entity.putProperty("shape","square");
        entity.putProperty("orientation","up");
        entity.putProperty("sides", 4);
        entity.save();

        UsergridEntity eLookUp = Usergrid.GET(collectionName, entityName).first();

        //Check if the property was added correctly
        assertNotNull("The entity returned is not null.", eLookUp);
        assertEquals("The entity putProperty() was successful ", eLookUp.getStringProperty("orientation"),"up");
        assertEquals("The entity putProperty() was successful ", eLookUp.getIntegerProperty("sides"), new Integer(4));

        //Overwrite the property if it exists.
        entity.putProperty("orientation", "horizontal");
        entity.save();

        eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The returned entity was null!", eLookUp);
        assertEquals("The entity putProperty() was successful ", eLookUp.getStringProperty("orientation"),"horizontal");

        //should not be able to set the name key (name is immutable)
        entity.putProperty("name","entityNew");
        entity.save();

        eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The returned entity was null!", eLookUp);
        assertEquals("The entity putProperty() was successful ", eLookUp.getName(),"testEntity9");
    }

    @Test
    public void testEntityPutProperties() {
        String collectionName = "testEntityProperties" + System.currentTimeMillis();
        String entityName = "testEntity9";

        UsergridEntity entity = new UsergridEntity(collectionName,entityName);
        entity.putProperty("color","black");
        entity.putProperty("orientation","up");
        entity.save();

        UsergridEntity eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The entity returned is not null.", eLookUp);
        assertEquals("The entity putProperty() was successful ", eLookUp.getStringProperty("orientation"),"up");
        assertEquals("overwrite existing property", eLookUp.getStringProperty("color"),"black");
    }

    @Test
    public void testEntityRemovePropertiesAndSave() {
        String collectionName = "testEntityProperties" + System.currentTimeMillis();

        Map<String, String> fields = new HashMap<>(3);
        fields.put("color", "red");

        String entityName = "testEntity9";

        //should set properties for a given object, overwriting properties that exist and creating those that don\'t
        UsergridEntity entity = SDKTestUtils.createEntity(collectionName, entityName, fields);
        Map<String, Object> properties = new HashMap<>();
        properties.put("shape", "square");
        properties.put("orientation", "up");
        properties.put("color", "black");
        entity.putProperties(properties);
        entity.save();

        UsergridEntity eLookUp = Usergrid.GET(collectionName, "testEntity9").first();
        assertNotNull("The entity returned is not null.", eLookUp);

        String[] removeProperties = {"shape", "color"};
        entity.removeProperties(Arrays.asList(removeProperties));
        entity.save();

        eLookUp = Usergrid.GET(collectionName, "testEntity9").first();
        assertNotNull("The entity returned is not null.", eLookUp);
        assertTrue("overwrite existing property", eLookUp.getStringProperty("color") == null);
        assertTrue("overwrite existing property", eLookUp.getStringProperty("shape") == null);

    }

    @Test
    public void testEntityRemoveProperty() {
        String collectionName = "testEntityProperties" + System.currentTimeMillis();

        Map<String, String> fields = new HashMap<>(3);
        fields.put("color", "red");

        String entityName = "testEntity11";

        //should set properties for a given object, overwriting properties that exist and creating those that don\'t
        UsergridEntity entity = SDKTestUtils.createEntity(collectionName, entityName, fields);
        Map<String, Object> properties = new HashMap<>();
        properties.put("shape", "square");
        properties.put("orientation", "up");
        properties.put("color", "black");
        entity.putProperties(properties);
        entity.save();

        UsergridEntity eLookUp = Usergrid.GET(collectionName, "testEntity11").first();
        assertNotNull("The entity returned is not null.", eLookUp);

        entity.removeProperty("color");
        entity.removeProperty("shape");
        entity.save();

        eLookUp = Usergrid.GET(collectionName, "testEntity11").first();
        assertNotNull("The entity returned is not null.", eLookUp);
        assertTrue("overwrite existing property", eLookUp.getStringProperty("color") == null);
        assertTrue("overwrite existing property", eLookUp.getStringProperty("shape") == null);

    }

    @Test
    public void testEntityAppendInArray() {
        String collectionName = "testEntityProperties" + System.currentTimeMillis();
        String entityName = "testEntity1";

        UsergridEntity entity = new UsergridEntity(collectionName,entityName);
        entity.save();

        ArrayList<Object> lenArr = new ArrayList<>();
        lenArr.add(1);
        lenArr.add(2);
        lenArr.add(3);
        lenArr.add(4);
        entity.insert("lenArray", lenArr);
        entity.save();

        lenArr = new ArrayList<>();
        lenArr.add(6);
        lenArr.add(7);
        entity.append("lenArray", lenArr);
        entity.save();

        UsergridEntity eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The entity returned is not null.", eLookUp);

        ArrayNode toCompare = new ArrayNode(JsonNodeFactory.instance);
        toCompare.add(1).add(2).add(3).add(4).add(6).add(7);
        assertEquals("The two arrays should be equal.", eLookUp.getJsonNodeProperty("lenArray"),toCompare);
    }

    @Test
    public void testEntityPrependInArray() {
        String collectionName = "testEntityProperties" + System.currentTimeMillis();
        String entityName = "testEntity1";

        UsergridEntity entity = new UsergridEntity(collectionName,entityName);
        entity.save();

        ArrayList<Object> lenArr = new ArrayList<>();
        lenArr.add(1);
        lenArr.add(2);
        lenArr.add(3);
        lenArr.add(4);
        entity.putProperty("lenArray", lenArr);
        entity.save();

        lenArr = new ArrayList<>();
        lenArr.add(6);
        lenArr.add(7);

        entity.insert("lenArray", lenArr, 0);
        entity.save();
        UsergridEntity eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The entity returned is not null.", eLookUp);

        ArrayNode toCompare = new ArrayNode(JsonNodeFactory.instance);
        toCompare.add(6).add(7).add(1).add(2).add(3).add(4);
        assertEquals("The two arrays should be equal.", eLookUp.getJsonNodeProperty("lenArray"),toCompare);
    }

    @Test
    public void testEntityPopInArray() {
        String collectionName = "testEntityProperties" + System.currentTimeMillis();
        String entityName = "testEntity1";

        UsergridEntity entity = new UsergridEntity(collectionName,entityName);
        entity.save();

        ArrayList<Object> lenArr = new ArrayList<>();
        lenArr.add(1);
        lenArr.add(2);
        lenArr.add(3);
        entity.putProperty("lenArray", lenArr);
        entity.save();

        // should remove the last value of an existing array
        entity.pop("lenArray");
        entity.save();

        UsergridEntity eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The entity returned is not null.", eLookUp);

        ArrayNode toCompare = new ArrayNode(JsonNodeFactory.instance);
        toCompare.add(1).add(2);
        assertEquals("The two arrays should be equal.", eLookUp.getJsonNodeProperty("lenArray"),toCompare);

        // value should remain unchanged if it is not an array
        entity.putProperty("foo", "test1");
        entity.save();

        entity.pop("foo");
        entity.save();

        eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The entity returned is not null.", eLookUp);
        assertEquals("foo should equal test1.", eLookUp.getStringProperty("foo"), "test1");

        //should gracefully handle empty arrays
        ArrayList<Object> lenArr2 = new ArrayList<>();
        entity.putProperty("foo", lenArr2);
        entity.save();
        entity.pop("foo");

        eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The entity returned is not null.", eLookUp);

        toCompare = new ArrayNode(JsonNodeFactory.instance);
        assertEquals("The two arrays should be equal.", eLookUp.getJsonNodeProperty("foo"),toCompare);
    }

    @Test
    public void testEntityShiftInArray() {
        String collectionName = "testEntityProperties" + System.currentTimeMillis();
        String entityName = "testEntity1";

        //should remove the last value of an existing array
        UsergridEntity entity = new UsergridEntity(collectionName,entityName);
        entity.save();

        ArrayList<Object> lenArr = new ArrayList<>();
        lenArr.add(1);
        lenArr.add(2);
        lenArr.add(3);
        entity.putProperty("lenArray", lenArr);
        entity.save();

        entity.shift("lenArray");
        entity.save();

        UsergridEntity eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The entity returned is not null.", eLookUp);

        ArrayNode toCompare = new ArrayNode(JsonNodeFactory.instance);
        toCompare.add(2).add(3);
        assertEquals("The two arrays should be equal.", eLookUp.getJsonNodeProperty("lenArray"),toCompare);

        //value should remain unchanged if it is not an array
        entity.putProperty("foo", "test1");
        entity.shift("foo");
        entity.save();

        eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The entity returned is not null.", eLookUp);
        assertEquals("The entity returned is not null.", eLookUp.getStringProperty("foo"), "test1");

        //should gracefully handle empty arrays
        ArrayList<Object> lenArr2 = new ArrayList<>();
        entity.putProperty("foo", lenArr2);
        entity.shift("foo");
        entity.save();

        eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The entity returned is not null.", eLookUp);
        assertEquals("The two arrays should be equal.", eLookUp.getJsonNodeProperty("foo"), new ArrayNode(JsonNodeFactory.instance));
    }

    @Test
    public void testEntityInsertInArray() {
        String collectionName = "testEntityProperties" + System.currentTimeMillis();
        String entityName = "testEntity1";

        //should set properties for a given object, overwriting properties that exist and creating those that don\'t
        UsergridEntity entity = new UsergridEntity(collectionName,entityName);
        entity.save();

        ArrayList<Object> lenArr = new ArrayList<>();
        lenArr.add(1);
        lenArr.add(2);
        lenArr.add(3);
        lenArr.add(4);
        entity.putProperty("lenArray", lenArr);
        entity.save();

        ArrayList<Object> lenArr2 = new ArrayList<>();
        lenArr2.add(6);
        lenArr2.add(7);

        entity.insert("lenArray", lenArr2, 6);
        entity.save();

        UsergridEntity eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The entity returned is not null.", eLookUp);

        ArrayNode toCompare = new ArrayNode(JsonNodeFactory.instance);
        toCompare.add(1).add(2).add(3).add(4).add(6).add(7);
        assertEquals("The two arrays should be equal.", eLookUp.getJsonNodeProperty("lenArray"),toCompare);

        //should merge an array of values into an existing array at the specified index
        lenArr = new ArrayList<>();
        lenArr.add(1);
        lenArr.add(2);
        lenArr.add(3);
        lenArr.add(4);

        entity.putProperty("lenArray", lenArr);
        entity.save();

        lenArr2 = new ArrayList<>();
        lenArr2.add(5);
        lenArr2.add(6);
        lenArr2.add(7);
        lenArr2.add(8);

        entity.insert("lenArray", lenArr2, 2);
        entity.save();

        eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The entity returned is not null.", eLookUp);

        toCompare = new ArrayNode(JsonNodeFactory.instance);
        toCompare.add(1).add(2).add(5).add(6).add(7).add(8).add(3).add(4);
        assertEquals("The two arrays should be equal.", eLookUp.getJsonNodeProperty("lenArray"),toCompare);

        //should convert an existing value into an array when inserting a second value
        entity.putProperty("foo", "test");
        entity.insert("foo", "test1", 1);
        entity.save();

        eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The entity returned is not null.", eLookUp);

        toCompare = new ArrayNode(JsonNodeFactory.instance);
        toCompare.add("test").add("test1");
        assertEquals("The two arrays should be equal.", eLookUp.getJsonNodeProperty("foo"),toCompare);

        //should create a new array when a property does not exist
        entity.insert("foo1", "test2", 1);
        entity.save();

        eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The entity returned is not null.", eLookUp);

        toCompare = new ArrayNode(JsonNodeFactory.instance);
        toCompare.add("test2");
        assertEquals("The two arrays should be equal.", eLookUp.getJsonNodeProperty("foo1"),toCompare);

        //should gracefully handle index out of positive range
        entity.putProperty("ArrayIndex", "test1");
        entity.insert("ArrayIndex", "test2", 1000);
        entity.save();

        eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The entity returned is not null.", eLookUp);

        toCompare = new ArrayNode(JsonNodeFactory.instance);
        toCompare.add("test1").add("test2");
        assertEquals("The two arrays should be equal.", eLookUp.getJsonNodeProperty("ArrayIndex"),toCompare);

        //should gracefully handle index out of negative range
        entity.insert("ArrayIndex", "test3", -1000);
        entity.save();

        eLookUp = Usergrid.GET(collectionName, entityName).first();
        assertNotNull("The entity returned is not null.", eLookUp);

        toCompare = new ArrayNode(JsonNodeFactory.instance);
        toCompare.add("test3").add("test1").add("test2");
        assertEquals("The two arrays should be equal.", eLookUp.getJsonNodeProperty("ArrayIndex"),toCompare);
    }

    @Test
    public void testEntityConnectDisconnectGetConnections() {
        String collectionName = "testEntityProperties" + System.currentTimeMillis();
        String entityOneName = "testEntity1";
        String entityTwoName = "testEntity2";

        UsergridEntity entityOne = new UsergridEntity(collectionName,entityOneName);
        entityOne.putProperty("color","red");
        entityOne.putProperty("shape","square");
        entityOne.save();

        UsergridEntity entityTwo = new UsergridEntity(collectionName,entityTwoName);
        entityTwo.putProperty("color","green");
        entityTwo.putProperty("shape","circle");
        entityTwo.save();

        assertNotNull(entityOne.getUuid());
        assertNotNull(entityTwo.getUuid());
        assertNotNull(entityOne.getName());
        assertNotNull(entityTwo.getName());
        assertNotNull(entityOne.uuidOrName());
        assertNotNull(entityTwo.uuidOrName());

        //should connect entities by passing a target UsergridEntity object as a parameter
        entityOne.connect("likes", entityTwo);
        entityOne.save();

        UsergridEntity eLookUpConnectedEntity = entityOne.getConnections(UsergridDirection.OUT, "likes").first();
        assertNotNull("The connected entity returned is not null.", eLookUpConnectedEntity);

        assertEquals("The entity name should be equals.", eLookUpConnectedEntity.getName(),entityTwoName);

        eLookUpConnectedEntity = entityTwo.getConnections(UsergridDirection.IN, "likes").first();
        assertNotNull("The connected entity returned is not null.", eLookUpConnectedEntity);
        assertEquals("The entity name should be equals.", eLookUpConnectedEntity.getName(),entityOneName);

        entityOne.disconnect("likes", entityTwo);
        entityOne.save();

        eLookUpConnectedEntity = entityTwo.getConnections(UsergridDirection.IN, "likes").first();
        assertNull("The entity returned is not null.", eLookUpConnectedEntity);

        //should connect entities by passing target uuid as a parameter
        Usergrid.connect(entityOne.getType(),entityOne.getUuid(),"visited",entityTwo.getUuid());
        entityOne.save();

        eLookUpConnectedEntity = entityOne.getConnections(UsergridDirection.OUT, "visited").first();
        assertNotNull("The connected entity returned is not null.", eLookUpConnectedEntity);
        assertEquals("The entity name should be equals.", eLookUpConnectedEntity.getName(),entityTwoName);

        Usergrid.disconnect(entityOne.getType(),entityOne.getUuid(),"visited",entityTwo.getUuid());
        entityOne.save();

        eLookUpConnectedEntity = entityOne.getConnections(UsergridDirection.OUT, "visited").first();
        assertNull("The entity returned is not null.", eLookUpConnectedEntity);

        //should connect entities by passing target type and name as parameters
        Usergrid.connect(entityOne.getType(),entityOne.getUuid(),"revisit",entityTwo.getType(),entityTwo.getName());
        entityOne.save();

        eLookUpConnectedEntity = entityOne.getConnections(UsergridDirection.OUT, "revisit").first();
        assertNotNull("The connected entity returned is not null.", eLookUpConnectedEntity);
        assertEquals("The entity name should be equals.", eLookUpConnectedEntity.getName(),entityTwoName);

        Usergrid.disconnect(entityOne.getType(),entityOne.getUuid(),"revisit",entityTwo.getType(),entityTwo.getName());
        entityOne.save();

        eLookUpConnectedEntity = entityOne.getConnections(UsergridDirection.OUT, "revisit").first();
        assertNull("The entity returned is not null.", eLookUpConnectedEntity);
    }
}
