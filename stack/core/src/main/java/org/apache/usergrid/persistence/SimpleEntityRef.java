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
package org.apache.usergrid.persistence;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;


public class SimpleEntityRef implements EntityRef {

    public static final UUID NULL_ID = new UUID( 0, 0 );

    protected final String type;

    protected final UUID uuid;


    public SimpleEntityRef( UUID uuid ) {
        this.uuid = uuid;
        type = null;
    }


    @JsonCreator
    public SimpleEntityRef(@JsonProperty("type")  String type,@JsonProperty("uuid")  UUID uuid ) {
        this.type = type;
        this.uuid = uuid;
    }


    public SimpleEntityRef( EntityRef entityRef ) {
        type = entityRef.getType();
        uuid = entityRef.getUuid();
    }


    public static SimpleEntityRef fromId(final Id id){
        return new SimpleEntityRef(id.getType(), id.getUuid()  );
    }

    public static EntityRef ref() {
        return new SimpleEntityRef( null, null );
    }


    @Override
    public UUID getUuid() {
        return uuid;
    }


    @Override
    public String getType() {
        return type;
    }


    @Override
    public Id asId() {
        return new SimpleId( uuid, type );
    }


    public static EntityRef ref( String entityType, UUID entityId ) {
        return new SimpleEntityRef( entityType, entityId );
    }


    public static EntityRef ref( UUID uuid ) {
        return new SimpleEntityRef( null, uuid );
    }


    public static EntityRef ref( EntityRef ref ) {
        return new SimpleEntityRef( ref );
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( uuid == null ) ? 0 : uuid.hashCode() );
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
        return result;
    }


    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        SimpleEntityRef other = ( SimpleEntityRef ) obj;
        if ( uuid == null ) {
            if ( other.uuid != null ) {
                return false;
            }
        }
        else if ( !uuid.equals( other.uuid ) ) {
            return false;
        }
        if ( type == null ) {
            if ( other.type != null ) {
                return false;
            }
        }
        else if ( !type.equals( other.type ) ) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        if ( ( type == null ) && ( uuid == null ) ) {
            return "EntityRef(" + NULL_ID.toString() + ")";
        }
        if ( type == null ) {
            return "EntityRef(" + uuid.toString() + ")";
        }
        return type + "(" + uuid + ")";
    }


    public static UUID getUuid( EntityRef ref ) {
        if ( ref == null ) {
            return null;
        }
        return ref.getUuid();
    }


    public static String getType( EntityRef ref ) {
        if ( ref == null ) {
            return null;
        }
        return ref.getType();
    }
}
