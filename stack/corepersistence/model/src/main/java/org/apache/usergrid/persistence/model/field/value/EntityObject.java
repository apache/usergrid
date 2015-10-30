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
package org.apache.usergrid.persistence.model.field.value;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.usergrid.persistence.model.field.Field;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Simple wrapper for holding nested objects
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityObject implements Serializable {

    @JsonIgnore
    private long size;

    /**
     * Fields the users can set
     */
    @JsonTypeInfo( use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class" )
    private final Map<String, Field> fields = new HashMap<String, Field>();

    /**
     * Add the field, return the old one if it existed
     */
    @JsonIgnore
    public <T extends java.lang.Object> Field<T> setField( Field<T> value ) {
        return fields.put( value.getName(), value );
    }

    /**
     * Get the field by name the user has set into the entity
     */
    @JsonIgnore
    public <T extends java.lang.Object> Field<T> getField( String name ) {
        return fields.get( name );
    }

    public void removeField( String name ) {
        fields.remove( name );
    }

    //@JsonAnySetter
    public void setFields(ArrayList al) {

        if ( al.isEmpty()) {
            return;
        }

        for(int i = 0; i < al.size(); i++) {
            String str = al.get( i ).toString();
            if ( str.contains( "version" )) {
                continue;
            }
            Field fd = ( Field ) al.get( i );
            fields.put( fd.getName(), fd);
        }
    }

    /**
     * Get all fields in the entity
     */
    //@JsonAnyGetter
    public Collection<Field> getFields() {
        return fields.values();
    }

    @JsonAnyGetter
    public Map<String, Field> getFieldMap() {
        return fields;
    }

    @JsonAnySetter
    public void setFieldMap( Map<String, Field> fieldMap ) {
        fields.clear();
        fields.putAll( fieldMap );
    }

    public long getSize() {
        return this.size;
    }
    public void setSize(long size) {
        this.size = size;
    }
}
