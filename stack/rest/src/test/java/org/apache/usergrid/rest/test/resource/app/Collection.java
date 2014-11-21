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

import java.io.IOException;
import java.util.Map;
import org.apache.usergrid.rest.test.resource.NamedResource;
import org.apache.usergrid.rest.test.resource.SetResource;
import org.apache.usergrid.utils.MapUtils;

import com.fasterxml.jackson.databind.JsonNode;


//TODO: G make sure this no longer returns JsonNodes and instead returns EntityObjects.
//TODO: Add in full rest suite of GET,PUT,DELETE methods. Delete will be mostly universal.
public class Collection extends SetResource {

    public Collection( String collectionName, NamedResource parent ) {
        super( collectionName, parent );
    }

    /** Create the user in a collection using only the username */
    /**
     * POST an entity with only a name
     * @param name
     * @return JsonNode
     * @throws IOException
     */
    public JsonNode post( String name ) throws IOException {
        Map<String, String> data = MapUtils.hashMap( "name", name );

        JsonNode response = this.postInternal( data );

        return getEntity( response, 0 );
    }

    /**
     * POST an entity with a name and a Map (e.g. if you want to add in a location sub-object
     * @param name
     * @param entityData
     * @return JsonNode
     * @throws IOException
     */
    public JsonNode post( String name, Map entityData ) throws IOException {
        Map<String, String> data = MapUtils.hashMap( "name", name );
        data.putAll(entityData);
        JsonNode response = this.postInternal( data );

        return getEntity( response, 0 );
    }

    /**
     * POST an entity with only a Map
     * @param entityData
     * @return JsonNode
     * @throws IOException
     */
    public JsonNode post(Map entityData) throws IOException{

        JsonNode response = this.postInternal( entityData );

        return getEntity( response, 0 );
    }

}

