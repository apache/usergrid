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

package org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt;


import javax.ws.rs.core.MediaType;

import org.apache.usergrid.rest.test.resource2point0.endpoints.EntityEndpoint;
import org.apache.usergrid.rest.test.resource2point0.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import com.sun.jersey.api.client.WebResource;


/**
 * Handles calls to the users management endpoint
 * Example: /management/orgs/org_name/users
 */
public class UsersResource extends NamedResource {
    public UsersResource( final ClientContext context, final UrlResource parent ) {
        super( "users", context, parent );
    }


    /**
     * Should this be here? this would facilitate calling the entity endpoint as a way to get/put things
     * @param identifier
     * @return
     */
    //TODO: See if this should be reused here or if we should rename it to something else.
    public EntityEndpoint entity(String identifier) {
        return new EntityEndpoint(identifier, context, this);
    }

    public UserResource user(String identifier) {
        return new UserResource( identifier, context, this );
    }

}
