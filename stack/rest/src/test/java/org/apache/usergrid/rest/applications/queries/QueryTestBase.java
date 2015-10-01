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
package org.apache.usergrid.rest.applications.queries;

import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base class containing common methods used by query tests
 */
public class QueryTestBase  extends AbstractRestIT {
    private static Logger log = LoggerFactory.getLogger(QueryTestBase.class);
    /**
     * Create a number of entities in the specified collection
     * with properties to make them independently searchable
     *
     * @param numberOfEntities
     * @param collectionName
     * @return an array of the Entity objects created
     */
    protected Entity[] generateTestEntities(int numberOfEntities, String collectionName) {
        Entity[] entities = new Entity[numberOfEntities];
        Entity props = new Entity();
        //Insert the desired number of entities
        for (int i = 0; i < numberOfEntities; i++) {
            Entity actor = new Entity();
            actor.put("displayName", String.format("Test User %d", i));
            actor.put("username", String.format("user%d", i));
            props.put("actor", actor);
            props.put("sometestprop","testprop");

            //give each entity a unique, numeric ordinal value
            props.put("ordinal", i);
            //Set half the entities to have a 'madeup' property of 'true'
            // and set the other half to 'false'
            if (i < numberOfEntities / 2) {
                props.put("madeup", false);
            } else {
                props.put("madeup", true);
            }
            //Set even-numbered users to have a verb of 'go' and the rest to 'stop'
            if (i % 2 == 0) {
                props.put("verb", "go");
            } else {
                props.put("verb", "stop");
            }
            //create the entity in the desired collection and add it to the return array
            entities[i] = this.app().collection(collectionName).post(props);
            log.info(entities[i].entrySet().toString());
        }
        //refresh the index so that they are immediately searchable
        this.refreshIndex();

        return entities;
    }


}
