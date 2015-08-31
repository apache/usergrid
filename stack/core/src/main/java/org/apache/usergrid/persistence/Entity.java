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


import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.usergrid.persistence.annotations.EntityProperty;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.apache.usergrid.persistence.Schema.PROPERTY_URI;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;


/** Entities are the base object type in the service. */
@XmlRootElement
@JsonPropertyOrder({ PROPERTY_UUID, PROPERTY_TYPE, PROPERTY_URI, PROPERTY_NAME })
public interface Entity extends EntityRef, Comparable<Entity> {

    @Override
    @EntityProperty(required = true, mutable = false, basic = true, indexed = false)
    public UUID getUuid();

    public void setUuid( UUID id );

    @Override
    @EntityProperty(required = true, mutable = false, basic = true, indexed = true)
    public String getType();

    public void setType( String type );

    public abstract String getName();

    @EntityProperty(indexed = true, required = true, mutable = false)
    public abstract Long getCreated();

    public abstract void setCreated( Long created );

    @EntityProperty(indexed = true, required = true, mutable = true)
    public abstract Long getModified();

    public abstract void setModified( Long modified );

    @JsonIgnore
    public Map<String, Object> getProperties();

    void setProperties( Map<String, Object> properties );

    void setProperties(org.apache.usergrid.persistence.model.entity.Entity cpEntity);

    public void addProperties( Map<String, Object> properties );

    public abstract Object getProperty( String propertyName );

    public abstract void setProperty( String propertyName, Object propertyValue );

    @Override
    public abstract int compareTo( Entity o );

    public abstract Entity toTypedEntity();

    public abstract Object getMetadata( String key );

    public abstract void setMetadata( String key, Object value );

    public abstract void mergeMetadata( Map<String, Object> metadata );

    public abstract void clearMetadata();

    public abstract List<Entity> getCollections( String key );

    public abstract void setCollections( String name, List<Entity> results );

    public abstract List<Entity> getConnections( String key );

    public abstract void setConnections( String name, List<Entity> results );

    @JsonAnySetter
    public abstract void setDynamicProperty( String key, Object value );

    @JsonAnyGetter
    public abstract Map<String, Object> getDynamicProperties();

    @JsonIgnore
    long getSize();
    void setSize(long size);

}
