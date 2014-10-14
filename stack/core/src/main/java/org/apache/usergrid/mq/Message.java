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


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import com.clearspring.analytics.hash.MurmurHash;
import org.apache.usergrid.utils.UUIDUtils;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.uuid.UUIDComparator;

import static org.apache.commons.collections.IteratorUtils.asEnumeration;
import static org.apache.commons.collections.MapUtils.getBooleanValue;
import static org.apache.commons.collections.MapUtils.getByteValue;
import static org.apache.commons.collections.MapUtils.getDoubleValue;
import static org.apache.commons.collections.MapUtils.getFloatValue;
import static org.apache.commons.collections.MapUtils.getIntValue;
import static org.apache.commons.collections.MapUtils.getLongValue;
import static org.apache.commons.collections.MapUtils.getShortValue;
import static org.apache.commons.collections.MapUtils.getString;
import static org.apache.usergrid.utils.ClassUtils.cast;
import static org.apache.usergrid.utils.ConversionUtils.bytes;
import static org.apache.usergrid.utils.ConversionUtils.coerceMap;
import static org.apache.usergrid.utils.ConversionUtils.getInt;
import static org.apache.usergrid.utils.ConversionUtils.uuid;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMillis;
import static org.apache.usergrid.utils.UUIDUtils.isTimeBased;
import static org.apache.usergrid.utils.UUIDUtils.newTimeUUID;


@XmlRootElement
public class Message {

    public static final String MESSAGE_CORRELATION_ID = "correlation_id";
    public static final String MESSAGE_DESTINATION = "destination";
    public static final String MESSAGE_ID = "uuid";
    public static final String MESSAGE_REPLY_TO = "reply_to";
    public static final String MESSAGE_TIMESTAMP = "timestamp";
    public static final String MESSAGE_TYPE = "type";
    public static final String MESSAGE_CATEGORY = "category";
    public static final String MESSAGE_INDEXED = "indexed";
    public static final String MESSAGE_PERSISTENT = "persistent";
    public static final String MESSAGE_TRANSACTION = "transaction";

    @SuppressWarnings("rawtypes")
    public static final Map<String, Class> MESSAGE_PROPERTIES =
            hashMap( MESSAGE_CORRELATION_ID, ( Class ) String.class ).map( MESSAGE_DESTINATION, String.class )
                    .map( MESSAGE_ID, UUID.class ).map( MESSAGE_REPLY_TO, String.class )
                    .map( MESSAGE_TIMESTAMP, Long.class ).map( MESSAGE_TYPE, String.class )
                    .map( MESSAGE_CATEGORY, String.class ).map( MESSAGE_INDEXED, Boolean.class )
                    .map( MESSAGE_PERSISTENT, Boolean.class ).map( MESSAGE_TRANSACTION, UUID.class );


    public static int compare( Message m1, Message m2 ) {
        if ( ( m1 == null ) && ( m2 == null ) ) {
            return 0;
        }
        else if ( m1 == null ) {
            return -1;
        }
        else if ( m2 == null ) {
            return 1;
        }
        return UUIDComparator.staticCompare( m1.getUuid(), m2.getUuid() );
    }


    public static List<Message> fromList( List<Map<String, Object>> l ) {
        List<Message> messages = new ArrayList<Message>( l.size() );
        for ( Map<String, Object> properties : l ) {
            messages.add( new Message( properties ) );
        }
        return messages;
    }


    public static List<Message> sort( List<Message> messages ) {
        Collections.sort( messages, new Comparator<Message>() {
            @Override
            public int compare( Message m1, Message m2 ) {
                return Message.compare( m1, m2 );
            }
        } );
        return messages;
    }


    public static List<Message> sortReversed( List<Message> messages ) {
        Collections.sort( messages, new Comparator<Message>() {
            @Override
            public int compare( Message m1, Message m2 ) {
                return Message.compare( m2, m1 );
            }
        } );
        return messages;
    }


    protected Map<String, Object> properties = new TreeMap<String, Object>( String.CASE_INSENSITIVE_ORDER );


    public Message() {
    }


