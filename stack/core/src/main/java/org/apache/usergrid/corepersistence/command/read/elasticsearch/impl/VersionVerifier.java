/*
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

package org.apache.usergrid.corepersistence.command.read.elasticsearch.impl;


import java.util.Collection;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.VersionSet;
import org.apache.usergrid.persistence.index.CandidateResult;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.uuid.UUIDComparator;


/**
 * A loader that verifies versions are correct in Cassandra and match ElasticSearch
 */
public abstract class VersionVerifier implements ResultsVerifier {

    private static final Logger logger = LoggerFactory.getLogger( VersionVerifier.class );

    private VersionSet ids;


    @Override
    public void loadResults( final Collection<Id> idsToLoad, final EntityCollectionManager ecm ) {
        ids = ecm.getLatestVersion( idsToLoad ).toBlocking().last();
    }


    @Override
    public boolean isValid( final CandidateResult candidateResult ) {
        final Id entityId = candidateResult.getId();

        final MvccLogEntry version = ids.getMaxVersion( entityId );

        //version wasn't found ,deindex
        if ( version == null ) {
            logger.warn( "Version for Entity {}:{} not found",
                    entityId.getUuid(), entityId.getUuid() );

            return false;
        }

        final UUID savedVersion = version.getVersion();

        if ( UUIDComparator.staticCompare( savedVersion, candidateResult.getVersion() ) > 0 ) {
            logger.debug( "Stale version of Entity uuid:{} type:{}, stale v:{}, latest v:{}",
                new Object[] {
                    entityId.getUuid(),
                    entityId.getType(),
                    candidateResult.getVersion(),
                    savedVersion
            } );

            return false;
        }

        return true;
    }

}
