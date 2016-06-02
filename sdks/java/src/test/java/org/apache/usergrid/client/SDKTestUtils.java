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
import org.apache.usergrid.java.client.model.UsergridEntity;
import org.apache.usergrid.java.client.response.UsergridResponse;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SDKTestUtils {


    public static Map<String, UsergridEntity> createColorShapes(String collection) {

        Map<String, Map<String, String>> entityMap = new HashMap<>(7);

        Map<String, String> fields = new HashMap<>(3);
        fields.put("color", "red");
        fields.put("shape", "square");

        entityMap.put("redsquare", fields);

        fields = new HashMap<>(3);
        fields.put("color", "blue");
        fields.put("shape", "circle");

        entityMap.put("bluecircle", fields);

        fields = new HashMap<>(3);
        fields.put("color", "yellow");
        fields.put("shape", "triangle");

        entityMap.put("yellowtriangle", fields);

        return createEntities(collection, entityMap);
    }

    public static Map<String, UsergridEntity> createEntities(final String collection,
                                                             final Map<String, Map<String, String>> entities) {

        Map<String, UsergridEntity> entityMap = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> entity : entities.entrySet()) {

            UsergridEntity e = createEntity(collection, entity.getKey(), entity.getValue());
            entityMap.put(e.getUuid(), e);
        }

        return entityMap;
    }

    public static UsergridEntity createEntity(final String collection,
                                              final String name,
                                              final Map<String, String> fields) {

        UsergridEntity e = new UsergridEntity(collection, name);

        if( fields != null ) {
            for (Map.Entry<String, String> field : fields.entrySet()) {
                e.putProperty(field.getKey(), field.getValue());
            }
        }

        UsergridResponse r = Usergrid.getInstance().POST(e);

        if (r.getResponseError() != null) {
            assertTrue("UUID should not be null", e.getUuid() != null);
            if( fields != null ) {
                for (Map.Entry<String, String> field : fields.entrySet()) {
                    assertEquals("attempted to set a property which did not persist on the entity", e.getStringProperty(field.getKey()),field.getValue());
                }
            }
        }

        return r.first();
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {
            ignore.printStackTrace();
        }
    }

    public static void indexSleep() {
        sleep(1000);
    }
}