    @SuppressWarnings("unchecked")
    public Message( Map<String, Object> properties ) {
        this.properties.putAll( coerceMap( ( Map<String, Class<?>> ) cast( MESSAGE_PROPERTIES ), properties ) );
    }


    @SuppressWarnings("unchecked")
    public void addCounter( String name, int value ) {
        Map<String, Integer> counters = null;
        if ( properties.get( "counters" ) instanceof Map ) {
            counters = ( Map<String, Integer> ) properties.get( "counters" );
        }
        else {
            counters = new HashMap<String, Integer>();
            properties.put( "counters", counters );
        }
        counters.put( name, value );
    }


    public void clearBody() {
        properties.clear();
    }


    public void clearProperties() {
        properties.clear();
    }


    public boolean getBooleanProperty( String name ) {
        return getBooleanValue( properties, name );
    }


    public byte getByteProperty( String name ) {
        return getByteValue( properties, name );
    }


    @JsonIgnore
    public String getCategory() {
        return getString( properties, MESSAGE_CATEGORY );
    }


    @JsonIgnore
    public String getCorrelationID() {
        return getString( properties, MESSAGE_CORRELATION_ID );
    }


    @JsonIgnore
    public byte[] getCorrelationIDAsBytes() {
        return bytes( properties.get( MESSAGE_CORRELATION_ID ) );
    }


    @JsonIgnore
    public Map<String, Integer> getCounters() {
        Map<String, Integer> counters = new HashMap<String, Integer>();
        if ( properties.get( "counters" ) instanceof Map ) {
            @SuppressWarnings("unchecked") Map<String, Object> c = ( Map<String, Object> ) properties.get( "counters" );
            for ( Entry<String, Object> e : c.entrySet() ) {
                counters.put( e.getKey(), getInt( e.getValue() ) );
            }
        }
        return counters;
    }


    @JsonIgnore
    public int getDeliveryMode() {
        return 2;
    }


    @JsonIgnore
    public Queue getDestination() {
        return Queue.getDestination( getString( properties, MESSAGE_DESTINATION ) );
    }


    public double getDoubleProperty( String name ) {
        return getDoubleValue( properties, name );
    }


    @JsonIgnore
    public long getExpiration() {
        return 0;
    }


    public float getFloatProperty( String name ) {
        return getFloatValue( properties, name );
    }


    public int getIntProperty( String name ) {
        return getIntValue( properties, name );
    }


    public long getLongProperty( String name ) {
        return getLongValue( properties, name );
    }


    @JsonIgnore
    public String getMessageID() {
        return getUuid().toString();
    }


    public Object getObjectProperty( String name ) {
        return properties.get( name );
    }


