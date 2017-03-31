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
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.serialization.impl.EntitySetImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.persistence.model.entity.Entity;

import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.utils.InflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.schedulers.Schedulers;


import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Singleton
public class ExportServiceImpl implements ExportService {

    private static final Logger logger = LoggerFactory.getLogger( ExportServiceImpl.class );

    private final AllEntityIdsObservable allEntityIdsObservable;
    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final ManagerCache managerCache;
    private final ObjectJsonSerializer jsonSerializer = ObjectJsonSerializer.INSTANCE;
    private final int exportVersion = 1;
    private final String keyTotalEntityCount = "__totalEntityCount__";



    @Inject
    public ExportServiceImpl(final AllEntityIdsObservable allEntityIdsObservable,
                             final ManagerCache managerCache,
                             final EntityCollectionManagerFactory entityCollectionManagerFactory) {
        this.allEntityIdsObservable = allEntityIdsObservable;
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.managerCache = managerCache;
    }


    @Override
    public void export(final ExportRequestBuilder exportRequestBuilder, OutputStream stream) throws RuntimeException {

        final ZipOutputStream zipOutputStream = new ZipOutputStream(stream);
        zipOutputStream.setLevel(9); // best compression to reduce the amount of data to stream over the wire

        final ApplicationScope appScope = exportRequestBuilder.getApplicationScope().get();
        final Observable<ApplicationScope> applicationScopes = Observable.just(appScope);

        final EntityCollectionManager ecm = entityCollectionManagerFactory.createCollectionManager(appScope);

        GraphManager gm = managerCache.getGraphManager( appScope );

        final AtomicInteger entityFileCount = new AtomicInteger(); // entities are batched into files
        final AtomicInteger connectionCount = new AtomicInteger();
        final Map<String, AtomicInteger> collectionStats = new HashMap<>();
        collectionStats.put(keyTotalEntityCount, new AtomicInteger());
        final Map<String, Object> infoMap = new HashMap<>();
        infoMap.put("application", appScope.getApplication().getUuid().toString());
        infoMap.put("exportVersion", exportVersion);
        infoMap.put("exportStarted", System.currentTimeMillis());

        logger.info("Starting export of application: {}", appScope.getApplication().getUuid().toString());

        allEntityIdsObservable.getEdgesToEntities( applicationScopes, Optional.absent(), Optional.absent() )
            .buffer(500)
            .map( edgeScopes -> {

                List<Id> entityIds = new ArrayList<>();
                edgeScopes.forEach( edgeScope -> {
                    if (edgeScope.getEdge().getTargetNode() != null) {
                        logger.debug("adding entity to list: {}", edgeScope.getEdge().getTargetNode());
                        entityIds.add(edgeScope.getEdge().getTargetNode());
                    }
                });

                return entityIds;

            })
            .flatMap( entityIds -> {

                logger.debug("entityIds: {}", entityIds);

                // batch load the entities
                EntitySet entitySet = ecm.load(entityIds).toBlocking().lastOrDefault(new EntitySetImpl(0));

                final String filenameWithPath = "entities/entities." + entityFileCount.get() + ".json";

                try {

                    logger.debug("adding zip entry: {}", filenameWithPath);
                    zipOutputStream.putNextEntry(new ZipEntry(filenameWithPath));

                    entitySet.getEntities().forEach(mvccEntity -> {

                        if (mvccEntity.getEntity().isPresent()) {
                            Map entityMap = CpEntityMapUtils.toMap(mvccEntity.getEntity().get());

                            try {

                                collectionStats.putIfAbsent(mvccEntity.getId().getType(), new AtomicInteger());
                                collectionStats.get(mvccEntity.getId().getType()).incrementAndGet();
                                collectionStats.get(keyTotalEntityCount).incrementAndGet();

                                logger.debug("writing and flushing entity {} to zip stream for file: {}", mvccEntity.getId().getUuid().toString(), filenameWithPath);
                                zipOutputStream.write(jsonSerializer.toString(entityMap).getBytes());
                                zipOutputStream.write("\n".getBytes());
                                zipOutputStream.flush(); // entities can be large, flush after each


                            } catch (IOException e) {
                                logger.warn("unable to write entry in zip stream for entityId: {}", mvccEntity.getId());
                                throw new RuntimeException("Unable to export data. Error writing to stream.");

                            }
                        } else {
                            logger.warn("entityId {} did not have corresponding entity, not writing", mvccEntity.getId());
                        }
                    });

                    zipOutputStream.closeEntry();
                    entityFileCount.incrementAndGet();

                }catch (IOException e){
                    throw new RuntimeException("Unable to export data. Error writing to stream.");
                }

                return Observable.from(entitySet.getEntities());

            })
            .doOnNext( mvccEntity -> {

                    gm.getEdgeTypesFromSource(CpNamingUtils.createConnectionTypeSearch(mvccEntity.getId()))
                        .flatMap(emittedEdgeType -> {

                            logger.debug("loading edges of type {} from node {}", emittedEdgeType, mvccEntity.getId());
                            return gm.loadEdgesFromSource(new SimpleSearchByEdgeType(mvccEntity.getId(),
                                emittedEdgeType, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING, Optional.absent()));

                        })
                        .doOnNext(markedEdge -> {

                            if (!markedEdge.isDeleted() && !markedEdge.isTargetNodeDeleted() && markedEdge.getTargetNode() != null ) {

                                // doing the load to just again make sure bad connections are not exported
                                Entity entity = ecm.load(markedEdge.getTargetNode()).toBlocking().lastOrDefault(null);

                                if (entity != null) {

                                    try {
                                        // since a single stream is being written, and connecitons are loaded per entity,
                                        // it cannot easily be batched eventlyinto files so write them separately
                                        final String filenameWithPath = "connections/" +
                                            markedEdge.getSourceNode().getUuid().toString()+"_" +
                                            CpNamingUtils.getConnectionNameFromEdgeName(markedEdge.getType()) + "_" +
                                            markedEdge.getTargetNode().getUuid().toString() + ".json";

                                        logger.debug("adding zip entry: {}", filenameWithPath);
                                        zipOutputStream.putNextEntry(new ZipEntry(filenameWithPath));


                                        final Map<String,String> connectionMap = new HashMap<String,String>(1){{
                                            put("sourceNodeUUID", markedEdge.getSourceNode().getUuid().toString() );
                                            put("relationship", CpNamingUtils.getConnectionNameFromEdgeName(markedEdge.getType()) );
                                            put("targetNodeUUID", markedEdge.getTargetNode().getUuid().toString());
                                        }};

                                        logger.debug("writing and flushing connection to zip stream: {}", jsonSerializer.toString(connectionMap).getBytes());
                                        zipOutputStream.write(jsonSerializer.toString(connectionMap).getBytes());

                                        zipOutputStream.closeEntry();
                                        zipOutputStream.flush();

                                        connectionCount.incrementAndGet();


                                    } catch (IOException e) {
                                        logger.warn("Unable to create entry in zip export for edge {}", markedEdge.toString());
                                        throw new RuntimeException("Unable to export data. Error writing to stream.");
                                    }
                                } else {
                                    logger.warn("Exported connection has a missing target node, not creating connection in export. Edge: {}", markedEdge);
                                }
                            }

                        }).toBlocking().lastOrDefault(null);
            })
            .doOnCompleted(() -> {

                infoMap.put("exportFinished", System.currentTimeMillis());


                try {

                    zipOutputStream.putNextEntry(new ZipEntry("metadata.json"));
                    zipOutputStream.write(jsonSerializer.toString(infoMap).getBytes());
                    zipOutputStream.closeEntry();

                    zipOutputStream.putNextEntry(new ZipEntry("stats.json"));
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("totalEntities", collectionStats.get(keyTotalEntityCount).get());
                    stats.put("totalConnections", connectionCount.get());
                    collectionStats.remove(keyTotalEntityCount);
                    stats.put("collectionCounts", new HashMap<String, Integer>(collectionStats.size()){{
                        collectionStats.forEach( (collection,count) -> {
                            put(InflectionUtils.pluralize(collection),count.get());
                        });
                    }});
                    zipOutputStream.write(jsonSerializer.toString(stats).getBytes());
                    zipOutputStream.closeEntry();

                    logger.debug("closing zip stream");
                    zipOutputStream.close();

                    logger.info("Finished export of application: {}", appScope.getApplication().getUuid().toString());


                } catch (IOException e) {
                    throw new RuntimeException("Unable to export data due to inability to close zip stream.");
                }

            })
            .subscribeOn( Schedulers.io() ).toBlocking().lastOrDefault(null);
    }


    @Override
    public ExportRequestBuilder getBuilder() {
        return new ExportRequestBuilderImpl();
    }

}


