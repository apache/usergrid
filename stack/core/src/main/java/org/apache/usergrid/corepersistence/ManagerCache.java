/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence;


import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapScope;


/**
 * The cache of the manager
 */
public interface ManagerCache {

    /**
     * Get the entity collection manager for the specified scope
     * @param scope
     * @return
     */
    EntityCollectionManager getEntityCollectionManager( ApplicationScope scope );

    /**
     * Get the entity index for the specified app scope
     *
     * @param appScope
     * @return
     */
    ApplicationEntityIndex getEntityIndex( ApplicationScope appScope );

    /**
     * Get the graph manager for the graph scope
     *
     * @param appScope
     * @return
     */
    GraphManager getGraphManager(ApplicationScope appScope);

    /**
     * Get the map manager for the map scope
     *
     * @param mapScope
     * @return
     */
    MapManager getMapManager(MapScope mapScope);

    /**
     * invalidate the cache
     */
    void invalidate();

}
