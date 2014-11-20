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
package org.apache.usergrid.rest.test.resource.app;


import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.apache.usergrid.rest.test.resource.CollectionResource;
import org.apache.usergrid.rest.test.resource.Me;
import org.apache.usergrid.rest.test.resource.NamedResource;
import org.apache.usergrid.utils.MapUtils;


/** @author tnine */
public class UsersCollection extends Collection {


    public UsersCollection( NamedResource parent ) {
        super( "users", parent );
    }


    public User user( String username ) {
        return new User( username, this );
    }


    public User user( UUID id ) {
        return new User( id, this );
    }


    /** Create the user */
    public JsonNode post( String username, String email, String password ) throws IOException {
        Map<String, String> data =
                MapUtils.hashMap( "username", username ).map( "email", email ).map( "password", password );

        JsonNode response = this.postInternal( data );

        return getEntity( response, 0 );
    }

    /** Create the user */
    //TODO: delete create method once rest calls are implemented
    public JsonNode create( String username, String email, String password ) throws IOException {
        Map<String, String> data =
                MapUtils.hashMap( "username", username ).map( "email", email ).map( "password", password );

        JsonNode response = this.postInternal( data );

        return getEntity( response, 0 );
    }

    public Me me() {
        return new Me( this );
    }
}
