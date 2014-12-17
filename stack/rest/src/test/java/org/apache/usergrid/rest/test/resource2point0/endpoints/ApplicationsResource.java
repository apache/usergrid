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
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Application;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import javax.ws.rs.core.MediaType;


/**
 * Holds the information required for building and chaining application objects to collections.
 * Should also contain the GET,PUT,POST,DELETE methods of functioning in here.
 * This class also holds how we're currently interaction with collections.
 */
public class ApplicationsResource extends AbstractCollectionResource<Application,CollectionResource> {


    public ApplicationsResource(final String name, final ClientContext context, final UrlResource parent) {
        super( name, context, parent );
    }

    @Override
    protected Application instantiateT(ApiResponse response) {
        return new Application(response);
    }

    @Override
    protected CollectionResource instantiateSubResource(String identifier, ClientContext context, UrlResource parent) {
        return new CollectionResource(identifier,context,parent);
    }

    public CollectionResource collections(String name) {
        return this.uniqueID(name);
    }


    /**
     * Currently hardcoded to users, this is because we expect to create and chain different cases of collections.
     * The pattern should look like: orgs.apps.users , orgs.apps.groups and so on...
     * @return
     */
    public UsersResource users(){
        return new UsersResource( context , this);
    }

    public GroupsResource groups(){
        return new GroupsResource( context , this);
    }

    public RolesResource roles(){
        return new RolesResource( context , this);
    }

    /**
     * Currently hardcoded to users, this is because we expect to create and chain different cases of collections.
     * The pattern should look like: orgs.apps.users , orgs.apps.groups and so on...
     * @return
     */
    /*
    public CollectionResource roles(){
        return new CollectionResource("roles", context , this);
    }
*/
    /**
     * Currently hardcoded to users, this is because we expect to create and chain different cases of collections.
     * The pattern should look like: orgs.apps.users , orgs.apps.groups and so on...
     * @return
     */
    public CollectionResource permissions(){
        return new CollectionResource("permissions", context , this);
    }

    /**
     * Currently hardcoded to users, this is because we expect to create and chain different cases of collections.
     * The pattern should look like: orgs.apps.users , orgs.apps.groups and so on...
     * @return
     */
    public CollectionResource notifications(){
        return new CollectionResource("notifications", context , this);
    }

}
