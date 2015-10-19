/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.map.impl;


import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapScope;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
;


/**
 * Implementation of the map manager
 */
public class MapManagerImpl implements MapManager {

    private final MapScope scope;
    private final MapSerialization mapSerialization;


    @Inject
    public MapManagerImpl( @Assisted final MapScope scope, final MapSerialization mapSerialization) {
        this.scope = scope;
        this.mapSerialization = mapSerialization;
    }


    @Override
    public String getString( final String key ) {
        return mapSerialization.getString( scope, key );
    }


    @Override
    public String getStringHighConsistency( final String key ) {
        return mapSerialization.getStringHighConsistency(scope, key);
    }


    @Override
    public Map<String, String> getStrings( final Collection<String> keys ) {
        return mapSerialization.getStrings( scope, keys );
    }


    @Override
    public void putString( final String key, final String value ) {
          mapSerialization.putString( scope, key, value );
    }


    @Override
    public void putString( final String key, final String value, final int ttl ) {
        mapSerialization.putString( scope, key, value, ttl );
    }


    @Override
    public UUID getUuid( final String key ) {
        return mapSerialization.getUuid(scope,key);
    }


    @Override
    public void putUuid( final String key, final UUID putUuid ) {
         mapSerialization.putUuid(scope,key,putUuid);
    }


    @Override
    public Long getLong( final String key ) {
        return mapSerialization.getLong(scope,key);
    }


    @Override
    public void putLong( final String key, final Long value ) {
         mapSerialization.putLong(scope,key,value);
    }


    @Override
    public void delete( final String key ) {
        mapSerialization.delete(scope,key);
    }



}
