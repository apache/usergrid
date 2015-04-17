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

import java.util.*;

import org.apache.usergrid.persistence.core.future.BetterFuture;
import org.apache.usergrid.persistence.index.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.index.utils.IndexValidationUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;



public class EsEntityIndexBatchImpl implements EntityIndexBatch {

    private static final Logger log = LoggerFactory.getLogger( EsEntityIndexBatchImpl.class );

    private final ApplicationScope applicationScope;

    private final IndexAlias alias;
    private final IndexIdentifier indexIdentifier;

    private final IndexBufferProducer indexBatchBufferProducer;

    private final AliasedEntityIndex entityIndex;
    private IndexOperationMessage container;



    public EsEntityIndexBatchImpl(final ApplicationScope applicationScope,
                                  final IndexBufferProducer indexBatchBufferProducer,
                                  final AliasedEntityIndex entityIndex, IndexIdentifier indexIdentifier ) {

        this.applicationScope = applicationScope;
        this.indexBatchBufferProducer = indexBatchBufferProducer;
        this.entityIndex = entityIndex;
        this.indexIdentifier = indexIdentifier;
        this.alias = indexIdentifier.getAlias();
        //constrained
        this.container = new IndexOperationMessage();
    }


    @Override
    public EntityIndexBatch index( final IndexScope indexScope, final Entity entity ) {
        IndexValidationUtils.validateIndexScope( indexScope );
        ValidationUtils.verifyEntityWrite( entity );
        ValidationUtils.verifyVersion( entity.getVersion() );

        //add app id for indexing
        container.addIndexRequest(new IndexRequest(alias.getWriteAlias(), applicationScope,indexScope, entity));
        return this;
    }


    @Override
    public EntityIndexBatch deindex( final IndexScope indexScope, final Id id, final UUID version) {

        IndexValidationUtils.validateIndexScope( indexScope );
        ValidationUtils.verifyIdentity(id);
        ValidationUtils.verifyVersion( version );

        String[] indexes = entityIndex.getUniqueIndexes();
        //get the default index if no alias exists yet
        if(indexes == null ||indexes.length == 0){
            indexes = new String[]{indexIdentifier.getIndex(null)};
        }

        container.addDeIndexRequest(new DeIndexRequest(indexes, applicationScope,indexScope,id,version));

        return this;
    }


    @Override
    public EntityIndexBatch deindex( final IndexScope indexScope, final Entity entity ) {
        return deindex( indexScope, entity.getId(), entity.getVersion() );
    }


    @Override
    public EntityIndexBatch deindex( final IndexScope indexScope, final CandidateResult entity ) {

        return deindex( indexScope, entity.getId(), entity.getVersion() );
    }

    @Override
    public BetterFuture execute() {
        IndexOperationMessage tempContainer = container;
        container = new IndexOperationMessage();

        /**
         * No-op, just disregard it
         */
        if(tempContainer.isEmpty()){
            tempContainer.done();
            return tempContainer.getFuture();
        }

        return indexBatchBufferProducer.put(tempContainer);
    }


    @Override
    public int size() {
        return container.getDeIndexRequests().size() + container.getIndexRequests().size();
    }




}
