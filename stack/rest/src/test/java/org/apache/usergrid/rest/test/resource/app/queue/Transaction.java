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


import com.fasterxml.jackson.databind.JsonNode;
import org.apache.usergrid.rest.test.resource.CollectionResource;
import org.apache.usergrid.rest.test.resource.NamedResource;

import com.sun.jersey.api.client.WebResource;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A resource for testing queues
 *
 * @author tnine
 */
public class Transaction extends CollectionResource {

    private String clientId;
    private long timeout = 0;


    /**
     *
     */
    public Transaction( String transactionName, NamedResource parent ) {
        super( transactionName, parent );
    }


    /** Set the client id with the string */
    public Transaction withClientId( String clientId ) {
        this.clientId = clientId;
        return this;
    }


    /** post to the entity set */
    public JsonNode delete() {
        try {
            return mapper.readTree( jsonMedia( withParams( withToken( resource() ) ) ).delete( String.class ));
        } catch (IOException ex) {
            throw new RuntimeException("Cannot parse JSON data", ex);
        }
    }


    /** Renew this transaction to the set timeout */
    public JsonNode renew( long timeout ) throws IOException {
        this.timeout = timeout;
        return super.putInternal( null );
    }


    /** Set the queue client ID if set */
    protected WebResource withParams( WebResource resource ) {
        if ( clientId != null ) {
            resource = resource.queryParam( "consumer", clientId );
        }
        if ( timeout > 0 ) {
            resource = resource.queryParam( "timeout", String.valueOf( timeout ) );
        }

        return resource;
    }
}
