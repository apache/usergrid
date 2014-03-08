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
package org.apache.usergrid.services;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.utils.JsonUtils;

import org.apache.commons.collections.iterators.EmptyIterator;
import org.apache.commons.collections.iterators.SingletonListIterator;

import static org.apache.usergrid.utils.JsonUtils.normalizeJsonTree;


public class ServicePayload {

    private final Map<String, Object> properties;
    private final List<Map<String, Object>> batch;
    private final List<UUID> list;


    public ServicePayload() {
        properties = new LinkedHashMap<String, Object>();
        batch = null;
        list = null;
    }


    private ServicePayload( Map<String, Object> properties, List<Map<String, Object>> batch, List<UUID> list ) {
        this.properties = properties;
        this.batch = batch;
        this.list = list;
    }


    public static ServicePayload payload( Map<String, Object> properties ) {
        return new ServicePayload( properties, null, null );
    }


    public static ServicePayload batchPayload( List<Map<String, Object>> batch ) {
        return new ServicePayload( null, batch, null );
    }


    public static ServicePayload idListPayload( List<UUID> list ) {
        return new ServicePayload( null, null, list );
    }


    @SuppressWarnings("unchecked")
    public static ServicePayload jsonPayload( Object json ) {
        ServicePayload payload = null;
        json = normalizeJsonTree( json );
        if ( json instanceof Map ) {
            Map<String, Object> jsonMap = ( Map<String, Object> ) json;
            payload = payload( jsonMap );
        }
        else if ( json instanceof List ) {
            List<?> jsonList = ( List<?> ) json;
            if ( jsonList.size() > 0 ) {
                if ( jsonList.get( 0 ) instanceof UUID ) {
                    payload = idListPayload( ( List<UUID> ) json );
                }
                else if ( jsonList.get( 0 ) instanceof Map ) {
                    payload = ServicePayload.batchPayload( ( List<Map<String, Object>> ) jsonList );
                }
            }
        }
        return payload;
    }


    public static ServicePayload stringPayload( String str ) {
        return jsonPayload( JsonUtils.parse( str ) );
    }


    public boolean isBatch() {
        return batch != null;
    }


    public boolean isList() {
        return list != null;
    }


    public Map<String, Object> getProperties() {
        if ( properties != null ) {
            return properties;
        }
        if ( ( batch != null ) && ( batch.size() > 0 ) ) {
            return batch.get( 0 );
        }
        return null;
    }


    public Object getProperty( String property ) {
        Map<String, Object> p = getProperties();
        if ( p == null ) {
            return null;
        }
        return p.get( property );
    }


    public void setProperty( String property, Object value ) {
        Map<String, Object> p = getProperties();

        if ( p == null ) {
            throw new NullPointerException( "No payload exists, cannot add properties to it" );
        }

        p.put( property, value );
    }


    public String getStringProperty( String property ) {
        Object obj = getProperty( property );
        if ( obj instanceof String ) {
            return ( String ) obj;
        }
        return null;
    }


    public Long getLongProperty( String property ) {
        Object obj = getProperty( property );

        if ( obj instanceof Long ) {
            return ( Long ) obj;
        }

        return null;
    }


    public List<Map<String, Object>> getBatchProperties() {
        if ( batch != null ) {
            return batch;
        }
        if ( properties != null ) {
            List<Map<String, Object>> l = new ArrayList<Map<String, Object>>( 1 );
            l.add( properties );
            return l;
        }
        return null;
    }


    public List<UUID> getIdList() {
        return list;
    }


    @Override
    public String toString() {
        if ( batch != null ) {
            return JsonUtils.mapToJsonString( batch );
        }
        else if ( list != null ) {
            return JsonUtils.mapToJsonString( list );
        }
        return JsonUtils.mapToJsonString( properties );
    }


    @SuppressWarnings("unchecked")
    public Iterator<Map<String, Object>> payloadIterator() {
        if ( isBatch() ) {
            return batch.iterator();
        }
        if ( properties != null ) {
            return new SingletonListIterator( properties );
        }
        return EmptyIterator.INSTANCE;
    }
}
