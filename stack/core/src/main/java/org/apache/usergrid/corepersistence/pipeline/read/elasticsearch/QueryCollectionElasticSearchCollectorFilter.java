/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.pipeline.read.elasticsearch;


import org.apache.usergrid.corepersistence.pipeline.read.elasticsearch.impl.ConnectionResultsLoaderFactoryImpl;
import org.apache.usergrid.corepersistence.pipeline.read.elasticsearch.impl.ResultsLoaderFactory;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.index.SearchTypes;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.createCollectionSearchEdge;


/**
 * Command for querying collections
 */
public class QueryCollectionElasticSearchCollectorFilter extends AbstractQueryElasticSearchCollectorFilter {

    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final EntityIndexFactory entityIndexFactory;
    private final String collectionName;


    @Inject
    public QueryCollectionElasticSearchCollectorFilter( final EntityCollectionManagerFactory entityCollectionManagerFactory,
                                                           final EntityIndexFactory entityIndexFactory,
                                                           @Assisted final String collectionName , @Assisted final Query query ) {
        super( entityIndexFactory, query );
        this.entityIndexFactory = entityIndexFactory;
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.collectionName = collectionName;
    }


    @Override
    protected SearchTypes getSearchTypes() {
        final SearchTypes types = SearchTypes.fromTypes( collectionName );

        return types;
    }


    @Override
    protected SearchEdge getSearchEdge( final Id incomingId ) {
        final SearchEdge searchEdge = createCollectionSearchEdge( incomingId, collectionName );

        return searchEdge;
    }


    @Override
    protected ResultsLoaderFactory getResultsLoaderFactory( final Id id ) {
        final EntityCollectionManager entityCollectionManager = entityCollectionManagerFactory.createCollectionManager( applicationScope );
        final ApplicationEntityIndex entityIndex = entityIndexFactory.createApplicationEntityIndex( applicationScope );

        final EntityRef entityRef = getRef( id );
        return new ConnectionResultsLoaderFactoryImpl( entityCollectionManager, entityIndex, entityRef,
            collectionName );
    }
}
