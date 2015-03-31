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


import java.util.UUID;

import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.CpEntityManagerFactory;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.event.EntityDeleted;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.impl.IndexScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import static org.apache.usergrid.corepersistence.CoreModule.EVENTS_DISABLED;


/**
 * Delete all Query Index indexes associated with an Entity that has just been deleted.
 */
@Singleton
public class EntityDeletedHandler implements EntityDeleted {
    private static final Logger logger = LoggerFactory.getLogger( EntityDeletedHandler.class );


    private final EntityManagerFactory emf;


    @Inject
    public EntityDeletedHandler( final EntityManagerFactory emf ) {this.emf = emf;}


    @Override
    public void deleted( CollectionScope scope, Id entityId, UUID version ) {

        // This check is for testing purposes and for a test that to be able to dynamically turn
        // off and on delete previous versions so that it can test clean-up on read.
        if ( System.getProperty( EVENTS_DISABLED, "false" ).equals( "true" ) ) {
            return;
        }

        logger.debug( "Handling deleted event for entity {}:{} v {} " + "scope\n   name: {}\n   owner: {}\n   app: {}",
            new Object[] {
                entityId.getType(), entityId.getUuid(), version, scope.getName(), scope.getOwner(),
                scope.getApplication()
            } );

        CpEntityManagerFactory cpemf = ( CpEntityManagerFactory ) emf;
        final ApplicationEntityIndex ei = cpemf.getManagerCache().getEntityIndex( scope );

        final IndexScope indexScope =
            new IndexScopeImpl( new SimpleId( scope.getOwner().getUuid(), scope.getOwner().getType() ),
                scope.getName() );


        ei.createBatch().deindex( indexScope, entityId, version ).execute();
    }
}
