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

package org.apache.usergrid.corepersistence.events;

import java.util.Map;
import java.util.Set;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.event.EntityDeleted;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.impl.IndexScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import java.util.UUID;
import org.apache.usergrid.corepersistence.CpEntityManager;
import org.apache.usergrid.corepersistence.CpEntityManagerFactory;
import org.apache.usergrid.corepersistence.CpSetup;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.RelationManager;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * purge most current entity
 */
public class EntityDeletedImpl implements EntityDeleted {
    private static final Logger logger = LoggerFactory.getLogger( EntityDeletedImpl.class );


    public EntityDeletedImpl(){

    }

    @Override
    public void deleted(CollectionScope scope, Id entityId, UUID version) {

        logger.debug("Entering deleted for entity {}:{} v {} "
                + "scope\n   name: {}\n   owner: {}\n   app: {}",
            new Object[] { entityId.getType(), entityId.getUuid(), version,
                scope.getName(), scope.getOwner(), scope.getApplication()});


        CpEntityManagerFactory emf = (CpEntityManagerFactory)
                CpSetup.getInjector().getInstance( EntityManagerFactory.class );

        CpEntityManager em = (CpEntityManager)
                emf.getEntityManager( scope.getOwner().getUuid() );

        EntityCollectionManager ecm = emf.getManagerCache().getEntityCollectionManager(scope);


        // TODO: change this so that it gets every version of the entity that 
        // exists as we need to de-index each and every one of them

        org.apache.usergrid.persistence.model.entity.Entity entity = 
            ecm.load( entityId ).toBlocking().last();


        SimpleEntityRef entityRef = new SimpleEntityRef( entityId.getType(), entityId.getUuid());

        if ( entity != null ) {

            // first, delete entity in every collection and connection scope in which it is indexed 

            RelationManager rm = em.getRelationManager( entityRef );
            Map<String, Map<UUID, Set<String>>> owners = null;
            try {
                owners = rm.getOwners();

                logger.debug( "Deleting indexes of all {} collections owning the entity {}:{}", 
                    new Object[] { owners.keySet().size(), entityId.getType(), entityId.getUuid()});

                final EntityIndex ei = emf.getManagerCache().getEntityIndex(scope);

                final EntityIndexBatch batch = ei.createBatch();

                for ( String ownerType : owners.keySet() ) {
                    Map<UUID, Set<String>> collectionsByUuid = owners.get( ownerType );

                    for ( UUID uuid : collectionsByUuid.keySet() ) {
                        Set<String> collectionNames = collectionsByUuid.get( uuid );
                        for ( String coll : collectionNames ) {

                            IndexScope indexScope = new IndexScopeImpl(
                                new SimpleId( uuid, ownerType ), 
                                CpNamingUtils.getCollectionScopeNameFromCollectionName( coll ));

                            batch.index( indexScope, entity );
                        }
                    }
                }

                // deindex from default index scope
                IndexScope defaultIndexScope = new IndexScopeImpl( scope.getApplication(),
                    CpNamingUtils.getCollectionScopeNameFromEntityType( entityRef.getType()));

                batch.deindex(defaultIndexScope,  entity );

                IndexScope allTypesIndexScope = new IndexScopeImpl(
                    scope.getApplication(), CpNamingUtils.ALL_TYPES);

                batch.deindex( allTypesIndexScope,  entity );

                batch.execute();

            } catch (Exception e) {
                logger.error("Cannot deindex from owners of entity {}:{}", 
                        entityId.getType(), entityId.getUuid());
                logger.error("The exception", e);
            }
        }
    }
}
