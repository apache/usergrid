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
package org.apache.usergrid.persistence.map;


import java.util.Collection;
import java.util.Map;
import java.util.UUID;



/**
 * Generator of a map manager instance
 */
public interface MapManager {


    /**
     * Return the string, null if not found
     */
    public String getString( final String key );


    /**
     * Get the values for all the keys.  If a value does not exist, it won't be present in the map
     * @param keys
     * @return
     */
    public Map<String, String> getStrings(final Collection<String> keys);

    /**
     * Return the string, null if not found
     */
    public void putString( final String key, final String value );

    /**
     * The time to live (in seconds) of the string
     * @param key
     * @param value
     * @param ttl
     */
    public void putString( final String key, final String value, final int ttl );


    /**
     * Return the uuid, null if not found
     */
    public UUID getUuid( final String key );

    /**
     * Return the uuid, null if not found
     */
    public void putUuid( final String key, final UUID putUuid );

    /**
     * Return the long, null if not found
     */
    public Long getLong( final String key );

    /**
     * Return the long, null if not found
     */
    public void putLong( final String key, final Long value );

    /**
     * Delete the key
     *
     * @param key The key used to delete the entry
     */
    public void delete( final String key );
}
