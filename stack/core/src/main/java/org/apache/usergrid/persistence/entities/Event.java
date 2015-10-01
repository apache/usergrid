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
package org.apache.usergrid.persistence.entities;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.usergrid.persistence.TypedEntity;
import org.apache.usergrid.persistence.annotations.EntityDictionary;
import org.apache.usergrid.persistence.annotations.EntityProperty;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;


/** An event type posted by the application. */
@XmlRootElement
public class Event extends TypedEntity {

    public static final String ENTITY_TYPE = "event";

    @EntityProperty(required = true, indexed = true, mutable = false)
    long timestamp = System.currentTimeMillis();

    @EntityProperty(required = false, indexed = true, mutable = false)
    UUID user;

    @EntityProperty(required = false, indexed = true, mutable = false)
    UUID group;

    @EntityProperty(fulltextIndexed = false, required = false, mutable = false, indexed = true)
    String category;

    @EntityProperty(indexed = false, required = false, mutable = false)
    Map<String, Integer> counters;

    @EntityProperty(indexed = true, required = false, mutable = false)
    String message;

    @EntityDictionary(keyType = java.lang.String.class)
    protected Set<String> connections;


    public Event() {
        // id = UUIDUtils.newTimeUUID();
    }


    public Event( UUID id ) {
        uuid = id;
    }


    public long getTimestamp() {
        return timestamp;
    }


    public void setTimestamp( long timestamp ) {
        if ( timestamp == 0 ) {
            timestamp = System.currentTimeMillis();
        }
        this.timestamp = timestamp;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public UUID getUser() {
        return user;
    }


    public void setUser( UUID user ) {
        this.user = user;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public UUID getGroup() {
        return group;
    }


    public void setGroup( UUID group ) {
        this.group = group;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getCategory() {
        return category;
    }


    public void setCategory( String category ) {
        this.category = category;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Map<String, Integer> getCounters() {
        return counters;
    }


    public void setCounters( Map<String, Integer> counters ) {
        this.counters = counters;
    }


    public void addCounter( String name, int value ) {
        if ( counters == null ) {
            counters = new HashMap<String, Integer>();
        }
        counters.put( name, value );
    }


    public String getMessage() {
        return message;
    }


    public void setMessage( String message ) {
        this.message = message;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Set<String> getConnections() {
        return connections;
    }


    public void setConnections( Set<String> connections ) {
        this.connections = connections;
    }
}
