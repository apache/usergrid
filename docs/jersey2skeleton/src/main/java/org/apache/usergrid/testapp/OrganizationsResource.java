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

package org.apache.usergrid.testapp;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jersey.repackaged.com.google.common.collect.Lists;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;


@Api(value="/management/organizations", description = "Access to organizations.", tags="management")
@Path("/management/organizations")
@Component
@Scope("singleton")
@Produces({
        MediaType.APPLICATION_JSON,
        "application/javascript",
        "application/x-javascript",
        "text/ecmascript",
        "application/ecmascript",
        "text/jscript"
})
public class OrganizationsResource extends AbstractResource {

    @Path("/{id}")
    @ApiOperation(value = "Get organization by id.", response=ApiResponse.class)
    public OrganizationResource getOrganizationById(@PathParam("id") String id) {
        OrganizationResource or = getSubResource( OrganizationResource.class );
        or.init(id);
        return or;
    }

    @GET
    @ApiOperation(value = "Get organizations.", response=ApiResponse.class)
    public ApiResponse getOrganizations() {
        
        Entity org1 = new Entity();
        org1.setId( UUID.randomUUID() );
        org1.setName( "org1" );
        org1.setType( "organization" );
        
        Entity org2 = new Entity();
        org2.setId( UUID.randomUUID() );
        org2.setName( "org2" );
        org2.setType( "organization" );

        ApiResponse response = new ApiResponse();
        response.setContent( "All Organizations" );
        response.setEntities( Lists.newArrayList( org1, org2 ) );
        
        return response;
    } 
}
