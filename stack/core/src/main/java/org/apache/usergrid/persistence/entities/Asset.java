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


import java.util.Set;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;


import org.apache.usergrid.persistence.TypedEntity;
import org.apache.usergrid.persistence.annotations.EntityDictionary;
import org.apache.usergrid.persistence.annotations.EntityProperty;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;



/**
 * Asset entity for representing file-like objects.
 *
 * @deprecated
 */
@XmlRootElement
public class Asset extends TypedEntity {

    public static final String ENTITY_TYPE = "asset";

    @EntityProperty(required = true, indexed = true, mutable = false)
    UUID owner;

    @EntityProperty(indexed = true, fulltextIndexed = false, required = true, aliasProperty = true,
            pathBasedName = true, mutable = false, unique = true)
    protected String path;

    @EntityDictionary(keyType = java.lang.String.class)
    protected Set<String> connections;


    public Asset() {
        // id = UUIDUtils.newTimeUUID();
    }


    public Asset( UUID id ) {
        this.uuid = id;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getPath() {
        return path;
    }


    public void setPath( String name ) {
        path = name;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public UUID getOwner() {
        return owner;
    }


    public void setOwner( UUID owner ) {
        this.owner = owner;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Set<String> getConnections() {
        return connections;
    }


    public void setConnections( Set<String> connections ) {
        this.connections = connections;
    }
}
