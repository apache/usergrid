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
package org.apache.usergrid.rest.applications.collection.users;


import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.endpoints.CollectionEndpoint;
import org.apache.usergrid.rest.test.resource.endpoints.EntityEndpoint;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;


public class RetrieveUsersTest extends AbstractRestIT {
    private static final Logger log = LoggerFactory.getLogger( RetrieveUsersTest.class );



    @Test // USERGRID-1222
    public void queryForUsername() throws IOException {

        CollectionEndpoint users =this.app().collection( "users" );

        Entity props = new Entity();
        props.put( "username", "Bob" );
        users.post(props);
        props.put( "username", "Alica" );
        users.post(props);



        refreshIndex();

        String query = "select *";
        String incorrectQuery = "select * where username = 'Alica'";

        assertEquals( users.get(new QueryParameters().setQuery( query) ).next().get( "username").toString(), users.get(new QueryParameters().setQuery( incorrectQuery)).next().get( "username").toString() );
    }


    @Test // USERGRID-1727
    public void userEntityDictionaryHasRoles() throws IOException {
        CollectionEndpoint users = this.app().collection("users");

        Entity props = new Entity();
        props.put( "username", "Nina" );

        Entity entity = users.post(props);
        refreshIndex();

        Map<String,Object> metadata = (Map)entity.get( "metadata" );
        Map<String,Object> sets = (Map)metadata.get( "sets" );
        String rolenames =(String) sets.get( "rolenames" );
        Assert.assertTrue( "rolenames URL ends with /roles", rolenames.endsWith( "/roles" ) );
    }
}
