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
package org.apache.usergrid.rest.test.resource.app.queue;


import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.usergrid.rest.test.resource.CollectionResource;
import org.apache.usergrid.rest.test.resource.NamedResource;

import com.sun.jersey.api.client.WebResource;
import java.io.IOException;


/**
 * A resource for testing queues
 *
 * @author tnine
 */
public class Queue extends CollectionResource {

    private String clientId;
    private int limit = 0;
    private long timeout = 0;
    private String position;
    private String last;
    private String[] filters = { };


    /**
     *
     */
    public Queue( String queueName, NamedResource parent ) {
        super( queueName, parent );
    }


    /** Set the client id with the string */
    public Queue withClientId( String clientId ) {
        this.clientId = clientId;
        return this;
    }


    /** Set this with the next page size */
    public Queue withLimit( int limit ) {
        this.limit = limit;
        return this;
    }


    public Queue withPosition( String position ) {
        this.position = position;
        return this;
    }


    public Queue withLast( String last ) {
        this.last = last;
        return this;
    }


    public Queue withTimeout( long timeout ) {
        this.timeout = timeout;
        return this;
    }


    public Queue withFilters( String... filters ) {
        this.filters = filters;
        return this;
    }


    /**
     * @return
     */
    public SubscribersCollection subscribers() {
        return new SubscribersCollection( this );
    }


    /**
     * @return
     */
    public TransactionsCollection transactions() {
        return new TransactionsCollection( this );
    }


    public JsonNode post( Map<String, ?> payload ) throws IOException {
        JsonNode node = super.postInternal( payload );
        return node;
    }


    /**
     *
     * @param payload
     * @return
     */
    public JsonNode post( Map<String, ?>[] payload ) throws IOException {
        JsonNode node = super.postInternal( payload );
        return node;
    }


    /** Get entities in this collection. Cursor is optional */
    public JsonNode get() {
        try {
            return mapper.readTree( jsonMedia( withQueueParams( withToken( resource() ) ) ).get( String.class ));
        } catch (IOException ex) {
            throw new RuntimeException("Cannot parse JSON data", ex);
        }
    }


    /** post to the entity set */
    public JsonNode delete() {
        try {
            return mapper.readTree( jsonMedia( withToken( resource() ) ).delete( String.class ));
        } catch (IOException ex) {
            throw new RuntimeException("Cannot parse JSON data", ex);
        }
    }


    /** Set the queue client ID if set */
    private WebResource withQueueParams( WebResource resource ) {
        if ( clientId != null ) {
            resource = resource.queryParam( "consumer", clientId );
        }
        if ( position != null ) {
            resource = resource.queryParam( "pos", position );
        }
        if ( last != null ) {
            resource = resource.queryParam( "last", last );
        }

        if ( limit > 0 ) {
            resource = resource.queryParam( "limit", String.valueOf( limit ) );
        }

        if ( timeout > 0 ) {
            resource = resource.queryParam( "timeout", String.valueOf( timeout ) );
        }

        for ( String filter : filters ) {
            resource = resource.queryParam( "filter", filter );
        }

        return resource;
    }


    /** Get the next entry in the queue. Returns null if one doesn't exist */
    public JsonNode getNextEntry() {
        List<JsonNode> messages = getNodesAsList( "messages", get() );

        return messages.size() == 1 ? messages.get( 0 ) : null;
    }


    /** Get the json response of the messages nodes */
    public List<JsonNode> getNextPage() {
        JsonNode response = get();

        JsonNode last = response.get( "last" );

        if ( last != null ) {
            this.last = last.asText();
        }

        return getNodesAsList( "messages", response );
    }
}
