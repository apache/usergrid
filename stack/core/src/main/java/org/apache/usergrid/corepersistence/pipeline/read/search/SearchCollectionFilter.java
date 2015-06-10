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

package org.apache.usergrid.corepersistence.pipeline.read.search;


import org.apache.usergrid.corepersistence.index.IndexLocationStrategyFactory;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.index.SearchTypes;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.createCollectionSearchEdge;


public class SearchCollectionFilter extends AbstractElasticSearchFilter {

    private final IndexLocationStrategyFactory indexLocationStrategyFactory;
    private final String collectionName;
    private final String entityType;

    /**
     * Create a new instance of our command
     *
     * @param entityIndexFactory The entity index factory used to search
     * @param  metricsFactory The metrics factory for metrics
     * @param collectionName The name of the collection
     * @param entityType The entity type
     */
    @Inject
    public SearchCollectionFilter( final EntityIndexFactory entityIndexFactory,
                                   final IndexLocationStrategyFactory indexLocationStrategyFactory,
                                   final MetricsFactory metricsFactory,
                                   @Assisted( "query" ) final String query,
                                   @Assisted( "collectionName" ) final String collectionName,
                                   @Assisted( "entityType" ) final String entityType ) {
        super( entityIndexFactory, metricsFactory, indexLocationStrategyFactory, query );
        this.indexLocationStrategyFactory = indexLocationStrategyFactory;
        this.collectionName = collectionName;
        this.entityType = entityType;
    }



    @Override
    protected SearchTypes getSearchTypes() {
        final SearchTypes types = SearchTypes.fromTypes( entityType );

        return types;
    }


    @Override
    protected SearchEdge getSearchEdge( final Id incomingId ) {
        final SearchEdge searchEdge = createCollectionSearchEdge( incomingId, collectionName );

        return searchEdge;
    }



}
