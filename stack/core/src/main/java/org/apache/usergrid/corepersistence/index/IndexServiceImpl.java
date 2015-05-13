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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.exception.NotImplementedException;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.metrics.ObservableTimer;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.serialization.EdgesObservable;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.CandidateResult;
import org.apache.usergrid.persistence.index.CandidateResults;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.SearchTypes;
import org.apache.usergrid.persistence.index.impl.IndexOperationMessage;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.schema.CollectionInfo;
import org.apache.usergrid.utils.InflectionUtils;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;

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
    private final Timer indexTimer;
    private final Timer addTimer;


    @Inject
    public IndexServiceImpl( final GraphManagerFactory graphManagerFactory, final EntityIndexFactory entityIndexFactory,
                             final EdgesObservable edgesObservable, final IndexFig indexFig, final MetricsFactory metricsFactory ) {
        this.graphManagerFactory = graphManagerFactory;
        this.entityIndexFactory = entityIndexFactory;
        this.edgesObservable = edgesObservable;
        this.indexFig = indexFig;
        this.indexTimer = metricsFactory.getTimer( IndexServiceImpl.class, "index.update_all");
        this.addTimer = metricsFactory.getTimer( IndexServiceImpl.class, "index.add" );
    }


    @Override
    public Observable<IndexOperationMessage> indexEntity( final ApplicationScope applicationScope,
                                                          final Entity entity ) {
        //bootstrap the lower modules from their caches
        final GraphManager gm = graphManagerFactory.createEdgeManager( applicationScope );
        final ApplicationEntityIndex ei = entityIndexFactory.createApplicationEntityIndex( applicationScope );


        final Id entityId = entity.getId();


        //we always index in the target scope
        final Observable<Edge> edgesToTarget = edgesObservable.edgesToTarget( gm, entityId );

        //we may have to index  we're indexing from source->target here
        final Observable<IndexEdge> sourceEdgesToIndex = edgesToTarget.map( edge -> generateScopeFromSource( edge ) );


        //we might or might not need to index from target-> source
        final Observable<IndexEdge> targetSizes = getIndexEdgesAsTarget( gm, entityId );


        //merge the edges together
        final Observable<IndexEdge> observable = Observable.merge( sourceEdgesToIndex, targetSizes);
        //do our observable for batching
        //try to send a whole batch if we can


        //do our observable for batching
        //try to send a whole batch if we can
        final Observable<IndexOperationMessage>  batches =  observable.buffer( indexFig.getIndexBatchSize() )

            //map into batches based on our buffer size
            .flatMap( buffer -> Observable.from( buffer )
                //collect results into a single batch
                .collect( () -> ei.createBatch(), ( batch, indexEdge ) -> {
                    logger.debug( "adding edge {} to batch for entity {}", indexEdge, entity );
                    batch.index( indexEdge, entity );
                } )
                    //return the future from the batch execution
                .flatMap( batch -> batch.execute() ) );

        return ObservableTimer.time( batches, indexTimer );
    }


    @Override
    public Observable<IndexOperationMessage> indexEdge( final ApplicationScope applicationScope, final Entity entity, final Edge edge ) {



        final Observable<IndexOperationMessage> batches =  Observable.just( edge ).map( observableEdge -> {

            //if the node is the target node, generate our scope correctly
            if ( edge.getTargetNode().equals( entity.getId() ) ) {
                return generateScopeFromSource( edge );
            }

            return generateScopeFromTarget( edge );
        } ).flatMap( indexEdge -> {

            final ApplicationEntityIndex ei = entityIndexFactory.createApplicationEntityIndex( applicationScope );

            final EntityIndexBatch batch = ei.createBatch();

            logger.debug( "adding edge {} to batch for entity {}", indexEdge, entity );

            batch.index( indexEdge, entity );

            return batch.execute();
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
            Observable.just( edge ).flatMap( edgeValue -> {
                final ApplicationEntityIndex ei = entityIndexFactory.createApplicationEntityIndex( applicationScope );
                EntityIndexBatch batch = ei.createBatch();


                //review why generating the Scope from the Source  and the target node makes sense.
                final IndexEdge fromSource = generateScopeFromSource( edge );
                final Id targetId = edge.getTargetNode();


                CandidateResults targetEdgesToBeDeindexed = ei.getAllEdgeDocuments( fromSource, targetId, 1000, 0 );

                //Should loop thorugh and query for all documents and if there are no documents then the loop should exit.
                do{
                    batch = deindexBatchIteratorResolver( fromSource, targetEdgesToBeDeindexed, batch );
                    if(!targetEdgesToBeDeindexed.getOffset().isPresent())
                        break;
                    targetEdgesToBeDeindexed = ei.getAllEdgeDocuments( fromSource, targetId, 1000, targetEdgesToBeDeindexed.getOffset().get() );
                }while(!targetEdgesToBeDeindexed.isEmpty());



                final IndexEdge fromTarget = generateScopeFromTarget( edge );
                final Id sourceId = edge.getSourceNode();

                CandidateResults sourceEdgesToBeDeindexed = ei.getAllEdgeDocuments( fromTarget, sourceId, 1000, 0 );

                do{
                    batch = deindexBatchIteratorResolver( fromTarget, sourceEdgesToBeDeindexed, batch );
                    if(!sourceEdgesToBeDeindexed.getOffset().isPresent())
                        break;
                    sourceEdgesToBeDeindexed = ei.getAllEdgeDocuments( fromTarget, sourceId, 1000, sourceEdgesToBeDeindexed.getOffset().get()  );
                }while(!sourceEdgesToBeDeindexed.isEmpty());

                return batch.execute();
            } );

        return ObservableTimer.time( batches, addTimer );
    }

    @Override
    public Observable<IndexOperationMessage> deleteEntityIndexes( final ApplicationScope applicationScope,
                                                                  final Id entityId ) {

        //TODO query ES and remove this entityId
        throw new NotImplementedException( "Implement me" );
    }


    /**
     * Get index edges to the target.  Used in only certain entity types, such as roles, users, groups etc
     * where we doubly index on both directions of the edge
     *
     * @param graphManager The graph manager
     * @param entityId The entity's id
     */
    private Observable<IndexEdge> getIndexEdgesAsTarget( final GraphManager graphManager, final Id entityId ) {

        final String collectionName = InflectionUtils.pluralize( entityId.getType() );


        final CollectionInfo collection = getDefaultSchema().getCollection( Application.ENTITY_TYPE, collectionName );

        //nothing to do
        if ( collection == null ) {
            return Observable.empty();
        }


        final String linkedCollection = collection.getLinkedCollection();

        /**
         * Nothing to link
         */
        if ( linkedCollection == null ) {
            return Observable.empty();
        }


        /**
         * An observable of sizes as we execute batches
         *
         * we're indexing from target->source here
         */
        return edgesObservable.getEdgesFromSource( graphManager, entityId, linkedCollection )
                              .map( edge -> generateScopeFromTarget( edge ) );
    }

    /**
     * Takes in candidate results and uses the iterator to create batch commands
     */

    public EntityIndexBatch deindexBatchIteratorResolver(IndexEdge edge,CandidateResults edgesToBeDeindexed, EntityIndexBatch batch){
        Iterator itr = edgesToBeDeindexed.iterator();
        while( itr.hasNext() ) {
            CandidateResult cr = ( CandidateResult ) itr.next();
            batch.deindex( edge, cr.getId(), cr.getVersion() );
        }
        return batch;
    }





}
