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


import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.index.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.index.utils.IndexValidationUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import rx.Observable;


public class EsEntityIndexBatchImpl implements EntityIndexBatch {

    private static final Logger log = LoggerFactory.getLogger( EsEntityIndexBatchImpl.class );


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
        IndexValidationUtils.validateIndexEdge(indexEdge);
        ValidationUtils.verifyEntityWrite(entity);
        ValidationUtils.verifyVersion( entity.getVersion() );

        final String writeAlias = alias.getWriteAlias();

        if ( log.isDebugEnabled() ) {
            log.debug( "Indexing to alias {} with scope {} on edge {} with entity data {}",
                    new Object[] { writeAlias, applicationScope, indexEdge, entity } );
        }

        //add app id for indexing
        container.addIndexRequest(new IndexOperation(writeAlias, applicationScope, indexEdge, entity));
        return this;
    }


    @Override
    public EntityIndexBatch deindex( final SearchEdge searchEdge, final Id id, final UUID version ) {

        IndexValidationUtils.validateSearchEdge( searchEdge );
        ValidationUtils.verifyIdentity( id );
        ValidationUtils.verifyVersion( version );

        String[] indexes = entityIndex.getIndexes();
        //get the default index if no alias exists yet
        if ( indexes == null || indexes.length == 0 ) {
           throw new IllegalStateException("No indexes exist for " + indexLocationStrategy.getAlias().getWriteAlias());
        }

        if ( log.isDebugEnabled() ) {
            log.debug( "Deindexing to indexes {} with scope {} on edge {} with id {} and version {} ",
                    new Object[] { indexes, applicationScope, searchEdge, id, version } );
        }


        container.addDeIndexRequest( new DeIndexOperation( indexes, applicationScope, searchEdge, id, version ) );

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
    public IndexOperationMessage build() {
        return container;
    }

    @Override
    public int size() {
        return container.getDeIndexRequests().size() + container.getIndexRequests().size();
    }
}
