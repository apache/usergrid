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
import java.util.UUID;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.index.EntityConnectionIndex;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Results;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EsEntityConnectionIndexImpl extends EsEntityIndex implements EntityConnectionIndex {

    private static final Logger log = 
            LoggerFactory.getLogger(EsEntityConnectionIndexImpl.class);

    private Entity sourceEntity;

    private String type;

    private String typeName;


    @Inject
    public EsEntityConnectionIndexImpl(
            @Assisted Entity sourceEntity,
            @Assisted String type,
            @Assisted final OrganizationScope orgScope, 
            @Assisted("appScope") final CollectionScope appScope,
            IndexFig config,
            EsProvider provider,
            EntityCollectionManagerFactory factory) {

        super( orgScope, appScope, config, provider, factory );
        this.type = type;
        this.sourceEntity = sourceEntity;

        initIndex();
    }

    
    @Override
    public void indexConnection( Entity target, CollectionScope targetScope ) {
        super.index( target, targetScope );
    }

    
    @Override
    public void deleteConnection( Id target ) {
        // TODO
    }

    
    @Override
    public Results searchConnections( Query query) {
        return super.execute( query );
    }

    
    @Override
    public String getTypeName() {
        if ( typeName == null ) {
            typeName = createEntityConnectionScopeTypeName(); 
        }
        return typeName;
    }


    private String createEntityConnectionScopeTypeName() {
        StringBuilder sb = new StringBuilder();
        String sep = DOC_TYPE_SEPARATOR;
        sb.append( sourceEntity.getId().getUuid() ).append(sep);
        sb.append( sourceEntity.getId().getType() ).append(sep);
        sb.append( type ).append(sep);
        return sb.toString();
    }


    @Override
    public EntityCollectionManager getEntityCollectionManager( String scope ) {

        String[] scopeParts = scope.split( DOC_TYPE_SEPARATOR_SPLITTER );

        String scopeName      =                  scopeParts[0];
        UUID   scopeOwnerUuid = UUID.fromString( scopeParts[1] );
        String scopeOwnerType =                  scopeParts[2];
        UUID   scopeOrgUuid   = UUID.fromString( scopeParts[3] );
        String scopeOrgType   =                  scopeParts[4];

        Id ownerId = new SimpleId( scopeOwnerUuid, scopeOwnerType );
        Id orgId = new SimpleId( scopeOrgUuid, scopeOrgType );

        CollectionScope collScope = new CollectionScopeImpl( orgId, ownerId, scopeName );

        EntityCollectionManager ecm = factory.createCollectionManager(collScope);

        return ecm;
    }
}
