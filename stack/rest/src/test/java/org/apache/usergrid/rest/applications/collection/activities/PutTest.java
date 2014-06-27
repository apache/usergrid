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


import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.CustomCollection;

import static org.junit.Assert.assertEquals;
import static org.apache.usergrid.utils.MapUtils.hashMap;


/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class PutTest extends AbstractRestIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test //USERGRID-545
    public void putMassUpdateTest() throws IOException {

        CustomCollection activities = context.collection( "activities" );

        Map actor = hashMap( "displayName", "Erin" );
        Map newActor = hashMap( "displayName", "Bob" );
        Map props = new HashMap();

        props.put( "actor", actor );
        props.put( "verb", "go" );
        props.put( "content", "bragh" );


        for ( int i = 0; i < 5; i++ ) {

            props.put( "ordinal", i );
            JsonNode activity = activities.create( props );
        }

        String query = "select * ";

        JsonNode node = activities.withQuery( query ).get();
        String uuid = node.get( "entities" ).get( 0 ).get( "uuid" ).textValue();
        StringBuilder buf = new StringBuilder( uuid );


        activities.addToUrlEnd( buf );
        props.put( "actor", newActor );
        node = activities.put( props );
        node = activities.withQuery( query ).get();

        assertEquals( 6, node.get( "entities" ).size() );
    }
}
