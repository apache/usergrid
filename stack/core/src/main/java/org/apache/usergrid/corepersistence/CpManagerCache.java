/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.corepersistence;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.utils.LRUCache2;

class CpManagerCache {

    private final EntityCollectionManagerFactory ecmf;
    private final EntityIndexFactory eif;
    private final GraphManagerFactory gmf;
    private final MapManagerFactory mmf;

    // TODO: consider making these cache sizes and timeouts configurable
    // TODO: replace with Guava cache
    private final LRUCache2<CollectionScope, EntityCollectionManager> ecmCache
            = new LRUCache2<CollectionScope, EntityCollectionManager>(50, 1 * 60 * 60 * 1000);

    private final LRUCache2<ApplicationScope, EntityIndex> eiCache
            = new LRUCache2<>(50, 1 * 60 * 60 * 1000);

    private final LRUCache2<ApplicationScope, GraphManager> gmCache
            = new LRUCache2<ApplicationScope, GraphManager>(50, 1 * 60 * 60 * 1000);

    private final LRUCache2<MapScope, MapManager> mmCache
            = new LRUCache2<MapScope, MapManager>(50, 1 * 60 * 60 * 1000);


    public CpManagerCache(
            EntityCollectionManagerFactory ecmf, 
            EntityIndexFactory eif, 
            GraphManagerFactory gmf,
            MapManagerFactory mmf) {

        this.ecmf = ecmf;
        this.eif = eif;
        this.gmf = gmf;
        this.mmf = mmf;
    }

    public EntityCollectionManager getEntityCollectionManager(CollectionScope scope) {

        EntityCollectionManager ecm = ecmCache.get(scope);

        if (ecm == null) {
            ecm = ecmf.createCollectionManager(scope);
            ecmCache.put(scope, ecm);
        }
        return ecm;
    }

    public EntityIndex getEntityIndex(ApplicationScope applicationScope) {

        EntityIndex ei = eiCache.get(applicationScope);

        if (ei == null) {
            ei = eif.createEntityIndex(applicationScope);
            eiCache.put(applicationScope, ei);
        }
        return ei;
    }

    public GraphManager getGraphManager(ApplicationScope appScope) {

        GraphManager gm = gmCache.get(appScope);

        if (gm == null) {
            gm = gmf.createEdgeManager(appScope);
            gmCache.put(appScope, gm);
        }
        return gm;
    }

    public MapManager getMapManager( MapScope mapScope) {

        MapManager mm = mmCache.get(mapScope);

        if (mm == null) {
            mm = mmf.createMapManager(mapScope);
            mmCache.put(mapScope, mm);
        }
        return mm;
    }

    void flush() {
        gmCache.purge();
        ecmCache.purge();
        eiCache.purge();
    }


}
