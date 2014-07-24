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
package org.apache.usergrid.mq;


import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.utils.UUIDUtils;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import static java.util.UUID.nameUUIDFromBytes;

import static org.apache.commons.collections.MapUtils.getBooleanValue;
import static org.apache.commons.collections.MapUtils.getByteValue;
import static org.apache.commons.collections.MapUtils.getDoubleValue;
import static org.apache.commons.collections.MapUtils.getFloatValue;
import static org.apache.commons.collections.MapUtils.getIntValue;
import static org.apache.commons.collections.MapUtils.getLongValue;
import static org.apache.commons.collections.MapUtils.getShortValue;
import static org.apache.commons.collections.MapUtils.getString;
import static org.apache.usergrid.utils.MapUtils.hashMap;


public class Queue {

    public static final String QUEUE_ID = "uuid";
    public static final String QUEUE_PATH = "path";
    public static final String QUEUE_CREATED = "created";
    public static final String QUEUE_MODIFIED = "modified";
    public static final String QUEUE_NEWEST = "newest";
    public static final String QUEUE_OLDEST = "oldest";

    @SuppressWarnings("rawtypes")
    public static final Map<String, Class> QUEUE_PROPERTIES =
            hashMap( QUEUE_PATH, ( Class ) String.class ).map( QUEUE_ID, UUID.class ).map( QUEUE_CREATED, Long.class )
                    .map( QUEUE_MODIFIED, Long.class ).map( QUEUE_NEWEST, UUID.class ).map( QUEUE_OLDEST, UUID.class );

    protected Map<String, Object> properties = new TreeMap<String, Object>( String.CASE_INSENSITIVE_ORDER );


    public Queue( String path ) {
        setPath( path );
    }


    public Queue( Map<String, Object> properties ) {
        this.properties.putAll( properties );
    }


    @Override
    public String toString() {
        return getPath();
    }


    @JsonIgnore
    public String getPath() {
        return getString( properties, QUEUE_PATH );
    }


    public void setPath( String path ) {
        properties.put( QUEUE_PATH, path );
    }


    @JsonIgnore
    public long getCreated() {
        return getLongValue( properties, QUEUE_CREATED );
    }


    public void setCreated( long created ) {
        properties.put( QUEUE_CREATED, created );
    }


    @JsonIgnore
    public long getModified() {
        return getLongValue( properties, QUEUE_MODIFIED );
    }


    public void setModified( long modified ) {
        properties.put( QUEUE_MODIFIED, modified );
    }


    public static Queue getDestination( String path ) {
        if ( path == null ) {
            return null;
        }
        return new Queue( path );
    }


    @JsonAnySetter
    public void setProperty( String key, Object value ) {
        properties.put( key, value );
    }


    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }


    public static String[] getQueueParentPaths( String queuePath ) {
        queuePath = queuePath.toLowerCase().trim();
        String[] segments = StringUtils.split( queuePath, '/' );
        String[] paths = new String[segments.length + 1];
        paths[0] = "/";
        for ( int i = 0; i < segments.length; i++ ) {
            paths[i + 1] = "/" + StringUtils.join( segments, '/', 0, i + 1 ) + "/";
        }
        return paths;
    }


    public static String[] getQueuePathSegments( String queuePath ) {
        queuePath = queuePath.toLowerCase().trim();
        String[] segments = StringUtils.split( queuePath, '/' );
        return segments;
    }


    public static String normalizeQueuePath( String queuePath ) {
        if ( queuePath == null ) {
            return null;
        }
        queuePath = queuePath.toLowerCase().trim();
        if ( queuePath.length() == 0 ) {
            return null;
        }
        queuePath = "/" + StringUtils.join( StringUtils.split( queuePath, '/' ), '/' );
        if ( !queuePath.endsWith( "/" ) ) {
            queuePath += "/";
        }
        return queuePath;
    }


    public static UUID getQueueId( String queuePath ) {
        if ( queuePath == null ) {
            return null;
        }
        // is the queuePath already a UUID?
        UUID uuid = UUIDUtils.tryGetUUID( queuePath );
        if ( uuid != null ) {
            return uuid;
        }
        // UUID queuePath string might have been normalized
        // look for /00000000-0000-0000-0000-000000000000/
        // or /00000000-0000-0000-0000-000000000000
        if ( ( queuePath.length() == 38 ) && queuePath.startsWith( "/" ) && queuePath.endsWith( "/" ) ) {
            uuid = UUIDUtils.tryExtractUUID( queuePath, 1 );
            if ( uuid != null ) {
                return uuid;
            }
        }
        else if ( ( queuePath.length() == 37 ) && queuePath.startsWith( "/" ) ) {
            uuid = UUIDUtils.tryExtractUUID( queuePath, 1 );
            if ( uuid != null ) {
                return uuid;
            }
        }
        queuePath = normalizeQueuePath( queuePath );
        if ( queuePath == null ) {
            return null;
        }
        uuid = nameUUIDFromBytes( queuePath.getBytes() );
        return uuid;
    }


    @JsonIgnore
    public UUID getUuid() {
        return getQueueId( getPath() );
    }


    public float getFloatProperty( String name ) {
        return getFloatValue( properties, name );
    }


    public void setFloatProperty( String name, float value ) {
        properties.put( name, value );
    }


    public double getDoubleProperty( String name ) {
        return getDoubleValue( properties, name );
    }


    public void setDoubleProperty( String name, double value ) {
        properties.put( name, value );
    }


    public int getIntProperty( String name ) {
        return getIntValue( properties, name );
    }


    public void setIntProperty( String name, int value ) {
        properties.put( name, value );
    }


    public long getLongProperty( String name ) {
        return getLongValue( properties, name );
    }


    public void setLongProperty( String name, long value ) {
        properties.put( name, value );
    }


    public Object getObjectProperty( String name ) {
        return properties.get( name );
    }


    public void setObjectProperty( String name, Object value ) {
        properties.put( name, value );
    }


    public short getShortProperty( String name ) {
        return getShortValue( properties, name );
    }


    public void setShortProperty( String name, short value ) {
        properties.put( name, value );
    }


    public String getStringProperty( String name ) {
        return getString( properties, name );
    }


    public void setStringProperty( String name, String value ) {
        properties.put( name, value );
    }


    public boolean getBooleanProperty( String name ) {
        return getBooleanValue( properties, name );
    }


    public void setBooleanProperty( String name, boolean value ) {
        properties.put( name, value );
    }


    public byte getByteProperty( String name ) {
        return getByteValue( properties, name );
    }


    public void setByteProperty( String name, byte value ) {
        properties.put( name, value );
    }
}
