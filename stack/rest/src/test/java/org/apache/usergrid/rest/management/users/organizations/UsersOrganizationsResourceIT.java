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
package org.apache.usergrid.rest.management.users.organizations;


import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.rest.AbstractRestIT;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.apache.usergrid.utils.MapUtils.hashMap;


/** @author zznate */
@Concurrent()
public class UsersOrganizationsResourceIT extends AbstractRestIT {


    @Test
    public void createOrgFromUserConnectionFail() throws Exception {


        Map<String, String> payload = hashMap( "email", "orgfromuserconn@apigee.com" ).map( "password", "password" )
                .map( "organization", "orgfromuserconn" );

        JsonNode node = resource().path( "/management/organizations" ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );

        String userId = node.get( "data" ).get( "owner" ).get( "uuid" ).asText();

        assertNotNull( node );

        String token = mgmtToken( "orgfromuserconn@apigee.com", "password" );

        node = resource().path( String.format( "/management/users/%s/", userId ) ).queryParam( "access_token", token )
                .type( MediaType.APPLICATION_JSON_TYPE ).get( JsonNode.class );

        logNode( node );

        payload = hashMap( "organization", "Orgfromuserconn" );

        // try to create the same org again off the connection
        try {
            node = resource().path( String.format( "/management/users/%s/organizations", userId ) )
                    .queryParam( "access_token", token ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
            fail( "Should have thrown unique exception on org name" );
        }
        catch ( Exception ex ) {
        }
    }
}
