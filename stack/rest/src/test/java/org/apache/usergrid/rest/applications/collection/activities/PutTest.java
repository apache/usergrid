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
package org.apache.usergrid.rest.applications.collection.activities;


import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertEquals;

import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.endpoints.CollectionEndpoint;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Activity update test.
 */
public class PutTest extends AbstractRestIT {
    private static final Logger log= LoggerFactory.getLogger( PutTest.class );


    @Test //USERGRID-545
    public void putMassUpdateTest() throws IOException {

        CollectionEndpoint activities = this.app().collection("activities");

        Map actor = hashMap( "displayName", "Erin" );
        Map newActor = hashMap( "displayName", "Bob" );
        Map props = new HashMap();

        props.put( "actor", actor );
        props.put( "verb", "go" );
        props.put( "content", "bragh" );


        for ( int i = 0; i < 5; i++ ) {
            props.put( "ordinal", i );
            Entity activity = activities.post(new Entity(props));
        }

        refreshIndex();

        String query = "select * ";

        Collection collection = activities.get();
        String uuid = collection.getResponse().getEntities().get( 0 ).getUuid().toString();
        StringBuilder buf = new StringBuilder( uuid );
        buf.append( "/" );
        buf.append( buf );
        props.put( "actor", newActor );
        Entity activity = activities.post(new Entity(props));

        refreshIndex();

        collection = activities.get(  );
        assertEquals( 6, collection.getResponse().getEntities().size() );
    }
}