    @JsonIgnore
    public int getPriority() {
        return 0;
    }


    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        sync();
        return properties;
    }


    @JsonIgnore
    @SuppressWarnings("unchecked")
    public Enumeration<String> getPropertyNames() {
        return asEnumeration( properties.keySet().iterator() );
    }


    @JsonIgnore
    public boolean getRedelivered() {
        return false;
    }


    @JsonIgnore
    public Queue getReplyTo() {
        return Queue.getDestination( getString( properties, MESSAGE_REPLY_TO ) );
    }


    public short getShortProperty( String name ) {
        return getShortValue( properties, name );
    }


    public String getStringProperty( String name ) {
        return getString( properties, name );
    }


    @JsonIgnore
    public synchronized long getTimestamp() {
        if ( properties.containsKey( MESSAGE_TIMESTAMP ) ) {
            long ts = getLongValue( properties, MESSAGE_TIMESTAMP );
            if ( ts != 0 ) {
                return ts;
            }
        }
        long timestamp = getTimestampInMillis( getUuid() );
        properties.put( MESSAGE_TIMESTAMP, timestamp );
        return timestamp;
    }


    @JsonIgnore
    public String getType() {
        return getString( properties, MESSAGE_TYPE );
    }


    @JsonIgnore
    public synchronized UUID getUuid() {
        UUID uuid = uuid( properties.get( MESSAGE_ID ), null );
        if ( uuid == null ) {
            if ( properties.containsKey( MESSAGE_TIMESTAMP ) ) {
                long ts = getLongValue( properties, MESSAGE_TIMESTAMP );
                uuid = newTimeUUID( ts );
            }
            else {
                uuid = newTimeUUID();
            }

            properties.put( MESSAGE_ID, uuid );
            properties.put( MESSAGE_TIMESTAMP, getTimestampInMillis( uuid ) );
        }
        return uuid;
    }


    @JsonIgnore
    public boolean isIndexed() {
        return getBooleanValue( properties, MESSAGE_INDEXED );
    }


    @JsonIgnore
    public boolean isPersistent() {
        return getBooleanValue( properties, MESSAGE_PERSISTENT );
    }


    public boolean propertyExists( String name ) {
        return properties.containsKey( name );
    }


    public void setBooleanProperty( String name, boolean value ) {
        properties.put( name, value );
    }


    public void setByteProperty( String name, byte value ) {
        properties.put( name, value );
    }


    public void setCategory( String category ) {
        if ( category != null ) {
            properties.put( MESSAGE_CATEGORY, category.toLowerCase() );
        }
    }


    public void setCorrelationID( String correlationId ) {
        properties.put( MESSAGE_CORRELATION_ID, correlationId );
    }


    public void setCorrelationIDAsBytes( byte[] correlationId ) {
        properties.put( MESSAGE_CORRELATION_ID, correlationId );
    }


    public void setCounters( Map<String, Integer> counters ) {
        if ( counters == null ) {
            counters = new HashMap<String, Integer>();
        }
        properties.put( "counters", counters );
    }


    public void setDeliveryMode( int arg0 ) {
    }


    public void setDestination( Queue destination ) {
        properties.put( MESSAGE_CORRELATION_ID, destination.toString() );
    }


    public void setDoubleProperty( String name, double value ) {
        properties.put( name, value );
    }


    public void setExpiration( long expiration ) {
    }


    public void setFloatProperty( String name, float value ) {
        properties.put( name, value );
    }


    public void setIndexed( boolean indexed ) {
        properties.put( MESSAGE_INDEXED, indexed );
    }


    public void setIntProperty( String name, int value ) {
        properties.put( name, value );
    }


    public void setLongProperty( String name, long value ) {
        properties.put( name, value );
    }


    public void setMessageID( String id ) {
        if ( UUIDUtils.isUUID( id ) ) {
            properties.put( MESSAGE_ID, UUIDUtils.tryGetUUID( id ) );
        }
        else {
            throw new RuntimeException( "Not a UUID" );
        }
    }


    public void setObjectProperty( String name, Object value ) {
        properties.put( name, value );
    }


    public void setPersistent( boolean persistent ) {
        properties.put( MESSAGE_PERSISTENT, persistent );
    }


    public void setPriority( int priority ) {
    }


    @JsonAnySetter
    public void setProperty( String key, Object value ) {
        properties.put( key, value );
    }


    public void setRedelivered( boolean redelivered ) {
    }


    public void setReplyTo( Queue destination ) {
        properties.put( MESSAGE_REPLY_TO, destination.toString() );
    }


    public void setShortProperty( String name, short value ) {
        properties.put( name, value );
    }


    public void setStringProperty( String name, String value ) {
        properties.put( name, value );
    }


    public void setTimestamp( long timestamp ) {
        properties.put( MESSAGE_TIMESTAMP, timestamp );
    }


    public void setType( String type ) {
        properties.put( MESSAGE_TYPE, type );
    }


    public void setUuid( UUID uuid ) {
        properties.put(MESSAGE_ID, uuid);
        properties.put(MESSAGE_TIMESTAMP, UUIDUtils.getUUIDLong(uuid));
    }


    public void setTransaction( UUID transaction ) {
        properties.put( MESSAGE_TRANSACTION, transaction );
    }


    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public UUID getTransaction() {
        return ( UUID ) properties.get( MESSAGE_TRANSACTION );
    }


    public void sync() {
        getUuid();
        getTimestamp();
    }
}
