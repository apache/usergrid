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


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.antlr.misc.Graph;
import org.apache.usergrid.corepersistence.index.*;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.utils.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger( EventBuilderImpl.class );

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
    public Observable<IndexOperationMessage> buildNewEdge( final ApplicationScope applicationScope, final Entity entity,
                                                           final Edge newEdge ) {

        if (logger.isDebugEnabled()) {
            logger.debug("Indexing  in app scope {} with entity {} and new edge {}",
                applicationScope, entity, newEdge);
        }

        return indexService.indexEdge( applicationScope, entity, newEdge );
    }


    @Override
    public IndexOperationMessage buildDeleteEdge( final ApplicationScope applicationScope, final Edge
        edge ) {
        if (logger.isDebugEnabled()) {
            logger.debug("Deleting in app scope {} with edge {}", applicationScope, edge);
        }

        final GraphManager gm = graphManagerFactory.createEdgeManager( applicationScope );
        final EntityCollectionManager ecm = entityCollectionManagerFactory.createCollectionManager( applicationScope );

        final IndexOperationMessage combined = new IndexOperationMessage();

        gm.deleteEdge( edge )
            .doOnNext( deletedEdge -> {

                logger.debug("Processing deleted edge for de-indexing {}", deletedEdge);

                // get ALL versions of the target node as any connection from this source node needs to be removed
                ecm.getVersionsFromMaxToMin(deletedEdge.getTargetNode(), UUIDUtils.newTimeUUID())
                    .doOnNext(mvccLogEntry -> {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Adding edge {} mvccLogEntry {} to de-index batch", deletedEdge.getTargetNode(), mvccLogEntry);
                        }
                        combined.ingest(
                            indexService
                                .deIndexEdge(applicationScope, deletedEdge, mvccLogEntry.getEntityId(), mvccLogEntry.getVersion())
                                .toBlocking().lastOrDefault(new IndexOperationMessage()));

                    }).toBlocking().lastOrDefault(null);

            }).toBlocking().lastOrDefault(null);

        return combined;
    }


    //Does the queue entityDelete mark the entity then immediately does to the deleteEntityIndex. seems like
    //it'll need to be pushed up higher so we can do the marking that isn't async or does it not matter?

    @Override
    public IndexOperationMessage buildEntityDelete(final ApplicationScope applicationScope, final Id entityId) {
        return buildEntityDelete(applicationScope, entityId, false, Long.MAX_VALUE);
    }

    @Override
    public IndexOperationMessage buildEntityDelete(final ApplicationScope applicationScope, final Id entityId,
                                                   final boolean isCollectionDelete, final long updatedBefore) {

        if (logger.isDebugEnabled()) {
            logger.debug("Deleting entity id (marked versions) from index in app scope {} with entityId {}, isCollectionDelete {}, updatedBefore={}",
                applicationScope, entityId, isCollectionDelete, updatedBefore);
        }

        final EntityCollectionManager ecm = entityCollectionManagerFactory.createCollectionManager(applicationScope);
        final GraphManager gm = graphManagerFactory.createEdgeManager(applicationScope);

        boolean deleteEntity = ecm.load(entityId).
            map(entity -> {
                final Field<Long> modified = entity.getField( Schema.PROPERTY_MODIFIED );

                boolean willDelete = false;
                if ( modified == null ) {
                    // We don't have a modified field, so we can't check, so delete it
                    willDelete = true;
                } else if (modified.getValue() <= updatedBefore) {
                    willDelete = true;
                }

                if (isCollectionDelete && willDelete) {
                    // need to mark for deletion
                    ecm.mark(entityId, null)
                        .mergeWith(gm.markNode(entityId, CpNamingUtils.createGraphOperationTimestamp()))
                        .toBlocking().last();
                }

                return willDelete;
            }).toBlocking().firstOrDefault(true);

        if (!deleteEntity) {
            return new IndexOperationMessage();
        }


        MvccLogEntry mostRecentToDelete =
            ecm.getVersionsFromMaxToMin( entityId, UUIDUtils.newTimeUUID() )
                .toBlocking()
                .firstOrDefault( null, mvccLogEntry -> mvccLogEntry.getState() == MvccLogEntry.State.DELETED );

//        logger.info("mostRecent stage={} entityId={} version={} state={}",
//            mostRecentToDelete.getStage().name(), mostRecentToDelete.getEntityId(),
//            mostRecentToDelete.getVersion().toString(), mostRecentToDelete.getState().name());

        if (mostRecentToDelete == null) {
            logger.info("No entity versions to delete for id {}", entityId.toString());
        }
        // if nothing is marked, then abort
        if(mostRecentToDelete == null){
            return new IndexOperationMessage();
        }

        final List<MvccLogEntry> logEntries = new ArrayList<>();
        Observable<MvccLogEntry> mvccLogEntryListObservable =
            ecm.getVersionsFromMaxToMin( entityId, UUIDUtils.newTimeUUID() );
        mvccLogEntryListObservable
            .filter( mvccLogEntry-> mvccLogEntry.getVersion().timestamp() <= mostRecentToDelete.getVersion().timestamp() )
            .buffer( serializationFig.getBufferSize() )
            .doOnNext( buffer -> ecm.delete( buffer ) )
            .doOnNext(mvccLogEntries -> {
                logEntries.addAll(mvccLogEntries);
            }).toBlocking().lastOrDefault(null);

        //logger.info("logEntries size={}", logEntries.size());

        IndexOperationMessage combined = new IndexOperationMessage();

        // do the edge deletes and build up de-index messages for each edge deleted
        // assume we have "server1" and "region1" nodes in the graph with the following relationships (edges/connections):
        //
        // region1  -- zzzconnzzz|has -->  server1
        // server1  -- zzzconnzzz|in  -->  region1
        //
        // there will always be a relationship from the appId to each entity based on the entity type (collection):
        //
        // application -- zzzcollzzz|servers --> server1
        // application -- zzzcollzzz|regions --> region1
        //
        // When deleting either "server1" or "region1" entity, the connections should get deleted and de-indexed along
        // with the entry for the entity itself in the collection. The above example should have at minimum 3 things to
        // be de-indexed. There may be more as either "server1" or "region1" could have multiple versions.
        //
        // Further comments using the example of deleting "server1" from the above example.
        gm.compactNode(entityId).doOnNext(markedEdge -> {

            if (logger.isDebugEnabled()) {
                logger.debug("Processing deleted edge for de-indexing {}", markedEdge);
            }

            // if the edge was for a connection where the entity to be deleted is the source node, we need to load
            // the target node's versions so that all versions of connections to that entity can be de-indexed
            // server1  -- zzzconnzzz|in  -->  region1
            if(!markedEdge.getTargetNode().getType().equals(entityId.getType())){

                // get ALL versions of the target node as any connection from this source node needs to be removed
                ecm.getVersionsFromMaxToMin( markedEdge.getTargetNode(), UUIDUtils.newTimeUUID() )
                    .doOnNext(mvccLogEntry -> {
                        logger.debug("Adding edge {} mvccLogEntry {} to de-index batch", markedEdge, mvccLogEntry);
                        combined.ingest(
                            indexService
                                .deIndexEdge(applicationScope, markedEdge, mvccLogEntry.getEntityId(), mvccLogEntry.getVersion())
                                .toBlocking().lastOrDefault(new IndexOperationMessage()));

                    }).toBlocking().lastOrDefault(null);

            }else {

                // for each version of the entity being deleted, de-index the connections where the entity is the target
                // node ( application -- zzzcollzzz|servers --> server1 ) or (region1  -- zzzconnzzz|has -->  server1)
                logEntries.forEach(logEntry -> {
                    logger.debug("Adding edge {} mvccLogEntry {} to de-index batch", markedEdge, logEntry);
                    combined.ingest(
                        indexService
                            .deIndexEdge(applicationScope, markedEdge, logEntry.getEntityId(), logEntry.getVersion())
                            .toBlocking().lastOrDefault(new IndexOperationMessage()));
                });
            }

        }).toBlocking().lastOrDefault(null);

        return combined;
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


    @Override
    public Observable<IndexOperationMessage> deIndexOldVersions( final ApplicationScope applicationScope,
                                                                 final Id entityId, final UUID markedVersion ){

        if (logger.isDebugEnabled()) {
            logger.debug("Removing old versions of entity {} from index in app scope {}", entityId, applicationScope );
        }

        final EntityCollectionManager ecm = entityCollectionManagerFactory.createCollectionManager( applicationScope );


        return indexService.deIndexOldVersions( applicationScope, entityId,
            getVersionsOlderThanOrEqualToMarked(ecm, entityId, markedVersion));

    }


    private List<UUID> getVersionsOlderThanOrEqualToMarked(final EntityCollectionManager ecm,
                                                           final Id entityId, final UUID markedVersion ){

        final List<UUID> versions = new ArrayList<>();

        // only take last 100 versions to avoid eating memory. a tool can be built for massive cleanups for old usergrid
        // clusters that do not have this in-line cleanup
        ecm.getVersionsFromMaxToMin( entityId, markedVersion)
            .take(100)
            .forEach( mvccLogEntry -> {
                if ( mvccLogEntry.getVersion().timestamp() <= markedVersion.timestamp() ) {
                    versions.add(mvccLogEntry.getVersion());
                }

            });


        return versions;
    }

    private List<UUID> getAllVersions( final EntityCollectionManager ecm,
                                       final Id entityId ) {

        final List<UUID> versions = new ArrayList<>();

        ecm.getVersionsFromMaxToMin(entityId, UUIDUtils.newTimeUUID())
            .forEach( mvccLogEntry -> {
                versions.add(mvccLogEntry.getVersion());
            });

        return versions;
    }

}
