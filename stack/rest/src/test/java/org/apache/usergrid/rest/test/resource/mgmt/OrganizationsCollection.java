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
package org.apache.usergrid.rest.test.resource.mgmt;


import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.apache.usergrid.rest.test.resource.CollectionResource;
import org.apache.usergrid.rest.test.resource.NamedResource;
import org.apache.usergrid.rest.test.resource.OrgUserUUIDWrapper;
import org.apache.usergrid.rest.test.security.TestUser;
import org.apache.usergrid.utils.MapUtils;


/** @author tnine */
public class OrganizationsCollection extends CollectionResource {

    /**
     * @param collectionName
     * @param parent
     */
    public OrganizationsCollection( NamedResource parent ) {
        super( "orgs", parent );
    }


    public Organization organization( String name ) {
        return new Organization( name, this );
    }


    /** Create the org and return it's UUID */
    public OrgUserUUIDWrapper create( String name, TestUser owner ) throws IOException {
//not entirely convinced we want to do this here, maybe we should shove the node and data get another level deeper?
        JsonNode node = postInternal(mapOrganization( name,owner.getUser(),owner.getEmail(),owner.getUser(),owner.getPassword() ) );

//org && user uuid wrapper
        OrgUserUUIDWrapper wrapper = new OrgUserUUIDWrapper(getOrgUUID( node ),getUserUUID(node) );

        return wrapper;
    }

    public UUID getOrgUUID(JsonNode node){
        return UUID.fromString(node.get("data").get( "organization" ).get("uuid").asText());
    }

    public UUID getUserUUID (JsonNode node){
        return UUID.fromString( node.get( "data" ).get( "owner" ).get( "uuid" ).asText() );
    }

    public Map<String,String> mapOrganization(String orgName, String username, String email, String name, String password){

        return MapUtils.hashMap( "organization", orgName ).map( "username", username )
                       .map( "email", email ).map( "name", name )
                       .map( "password", password);
    }
}
