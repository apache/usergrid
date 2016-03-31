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
package org.apache.usergrid.corepersistence.index;


import java.util.Map;
import java.util.Optional;


public interface IndexSchemaCache {

    /**
     * Get the collection schema from the cache.
     * @param collectionName
     * @return
     */
    public Optional<Map> getCollectionSchema( String collectionName );

    void putCollectionSchema( String collectionName, String collectionSchema );

    void deleteCollectionSchema( String collectionName );

    /**
     * Evict the collection schema from the cache.
     * @param collectionName
     */
    public void evictCollectionSchema(String collectionName);

    /**
     * Evict everything from the cache.
     */
    public void evictCache();


}
