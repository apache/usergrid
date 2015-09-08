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
package org.apache.usergrid.rest;


import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class CollectionMetadataIT extends AbstractRestIT {

    private static final Logger LOG = LoggerFactory.getLogger( CollectionMetadataIT.class );


    public CollectionMetadataIT() throws Exception {
        super();
    }

    private final String collectionName = "collectionQueryParameterCollection";

    /**
     * USERGRID-918: control inclusion/exclusion of connection metadata via query parameter
     */
    @Test
    public void testCollectionQueryParameter() throws Exception {

        // create entities
        Entity e1 = new Entity();
        e1.put("name", "entity1");
        e1 = this.app().collection(collectionName).post(e1);
        assertNotNull(e1);

        Entity e2 = new Entity();
        e2.put("name", "entity2");
        e2 = this.app().collection(collectionName).post(e2);
        assertNotNull(e2);

        Entity e3 = new Entity();
        e3.put("name", "entity3");
        e3 = this.app().collection(collectionName).post(e3);
        assertNotNull(e3);

        refreshIndex();

        // create connections
        // e1 hates e3
        // e2 likes e1
        // e1 has 1 in (likes) & 1 out (hates) connection
        // e2 has one out (likes) connection
        // e3 has one in (hates) connection
        this.app().collection(collectionName).entity(e1).connection("hates").entity(e3).post();
        this.app().collection(collectionName).entity(e2).connection("likes").entity(e1).post();
        refreshIndex();

        // no query param, "all", and invalid param all the same
        checkMetadata(e1, null, "hates", "likes");
        checkMetadata(e1, "all", "hates", "likes");
        checkMetadata(e1, "foo", "hates", "likes");
        checkMetadata(e2, null, "likes", null);
        checkMetadata(e2, "all", "likes", null);
        checkMetadata(e2, "foo", "likes", null);
        checkMetadata(e3, null, null, "hates");
        checkMetadata(e3, "all", null, "hates");
        checkMetadata(e3, "foo", null, "hates");

        // "none" query param blocks connections and connecting
        checkMetadata(e1, "none", null, null);
        checkMetadata(e2, "none", null, null);
        checkMetadata(e3, "none", null, null);

        // "in" query param blocks connections
        checkMetadata(e1, "in", null, "likes");
        checkMetadata(e2, "in", null, null);
        checkMetadata(e3, "in", null, "hates");

        // "out" query param blocks connecting
        checkMetadata(e1, "out", "hates", null);
        checkMetadata(e2, "out", "likes", null);
        checkMetadata(e3, "out", null, null);

    }

    /**
     * validates that connections and connecting data are as expected
     *
     * if paramStr = null, means don't send query parameter
     * if connectionsType or connectingType = null, means that section shouldn't exist
     *
     * unchecked warnings suppressed to avoid warnings casting payload entries to maps
     */
    @SuppressWarnings("unchecked")
    private void checkMetadata(Entity origEntity, String paramStr, String connectionsType, String connectingType) throws Exception {
        QueryParameters params = new QueryParameters();
        if (paramStr != null)
            params.setConnections(paramStr);

        Entity e = this.app().collection(collectionName).entity(origEntity).get(params,true);

        Map <String,Object> metadata = (Map<String,Object>)e.get("metadata");
        assertNotNull(metadata);

        Map <String,Object> connections = (Map<String,Object>)metadata.get("connections");
        if (connectionsType != null) {
            assertNotNull(connections);
            assertNotNull(connections.get(connectionsType));
        } else {
            assertNull(connections);
        }

        Map <String,Object> connecting = (Map<String,Object>)metadata.get("connecting");
        if (connectingType != null) {
            assertNotNull(connecting);
            assertNotNull(connecting.get(connectingType));
        } else {
            assertNull(connecting);
        }
    }
}
