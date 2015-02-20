/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.model.entity;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.usergrid.persistence.model.field.*;
import org.apache.usergrid.persistence.model.field.value.EntityObject;

import com.fasterxml.jackson.annotation.*;
import com.google.common.base.Preconditions;
import org.apache.usergrid.persistence.model.field.value.Location;



/**
 * Simple entity that is used for persistence.  It has 1 required property, the Id.
 * Equality is based both on id an on version.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo( use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class" )
public class Entity extends EntityObject {

    private static MapToEntityConverter mapToEntityConverter = new MapToEntityConverter();

    /**
     * The id.  We should never serialize this
     */
    @JsonIgnore
    private transient Id id;

    /**
     * The version of this entity.
     *
     * Do not remove this, set by the collection manager
     */
    @JsonProperty
    private UUID version;


    /**
     * Create an entity with the given type and id.  Should be used for all update operations to an existing entity
     */
    public Entity( Id id ) {
       Preconditions.checkNotNull( id, "id must not be null" );

        this.id = id;
    }

    protected Entity(Id id, UUID version){
        this(id);
        this.version = version;
    }


    /**
     * Generate a new entity with the given type and a new id
     * @param type
     */
    public Entity(String type){
        this(new SimpleId( type ));
    }

    /**
     * Do not use!  This is only for serialization.
     */
    public Entity() {

    }

    /**
     * Generate an entity based on the map
     * @param map
     */
    public static Entity fromMap(EntityMap map){
        return mapToEntityConverter.fromMap(map,true);
    }

    @JsonIgnore
    public Id getId() {
        return id;
    }

    public UUID getVersion() {
        return version;
    }

    /**
     * Equality is based both on id and version.  If an entity
     * has the same id but different versions, they are not equals
     * @param o
     * @return
     */
    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof Entity ) ) {
            return false;
        }

        final Entity entity = ( Entity ) o;

        if ( id != null ? !id.equals( entity.id ) : entity.id != null ) {
            return false;
        }
        if ( version != null ? !version.equals( entity.version ) : entity.version != null ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + ( version != null ? version.hashCode() : 0 );
        return result;
    }


    @Override
    public String toString() {
        return "Entity{" +
                "id=" + id +
                ", version=" + version +
                '}';
    }

    public boolean hasVersion(){
        return getVersion() != null;
    }

}
