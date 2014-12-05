/**
 * Created by ApigeeCorporation on 12/4/14.
 */
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


package org.apache.usergrid.rest.test.resource2point0.model;


import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.usergrid.persistence.EntityFactory;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.annotations.EntityProperty;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.apache.usergrid.persistence.Schema.PROPERTY_URI;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;


/**
 * Contains a model that can be deconstructed from the api response. This is a base level value that contains the bare
 * minumum of what other classes use. Such as . users or groups.
 */

@XmlRootElement
public class Entity implements Serializable {

    protected UUID uuid;

    protected Long created;

    protected Long modified;

    protected String type;

    protected Map<String, Object> dynamic_properties = new TreeMap<String, Object>( String.CASE_INSENSITIVE_ORDER );

    protected Map<String, Set<Object>> dynamic_sets = new TreeMap<String, Set<Object>>( String.CASE_INSENSITIVE_ORDER );


    @EntityProperty( required = true, mutable = false, basic = true, indexed = false )
    @JsonSerialize( include = JsonSerialize.Inclusion.NON_NULL )
    public UUID getUuid() {
        return uuid;
    }


    public void setUuid( UUID uuid ) {
        this.uuid = uuid;
    }


    @EntityProperty( required = true, mutable = false, basic = true, indexed = false )
    public String getType() {
        return type;
        //return Schema.getDefaultSchema().getEntityType( this.getClass() );
    }


    public void setType( String type ) {
        this.type = type;
    }


    @EntityProperty( indexed = true, required = true, mutable = false )
    @JsonSerialize( include = JsonSerialize.Inclusion.NON_NULL )
    public Long getCreated() {
        return created;
    }


    public void setCreated( Long created ) {
        if ( created == null ) {
            created = System.currentTimeMillis();
        }
        this.created = created;
    }


    @EntityProperty( indexed = true, required = true, mutable = true )
    @JsonSerialize( include = JsonSerialize.Inclusion.NON_NULL )
    public Long getModified() {
        return modified;
    }


    public void setModified( Long modified ) {
        if ( modified == null ) {
            modified = System.currentTimeMillis();
        }
        this.modified = modified;
    }


    @JsonSerialize( include = JsonSerialize.Inclusion.NON_NULL )
    public String getName() {
        Object value = getProperty( PROPERTY_NAME );

        if ( value instanceof UUID ) {
            // fixes existing data that uses UUID in USERGRID-2099
            return value.toString();
        }

        return ( String ) value;
    }


    @JsonIgnore
    public Map<String, Object> getProperties() {
        return dynamic_properties;
    }


    public final Object getProperty( String propertyName ) {
        return dynamic_properties.get( propertyName );
    }


    public final void setProperty( String propertyName, Object propertyValue ) {
        if ( propertyValue == null || propertyValue.equals( "" ) ) {
            if ( dynamic_properties.containsKey( propertyName ) ) {
                dynamic_properties.remove( propertyName );
            }
        }
        else {
            dynamic_properties.put( propertyName, propertyValue );
        }
    }


    public void setProperties( Map<String, Object> properties ) {
        dynamic_properties = new TreeMap<String, Object>( String.CASE_INSENSITIVE_ORDER );
        addProperties( properties );
    }


    public void addProperties( Map<String, Object> properties ) {
        if ( properties == null ) {
            return;
        }
        for ( Map.Entry<String, Object> entry : properties.entrySet() ) {
            setProperty( entry.getKey(), entry.getValue() );
        }
    }


    @JsonSerialize( include = JsonSerialize.Inclusion.NON_NULL )
    public Object getMetadata( String key ) {
        return getDataset( "metadata", key );
    }


    public void setMetadata( String key, Object value ) {
        setDataset( "metadata", key, value );
    }


    public void mergeMetadata( Map<String, Object> new_metadata ) {
        mergeDataset( "metadata", new_metadata );
    }


    public void clearMetadata() {
        clearDataset( "metadata" );
    }


    public <T> T getDataset( String property, String key ) {
        Object md = dynamic_properties.get( property );
        if ( md == null ) {
            return null;
        }
        if ( !( md instanceof Map<?, ?> ) ) {
            return null;
        }
        @SuppressWarnings( "unchecked" ) Map<String, T> metadata = ( Map<String, T> ) md;
        return metadata.get( key );
    }


    public <T> void setDataset( String property, String key, T value ) {
        if ( key == null ) {
            return;
        }
        Object md = dynamic_properties.get( property );
        if ( !( md instanceof Map<?, ?> ) ) {
            md = new HashMap<String, T>();
            dynamic_properties.put( property, md );
        }
        @SuppressWarnings( "unchecked" ) Map<String, T> metadata = ( Map<String, T> ) md;
        metadata.put( key, value );
    }


    public <T> void mergeDataset( String property, Map<String, T> new_metadata ) {
        Object md = dynamic_properties.get( property );
        if ( !( md instanceof Map<?, ?> ) ) {
            md = new HashMap<String, T>();
            dynamic_properties.put( property, md );
        }
        @SuppressWarnings( "unchecked" ) Map<String, T> metadata = ( Map<String, T> ) md;
        metadata.putAll( new_metadata );
    }


    public void clearDataset( String property ) {
        dynamic_properties.remove( property );
    }


    public List<org.apache.usergrid.persistence.Entity> getCollections( String key ) {
        return getDataset( "collections", key );
    }


    public void setCollections( String key, List<org.apache.usergrid.persistence.Entity> results ) {
        setDataset( "collections", key, results );
    }


    public List<org.apache.usergrid.persistence.Entity> getConnections( String key ) {
        return getDataset( "connections", key );
    }


    public void setConnections( String key, List<org.apache.usergrid.persistence.Entity> results ) {
        setDataset( "connections", key, results );
    }


    public String toString() {
        return "Entity(" + getProperties() + ")";
    }


    @JsonAnySetter
    public void setDynamicProperty( String key, Object value ) {
        if ( value == null || value.equals( "" ) ) {
            if ( dynamic_properties.containsKey( key ) ) {
                dynamic_properties.remove( key );
            }
        }
        else {
            dynamic_properties.put( key, value );
        }
    }


    @JsonAnyGetter
    public Map<String, Object> getDynamicProperties() {
        return dynamic_properties;
    }


    public final int compareTo( org.apache.usergrid.persistence.Entity o ) {
        if ( o == null ) {
            return 1;
        }
        try {
            long t1 = getUuid().timestamp();
            long t2 = o.getUuid().timestamp();
            return ( t1 < t2 ) ? -1 : ( t1 == t2 ) ? 0 : 1;
        }
        catch ( UnsupportedOperationException e ) {
        }
        return getUuid().compareTo( o.getUuid() );
    }


    public org.apache.usergrid.persistence.Entity toTypedEntity() {
        org.apache.usergrid.persistence.Entity entity = EntityFactory.newEntity( getUuid(), getType() );
        entity.setProperties( getProperties() );
        return entity;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */


    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( uuid == null ) ? 0 : uuid.hashCode() );
        return result;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */

    //    public boolean equals( Object obj ) {
    //        if ( this == obj ) {
    //            return true;
    //        }
    //        if ( obj == null ) {
    //            return false;
    //        }
    //        if ( getClass() != obj.getClass() ) {
    //            return false;
    //        }
    //        AbstractEntity other = ( AbstractEntity ) obj;
    //        if ( uuid == null ) {
    //            if ( other.uuid != null ) {
    //                return false;
    //            }
    //        }
    //        else if ( !uuid.equals( other.uuid ) ) {
    //            return false;
    //        }
    //        return true;
    //    }
}
