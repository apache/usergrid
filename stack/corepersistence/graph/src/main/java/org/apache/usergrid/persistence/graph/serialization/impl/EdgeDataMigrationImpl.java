/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.graph.serialization.impl;

import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.core.scope.ApplicationEntityGroup;
import org.apache.usergrid.persistence.core.scope.EntityIdScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.serialization.EdgeMigrationStrategy;
import org.apache.usergrid.persistence.graph.serialization.EdgesObservable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Encapsulates data mi
 */

public class EdgeDataMigrationImpl implements DataMigration {

    private static final Logger logger = LoggerFactory.getLogger(EdgeDataMigrationImpl.class);

    private final Keyspace keyspace;
    private final GraphManagerFactory graphManagerFactory;
    private final EdgesObservable edgesFromSourceObservable;
    private final EdgeMigrationStrategy edgeMigrationStrategy;

    @Inject
    public EdgeDataMigrationImpl(final Keyspace keyspace,
                                 final GraphManagerFactory graphManagerFactory,
                                 final EdgesObservable edgesFromSourceObservable,
                                 final EdgeMigrationStrategy edgeMigrationStrategy
    ) {

        this.keyspace = keyspace;
        this.graphManagerFactory = graphManagerFactory;
        this.edgesFromSourceObservable = edgesFromSourceObservable;
        this.edgeMigrationStrategy = edgeMigrationStrategy;
    }
    @Override
    public Observable<Long> migrate(final ApplicationEntityGroup applicationEntityGroup,
                                    final DataMigration.ProgressObserver observer) {
        final GraphManager gm = graphManagerFactory.createEdgeManager(applicationEntityGroup.applicationScope);
        final Observable<Edge> edgesFromSource = edgesFromSourceObservable.edgesFromSource(gm, applicationEntityGroup.applicationScope.getApplication());

        final AtomicLong counter = new AtomicLong();
        rx.Observable o =
            Observable
                .from(applicationEntityGroup.entityIds)

                .flatMap(new Func1<EntityIdScope, Observable<List<Edge>>>() {
                    //for each id in the group, get it's edges
                    @Override
                    public Observable<List<Edge>> call(final EntityIdScope idScope) {
                        logger.info("Migrating edges from node {} in scope {}", idScope.getId(),
                            applicationEntityGroup.applicationScope);


                        //get each edge from this node as a source
                        return edgesFromSource

                            //for each edge, re-index it in v2  every 1000 edges or less
                            .buffer(1000)
                            .doOnNext(new Action1<List<Edge>>() {
                                @Override
                                public void call(List<Edge> edges) {
                                    final MutationBatch batch =
                                        keyspace.prepareMutationBatch();

                                    for (Edge edge : edges) {
                                        logger.info("Migrating meta for edge {}", edge);
                                        final MutationBatch edgeBatch = edgeMigrationStrategy.getMigration().to()
                                            .writeEdge(
                                                applicationEntityGroup
                                                    .applicationScope,
                                                edge);
                                        batch.mergeShallow(edgeBatch);
                                    }

                                    try {
                                        batch.execute();
                                    } catch (ConnectionException e) {
                                        throw new RuntimeException(
                                            "Unable to perform migration", e);
                                    }

                                    //update the observer so the admin can see it
                                    final long newCount =
                                        counter.addAndGet(edges.size());

                                    observer.update(getVersion(), String.format(
                                        "Currently running.  Rewritten %d edge types",
                                        newCount));
                                }
                            });
                    }


                })
                .map(new Func1<List<Edge>, Long>() {
                    @Override
                    public Long call(List<Edge> edges) {
                        return counter.get();
                    }
                });
        return o;
    }

    @Override
    public int getVersion() {
        return edgeMigrationStrategy.getVersion();
    }
}
