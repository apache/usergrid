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
package org.apache.usergrid.rest.test.resource.endpoints.mgmt;

import org.apache.usergrid.rest.test.resource.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.state.ClientContext;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;



/**
 * Relations to the following endpoint
 * /management/users/"username"
 * Store endpoints relating to specific users
 */
public class UserResource extends NamedResource {

    public UserResource( final String name, final ClientContext context, final UrlResource parent ) {
        super( name, context, parent );
    }

    public ReactivateResource reactivate() {
        return new ReactivateResource( context, this );
    }

    public ConfirmResource confirm() {
        return new ConfirmResource(context,this);
    }

    public PasswordResource password() {
        return new PasswordResource( context, this );
    }

    public FeedResource feed() {
        return new FeedResource( context, this );
    }

    public ResetResource resetpw() {
        return new ResetResource(context,this);
    }

    public OrgResource organizations() {
        return new OrgResource( context, this );
    }

    public RevokeTokensResource revokeTokens() {
        return new RevokeTokensResource( context, this );
    }

    public RevokeTokenResource revokeToken() {
        return new RevokeTokenResource( context, this );
    }

    public Entity get() {
        WebTarget resource = getTarget( true );
        ApiResponse response = resource.request()
            .accept( MediaType.APPLICATION_JSON )
            .get( ApiResponse.class );
        return new Entity(response);
    }

    public Entity put(Entity userPayload){
        WebTarget resource = getTarget( true );

        ApiResponse response = resource.request()
            .accept( MediaType.APPLICATION_JSON )
            .put( javax.ws.rs.client.Entity.json(userPayload), ApiResponse.class);
        return new Entity(response);
    }

}
