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
import org.apache.usergrid.persistence.core.migration.data.newimpls.DataMigration2;
import org.apache.usergrid.persistence.core.migration.data.newimpls.MigrationDataProvider;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
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

public class EdgeDataMigrationImpl implements DataMigration2<ApplicationScope> {

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
    public void migrate( final MigrationDataProvider<ApplicationScope> migrationDataProvider,
                         final ProgressObserver observer ) {
        final AtomicLong counter = new AtomicLong();

               migrationDataProvider.getData().flatMap(new Func1<ApplicationScope, Observable<?>>() {
                  @Override
                  public Observable call(final ApplicationScope applicationScope) {
                      final GraphManager gm = graphManagerFactory.createEdgeManager(applicationScope);
                      final Observable<Edge> edgesFromSource = edgesFromSourceObservable.edgesFromSource(gm, applicationScope.getApplication());
                      logger.info("Migrating edges scope {}", applicationScope);

                      //get each edge from this node as a source
                      return edgesFromSource

                          //for each edge, re-index it in v2  every 1000 edges or less
                          .buffer( 1000 )
                          //do the writes of 1k in parallel
                          .parallel( new Func1<Observable<List<Edge>>, Observable>() {
                                  @Override
                                  public Observable call( final Observable<List<Edge>> listObservable ) {
                                      return listObservable.doOnNext( new Action1<List<Edge>>() {
                                                @Override
                                                public void call( List<Edge> edges ) {
                                                    final MutationBatch batch = keyspace.prepareMutationBatch();

                                                    for ( Edge edge : edges ) {
                                                        logger.info( "Migrating meta for edge {}", edge );
                                                        final MutationBatch edgeBatch =
                                                                edgeMigrationStrategy.getMigration().to()
                                                                                     .writeEdge( applicationScope, edge );
                                                        batch.mergeShallow( edgeBatch );
                                                    }

                                                    try {
                                                        batch.execute();
                                                    }
                                                    catch ( ConnectionException e ) {
                                                        throw new RuntimeException( "Unable to perform migration", e );
                                                    }

                                                    //update the observer so the admin can see it
                                                    final long newCount = counter.addAndGet( edges.size() );

                                                    observer.update( getVersion(),
                                                            String.format( "Currently running.  Rewritten %d edge types",
                                                                    newCount ) );
                                                }
                                            } );
                                  }
                              } );
                  }
              });

    }


    @Override
    public int getVersion() {
        return edgeMigrationStrategy.getVersion();
    }

}
