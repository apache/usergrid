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
package org.apache.usergrid.persistence.model.field;

import org.apache.usergrid.persistence.model.field.value.EntityObject;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base class for data information
 */
@JsonTypeInfo( use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class" )

public abstract class AbstractField<T> implements Field<T> {

    /**
     * Set the object this field belongs to
     */
    protected EntityObject parent;
    protected String name;
    protected Boolean unique;
    protected T value;

    /**
     * Create field with unqiue value; name and value must always be present.
     *
     * @param name The name of this field
     * @param value The value to set. If value is null, this means that the value should be
     * explicitly removed from the field storage
     */
    protected AbstractField( String name, T value, boolean unique  ) {
        this.name = name;
        this.value = value;
        this.unique = unique;
    }

    /**
     * Create field with non-unique value; name and value must always be present.
     *
     * @param name The name of this field
     * @param value The value to set. If value is null, this means that the value should be
     * explicitly removed from the field storage
     */
    protected AbstractField( String name, T value ) {
        this.name = name;
        this.value = value;
        this.unique = false;
    }

    /**
     * Default constructor for serialization
     */
    protected AbstractField() {

    }

    public String getName() {
        return name;
    }

    public boolean isUnique() {
        return unique;
    }


    /**
     * Validate the entity.  Should throw an IllegalArgumentException if this value cannot be validated
     */
    public void validate(){

    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        AbstractField that = (AbstractField) o;

        if ( !name.equals( that.name ) ) {
            return false;
        }

        if ( value != null && !value.equals( that.value )) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }


}
