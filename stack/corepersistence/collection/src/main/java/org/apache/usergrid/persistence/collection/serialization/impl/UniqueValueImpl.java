/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.common.base.Preconditions;

/**
 * Represents a Unique Value of a field within a collection.
 */
public class UniqueValueImpl implements UniqueValue {
    private final Field field;
    private final Id entityId;
    private final UUID entityVersion;

    public UniqueValueImpl(final Field field, Id entityId, final UUID version ) {

        Preconditions.checkNotNull( field, "field is required" );
        Preconditions.checkNotNull( version, "version is required" );
        Preconditions.checkNotNull( entityId, "entityId is required" );

        this.field = field;
        this.entityVersion = version;
        this.entityId = entityId;
    }



    @Override
    public Field getField() {
        return field;
    }

    @Override
    public UUID getEntityVersion() {
        return entityVersion;
    }

    @Override
    public Id getEntityId() {
        return entityId;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        final UniqueValueImpl that = ( UniqueValueImpl ) o;



        if ( !getField().equals( that.getField()) ) {
            return false;
        }

        if ( !getEntityVersion().equals( that.getEntityVersion() ) ) {
            return false;
        }

        if ( !getEntityId().equals( that.getEntityId() ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = 31 * getField().hashCode();
        result = 31 * result + getEntityVersion().hashCode();
        result = 31 * result + getEntityId().hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "UniqueValueImpl{" +
                ", field =" + field +
                ", entityVersion=" + entityVersion +
                ", entityId =" + entityId +
                '}';
    }

}
