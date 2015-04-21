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


import java.util.Collection;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.serialization.EdgesObservable;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.impl.IndexOperationMessage;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.schema.CollectionInfo;
import org.apache.usergrid.utils.InflectionUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.observables.ConnectableObservable;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.generateScopeFromSource;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.generateScopeToTarget;
import static org.apache.usergrid.persistence.Schema.getDefaultSchema;


/**
 * Implementation of the indexing service
 */
@Singleton
public class IndexServiceImpl implements IndexService {

    private final GraphManagerFactory graphManagerFactory;
    private final EntityIndexFactory entityIndexFactory;
    private final EdgesObservable edgesObservable;
    private final IndexFig indexFig;


    @Inject
    public IndexServiceImpl( final GraphManagerFactory graphManagerFactory, final EntityIndexFactory entityIndexFactory,
                             final EdgesObservable edgesObservable, IndexFig indexFig ) {
        this.graphManagerFactory = graphManagerFactory;
        this.entityIndexFactory = entityIndexFactory;
        this.edgesObservable = edgesObservable;
        this.indexFig = indexFig;
    }


    @Override
    public Observable<Long> indexEntity( final ApplicationScope applicationScope, final Entity entity ) {


        //bootstrap the lower modules from their caches
        final GraphManager gm = graphManagerFactory.createEdgeManager( applicationScope );
        final ApplicationEntityIndex ei = entityIndexFactory.createApplicationEntityIndex( applicationScope );


        final Id entityId = entity.getId();


        //we always index in the target scope
        final Observable<Edge> edgesToTarget = edgesObservable.edgesToTarget( gm, entityId );

        //we may have to index  we're indexing from source->target here
        final Observable<IndexEdge> sourceEdgesToIndex = edgesToTarget.map( edge -> generateScopeFromSource( edge ) );


        //we might or might not need to index from target-> source


        final Observable<IndexEdge> targetSizes = getIndexEdgesToTarget( gm, entityId );


        //start the observable via publish
        final ConnectableObservable<IndexOperationMessage> observable =
            //try to send a whole batch if we can
            Observable.merge( sourceEdgesToIndex, targetSizes ).buffer( indexFig.getIndexBatchSize() )

                //map into batches based on our buffer size
                .flatMap( buffer -> Observable.from( buffer ).collect( () -> ei.createBatch(),
                    ( batch, indexEdge ) -> batch.index( indexEdge, entity ) )
                    //return the future from the batch execution
                    .flatMap( batch -> Observable.from( batch.execute() ) ) ).publish();



        return observable.countLong();
    }


    /**
     * Get index edgs to the target
     *
     * @param graphManager The graph manager
     * @param entityId The entitie's id
     */
    private Observable<IndexEdge> getIndexEdgesToTarget( final GraphManager graphManager, final Id entityId ) {

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
        return edgesObservable.getEdgesFromSource( graphManager, entityId, linkedCollection ).map( edge -> generateScopeToTarget( edge ) );
    }
}
