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
package org.apache.usergrid.rest.applications.users;


import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.CustomCollection;

import static org.junit.Assert.assertEquals;


/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class RetrieveUsersTest extends AbstractRestIT {
    private static final Logger log = LoggerFactory.getLogger( RetrieveUsersTest.class );

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test // USERGRID-1222
    public void queryForUsername() {
        CustomCollection users = context.collection( "users" );

        Map props = new HashMap();

        props.put( "username", "Alica" );
        users.create( props );

        props.put( "username", "Bob" );
        users.create( props );

        String query = "select *";
        String incorrectQuery = "select * where username = 'Alica'";

        assertEquals( users.entityValue( query, "username", 0 ), users.entityValue( incorrectQuery, "username", 0 ) );
    }


    @Test // USERGRID-1727
    public void userEntityDictionaryHasRoles() {
        CustomCollection users = context.collection( "users" );

        Map props = new HashMap();
        props.put( "username", "Nina" );

        JsonNode response = users.create( props );
        JsonNode entity = response.get( "entities" ).get( 0 );
        JsonNode metadata = entity.get( "metadata" );
        JsonNode sets = metadata.get( "sets" );
        JsonNode rolenames = sets.get( "rolenames" );
        Assert.assertTrue( "rolenames URL ends with /roles", rolenames.asText().endsWith( "/roles" ) );
    }
}
