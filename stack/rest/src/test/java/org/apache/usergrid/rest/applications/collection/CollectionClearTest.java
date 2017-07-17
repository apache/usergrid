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
package org.apache.usergrid.rest.applications.collection;


import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests collection clear functionality.
 */

public class CollectionClearTest extends AbstractRestIT {

    private static final Logger logger = LoggerFactory.getLogger(CollectionClearTest.class);


    /**
     * Tests collection clear functionality.
     * @throws Exception
     */
    @Test
    public void collectionDelete() throws Exception {

        String collectionName = "children";
        int numEntities = 10;
        String namePrefix = "child";

        int numEntitiesAfterClear = 5;
        String namePrefixAfterClear = "abc";

        // verify collection version is empty
        ApiResponse tempResponse = this.app().collection(collectionName).collection("_version").get().getResponse();
        LinkedHashMap dataMap = (LinkedHashMap)tempResponse.getData();
        assertEquals("", dataMap.get("version"));
        assertEquals(collectionName, dataMap.get("collectionName"));

        createEntities( collectionName, namePrefix, numEntities );

        // retrieve entities, provide 1 more than num entities
        QueryParameters parms = new QueryParameters().setLimit( numEntities + 1 ).setQuery("order by created asc");
        List<Entity> entities = retrieveEntities(collectionName, namePrefix, parms, numEntities, false);
        assertEquals(numEntities, entities.size());

        // clear the collection
        Map<String, Object> payload = new HashMap<String, Object>();
        parms = new QueryParameters().setKeyValue("confirm_collection_name", collectionName);
        tempResponse = this.app().collection(collectionName).collection("_clear").post(true, payload, parms);

        // verify collection version has changed
        tempResponse = this.app().collection(collectionName).collection("_version").get().getResponse();
        dataMap = (LinkedHashMap)tempResponse.getData();
        String newVersion = (String)dataMap.get("version");
        assertNotEquals("", newVersion);
        assertEquals(collectionName, dataMap.get("collectionName"));

        // validate that 0 entities left
        List<Entity> entitiesAfterClear = retrieveEntities(collectionName, namePrefix, parms, 0, true);
        assertEquals(0, entitiesAfterClear.size());

        // insert more entities using same collectionName
        createEntities( collectionName, namePrefixAfterClear, numEntitiesAfterClear );

        // validate correct number of entities
        parms = new QueryParameters().setLimit( numEntitiesAfterClear + 1 ).setQuery("order by created asc");
        List<Entity> newEntities = retrieveEntities(collectionName, namePrefixAfterClear, parms, numEntitiesAfterClear, false);
        assertEquals(numEntitiesAfterClear, newEntities.size());

        // verify collection version has not changed
        tempResponse = this.app().collection(collectionName).collection("_version").get().getResponse();
        dataMap = (LinkedHashMap)tempResponse.getData();
        assertEquals(newVersion, dataMap.get("version"));
        assertEquals(collectionName, dataMap.get("collectionName"));
    }


    /**
     * Creates a number of entities with sequential names going up to the numOfEntities and posts them to the
     * collection specified with collectionName.
     * @param collectionName
     * @param numOfEntities
     */
    public List<Entity> createEntities(String collectionName, String namePrefix, int numOfEntities ){
        List<Entity> entities = new LinkedList<>(  );

        for ( int i = 1; i <= numOfEntities; i++ ) {
            Map<String, Object> entityPayload = new HashMap<String, Object>();
            entityPayload.put( "name", namePrefix + String.valueOf( i ) );
            entityPayload.put( "num", i );

            Entity entity = new Entity( entityPayload );

            entities.add( entity );

            this.app().collection( collectionName ).post( entity );

            if ( i % 100 == 0){
                logger.info("created {} entities", i);
            }
        }
        logger.info("created {} total entities", numOfEntities);

        this.waitForQueueDrainAndRefreshIndex();

        return entities;
    }

    /**
     * Retrieves a specified number of entities from a collection.
     * @param collectionName
     * @param parms
     * @param numOfEntities
     */
    public List<Entity> retrieveEntities(String collectionName, String namePrefix, QueryParameters parms, int numOfEntities, boolean reverseOrder){
        List<Entity> entities = new LinkedList<>(  );
        Collection testCollection = this.app().collection( collectionName ).get(parms, true);

        int entityNum;
        if (reverseOrder) {
            entityNum = numOfEntities;
        } else {
            entityNum = 1;
        }
        while (testCollection.getCursor() != null) {
            while (testCollection.hasNext()) {
                Entity returnedEntity = testCollection.next();
                assertEquals(namePrefix + String.valueOf(entityNum), returnedEntity.get("name"));
                entities.add(returnedEntity);
                if (reverseOrder) {
                    entityNum--;
                } else {
                    entityNum++;
                }
            }

            testCollection = this.app().collection(collectionName).getNextPage(testCollection, parms, true);
        }

        // handle left over entities
        while (testCollection.hasNext()) {
            Entity returnedEntity = testCollection.next();
            assertEquals(namePrefix + String.valueOf(entityNum), returnedEntity.get("name"));
            entities.add(returnedEntity);
            if (reverseOrder) {
                entityNum--;
            } else {
                entityNum++;
            }
        }

        assertEquals(entities.size(), numOfEntities);
        return entities;
    }

}
