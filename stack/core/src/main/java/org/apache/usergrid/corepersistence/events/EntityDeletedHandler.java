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

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.event.EntityDeleted;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.model.entity.Id;

import java.util.UUID;
import org.apache.usergrid.corepersistence.CpEntityManagerFactory;
import org.apache.usergrid.corepersistence.CpSetup;
import org.apache.usergrid.corepersistence.HybridEntityManagerFactory;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Delete all Query Index indexes associated with an Entity that has just been deleted. 
 */
public class EntityDeletedHandler implements EntityDeleted {
    private static final Logger logger = LoggerFactory.getLogger(EntityDeletedHandler.class );


    public EntityDeletedHandler() {
        logger.debug("Created");        
    }

    @Override
    public void deleted(CollectionScope scope, Id entityId, UUID version) {

        logger.debug("Entering deleted for entity {}:{} v {} "
                + "scope\n   name: {}\n   owner: {}\n   app: {}",
            new Object[] { entityId.getType(), entityId.getUuid(), version,
                scope.getName(), scope.getOwner(), scope.getApplication()});

        HybridEntityManagerFactory hemf = (HybridEntityManagerFactory)CpSetup.getEntityManagerFactory();
        CpEntityManagerFactory cpemf = (CpEntityManagerFactory)hemf.getImplementation();

        final EntityIndex ei = cpemf.getManagerCache().getEntityIndex(scope);

        EntityIndexBatch batch = ei.createBatch();

        batch.deleteEntity( entityId );
        batch.execute();
    }
}
