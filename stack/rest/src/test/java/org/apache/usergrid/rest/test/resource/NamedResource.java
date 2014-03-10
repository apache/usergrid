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


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;


/**
 * @author tnine
 */
public abstract class NamedResource {

    protected static final String SLASH = "/";

    protected NamedResource parent;

    protected String accept;


    /**
     *
     */
    public NamedResource( NamedResource parent ) {
        this.parent = parent;
    }


    /**
     * Get the url to this resource
     */
    public String url() {
        StringBuilder buff = new StringBuilder();
        addToUrl( buff );
        return buff.toString();
    }


    /**
     * Get the resource for calling the url.  Will have the token pre-loaded if the token is set
     */
    protected WebResource resource() {
        return parent.resource();
    }


    /**
     * Get the token for this request
     */
    protected String token() {
        return parent.token();
    }


    /**
     * Set the media type on the webResource
     */
    protected Builder jsonMedia( WebResource resource ) {

        Builder builder = resource.type( MediaType.APPLICATION_JSON_TYPE );

        if(accept == null){
            builder = builder.accept( MediaType.APPLICATION_JSON );
        } else{
            builder = builder.accept( accept );
        }

        return builder;
    }


    protected WebResource withToken( WebResource resource ) {
        String token = token();

        resource = resource.path( url() );

        if ( token != null ) {
            resource = resource.queryParam( "access_token", token );
        }

        return resource;
    }


    public <T extends ValueResource> T withAccept( String accept ) {
        this.accept = accept;
        return ( T ) this;
    }


    /**
     * Method to add params to the generated url.  Subclasses can override it
     */
    protected WebResource withParams( WebResource resource ) {
        return resource;
    }


    /**
     * Get the entity from the entity array in the response
     */
    protected JsonNode getEntity( JsonNode response, int index ) {
        return response.get( "entities" ).get( index );
    }


    /**
     * Get the entity from the entity array in the response
     */
    protected JsonNode getEntity( JsonNode response, String name ) {
        return response.get( "entities" ).get( name );
    }


    /**
     * Get the uuid from the entity at the specified index
     */
    protected UUID getEntityId( JsonNode response, int index ) {
        return UUID.fromString( getEntity( response, index ).get( "uuid" ).asText() );
    }


    /**
     * Parse the root response and return each entity as a json node in a list
     */
    protected List<JsonNode> getEntries( JsonNode response ) {
        return getNodesAsList( "path", response );
    }


    /**
     * Get nodes as a list
     */
    protected List<JsonNode> getNodesAsList( String path, JsonNode response ) {
        JsonNode entities = response.path( path );

        int size = entities.size();


        List<JsonNode> entries = new ArrayList<JsonNode>();

        for ( int i = 0; i < size; i++ ) {

            entries.add( entities.get( i ) );
        }

        return entries;
    }


    /**
     * Get the error response
     */
    protected JsonNode getError( JsonNode response ) {
        return response.get( "error" );
    }


    /**
     * Add itself to the end of the URL. Should not append a final "/"  Shouldn't ever be used by the client!
     */
    public abstract void addToUrl( StringBuilder buffer );
}
