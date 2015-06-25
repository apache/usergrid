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


import org.apache.usergrid.rest.test.resource2point0.endpoints.*;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;


/**
 * Contains the REST methods to interacting with the ManagementEndpoints
 */
public class ManagementResource extends NamedResource {
    public ManagementResource( final ClientContext context, final UrlResource parent ) {
        super( "management", context, parent );
    }

    public TokenResource token(){
        return new TokenResource( context, this );
    }

    public AuthorizeResource authorize(){
        return new AuthorizeResource( context, this );
    }

    public OrgResource orgs() {
        return new OrgResource( context, this );
    }

    public UsersResource users() {
        return new UsersResource( context, this );
    }

    public EntityEndpoint get(final String identifier){
        return new EntityEndpoint(identifier, context, this);
    }

}
