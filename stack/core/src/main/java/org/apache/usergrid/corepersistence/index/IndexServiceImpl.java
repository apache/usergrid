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

package org.apache.usergrid.corepersistence.index;


import java.util.Iterator;
import java.util.UUID;

import org.apache.usergrid.persistence.index.*;
import org.apache.usergrid.utils.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.metrics.ObservableTimer;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.persistence.graph.serialization.EdgesObservable;
import org.apache.usergrid.persistence.index.impl.IndexOperationMessage;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.utils.InflectionUtils;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.createSearchEdgeFromSource;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.generateScopeFromSource;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.generateScopeFromTarget;
import static org.apache.usergrid.persistence.Schema.getDefaultSchema;


/**
 * Implementation of the indexing service
 */
@Singleton
public class IndexServiceImpl implements IndexService {


    private static final Logger logger = LoggerFactory.getLogger( IndexServiceImpl.class );

    private final GraphManagerFactory graphManagerFactory;
    private final EntityIndexFactory entityIndexFactory;
    private final EdgesObservable edgesObservable;
    private final IndexFig indexFig;
    private final IndexLocationStrategyFactory indexLocationStrategyFactory;
    private final Timer indexTimer;
    private final Timer addTimer;


    @Inject
    public IndexServiceImpl( final GraphManagerFactory graphManagerFactory, final EntityIndexFactory entityIndexFactory,
                             final EdgesObservable edgesObservable, final IndexFig indexFig, final IndexLocationStrategyFactory indexLocationStrategyFactory, final MetricsFactory metricsFactory ) {
        this.graphManagerFactory = graphManagerFactory;
        this.entityIndexFactory = entityIndexFactory;
        this.edgesObservable = edgesObservable;
        this.indexFig = indexFig;
        this.indexLocationStrategyFactory = indexLocationStrategyFactory;
        this.indexTimer = metricsFactory.getTimer( IndexServiceImpl.class, "index.update_all");
        this.addTimer = metricsFactory.getTimer( IndexServiceImpl.class, "index.add" );
    }


    @Override
    public Observable<IndexOperationMessage> indexEntity( final ApplicationScope applicationScope,
                                                          final Entity entity ) {
        //bootstrap the lower modules from their caches
        final GraphManager gm = graphManagerFactory.createEdgeManager( applicationScope );
        final EntityIndex ei = entityIndexFactory.createEntityIndex(indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope));


        final Id entityId = entity.getId();


        //we always index in the target scope
        final Observable<Edge> edgesToTarget = edgesObservable.edgesToTarget( gm, entityId );

        //we may have to index  we're indexing from source->target here
        final Observable<IndexEdge> sourceEdgesToIndex = edgesToTarget.map( edge -> generateScopeFromSource( edge ) );

        //do our observable for batching
        //try to send a whole batch if we can
        final Observable<IndexOperationMessage>  batches =  sourceEdgesToIndex.buffer( indexFig.getIndexBatchSize() )

            //map into batches based on our buffer size
            .flatMap( buffer -> Observable.from( buffer )
                //collect results into a single batch
                .collect( () -> ei.createBatch(), ( batch, indexEdge ) -> {
                    logger.debug( "adding edge {} to batch for entity {}", indexEdge, entity );
                    batch.index( indexEdge, entity );
                } )
                    //return the future from the batch execution
                .map( batch -> batch.build() ) );

