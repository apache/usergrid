/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
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

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.CpEntityManagerFactory;
import static org.apache.usergrid.corepersistence.CoreModule.EVENTS_DISABLED;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.event.EntityVersionCreated;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.model.entity.Entity;


/**
 * Clean up stale entity indexes when new version of Entity created. Called when an Entity is
 * updated by the Collections module and we react by calling the Query Index module and removing
 * any indexes that exist for previous versions of the the Entity.
 */
public class EntityVersionCreatedHandler implements EntityVersionCreated {
    private static final Logger logger = LoggerFactory.getLogger(EntityVersionCreatedHandler.class );

    @Inject
    EntityManagerFactory emf;


    @Override
    public void versionCreated( final CollectionScope scope, final Entity entity ) {

        // This check is for testing purposes and for a test that to be able to dynamically turn
        // off and on delete previous versions so that it can test clean-up on read.
        if ( System.getProperty( EVENTS_DISABLED, "false" ).equals( "true" )) {
            return;
        }

        logger.debug("Handling versionCreated for entity {}:{} v {} "
            + "scope\n   name: {}\n   owner: {}\n   app: {}",
            new Object[] {
                entity.getId().getType(),
                entity.getId().getUuid(),
                entity.getVersion(),
                scope.getName(),
                scope.getOwner(),
                scope.getApplication()});

        CpEntityManagerFactory cpemf = (CpEntityManagerFactory)emf;
        final EntityIndex ei = cpemf.getManagerCache().getEntityIndex(scope);

        ei.deletePreviousVersions( entity.getId(), entity.getVersion() );
    }
}
