/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.map.impl;


import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.persistence.core.migration.schema.Migration;
import org.apache.usergrid.persistence.map.MapScope;


public interface MapSerialization extends Migration {
    /**
     * Return the string, null if not found
     */
    String getString( final MapScope scope, final String key );


    /**
     * Get the key from all regions with a high consistency
     * @param scope
     * @param key
     * @return
     */
    String getStringHighConsistency( final MapScope scope, final String key );

    /**
     * Get strings from the map
     * @param keys
     * @return
     */
    Map<String, String> getStrings( final MapScope scope, final Collection<String> keys );

    /**
     * Return the string, null if not found
     */
    void putString( final MapScope scope, final String key, final String value );

    /**
     * Write the string
     */
    void putString( final MapScope scope, final String key, final String value, final int ttl );


    /**
     * Return the uuid, null if not found
     */
    UUID getUuid( final MapScope scope, final String key );

    /**
     * Return the uuid, null if not found
     */
    void putUuid( final MapScope scope, final String key, final UUID putUuid );

    /**
     * Return the long, null if not found
     */
    Long getLong( final MapScope scope, final String key );

    /**
     * Return the long, null if not found
     */
    void putLong( final MapScope scope, final String key, final Long value );

    /**
     * Delete the key
     *
     * @param key The key used to delete the entry
     */
    void delete( final MapScope scope, final String key );
}
