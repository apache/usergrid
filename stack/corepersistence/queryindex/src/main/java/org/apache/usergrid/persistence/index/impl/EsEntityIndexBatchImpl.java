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
package org.apache.usergrid.persistence.index.impl;


import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.index.CandidateResult;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.IndexAlias;
import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.index.IndexLocationStrategy;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.index.utils.IndexValidationUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;


public class EsEntityIndexBatchImpl implements EntityIndexBatch {

    private static final Logger logger = LoggerFactory.getLogger( EsEntityIndexBatchImpl.class );


    private final IndexAlias alias;

    private final IndexLocationStrategy indexLocationStrategy;

    private final EntityIndex entityIndex;
    private final ApplicationScope applicationScope;
    private IndexOperationMessage container;


    public EsEntityIndexBatchImpl( final IndexLocationStrategy locationStrategy,
                                   final EntityIndex entityIndex
    ) {
        this.indexLocationStrategy = locationStrategy;

        this.entityIndex = entityIndex;
        this.applicationScope = indexLocationStrategy.getApplicationScope();

        this.alias = indexLocationStrategy.getAlias();
        //constrained
        this.container = new IndexOperationMessage();
    }


    @Override
    public EntityIndexBatch index( final IndexEdge indexEdge, final Entity entity ) {
        return index( indexEdge,entity, Optional.empty() );
    }

    @Override
    public EntityIndexBatch index( final IndexEdge indexEdge, final Entity entity, final Optional<Set<String>> fieldsToIndex ) {
        IndexValidationUtils.validateIndexEdge(indexEdge);
        ValidationUtils.verifyEntityWrite(entity);
        ValidationUtils.verifyVersion( entity.getVersion() );

        final String writeAlias = alias.getWriteAlias();

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Indexing to alias {} with scope {} on edge {} with entity data {}",
                    writeAlias, applicationScope, indexEdge, entity.getFieldMap().keySet() );
        }

        //add app id for indexing
        container.addIndexRequest(new IndexOperation(writeAlias, applicationScope, indexEdge, entity,fieldsToIndex));
        return this;
    }

    @Override
    public EntityIndexBatch deindex( final SearchEdge searchEdge, final Id id, final UUID version ) {

        IndexValidationUtils.validateSearchEdge(searchEdge);
        ValidationUtils.verifyIdentity(id);
        ValidationUtils.verifyVersion( version );

        String[] indexes = entityIndex.getIndexes();
        //get the default index if no alias exists yet
        if ( indexes == null || indexes.length == 0 ) {
            throw new IllegalStateException("No indexes exist for " + indexLocationStrategy.getAlias().getWriteAlias());
        }

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Deindexing to indexes {} with scope {} on edge {} with id {} and version {} ",
                indexes, applicationScope, searchEdge, id, version );
        }


        container.addDeIndexRequest(new DeIndexOperation(indexes, applicationScope, searchEdge, id, version));

        return this;
    }

    public EntityIndexBatch deindexWithDocId( final String docId ) {

        String[] indexes = entityIndex.getIndexes();
        //get the default index if no alias exists yet
        if ( indexes == null || indexes.length == 0 ) {
            throw new IllegalStateException("No indexes exist for " + indexLocationStrategy.getAlias().getWriteAlias());
        }

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Deindexing to indexes {} with with documentId {} ",
                indexes, docId );
        }


        container.addDeIndexRequest( new DeIndexOperation( indexes, docId ) );

        return this;
    }


    @Override
    public EntityIndexBatch deindex( final SearchEdge searchEdge, final Entity entity ) {
        return deindex( searchEdge, entity.getId(), entity.getVersion() );
    }


    @Override
    public EntityIndexBatch deindex(final SearchEdge searchEdge, final CandidateResult entity) {

        return deindex( searchEdge, entity.getId(), entity.getVersion() );
    }
    @Override
    public EntityIndexBatch deindex( final CandidateResult entity ) {

        return deindexWithDocId(entity.getDocId());
    }

    @Override
    public IndexOperationMessage build() {
        return container;
    }

    @Override
    public int size() {
        return container.getDeIndexRequests().size() + container.getIndexRequests().size();
    }
}
