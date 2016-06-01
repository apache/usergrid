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

import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class SelectMappingsQueryTest extends QueryTestBase {
    private static final Logger logger = LoggerFactory.getLogger(OrderByTest.class);


    @Test
    public void testNestedSelectFieldNames() throws Exception {

        generateTestEntities(20, "things");

        QueryParameters params = new QueryParameters()
            .setQuery("select actor.displayName,sometestprop where sometestprop = 'testprop'");
        Collection things = this.app().collection("things").get(params);
        assertEquals( 10, things.getNumOfEntities() );

        Iterator<Entity> iter = things.iterator();
        while ( iter.hasNext() ) {

            Entity entity = iter.next();
            assertEquals( 5, entity.getDynamicProperties().size() );

            assertNotNull( entity.getDynamicProperties().get("uuid") );
            assertNotNull( entity.getDynamicProperties().get("type") );
            assertNotNull( entity.getDynamicProperties().get("metadata") );
            assertNotNull( entity.getDynamicProperties().get("sometestprop") );

            Map<String, Object> actor = (Map<String, Object>)entity.getDynamicProperties().get("actor");
            assertNotNull( actor );
            assertNotNull( actor.get("displayName") );

        }
    }


    /**
     * Shows that field names are case-insensitive.
     * If you define two fields with same name but different cases, behavior is undefined.
     */
    @Test
    public void testFieldNameCaseSensitivity() throws Exception {

        int numberOfEntities = 10;
        String collectionName = "things";

        Entity[] entities = new Entity[numberOfEntities];
        Entity props = new Entity();

        for (int i = 0; i < numberOfEntities; i++) {
            props.put("testProp", "a");
            props.put("testprop", "b");
            entities[i] = app().collection(collectionName).post(props);
        }
        refreshIndex();

        {
            QueryParameters params = new QueryParameters()
                .setQuery( "select * where testProp = 'b'" );
            Collection things = this.app().collection( "things" ).get( params );

            // if field names were case sensitive, this would fail
            assertEquals( numberOfEntities, things.getNumOfEntities() );
        }

        {
            QueryParameters params = new QueryParameters()
                .setQuery( "select * where testprop='b'" );
            Collection things = this.app().collection( "things" ).get( params );

            assertEquals( numberOfEntities, things.getNumOfEntities() );
        }

    }

}
