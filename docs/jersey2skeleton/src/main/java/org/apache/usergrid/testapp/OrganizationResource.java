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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.UUID;


@Component
@Scope( "singleton" )
@Produces({
    MediaType.APPLICATION_JSON, 
    "application/javascript", 
    "application/x-javascript", 
    "text/ecmascript", 
    "application/ecmascript", 
    "text/jscript"
})
public class OrganizationResource extends AbstractResource {
    private String id;

    public OrganizationResource() {
    }
    
    public void init( String id ) {
        this.id = id;
    }

    @GET
    public ApiResponse getOrganization() {
        Entity org = new Entity();
        org.setName( "org:" + id );
        org.setId( UUID.randomUUID() );
        org.setType( "organization" );
        ApiResponse response = new ApiResponse();
        response.setContent( "organization:" + id );
        response.setEntities( Collections.singletonList( org ));
        return response;
    }
}
