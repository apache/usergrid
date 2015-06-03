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


import org.apache.usergrid.corepersistence.pipeline.cursor.CursorSerializer;
import org.apache.usergrid.corepersistence.pipeline.read.AbstractPathFilter;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByIdType;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import rx.Observable;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.getEdgeTypeFromConnectionType;


/**
 * Command for reading graph edges on a connection
 */
public class ReadGraphConnectionByTypeFilter extends AbstractPathFilter<Id, Id, Edge>{

    private final GraphManagerFactory graphManagerFactory;
    private final String connectionName;
    private final String entityType;


    /**
     * Create a new instance of our command
     */
    @Inject
    public ReadGraphConnectionByTypeFilter( final GraphManagerFactory graphManagerFactory,
                                            @Assisted("connectionName") final String connectionName, @Assisted("entityType") final String entityType ) {
        this.graphManagerFactory = graphManagerFactory;
        this.connectionName = connectionName;
        this.entityType = entityType;
    }



    @Override
    public Observable<FilterResult<Id>> call( final Observable<FilterResult<Id>> filterResultObservable ) {

        //get the graph manager
        final GraphManager graphManager = graphManagerFactory.createEdgeManager( pipelineContext.getApplicationScope() );



        final String edgeName = getEdgeTypeFromConnectionType( connectionName );


        //return all ids that are emitted from this edge
        return filterResultObservable.flatMap( idFilterResult -> {

              //set our our constant state
            final Optional<Edge> startFromCursor = getSeekValue();
            final Id id = idFilterResult.getValue();

            final SimpleSearchByIdType search =
                new SimpleSearchByIdType( id, edgeName, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                    entityType, startFromCursor );

            return graphManager.loadEdgesFromSourceByType( search ).map(
                edge -> createFilterResult( edge.getTargetNode(), edge, idFilterResult.getPath() ));
        } );
    }


    @Override
    protected CursorSerializer<Edge> getCursorSerializer() {
        return EdgeCursorSerializer.INSTANCE;
    }


}
