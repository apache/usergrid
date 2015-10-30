/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.asyncevents.model;


import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.usergrid.persistence.queue.QueueFig;


/**
 * Marker class for serialization
 *
 * Note that when you add a subtype, you will need to add it's serialization value below in the JsonSubTypes annotation.
 *
 * Each name must be unique, and must map to a subclass that is serialized
 */
@JsonIgnoreProperties( ignoreUnknown = true )
@JsonTypeInfo( use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT, property = "type" )
@JsonSubTypes( {
    @JsonSubTypes.Type( value = EdgeDeleteEvent.class, name = "edgeDeleteEvent" ),
    @JsonSubTypes.Type( value = EdgeIndexEvent.class, name = "edgeIndexEvent" ),
    @JsonSubTypes.Type( value = EntityDeleteEvent.class, name = "entityDeleteEvent" ),
    @JsonSubTypes.Type( value = EntityIndexEvent.class, name = "entityIndexEvent" ),
    @JsonSubTypes.Type( value = InitializeApplicationIndexEvent.class, name = "initializeApplicationIndexEvent" ),
    @JsonSubTypes.Type( value = ElasticsearchIndexEvent.class, name = "elasticsearchIndexEvent" )
} )

public abstract class AsyncEvent implements Serializable {

    @JsonProperty
    protected long creationTime;

    @JsonProperty
    protected String sourceRegion;

    // Needed for jackson, do not remove
    protected AsyncEvent(){

    }

    //set by default, will be overridden when de-serializing
    protected AsyncEvent(String sourceRegion) {


        creationTime = System.currentTimeMillis();
        this.sourceRegion = sourceRegion;

    }


    public long getCreationTime() {
        return creationTime;
    }
}
