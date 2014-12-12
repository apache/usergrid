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


import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;


/**
 * Holds the information required for building and chaining application objects to collections.
 * Should also contain the GET,PUT,POST,DELETE methods of functioning in here.
 * This class also holds how we're currently interaction with collections.
 */
public class ApplicationResource extends NamedResource {


    public ApplicationResource( final String name,final ClientContext context,  final UrlResource parent ) {
        super( name, context, parent );
    }


    /**
     * Currently hardcoded to users, this is because we expect to create and chain different cases of collections.
     * The pattern should look like: orgs.apps.users , orgs.apps.groups and so on...
     * @return
     */
//    public Collection users(){
//        return new Collection("users", context , this);
//    }
}
