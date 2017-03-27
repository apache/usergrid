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

package org.apache.usergrid.corepersistence.export;


import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.corepersistence.index.*;
import org.apache.usergrid.corepersistence.rx.impl.AllEntityIdsObservable;
import org.apache.usergrid.corepersistence.util.CpEntityMapUtils;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.corepersistence.util.ObjectJsonSerializer;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.persistence.model.entity.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.schedulers.Schedulers;


import java.io.*;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Singleton
public class ExportServiceImpl implements ExportService {

    private static final Logger logger = LoggerFactory.getLogger( ReIndexServiceImpl.class );

    private static final MapScope RESUME_MAP_SCOPE =
        new MapScopeImpl( CpNamingUtils.getManagementApplicationId(), "export-status" );


    private static final String MAP_COUNT_KEY = "count";
    private static final String MAP_STATUS_KEY = "status";
    private static final String MAP_UPDATED_KEY = "lastUpdated";


    private final AllEntityIdsObservable allEntityIdsObservable;
    private final MapManager mapManager;
    private final MapManagerFactory mapManagerFactory;
    private final CollectionSettingsFactory collectionSettingsFactory;
    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final ManagerCache managerCache;

    ObjectJsonSerializer jsonSerializer = ObjectJsonSerializer.INSTANCE;



    @Inject
    public ExportServiceImpl(final AllEntityIdsObservable allEntityIdsObservable,
                             final ManagerCache managerCache,
                               final MapManagerFactory mapManagerFactory,
                             final CollectionSettingsFactory collectionSettingsFactory,
                             final EntityCollectionManagerFactory entityCollectionManagerFactory) {
        this.allEntityIdsObservable = allEntityIdsObservable;
        this.collectionSettingsFactory = collectionSettingsFactory;
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.mapManagerFactory = mapManagerFactory;
        this.mapManager = mapManagerFactory.createMapManager( RESUME_MAP_SCOPE );
        this.managerCache = managerCache;
    }


    @Override
    public void export(final ExportRequestBuilder exportRequestBuilder, OutputStream stream) throws IOException {

        final ZipOutputStream zipOutputStream = new ZipOutputStream(stream);

        //final AtomicInteger count = new AtomicInteger();
        final ApplicationScope appScope = exportRequestBuilder.getApplicationScope().get();
        final Observable<ApplicationScope> applicationScopes = Observable.just(appScope);


        //final String jobId = StringUtils.sanitizeUUID( UUIDGenerator.newTimeUUID() );
        final EntityCollectionManager ecm = entityCollectionManagerFactory.createCollectionManager(appScope);

        final String rootPath = appScope.getApplication().getUuid().toString();

        GraphManager gm = managerCache.getGraphManager( appScope );

        allEntityIdsObservable.getEdgesToEntities( applicationScopes, Optional.absent(), Optional.absent() )
            .doOnNext( edgeScope -> {

                try {

                    // load the entity and convert to a normal map
                    Entity entity = ecm.load(edgeScope.getEdge().getTargetNode()).toBlocking().lastOrDefault(null);
                    Map entityMap = CpEntityMapUtils.toMap(entity);

                    if (entity != null) {
                        final String filenameWithPath = rootPath + "/" +
                            edgeScope.getEdge().getSourceNode().getUuid().toString() + "_" +
                            edgeScope.getEdge().getType() + "_" +
                            edgeScope.getEdge().getTargetNode().getUuid().toString() + ".json";

                        logger.debug("adding zip entry: {}", filenameWithPath);
                        zipOutputStream.putNextEntry(new ZipEntry(filenameWithPath));


                        logger.debug("writing and flushing entity to zip stream: {}", jsonSerializer.toString(entityMap));
                        zipOutputStream.write(jsonSerializer.toString(entityMap).getBytes());
                        zipOutputStream.closeEntry();
                        zipOutputStream.flush();

                    } else {
                        logger.warn("{}  did not have corresponding entity, not writing", edgeScope.toString());
                    }

                } catch (IOException e) {
                    logger.warn("Unable to create entry in zip export for edge {}", edgeScope);
                }

                //writeStateMeta( jobId, Status.INPROGRESS, count.addAndGet(1), System.currentTimeMillis() );
            })
            .flatMap( edgeScope -> {

                // find all connection types for the each entity emitted from the app
                return gm.getEdgeTypesFromSource(CpNamingUtils.createConnectionTypeSearch(edgeScope.getEdge().getTargetNode()))
                    .flatMap(emittedEdgeType -> {

                        logger.debug("loading edges of type {} from node {}", emittedEdgeType, edgeScope.getEdge().getTargetNode());
                        return gm.loadEdgesFromSource(new SimpleSearchByEdgeType( edgeScope.getEdge().getTargetNode(),
                            emittedEdgeType, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING, Optional.absent() ));

                    }).doOnNext( markedEdge -> {

                        if (!markedEdge.isDeleted()){

                            // todo, probably don't need to load the target node itself since it would be loaded in normal collection walking
                            Entity entity = ecm.load(markedEdge.getTargetNode()).toBlocking().lastOrDefault(null);

                            Map entityMap = CpEntityMapUtils.toMap(entity);

                            try {
                                final String filenameWithPath = rootPath + "/" +
                                    markedEdge.getSourceNode().getUuid().toString() + "_" +
                                    markedEdge.getType() + "_" +
                                    markedEdge.getTargetNode().getUuid().toString() + ".json";

                                logger.debug("adding zip entry: {}", filenameWithPath);
                                zipOutputStream.putNextEntry(new ZipEntry(filenameWithPath));

                                logger.debug("writing and flushing entity to zip stream: {}", jsonSerializer.toString(entityMap));
                                zipOutputStream.write(jsonSerializer.toString(entityMap).getBytes());
                                zipOutputStream.closeEntry();
                                zipOutputStream.flush();

                            } catch (IOException e) {
                                logger.warn("Unable to create entry in zip export for edge {}", markedEdge.toString());
                            }
                        }

                    });

            })
            .doOnCompleted(() -> {

                //writeStateMeta( jobId, Status.COMPLETE, count.get(), System.currentTimeMillis() );
                try {
                    logger.debug("closing zip stream");
                    zipOutputStream.close();

                } catch (IOException e) {
                    logger.error( "unable to close zip stream");
                }

            })
            .subscribeOn( Schedulers.io() ).toBlocking().lastOrDefault(null);
    }


    @Override
    public ExportRequestBuilder getBuilder() {
        return new ExportRequestBuilderImpl();
    }




    /**
     * Write our state meta data into cassandra so everyone can see it
     * @param jobId
     * @param status
     * @param processedCount
     * @param lastUpdated
     */
    private void writeStateMeta( final String jobId, final Status status, final long processedCount,
                                 final long lastUpdated ) {

        if(logger.isDebugEnabled()) {
            logger.debug( "Flushing state for jobId {}, status {}, processedCount {}, lastUpdated {}",
                    jobId, status, processedCount, lastUpdated);
        }

        mapManager.putString( jobId + MAP_STATUS_KEY, status.name() );
        mapManager.putLong( jobId + MAP_COUNT_KEY, processedCount );
        mapManager.putLong( jobId + MAP_UPDATED_KEY, lastUpdated );
    }


}


