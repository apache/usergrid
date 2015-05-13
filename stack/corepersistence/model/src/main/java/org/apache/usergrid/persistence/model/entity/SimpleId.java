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


import java.io.Serializable;
import java.util.UUID;

import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.model.util.Verify;


import com.fasterxml.uuid.UUIDComparator;
import com.google.common.base.Preconditions;


/** @author tnine */
public class SimpleId implements Id, Serializable {


    private UUID uuid;
    private String type;


    /**
     * Do not delete!  Needed for Jackson
     */
    @SuppressWarnings( "unused" )
    public SimpleId(){

    }



    public SimpleId( final UUID uuid, final String type ) {
        Preconditions.checkNotNull( uuid, "uuid is required" );
        Verify.stringExists( type, "type is required" );

        this.uuid = uuid;
        this.type = type;
    }


    /**
     * Create a new ID.  Should only be used for new entities
     * @param type
     */
    public SimpleId( final String type ){
       this(UUIDGenerator.newTimeUUID(), type);
    }


    @Override
    public UUID getUuid() {
        return uuid;
    }


    @Override
    public String getType() {
        return type;
    }



    /**
     * Do not delete!  Needed for Jackson
     */
    @SuppressWarnings( "unused" )
    public void setType( final String type ) {
        this.type = type;
    }



    /**
     * Do not delete!  Needed for Jackson
     */
    @SuppressWarnings( "unused" )
    public void setUuid( final UUID uuid ) {
        this.uuid = uuid;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof Id ) ) {
            return false;
        }

        final Id id = ( Id ) o;

        if ( !type.equals( id.getType() ) ) {
            return false;
        }
        if ( !uuid.equals( id.getUuid() ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = uuid.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "SimpleId{" +
                "uuid=" + uuid +
                ", type='" + type + '\'' +
                '}';
    }


    @Override
    public int compareTo( final Id o ) {

        int compare = UUIDComparator.staticCompare( uuid, o.getUuid() );

        if(compare == 0){
            compare = type.compareTo( o.getType() );
        }

        return compare;
    }
}
