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


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.usergrid.corepersistence.util.CpEntityMapUtils;
import org.apache.usergrid.persistence.annotations.EntityProperty;
import org.apache.usergrid.persistence.model.entity.EntityToMapConverter;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;

//import org.codehaus.jackson.annotate.JsonAnyGetter;
//import org.codehaus.jackson.annotate.JsonAnySetter;
//import org.codehaus.jackson.annotate.JsonIgnore;
//import org.codehaus.jackson.map.annotate.JsonSerialize;
//import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;


/**
 * The abstract superclass implementation of the Entity interface.
 *
 * @author edanuff
 */
@XmlRootElement
public abstract class AbstractEntity implements Entity {

    protected UUID uuid;

    protected Long created;

    protected Long modified;

    protected Map<String, Object> dynamic_properties = new TreeMap<String, Object>( String.CASE_INSENSITIVE_ORDER );

    protected Map<String, Set<Object>> dynamic_sets = new TreeMap<String, Set<Object>>( String.CASE_INSENSITIVE_ORDER );
    protected long size;


    @Override
    @EntityProperty(required = true, mutable = false, basic = true, indexed = false)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public UUID getUuid() {
        return uuid;
    }


    @Override
    public void setUuid( UUID uuid ) {
        this.uuid = uuid;
    }


    @Override
    @EntityProperty(required = true, mutable = false, basic = true, indexed = false)
    public String getType() {
        return Schema.getDefaultSchema().getEntityType( this.getClass() );
    }


    @Override
    public void setType( String type ) {
    }



    @Override
    public Id asId() {
        return new SimpleId( uuid, getType() );
    }

    @Override
    public void setSize(final long size){this.setMetadata("size",size);}

    @JsonIgnore
    @Override
    public long getSize() {
        Object size = this.getMetadata("size");
        return size != null && size instanceof Long ? (Long) size : 0;
    }

    @Override
    @EntityProperty(indexed = true, required = true, mutable = false)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Long getCreated() {
        return created;
    }


    @Override
    public void setCreated( Long created ) {
        if ( created == null ) {
            created = System.currentTimeMillis();
        }
        this.created = created;
    }



    @Override
    @EntityProperty(indexed = true, required = true, mutable = true)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Long getModified() {
        return modified;
    }


    @Override
    public void setModified( Long modified ) {
        if ( modified == null ) {
            modified = System.currentTimeMillis();
        }
        this.modified = modified;
    }


    @Override
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String getName() {
        Object value = getProperty( PROPERTY_NAME );

        if ( value instanceof UUID ) {
            // fixes existing data that uses UUID in USERGRID-2099
            return value.toString();
        }

        return ( String ) value;
    }


    @Override
    @JsonIgnore
    public Map<String, Object> getProperties() {
        return Schema.getDefaultSchema().getEntityProperties( this );
    }


    @Override
    public final Object getProperty( String propertyName ) {
        return Schema.getDefaultSchema().getEntityProperty(this, propertyName);
    }


    @Override
    public final void setProperty( String propertyName, Object propertyValue ) {
        Schema.getDefaultSchema().setEntityProperty( this, propertyName, propertyValue );
    }


    @Override
    public void setProperties( Map<String, Object> properties ) {
        dynamic_properties = new TreeMap<String, Object>( String.CASE_INSENSITIVE_ORDER );
        addProperties(properties);
    }

    @Override
    public void setProperties(org.apache.usergrid.persistence.model.entity.Entity cpEntity){
        setProperties( CpEntityMapUtils.toMap(cpEntity) );
        this.setSize(cpEntity.getSize());
    }


    @Override
    public void addProperties( Map<String, Object> properties ) {
        if ( properties == null ) {
            return;
        }
        for ( Entry<String, Object> entry : properties.entrySet() ) {
            setProperty( entry.getKey(), entry.getValue() );
        }
    }


    @Override
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Object getMetadata( String key ) {
        return getDataset( "metadata", key );
    }


    @Override
    public void setMetadata( String key, Object value ) {
        setDataset( "metadata", key, value );
    }


    @Override
    public void mergeMetadata( Map<String, Object> new_metadata ) {
        mergeDataset( "metadata", new_metadata );
    }


    @Override
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
        @SuppressWarnings("unchecked") Map<String, T> metadata = ( Map<String, T> ) md;
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
        @SuppressWarnings("unchecked") Map<String, T> metadata = ( Map<String, T> ) md;
        metadata.put( key, value );
    }


    public <T> void mergeDataset( String property, Map<String, T> new_metadata ) {
        Object md = dynamic_properties.get( property );
        if ( !( md instanceof Map<?, ?> ) ) {
            md = new HashMap<String, T>();
            dynamic_properties.put( property, md );
        }
        @SuppressWarnings("unchecked") Map<String, T> metadata = ( Map<String, T> ) md;
        metadata.putAll( new_metadata );
    }


    public void clearDataset( String property ) {
        dynamic_properties.remove( property );
    }


    @Override
    public List<Entity> getCollections( String key ) {
        return getDataset( "collections", key );
    }


    @Override
    public void setCollections( String key, List<Entity> results ) {
        setDataset( "collections", key, results );
    }


    @Override
    public List<Entity> getConnections( String key ) {
        return getDataset( "connections", key );
    }


    @Override
    public void setConnections( String key, List<Entity> results ) {
        setDataset( "connections", key, results );
    }


    @Override
    public String toString() {
        return "Entity(" + getProperties() + ")";
    }


    @Override
    @JsonAnySetter
    public void setDynamicProperty( String key, Object value ) {
        if (value == null || value.equals("")) {
			if (dynamic_properties.containsKey(key)) {
				dynamic_properties.remove(key);
			}
		} else {
			dynamic_properties.put(key, value);
		}
    }


    @Override
    @JsonAnyGetter
    public Map<String, Object> getDynamicProperties() {
        return dynamic_properties;
    }


    @Override
    public final int compareTo( Entity o ) {
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


    @Override
    public Entity toTypedEntity() {
        Entity entity = EntityFactory.newEntity( getUuid(), getType() );
        entity.setProperties( getProperties() );
        return entity;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( uuid == null ) ? 0 : uuid.hashCode() );
        return result;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
        AbstractEntity other = ( AbstractEntity ) obj;
        if ( uuid == null ) {
            if ( other.uuid != null ) {
                return false;
            }
        }
        else if ( !uuid.equals( other.uuid ) ) {
            return false;
        }
        return true;
    }

}
