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

package org.apache.usergrid.persistence.index.legacy;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.utils.LRUCache2;


class ManagerCache {

    private final EntityCollectionManagerFactory ecmf;
    private final EntityIndexFactory eif;

    private final LRUCache2<CollectionScope, EntityCollectionManager> ecmCache = 
        new LRUCache2<CollectionScope, EntityCollectionManager>( 50, 1 * 60 * 60 * 1000);

    private final LRUCache2<FullScopeHashKey, EntityIndex> eiCache = 
        new LRUCache2<FullScopeHashKey, EntityIndex>( 50, 1 * 60 * 60 * 1000);


    public ManagerCache( 
        EntityCollectionManagerFactory ecmf, EntityIndexFactory eif ) {
        this.ecmf = ecmf;
        this.eif = eif;
    }

    public EntityCollectionManager getEntityCollectionManager( CollectionScope scope ) {

        EntityCollectionManager ecm = ecmCache.get(scope);

        if ( ecm == null ) {
            ecm = ecmf.createCollectionManager(scope);
            ecmCache.put( scope, ecm);
        }
        return ecm;
    }

    public EntityIndex getEntityIndex( OrganizationScope orgScope, CollectionScope collScope ) {

        FullScopeHashKey fshk = new FullScopeHashKey( orgScope, collScope );
        EntityIndex ei = eiCache.get( fshk );

        if ( ei == null ) {
            ei = eif.createEntityIndex( orgScope, collScope );
            eiCache.put( fshk, ei );
        }
        return ei;
    }

    public static class FullScopeHashKey {
        int hashCode;
        public FullScopeHashKey( OrganizationScope orgScope, CollectionScope collScope ) {
            hashCode = orgScope.hashCode();
            hashCode = hashCode * 31 + collScope.hashCode();
        }
        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}

