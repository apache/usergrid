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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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


public class CpManagerCache {

    private final EntityCollectionManagerFactory ecmf;
    private final EntityIndexFactory eif;
    private final GraphManagerFactory gmf;
    private final MapManagerFactory mmf;

    // TODO: consider making these cache sizes and timeouts configurable

    private LoadingCache<CollectionScope, EntityCollectionManager> ecmCache = 
        CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build( new CacheLoader<CollectionScope, EntityCollectionManager>() {
                public EntityCollectionManager load( CollectionScope scope ) { 
                    return ecmf.createCollectionManager( scope );
                }
            }
        );

    private LoadingCache<ApplicationScope, EntityIndex> eiCache = 
        CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build( new CacheLoader<ApplicationScope, EntityIndex>() {
                public EntityIndex load( ApplicationScope scope ) { 
                    return eif.createEntityIndex( scope );
                }
            }
        );

    private LoadingCache<ApplicationScope, GraphManager> gmCache = 
        CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build( new CacheLoader<ApplicationScope, GraphManager>() {
                public GraphManager load( ApplicationScope scope ) { 
                    return gmf.createEdgeManager( scope );
                }
            }
        );

    private LoadingCache<MapScope, MapManager> mmCache = 
        CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build( new CacheLoader<MapScope, MapManager>() {
                public MapManager load( MapScope scope ) { 
                    return mmf.createMapManager( scope );
                }
            }
        );

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
        try {
            return ecmCache.get( scope );
        } catch (ExecutionException ex) {
            throw new RuntimeException("Error getting manager", ex);
        }
    }

    public EntityIndex getEntityIndex(ApplicationScope appScope) {
        try {
            return eiCache.get( appScope );
        } catch (ExecutionException ex) {
            throw new RuntimeException("Error getting manager", ex);
        }
    }

    public GraphManager getGraphManager(ApplicationScope appScope) {
        try {
            return gmCache.get( appScope );
        } catch (ExecutionException ex) {
            throw new RuntimeException("Error getting manager", ex);
        }
    }

    public MapManager getMapManager( MapScope mapScope) {
        try {
            return mmCache.get( mapScope );
        } catch (ExecutionException ex) {
            throw new RuntimeException("Error getting manager", ex);
        }
    }

    void flush() {
        ecmCache.invalidateAll();
        eiCache.invalidateAll();
        gmCache.invalidateAll();
        mmCache.invalidateAll();
    }
}
