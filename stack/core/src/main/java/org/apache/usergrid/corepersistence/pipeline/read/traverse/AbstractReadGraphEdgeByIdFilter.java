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

package org.apache.usergrid.corepersistence.pipeline.read.traverse;


import org.apache.usergrid.corepersistence.pipeline.PipelineOperation;
import org.apache.usergrid.corepersistence.pipeline.read.AbstractFilter;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import rx.Observable;


/**
 * Filter should take and Id and a graph edge, and ensure the connection between the two exists
 */
public abstract class AbstractReadGraphEdgeByIdFilter extends AbstractFilter<FilterResult<Id>, FilterResult<Id>> implements
    PipelineOperation<FilterResult<Id>, FilterResult<Id>> {

    private final GraphManagerFactory graphManagerFactory;
    private final Id targetId;


    @Inject
    public AbstractReadGraphEdgeByIdFilter( final GraphManagerFactory graphManagerFactory, @Assisted final Id
        targetId ) {
        this.graphManagerFactory = graphManagerFactory;
        this.targetId = targetId;
    }


    @Override
    public Observable<FilterResult<Id>> call( final Observable<FilterResult<Id>> filterValueObservable ) {

        final GraphManager gm = graphManagerFactory.createEdgeManager( pipelineContext.getApplicationScope() );

        return filterValueObservable.flatMap( filterValue -> {
            final String edgeTypeName = getEdgeName();
            final Id id = filterValue.getValue();

            //create our search
            final SearchByEdge searchByEdge =
                new SimpleSearchByEdge( id, edgeTypeName, targetId, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                    Optional.absent() );

            //load the versions of the edge, take the first since that's all we need to validate existence, then emit the target node
            return gm.loadEdgeVersions( searchByEdge ).take( 1 ).map( edge -> edge.getTargetNode() ).map( targetId -> new FilterResult<>(targetId, filterValue.getPath()));
        } );
    }


    /**
     * Get the name of the edge to be used in the seek
     */
    protected abstract String getEdgeName();
}
