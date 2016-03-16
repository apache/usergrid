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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.index.*;
import org.apache.usergrid.persistence.index.impl.EntityField;
import org.apache.usergrid.persistence.index.impl.IndexOperation;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.utils.JsonUtils;
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
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;
import static org.apache.usergrid.persistence.Schema.getDefaultSchema;


/**
 * Implementation of the indexing service
 */
@Singleton
public class IndexServiceImpl implements IndexService {


    private static final Logger logger = LoggerFactory.getLogger( IndexServiceImpl.class );

    private final GraphManagerFactory graphManagerFactory;
    private final EntityIndexFactory entityIndexFactory;
    private final MapManagerFactory mapManagerFactory;
    private final EdgesObservable edgesObservable;
    private final IndexFig indexFig;
    private final IndexLocationStrategyFactory indexLocationStrategyFactory;
    private final Timer indexTimer;
    private final Timer addTimer;


    @Inject
    public IndexServiceImpl( final GraphManagerFactory graphManagerFactory, final EntityIndexFactory entityIndexFactory,
                             final MapManagerFactory mapManagerFactory,
                             final EdgesObservable edgesObservable, final IndexFig indexFig,
                             final IndexLocationStrategyFactory indexLocationStrategyFactory,
                             final MetricsFactory metricsFactory ) {
        this.graphManagerFactory = graphManagerFactory;
        this.entityIndexFactory = entityIndexFactory;
        this.mapManagerFactory = mapManagerFactory;
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
        final Observable<IndexOperationMessage>  batches =  sourceEdgesToIndex
            .buffer(250, TimeUnit.MILLISECONDS, indexFig.getIndexBatchSize() )

            //map into batches based on our buffer size
            .flatMap( buffer -> Observable.from( buffer )
                //collect results into a single batch
                .collect( () -> ei.createBatch(), ( batch, indexEdge ) -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("adding edge {} to batch for entity {}", indexEdge, entity);
                    }
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

            if (logger.isDebugEnabled()) {
                logger.debug("adding edge {} to batch for entity {}", indexEdge, entity);
            }

            Map<String, Object> map = getFilteredStringObjectMap( applicationScope, entity, indexEdge );

            if(map!=null){
                batch.index( indexEdge, entity ,map);
            }
            else{
                batch.index( indexEdge,entity );
            }


            return batch.build();
        } );

        return ObservableTimer.time( batches, addTimer  );


    }


    private Map<String, Object> getFilteredStringObjectMap( final ApplicationScope applicationScope, final Entity entity,
                                                    final IndexEdge indexEdge ) {IndexOperation indexOperation = new IndexOperation(  );


        indexEdge.getNodeId().getUuid();

        Id mapOwner = new SimpleId( indexEdge.getNodeId().getUuid(), TYPE_APPLICATION );

        final MapScope ms = CpNamingUtils.getEntityTypeMapScope(mapOwner );

        MapManager mm = mapManagerFactory.createMapManager( ms );

        String jsonMap = mm.getString( indexEdge.getEdgeName().split( "\\|" )[1] );

        Set<String> defaultProperties = null;

        if(jsonMap != null) {

            Map jsonMapData = ( Map ) JsonUtils.parse( jsonMap );
            Schema schema = Schema.getDefaultSchema();
            defaultProperties = schema.getRequiredProperties( indexEdge.getEdgeName().split( "\\|" )[1]);
            //TODO: additional logic to
            ArrayList fieldsToKeep = ( ArrayList ) jsonMapData.get( "fields" );
            defaultProperties.addAll( fieldsToKeep );
        }
        else
            return null;

        Map<String, Object> map = indexOperation.convertedEntityToBeIndexed( applicationScope,indexEdge,entity );
        HashSet mapFields = ( HashSet ) map.get( "fields" );

        if(defaultProperties!=null) {
            final Set<String> finalDefaultProperties = defaultProperties;
            Iterator collectionIterator = mapFields.iterator();
            while ( collectionIterator.hasNext() ) {
                EntityField testedField = ( EntityField ) collectionIterator.next();
                String fieldName = ( String ) (testedField).get( "name" );
                //TODO: need to change the logic here such that default properties does a check against each every field.
                //it is very likely that the below does O(n) comparisons anyways so doing it for each value in a loop
                //would be the right equivalent here. Even if it isn't very efficient.
                //maybe if it was a hashmap, and if the hashmap didn't contain the value then we remove the field.
                //but now for each hash value we have to do a check to see it doesn't start with a certain value.
                //making this an O(n) operation anyways.

                //so for each value in the finalDefaultProperties check to see if it matches the current property
                //then if it doesn't do a split of the properties and see if any of them match for the split
                //if they do match then allow in, otherwise don't allow.

                if ( !finalDefaultProperties.contains( fieldName ) ) {
                    mapFields.remove( testedField );
                }
            }
        }
        return map;
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
        if(crs.isEmpty()) {
            return Observable.empty();
        }

        UUID timeUUID = UUIDUtils.isTimeBased(entityId.getUuid()) ? entityId.getUuid() : UUIDUtils.newTimeUUID();
        //not actually sure about the timestamp but ah well. works.
        SearchEdge searchEdge = createSearchEdgeFromSource( new SimpleEdge( applicationScope.getApplication(),
            CpNamingUtils.getEdgeTypeFromCollectionName( InflectionUtils.pluralize( entityId.getType() ) ), entityId,
            timeUUID.timestamp() ) );


        final Observable<IndexOperationMessage>  batches = Observable.from( crs )
                //collect results into a single batch
                .collect( () -> ei.createBatch(), ( batch, candidateResult ) -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Deindexing on edge {} for entity {} added to batch", searchEdge, entityId);
                    }
                    batch.deindex( candidateResult );
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
