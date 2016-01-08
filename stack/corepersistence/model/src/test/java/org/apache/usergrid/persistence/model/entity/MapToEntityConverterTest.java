/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.usergrid.persistence.model.entity;


import org.apache.usergrid.persistence.model.field.FieldTypeName;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.assertTrue;


public class MapToEntityConverterTest {

    public static final Logger logger = LoggerFactory.getLogger(MapToEntityConverterTest.class);


    @Test
    public void fullMapToEntityConversion() {

        // build top-level map
        final Map<String,Object> data = new HashMap<>(1);

        final String stringField = "stringFieldValue";
        final List<String> listField = new ArrayList<>(1);
        listField.add("listFieldValue");
        final boolean booleanField = true;
        final double doubleField = new Double("1");
        final int intField = 2;
        final long longField = 3L;
        final float floatField = new Float("4");

        // build Usergrid location object
        final Map<String, Double> coordinates = new HashMap<>(2);
        coordinates.put("latitude", 37.3338716);
        coordinates.put("longitude", -121.894249);

        final Map<String, Object> objectField = new HashMap<>(1);
        objectField.put("key1", "value1");

        // add the data to the top-level map
        data.put("stringField", stringField);
        data.put("listField", listField);
        data.put("booleanField", booleanField);
        data.put("doubleField", doubleField);
        data.put("intField", intField);
        data.put("longField", longField);
        data.put("floatField", floatField);
        data.put("location", coordinates);
        data.put("objectField", objectField);


        // convert the map to an entity
        MapToEntityConverter converter = new MapToEntityConverter();
        Entity entity = converter.fromMap(data, true);

        // make sure the nested array got converted into a ListField
        assertTrue(entity.getField("stringField").getTypeName() == FieldTypeName.STRING);
        assertTrue(entity.getField("listField").getTypeName() == FieldTypeName.LIST);
        assertTrue(entity.getField("booleanField").getTypeName() == FieldTypeName.BOOLEAN);
        assertTrue(entity.getField("doubleField").getTypeName() == FieldTypeName.DOUBLE);
        assertTrue(entity.getField("intField").getTypeName() == FieldTypeName.INTEGER);
        assertTrue(entity.getField("longField").getTypeName() == FieldTypeName.LONG);
        assertTrue(entity.getField("floatField").getTypeName() == FieldTypeName.FLOAT);
        assertTrue(entity.getField("location").getTypeName() == FieldTypeName.LOCATION);
        assertTrue(entity.getField("objectField").getTypeName() == FieldTypeName.OBJECT);

    }

    @Test
    public void testNestedArrays() {

        // build top-level map
        final Map<String,Object> data = new HashMap<>(1);

        // build nested list structure
        final List<Object> childArray = new ArrayList<>(1);
        childArray.add("child");
        final List<Object> parentArray = new ArrayList<>(1);
        parentArray.add(childArray);

        // add the nested list to the map
        data.put("parentArray", parentArray);

        // convert the map to an entity
        MapToEntityConverter converter = new MapToEntityConverter();
        Entity entity = converter.fromMap(data, true);

        // make sure the nested array got converted into a ListField
        assertTrue(entity.getField("parentArray").getTypeName() == FieldTypeName.LIST);
    }

}
