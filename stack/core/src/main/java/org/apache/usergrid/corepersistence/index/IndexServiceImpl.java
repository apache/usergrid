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


import com.codahale.metrics.Timer;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.metrics.ObservableTimer;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.*;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.persistence.graph.serialization.EdgesObservable;
import org.apache.usergrid.persistence.index.*;
import org.apache.usergrid.persistence.index.impl.IndexOperationMessage;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.utils.InflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.*;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.*;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;


/**
 * Implementation of the indexing service
 */
@Singleton
public class IndexServiceImpl implements IndexService {


    private static final Logger logger = LoggerFactory.getLogger( IndexServiceImpl.class );

    private final GraphManagerFactory graphManagerFactory;
    private final EntityIndexFactory entityIndexFactory;
    private final MapManagerFactory mapManagerFactory;
    private final CollectionSettingsFactory collectionSettingsFactory;
    private final EdgesObservable edgesObservable;
    private final IndexFig indexFig;
    private final IndexLocationStrategyFactory indexLocationStrategyFactory;
    private final Timer indexTimer;
    private final Timer addTimer;


    @Inject
    public IndexServiceImpl( final GraphManagerFactory graphManagerFactory, final EntityIndexFactory entityIndexFactory,
                             final MapManagerFactory mapManagerFactory,
                             final CollectionSettingsFactory collectionSettingsFactory,
                             final EdgesObservable edgesObservable, final IndexFig indexFig,
                             final IndexLocationStrategyFactory indexLocationStrategyFactory,
                             final MetricsFactory metricsFactory ) {
        this.graphManagerFactory = graphManagerFactory;
        this.entityIndexFactory = entityIndexFactory;
        this.mapManagerFactory = mapManagerFactory;
        this.edgesObservable = edgesObservable;
        this.indexFig = indexFig;
        this.indexLocationStrategyFactory = indexLocationStrategyFactory;
        this.collectionSettingsFactory = collectionSettingsFactory;
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
        final Observable<Edge> edgesToTarget = edgesObservable.edgesToTarget( gm, entityId, true);

        //we may have to index  we're indexing from source->target here
        final Observable<IndexEdge> sourceEdgesToIndex = edgesToTarget.map( edge -> generateScopeFromSource( edge ) );

        //do our observable for batching
        //try to send a whole batch if we can

        final Observable<IndexOperationMessage>  batches =  sourceEdgesToIndex
            .buffer(indexFig.getIndexBatchSize() )

            //map into batches based on our buffer size
            .flatMap( buffer -> Observable.from( buffer )
                //collect results into a single batch
                .collect( () -> ei.createBatch(), ( batch, indexEdge ) -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("adding edge {} to batch for entity {}", indexEdge, entity);
                    }

                    final Optional<Set<String>> fieldsToIndex = getFilteredStringObjectMap( indexEdge );

                    batch.index( indexEdge, entity ,fieldsToIndex);
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

            Optional<Set<String>> fieldsToIndex = getFilteredStringObjectMap( indexEdge );

            batch.index( indexEdge, entity ,fieldsToIndex);


            return batch.build();
        } );

        return ObservableTimer.time( batches, addTimer  );

    }

    /**
     * This method takes in an entity and flattens it out then begins the process to filter out
     * properties that we do not want to index. Once flatted we parse through each property
     * and verify that we want it to be indexed on. The set of default properties that will always be indexed are as follows.
     * UUID - TYPE - MODIFIED - CREATED. Depending on the schema this may change. For instance, users will always require
     * NAME, but the above four will always be taken in.

     * @param indexEdge
     * @return This returns a filtered map that contains the flatted properties of the entity. If there isn't a schema
     * associated with the collection then return null ( and index the entity in its entirety )
     */
    private Optional<Set<String>> getFilteredStringObjectMap( final IndexEdge indexEdge ) {

        Id owner = new SimpleId( indexEdge.getNodeId().getUuid(), TYPE_APPLICATION );

        Set<String> defaultProperties;

        String collectionName = CpNamingUtils.getCollectionNameFromEdgeName( indexEdge.getEdgeName() );

        CollectionSettings collectionSettings =
            collectionSettingsFactory.getInstance( new CollectionSettingsScopeImpl( owner, collectionName) );

        Optional<Map<String, Object>> collectionIndexingSchema =
            collectionSettings.getCollectionSettings( collectionName );

        //If we do have a schema then parse it and add it to a list of properties we want to keep.Otherwise return.
        if ( collectionIndexingSchema.isPresent()) {

            Map jsonMapData = collectionIndexingSchema.get();
            Schema schema = Schema.getDefaultSchema();
            defaultProperties = schema.getRequiredProperties( collectionName );

            Object fields = jsonMapData.get("fields");

            // if "fields" field doesn't exist, should treat like fields=all
            if ( fields == null ) {
                return Optional.absent();
            }

            if ( fields != null && fields instanceof String && "all".equalsIgnoreCase(fields.toString())) {
                return Optional.absent();
            }

            if ( fields != null && fields instanceof List ) {
                defaultProperties.addAll( (List) fields );
            }

        } else {
            return Optional.absent();
        }

        return Optional.of(defaultProperties);
    }

    // DO NOT USE THIS AS THE QUERY TO ES CAN CAUSE EXTREME LOAD
    // TODO REMOVE THIS AND UPDATE THE TESTS TO NOT USE THIS METHOD
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

    @Override
    public Observable<IndexOperationMessage> deIndexEdge(final ApplicationScope applicationScope, final Edge edge,
                                                         final Id entityId, final UUID entityVersion){

        if (logger.isTraceEnabled()) {
            logger.trace("deIndexEdge edge={} entityId={} entityVersion={}", edge.toString(), entityId.toString(), entityVersion.toString());
        }
        final EntityIndex ei = entityIndexFactory.createEntityIndex(indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope));
        final EntityIndexBatch entityBatch = ei.createBatch();
        entityBatch.deindex(generateScopeFromSource( edge ), entityId, entityVersion);
        return Observable.just(entityBatch.build());

    }


    @Override
    public Observable<IndexOperationMessage> deIndexOldVersions(final ApplicationScope applicationScope,
                                                                final Id entityId,
                                                                final List<UUID> versions) {

        final EntityIndex ei = entityIndexFactory.
            createEntityIndex(indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope) );

        // use LONG.MAX_VALUE in search edge because this value is not used elsewhere in lower code for de-indexing
        // previously .timsetamp() was used on entityId, but some entities do not have type-1 UUIDS ( legacy data)
        final SearchEdge searchEdgeFromSource = createSearchEdgeFromSource( new SimpleEdge( applicationScope.getApplication(),
            CpNamingUtils.getEdgeTypeFromCollectionName( InflectionUtils.pluralize( entityId.getType() ) ), entityId,
            Long.MAX_VALUE ) );


        final EntityIndexBatch batch = ei.createBatch();

        versions.forEach( version -> {

            batch.deindex(searchEdgeFromSource, entityId, version);

        });

        return Observable.just(batch.build());

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
