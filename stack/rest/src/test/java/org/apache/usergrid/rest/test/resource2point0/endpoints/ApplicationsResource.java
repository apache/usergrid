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
package org.apache.usergrid.rest.test.resource2point0.endpoints;


import org.apache.usergrid.rest.test.resource.app.Collection;
import org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt.CredentialsResource;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Application;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.Token;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import javax.ws.rs.core.MediaType;


/**
 * Holds the information required for building and chaining application objects to collections.
 * app("applications").post();
 */
public class ApplicationsResource extends NamedResource {


    public ApplicationsResource( final String name, final ClientContext context, final UrlResource parent ) {
        super( name, context, parent );
    }


    public CollectionEndpoint collection(String name) {
        return new CollectionEndpoint(name,context,this);
    }


    public TokenResource token() {
        return new TokenResource( context, this );
    }


    /**
     * Delete this application.
     */
    public ApiResponse delete() {
        return getResource(true)
            .type( MediaType.APPLICATION_JSON_TYPE )
            .accept( MediaType.APPLICATION_JSON )
            .delete( ApiResponse.class );
    }

    public CredentialsResource credentials(){
        return new CredentialsResource(  context ,this );
    }


    /**
     * Used to get an application entity.
     */
    public ApiResponse get() {
        return getResource(true)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .accept( MediaType.APPLICATION_JSON )
            .get(ApiResponse.class);
    }
}
