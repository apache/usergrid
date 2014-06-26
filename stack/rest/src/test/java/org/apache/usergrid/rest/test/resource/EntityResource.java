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
package org.apache.usergrid.rest.test.resource;


import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/** @author tnine */
public class EntityResource extends ValueResource {

    private String entityName;
    private UUID entityId;


    public EntityResource( String entityName, NamedResource parent ) {
        super( entityName, parent );
        this.entityName = entityName;
    }


    public EntityResource( UUID entityId, NamedResource parent ) {
        super( entityId.toString(), parent );
        this.entityId = entityId;
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.rest.resource.NamedResource#addToUrl(java.lang.StringBuffer)
     */
    @Override
    public void addToUrl( StringBuilder buffer ) {
        parent.addToUrl( buffer );

        buffer.append( SLASH );

        if ( entityId != null ) {
            buffer.append( entityId.toString() );
        }
        else if ( entityName != null ) {
            buffer.append( entityName );
        }
    }


    public JsonNode get() {
        try {
            return getInternal();
        }
        catch ( UniformInterfaceException uie ) {
            if ( uie.getResponse().getClientResponseStatus() == Status.NOT_FOUND ) {
                return null;
            }
            throw uie;
        }
        catch ( Exception ex ) {
            throw new RuntimeException("Error parsing JSON", ex);
        }
    }


    public JsonNode delete() throws IOException {
        return deleteInternal();
    }


    public JsonNode post( Map<String, ?> data ) throws IOException {
        return postInternal( data );
    }


    @SuppressWarnings("unchecked")
    public JsonNode post() throws IOException {
        return postInternal( Collections.EMPTY_MAP );
    }


    public Connection connection( String name ) {
        return new Connection( name, this );
    }


    public CustomCollection collection( String name ) {
        return new CustomCollection( name, this );
    }
}
