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
package org.apache.usergrid.management;


import java.util.*;
import java.util.Map.Entry;

import static org.apache.usergrid.persistence.Schema.PROPERTY_PATH;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;


public class OrganizationInfo {

    public static final String PASSWORD_HISTORY_SIZE_KEY = "passwordHistorySize";

    private UUID id;
    private String name;
    private Map<String, Object> properties;


    public OrganizationInfo() {
    }


    public OrganizationInfo( UUID id, String name ) {
        this.id = id;
        this.name = name;
    }


    public OrganizationInfo( Map<String, Object> properties ) {
        id = ( UUID ) properties.get( PROPERTY_UUID );
        name = ( String ) properties.get( PROPERTY_PATH );
    }


    public <KeyType,ValueType> OrganizationInfo( UUID id, String name, Map<KeyType, ValueType> properties ) {
        this( id, name );
        setProperties(properties);
    }


    public UUID getUuid() {
        return id;
    }


    public void setUuid( UUID id ) {
        this.id = id;
    }


    public String getName() {
        return name;
    }


    public void setName( String name ) {
        this.name = name;
    }


    public int getPasswordHistorySize() {
        int size = 0;
        if ( properties != null ) {
            Object sizeValue = properties.get( PASSWORD_HISTORY_SIZE_KEY );
            if ( sizeValue instanceof Number ) {
                size = ( ( Number ) sizeValue ).intValue();
            }
            else if ( sizeValue instanceof String ) {
                try {
                    size = Integer.parseInt( ( String ) sizeValue );
                }
                catch ( NumberFormatException e ) { /* ignore */ }
            }
        }
        return size;
    }


    public static List<OrganizationInfo> fromNameIdMap( Map<String, UUID> map ) {
        List<OrganizationInfo> list = new ArrayList<>();
        for ( Entry<String, UUID> s : map.entrySet() ) {
            list.add( new OrganizationInfo( s.getValue(), s.getKey() ) );
        }
        return list;
    }


    public static List<OrganizationInfo> fromIdNameMap( Map<UUID, String> map ) {
        List<OrganizationInfo> list = new ArrayList<>();
        for ( Entry<UUID, String> s : map.entrySet() ) {
            list.add( new OrganizationInfo( s.getKey(), s.getValue() ) );
        }
        return list;
    }


    public static Map<String, UUID> toNameIdMap( List<OrganizationInfo> list ) {
        Map<String, UUID> map = new LinkedHashMap<>();
        for ( OrganizationInfo i : list ) {
            map.put( i.getName(), i.getUuid() );
        }
        return map;
    }


    public static Map<UUID, String> toIdNameMap( List<OrganizationInfo> list ) {
        Map<UUID, String> map = new LinkedHashMap<>();
        for ( OrganizationInfo i : list ) {
            map.put( i.getUuid(), i.getName() );
        }
        return map;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        return result;
    }


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
        OrganizationInfo other = ( OrganizationInfo ) obj;
        if ( id == null ) {
            if ( other.id != null ) {
                return false;
            }
        }
        else if ( !id.equals( other.id ) ) {
            return false;
        }
        if ( name == null ) {
            if ( other.name != null ) {
                return false;
            }
        }
        else if ( !name.equals( other.name ) ) {
            return false;
        }
        return true;
    }


    public Map<String, Object> getProperties() {
        return properties;
    }

    // using generics to avoid getting a bunch of unchecked type conversions
    public <KeyType, ValueType> void setProperties( Map<KeyType, ValueType> properties ) {
        this.properties = new HashMap<>();
        if (properties != null) {
            properties.forEach((k, v) -> this.properties.put(k.toString(), v));
        }
    }
}
