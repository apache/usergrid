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


import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Application;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import javax.ws.rs.core.MediaType;


/**
 * Holds the information required for building and chaining application objects to collections.
 * Should also contain the GET,PUT,POST,DELETE methods of functioning in here.
 * This class also holds how we're currently interaction with collections.
 */
public class ApplicationResource extends NamedResource {


    public ApplicationResource( final String name,final ClientContext context,  final UrlResource parent ) {
        super( name, context, parent );
    }




    public Application post(Application application){
        ApiResponse response = getResource().type( MediaType.APPLICATION_JSON_TYPE ).accept( MediaType.APPLICATION_JSON )
                .post( ApiResponse.class, application );

        return new Application(response);

    }

    public Application put(Application application){
        ApiResponse response = getResource().type( MediaType.APPLICATION_JSON_TYPE ).accept( MediaType.APPLICATION_JSON )
                .put( ApiResponse.class, application );

        return new Application(response);

    }


    public Application get(){
        ApiResponse response = getResource().type( MediaType.APPLICATION_JSON_TYPE ).accept( MediaType.APPLICATION_JSON )
                .get(ApiResponse.class);

        return new Application(response);

    }

    public void delete(Application application){
        ApiResponse response = getResource().type( MediaType.APPLICATION_JSON_TYPE ).accept( MediaType.APPLICATION_JSON )
                .delete( ApiResponse.class );
    }


    /**
     * Currently hardcoded to users, this is because we expect to create and chain different cases of collections.
     * The pattern should look like: orgs.apps.users , orgs.apps.groups and so on...
     * @return
     */
    public Collection users(){
        return new Collection("users", context , this);
    }

    /**
     * Currently hardcoded to users, this is because we expect to create and chain different cases of collections.
     * The pattern should look like: orgs.apps.users , orgs.apps.groups and so on...
     * @return
     */
    public Collection roles(){
        return new Collection("roles", context , this);
    }

    /**
     * Currently hardcoded to users, this is because we expect to create and chain different cases of collections.
     * The pattern should look like: orgs.apps.users , orgs.apps.groups and so on...
     * @return
     */
    public Collection permissions(){
        return new Collection("permissions", context , this);
    }

    /**
     * Currently hardcoded to users, this is because we expect to create and chain different cases of collections.
     * The pattern should look like: orgs.apps.users , orgs.apps.groups and so on...
     * @return
     */
    public Collection notifications(){
        return new Collection("notifications", context , this);
    }

}
