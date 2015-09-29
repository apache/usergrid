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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;


public class ApplicationInfo {

    private UUID id;
    private String name;

    /** Needed for Jackson since this class is serialized to the Shiro Cache */
    public ApplicationInfo() {}

    /**
     * @param id The application ID (not the same as the ID of the application_info).
     * @param name The application name in orgname/appname format.
     */
    public ApplicationInfo( UUID id, String name ) {
        this.id = id;
        this.name = name;
    }


    public ApplicationInfo( Map<String, Object> properties ) {
        id = ( UUID ) properties.get( PROPERTY_UUID );
        name = ( String ) properties.get( PROPERTY_NAME );
    }


    public UUID getId() {
        return id;
    }


    public String getName() {
        return name;
    }


    public static List<ApplicationInfo> fromNameIdMap( Map<String, UUID> map ) {
        List<ApplicationInfo> list = new ArrayList<ApplicationInfo>();
        for ( Entry<String, UUID> s : map.entrySet() ) {
            list.add( new ApplicationInfo( s.getValue(), s.getKey() ) );
        }
        return list;
    }


    public static List<ApplicationInfo> fromIdNameMap( Map<UUID, String> map ) {
        List<ApplicationInfo> list = new ArrayList<ApplicationInfo>();
        for ( Entry<UUID, String> s : map.entrySet() ) {
            list.add( new ApplicationInfo( s.getKey(), s.getValue() ) );
        }
        return list;
    }


    public static Map<String, UUID> toNameIdMap( List<ApplicationInfo> list ) {
        Map<String, UUID> map = new LinkedHashMap<String, UUID>();
        for ( ApplicationInfo i : list ) {
            map.put( i.getName(), i.getId() );
        }
        return map;
    }


    public static Map<UUID, String> toIdNameMap( List<ApplicationInfo> list ) {
        Map<UUID, String> map = new LinkedHashMap<UUID, String>();
        for ( ApplicationInfo i : list ) {
            map.put( i.getId(), i.getName() );
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
        ApplicationInfo other = ( ApplicationInfo ) obj;
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
}
