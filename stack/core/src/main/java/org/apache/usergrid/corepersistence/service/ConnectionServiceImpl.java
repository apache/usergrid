/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.corepersistence.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.pipeline.builder.EntityBuilder;
import org.apache.usergrid.corepersistence.pipeline.builder.IdBuilder;
import org.apache.usergrid.corepersistence.pipeline.builder.PipelineBuilderFactory;
import org.apache.usergrid.corepersistence.pipeline.read.ResultsPage;
import org.apache.usergrid.corepersistence.rx.impl.AllEntityIdsObservable;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;


@Singleton
public class ConnectionServiceImpl implements ConnectionService {


    private static final Logger logger = LoggerFactory.getLogger( ConnectionServiceImpl.class );

    private final PipelineBuilderFactory pipelineBuilderFactory;
    private final AllEntityIdsObservable allEntityIdsObservable;
    private final GraphManagerFactory graphManagerFactory;


    @Inject
    public ConnectionServiceImpl( final PipelineBuilderFactory pipelineBuilderFactory,
                                  final AllEntityIdsObservable allEntityIdsObservable,
                                  final GraphManagerFactory graphManagerFactory ) {
        this.pipelineBuilderFactory = pipelineBuilderFactory;
        this.allEntityIdsObservable = allEntityIdsObservable;
        this.graphManagerFactory = graphManagerFactory;
    }


    @Override
    public Observable<ResultsPage<Entity>> searchConnection( final ConnectionSearch search ) {
        //set startid -- graph | es query filter -- load entities filter (verifies exists) --> results page collector
        // -> 1.0 results

        //  startid -- graph edge load -- entity load (verify) from ids -> results page collector
        // startid -- eq query candiddate -- entity load (verify) from canddiates -> results page collector

        //startid -- graph edge load -- entity id verify --> filter to connection ref --> connection ref collector
        //startid -- eq query candiddate -- candidate id verify --> filter to connection ref --> connection ref
        // collector


        final Optional<String> query = search.getQuery();

        final IdBuilder pipelineBuilder =
            pipelineBuilderFactory.create( search.getApplicationScope() ).withCursor( search.getCursor() )
                                  .withLimit( search.getLimit() ).fromId( search.getSourceNodeId() );


        //we want to load all entities

        final EntityBuilder results;


        if ( !query.isPresent() ) {
            results =
                pipelineBuilder.traverseConnection( search.getConnectionName(), search.getEntityType() ).loadEntities();
        }

        else {

            results =
                pipelineBuilder.searchConnection( search.getConnectionName(), query.get(), search.getEntityType() )
                               .loadEntities();
        }


        return results.build();
    }


    @Override
    public Observable<ResultsPage<ConnectionRef>> searchConnectionAsRefs( final ConnectionSearch search ) {

        final Optional<String> query = search.getQuery();

        final Id sourceNodeId = search.getSourceNodeId();

        final IdBuilder pipelineBuilder =
            pipelineBuilderFactory.create( search.getApplicationScope() ).withCursor( search.getCursor() )
                                  .withLimit( search.getLimit() ).fromId( sourceNodeId );


        final IdBuilder traversedIds;
        final String connectionName = search.getConnectionName();

        if ( !query.isPresent() ) {
            traversedIds = pipelineBuilder.traverseConnection( connectionName, search.getEntityType() );
        }
        else {
            traversedIds =
                pipelineBuilder.searchConnection( connectionName, query.get(), search.getEntityType() ).loadIds();
        }

        //create connection refs

        final Observable<ResultsPage<ConnectionRef>> results =
            traversedIds.loadConnectionRefs( sourceNodeId, connectionName ).build();

        return results;
    }


    @Override
    public Observable<ConnectionScope> deDupeConnections(
        final Observable<ApplicationScope> applicationScopeObservable ) {


        final Observable<EntityIdScope> entityIds = allEntityIdsObservable.getEntities( applicationScopeObservable );
        //now we have an observable of entityIds.  Walk each connection type

        //get all edge types for connections
        return entityIds.flatMap( entityIdScope -> {

            final ApplicationScope applicationScope = entityIdScope.getApplicationScope();
            final Id entityId = entityIdScope.getId();

            final GraphManager gm = graphManagerFactory.createEdgeManager( applicationScope );

            logger.debug( "Checking connections of id {} in application {}", entityId, applicationScope );

            return gm.getEdgeTypesFromSource(
                new SimpleSearchEdgeType( entityId, CpNamingUtils.EDGE_CONN_PREFIX, Optional.absent() ) )

                //now load all edges from this node of this type
                .flatMap( edgeType -> {

                    logger.debug( "Found edge of types of {}, searching for edges", edgeType );

                    final SearchByEdgeType searchByEdge =
                        new SimpleSearchByEdgeType( entityId, edgeType, Long.MAX_VALUE,
                            SearchByEdgeType.Order.DESCENDING, Optional.absent() );

                    //load edges from the source the with type specified
                    return gm.loadEdgesFromSource( searchByEdge );
                } )

                    //now that we have a stream of edges, stream all versions
                .flatMap( edge -> {

                    logger.debug( "Found edge {}, searching for multiple versions of edge", edge );

                    //keep only the most recent
                    final SearchByEdge searchByEdge =
                        new SimpleSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getTargetNode(), Long.MAX_VALUE,
                            SearchByEdgeType.Order.DESCENDING, Optional.absent() );
                    return gm.loadEdgeVersions( searchByEdge )
                        //skip the first version since it's the one we want to retain
                        .skip( 1 )
                            //mark for deletion
                        .flatMap( edgeToDelete -> {

                            logger.debug( "Deleting edge {}", edgeToDelete );

                            //mark the edge and ignore the cleanup result
                            return gm.markEdge( edgeToDelete );
                        } )
                            //mark all versions, then on the last, delete them from cass
                        .flatMap( lastMarkedEdge -> gm.deleteEdge( lastMarkedEdge ) );


                    // validate there is only 1 version of it, delete anything > than the min

                } ).map( deletedEdge -> new ConnectionScope( applicationScope, deletedEdge ) );
        } );
    }
}
