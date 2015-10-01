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

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


/**
 * Interface for fields.  All fields must implement this method The T is the type of field
 * (in the java runtime) The V is the value of the field
 * @param <T>
 */
@JsonTypeInfo( use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class" )
public interface Field<T> extends Serializable {

    /**
     * Get the name of the field
     * @return
     */
    @JsonProperty
    public String getName();
    /**
     * Get the name of the field
     * @return
     */
    @JsonProperty
    public void setName(final String newName);
    /**
     * Get the value of the field
     * @return
     */
    @JsonProperty
    public T getValue();

    /**
     * True if field value must be unique within Entity Collection.
     * @return
     */
    public boolean isUnique();

    /**
     * Get the type of the field as a string.  string, boolean, long, integer etc
     * @return
     */
    @JsonIgnore
    public FieldTypeName getTypeName();

}
