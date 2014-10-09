/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.corepersistence.results;/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.util.CpEntityMapUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityFactory;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.uuid.UUIDComparator;
import com.google.common.base.Optional;


/**
 * A loader that verifies versions are correct in cassandra and match elasticsearch
 */
public class EntityVerifier implements ResultsVerifier {

    private static final Logger logger = LoggerFactory.getLogger( EntityVerifier.class );

    private EntitySet ids;

    private Map<Id, org.apache.usergrid.persistence.model.entity.Entity> entityMapping;


    public EntityVerifier( final int maxSize ) {
        this.entityMapping = new HashMap<>( maxSize );
    }


    @Override
    public void loadResults( final Collection<Id> idsToLoad, final EntityCollectionManager ecm ) {
        ids = ecm.load( idsToLoad ).toBlocking().last();
    }


    @Override
    public boolean isValid( final CandidateResult candidateResult ) {
        final Id entityId = candidateResult.getId();

        final MvccEntity version = ids.getEntity( entityId );

        //version wasn't found ,deindex
        if ( version == null ) {
            logger.warn( "Version for Entity {}:{} not found", entityId.getUuid(), entityId.getUuid() );


            return false;
        }

        final UUID savedVersion = version.getVersion();

        if ( UUIDComparator.staticCompare( savedVersion, candidateResult.getVersion() ) > 0 ) {
            logger.debug( "Stale version of Entity uuid:{} type:{}, stale v:{}, latest v:{}", new Object[] {
                    entityId.getUuid(), entityId.getType(), savedVersion, candidateResult.getVersion()
            } );

            return false;
        }


        final Optional<org.apache.usergrid.persistence.model.entity.Entity> entity = version.getEntity();

        if ( !entity.isPresent() ) {
            return false;
        }

        entityMapping.put( entityId, entity.get() );

        return true;
    }


    @Override
    public Results getResults( final Collection<Id> ids ) {

        final List<Entity> ugEntities = new ArrayList<>( ids.size() );

        for ( final Id id : ids ) {
            final org.apache.usergrid.persistence.model.entity.Entity cpEntity = entityMapping.get( id );

            Entity entity = EntityFactory.newEntity( id.getUuid(), id.getType() );

            Map<String, Object> entityMap = CpEntityMapUtils.toMap( cpEntity );
            entity.addProperties( entityMap );
            ugEntities.add( entity );
        }

        return Results.fromEntities( ugEntities );
    }
}
