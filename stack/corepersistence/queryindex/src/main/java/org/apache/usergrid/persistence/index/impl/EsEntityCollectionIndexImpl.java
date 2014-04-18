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
package org.apache.usergrid.persistence.index.impl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.index.EntityCollectionIndex;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implements index using ElasticSearch Java API and Core Persistence Collections.
 */
public class EsEntityCollectionIndexImpl extends EsEntityIndex implements EntityCollectionIndex {

    private static final Logger log = 
        LoggerFactory.getLogger(EsEntityCollectionIndexImpl.class);

    private String typeName;

    private final CollectionScope collectionScope;

    private final EntityCollectionManager manager;


    @Inject
    public EsEntityCollectionIndexImpl(
            @Assisted final OrganizationScope orgScope, 
            @Assisted("appScope") final CollectionScope appScope,
            @Assisted("scope") final CollectionScope scope,
            IndexFig config,
            EsProvider provider,
            EntityCollectionManagerFactory factory) {
        
        super( orgScope, appScope, config, provider, factory );
        this.manager = factory.createCollectionManager( scope );
        this.collectionScope = scope;

        initIndex();
    }


    @Override
    public String getTypeName() {
        if ( typeName == null) {
            typeName = createCollectionScopeTypeName( collectionScope );
        }
        return typeName;
    }


    @Override
    public void index(Entity entity) {
        super.index( entity, collectionScope );
    }


    @Override
    public EntityCollectionManager getEntityCollectionManager(String scope) {
        // we only have one scope, return manager for that
        return manager;
    }
}
