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


/** @author rockerston */
public class GroupsCollection extends CollectionResource {


    public GroupsCollection( NamedResource parent ) {
        super( "groups", parent );
    }


    public Group group( String username ) {
        return new Group( username, this );
    }


    public Group group( UUID id ) {
        return new Group( id, this );
    }


    /** Create the group */
    public JsonNode create( String path, String title ) throws IOException {
        Map<String, String> data =
                MapUtils.hashMap( "path", path ).map( "title", title );

        JsonNode response = this.postInternal( data );

        return getEntity( response, 0 );
    }




    public Me me() {
        return new Me( this );
    }
}
