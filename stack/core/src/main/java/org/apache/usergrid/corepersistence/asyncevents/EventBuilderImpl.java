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

package org.apache.usergrid.corepersistence.asyncevents;


import java.util.List;

import org.apache.usergrid.utils.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.index.EntityIndexOperation;
import org.apache.usergrid.corepersistence.index.IndexService;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.index.impl.IndexOperationMessage;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;


/**
 * Service that executes event flows
 */
@Singleton
public class EventBuilderImpl implements EventBuilder {

    private static final Logger log = LoggerFactory.getLogger( EventBuilderImpl.class );

    private final IndexService indexService;
    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final GraphManagerFactory graphManagerFactory;
    private final SerializationFig serializationFig;


    @Inject
    public EventBuilderImpl( final IndexService indexService,
                             final EntityCollectionManagerFactory entityCollectionManagerFactory,
                             final GraphManagerFactory graphManagerFactory, final SerializationFig serializationFig ) {
        this.indexService = indexService;
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.graphManagerFactory = graphManagerFactory;
        this.serializationFig = serializationFig;
    }


    @Override
    public Observable<IndexOperationMessage> buildEntityIndexUpdate( final ApplicationScope applicationScope,
                                                                     final Entity entity ) {
        //process the entity immediately
        //only process the same version, otherwise ignore

        if (log.isDebugEnabled()) {
            log.debug("Indexing  in app scope {} entity {}", entity, applicationScope);
        }

        return indexService.indexEntity( applicationScope, entity );
    }


    @Override
    public Observable<IndexOperationMessage> buildNewEdge( final ApplicationScope applicationScope, final Entity entity,
                                                           final Edge newEdge ) {

        if (log.isDebugEnabled()) {
            log.debug("Indexing  in app scope {} with entity {} and new edge {}",
                new Object[]{entity, applicationScope, newEdge});
        }

        return indexService.indexEdge( applicationScope, entity, newEdge );
    }


    @Override
    public Observable<IndexOperationMessage> buildDeleteEdge( final ApplicationScope applicationScope, final Edge
        edge ) {
        if (log.isDebugEnabled()) {
            log.debug("Deleting in app scope {} with edge {} }", applicationScope, edge);
        }

        final Observable<IndexOperationMessage> edgeObservable =
            indexService.deleteIndexEdge( applicationScope, edge ).flatMap( batch -> {
                final GraphManager gm = graphManagerFactory.createEdgeManager( applicationScope );
                return gm.deleteEdge( edge ).map( deletedEdge -> batch );
            } );

        return edgeObservable;
    }


    //Does the queue entityDelete mark the entity then immediately does to the deleteEntityIndex. seems like
    //it'll need to be pushed up higher so we can do the marking that isn't async or does it not matter?

    @Override
    public EntityDeleteResults buildEntityDelete( final ApplicationScope applicationScope, final Id entityId ) {
        if (log.isDebugEnabled()) {
            log.debug("Deleting entity id from index in app scope {} with entityId {} }", applicationScope, entityId);
        }

        final EntityCollectionManager ecm = entityCollectionManagerFactory.createCollectionManager( applicationScope );
        final GraphManager gm = graphManagerFactory.createEdgeManager( applicationScope );

        //TODO USERGRID-1123: Implement so we don't iterate logs twice (latest DELETED version, then to get all DELETED)

        MvccLogEntry mostRecentlyMarked = ecm.getVersions( entityId ).toBlocking()
            .firstOrDefault( null, mvccLogEntry -> mvccLogEntry.getState() == MvccLogEntry.State.DELETED );

        // De-indexing and entity deletes don't check log entiries.  We must do that first. If no DELETED logs, then
        // return an empty observable as our no-op.
        Observable<IndexOperationMessage> deIndexObservable = Observable.empty();
        Observable<List<MvccLogEntry>> ecmDeleteObservable = Observable.empty();

        if(mostRecentlyMarked != null){
            deIndexObservable =
                indexService.deleteEntityIndexes( applicationScope, entityId, mostRecentlyMarked.getVersion() );

            ecmDeleteObservable =
                ecm.getVersions( entityId )
                    .filter( mvccLogEntry->
                        UUIDUtils.compare(mvccLogEntry.getVersion(), mostRecentlyMarked.getVersion()) <= 0)
                    .buffer( serializationFig.getBufferSize() )
                    .doOnNext( buffer -> ecm.delete( buffer ) );
        }

        // Graph compaction checks the versions inside compactNode, just build this up for the caller to subscribe to
        final Observable<Id> graphCompactObservable = gm.compactNode(entityId);

        return new EntityDeleteResults( deIndexObservable, ecmDeleteObservable, graphCompactObservable );
    }


    @Override
    public Observable<IndexOperationMessage> buildEntityIndex( final EntityIndexOperation entityIndexOperation ) {

        final ApplicationScope applicationScope = entityIndexOperation.getApplicationScope();

        final Id entityId = entityIndexOperation.getId();

        //load the entity
        return entityCollectionManagerFactory.createCollectionManager( applicationScope ).load( entityId ).filter(
            entity -> {
                final Field<Long> modified = entity.getField( Schema.PROPERTY_MODIFIED );

                /**
                 * We don't have a modified field, so we can't check, pass it through
                 */
                if ( modified == null ) {
                    return true;
                }

                //entityIndexOperation.getUpdatedSince will always be 0 except for reindexing the application
                //only re-index if it has been updated and been updated after our timestamp
                return modified.getValue() >= entityIndexOperation.getUpdatedSince();
            } )
            //perform indexing on the task scheduler and start it
            .flatMap( entity -> indexService.indexEntity( applicationScope, entity ) );
    }
}