        return ObservableTimer.time( batches, indexTimer );
    }


    @Override
    public Observable<IndexOperationMessage> indexEdge( final ApplicationScope applicationScope, final Entity entity, final Edge edge ) {

        final Observable<IndexOperationMessage> batches =  Observable.just( edge ).map( observableEdge -> {

            //if the node is the target node, generate our scope correctly
            if ( edge.getTargetNode().equals( entity.getId() ) ) {

                return generateScopeFromSource( edge );
            }

            throw new IllegalArgumentException("target not equal to entity + "+entity.getId());
        } ).map( indexEdge -> {

            final EntityIndex ei = entityIndexFactory.createEntityIndex(indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope) );

            final EntityIndexBatch batch = ei.createBatch();

            logger.debug( "adding edge {} to batch for entity {}", indexEdge, entity );

            batch.index( indexEdge, entity );

            return batch.build();
        } );

        return ObservableTimer.time( batches, addTimer  );


    }


    //Steps to delete an IndexEdge.
    //1.Take the search edge given and search for all the edges in elasticsearch matching that search edge
    //2. Batch Delete all of those edges returned in the previous search.
    //TODO: optimize loops further.
    @Override
    public Observable<IndexOperationMessage> deleteIndexEdge( final ApplicationScope applicationScope,
                                                              final Edge edge ) {

        final Observable<IndexOperationMessage> batches =
            Observable.just( edge ).map( edgeValue -> {
                final EntityIndex ei = entityIndexFactory.createEntityIndex(indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope) );
                EntityIndexBatch batch = ei.createBatch();


                //review why generating the Scope from the Source  and the target node makes sense.
                final IndexEdge fromSource = generateScopeFromSource( edge );
                final Id targetId = edge.getTargetNode();

                CandidateResults targetEdgesToBeDeindexed = ei.getAllEdgeDocuments( fromSource, targetId );


                //1. Feed the observable the candidate results you got back. Since it now does the aggregation for you
                // you don't need to worry about putting your code in a do while.


                batch = deindexBatchIteratorResolver( fromSource, targetEdgesToBeDeindexed, batch );

                final IndexEdge fromTarget = generateScopeFromTarget( edge );
                final Id sourceId = edge.getSourceNode();

                CandidateResults sourceEdgesToBeDeindexed = ei.getAllEdgeDocuments( fromTarget, sourceId );

                batch = deindexBatchIteratorResolver( fromTarget, sourceEdgesToBeDeindexed, batch );

                return batch.build();
            } );

        return ObservableTimer.time( batches, addTimer );
    }

    //This should look up the entityId and delete any documents with a timestamp that comes before
    //The edges that are connected will be compacted away from the graph.
    @Override
    public Observable<IndexOperationMessage> deleteEntityIndexes( final ApplicationScope applicationScope,
                                                                  final Id entityId, final UUID markedVersion ) {

        //bootstrap the lower modules from their caches
        final EntityIndex ei = entityIndexFactory.createEntityIndex(indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope) );

        CandidateResults crs = ei.getAllEntityVersionsBeforeMarkedVersion( entityId, markedVersion );

        //If we get no search results, its possible that something was already deleted or
        //that it wasn't indexed yet. In either case we can't delete anything and return an empty observable..
        if(crs.isEmpty())
            return Observable.empty();

        UUID timeUUID = UUIDUtils.isTimeBased(entityId.getUuid()) ? entityId.getUuid() : UUIDUtils.newTimeUUID();
        //not actually sure about the timestamp but ah well. works.
        SearchEdge searchEdge = createSearchEdgeFromSource( new SimpleEdge( applicationScope.getApplication(),
            CpNamingUtils.getEdgeTypeFromCollectionName( InflectionUtils.pluralize( entityId.getType() ) ), entityId,
            timeUUID.timestamp() ) );


        final Observable<IndexOperationMessage>  batches = Observable.from( crs )
                //collect results into a single batch
                .collect( () -> ei.createBatch(), ( batch, candidateResult ) -> {
                    logger.debug( "Deindexing on edge {} for entity {} added to batch",searchEdge , entityId );
                    batch.deindex( searchEdge, candidateResult );
                } )
                    //return the future from the batch execution
                .map( batch ->batch.build() );

        return ObservableTimer.time(batches, indexTimer);
    }

    /**
     * Takes in candidate results and uses the iterator to create batch commands
     */

    public EntityIndexBatch deindexBatchIteratorResolver(IndexEdge edge,CandidateResults edgesToBeDeindexed, EntityIndexBatch batch){
        Iterator itr = edgesToBeDeindexed.iterator();
        while( itr.hasNext() ) {
            batch.deindex( edge, ( CandidateResult ) itr.next());
        }
        return batch;
    }

}
