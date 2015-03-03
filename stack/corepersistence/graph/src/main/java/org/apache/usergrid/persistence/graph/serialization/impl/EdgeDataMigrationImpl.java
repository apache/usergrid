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
import org.apache.usergrid.persistence.core.migration.data.newimpls.MigrationRelationship;
import org.apache.usergrid.persistence.core.migration.data.newimpls.ProgressObserver;
import org.apache.usergrid.persistence.core.migration.data.newimpls.VersionedMigrationSet;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgesObservable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Encapsulates the migration of edge meta data
 */
public class EdgeDataMigrationImpl implements DataMigration2<ApplicationScope> {

    private static final Logger logger = LoggerFactory.getLogger(EdgeDataMigrationImpl.class);

    private final Keyspace keyspace;
    private final GraphManagerFactory graphManagerFactory;
    private final EdgesObservable edgesFromSourceObservable;
    private final VersionedMigrationSet<EdgeMetadataSerialization> allVersions;
    private final EdgeMetadataSerializationV2Impl edgeMetadataSerializationV2;

    @Inject
    public EdgeDataMigrationImpl( final Keyspace keyspace, final GraphManagerFactory graphManagerFactory,
                                  final EdgesObservable edgesFromSourceObservable,

                                  final VersionedMigrationSet<EdgeMetadataSerialization> allVersions,
                                  final EdgeMetadataSerializationV2Impl edgeMetadataSerializationV2 ) {

        this.keyspace = keyspace;
        this.graphManagerFactory = graphManagerFactory;
        this.edgesFromSourceObservable = edgesFromSourceObservable;
        this.allVersions = allVersions;
        this.edgeMetadataSerializationV2 = edgeMetadataSerializationV2;
    }




    @Override
       public int migrate( final int currentVersion, final MigrationDataProvider<ApplicationScope> migrationDataProvider,
                           final ProgressObserver observer ) {

        final AtomicLong counter = new AtomicLong();

        final MigrationRelationship<EdgeMetadataSerialization>
                migration = allVersions.getMigrationRelationship( currentVersion );

        final Observable<List<Edge>> observable = migrationDataProvider.getData().flatMap(new Func1<ApplicationScope, Observable<List<Edge>>>() {
                  @Override
                  public Observable<List<Edge>> call(final ApplicationScope applicationScope) {
                      final GraphManager gm = graphManagerFactory.createEdgeManager( applicationScope );
                      final Observable<Edge> edgesFromSource =
                              edgesFromSourceObservable.edgesFromSource( gm, applicationScope.getApplication() );
                      logger.info( "Migrating edges scope {}", applicationScope );

                      //get each edge from this node as a source
                      return edgesFromSource

                              //for each edge, re-index it in v2  every 1000 edges or less
                              .buffer( 1000 ).parallel( new Func1<Observable<List<Edge>>, Observable<List<Edge>>>() {
                                  @Override
                                  public Observable<List<Edge>> call( final Observable<List<Edge>> listObservable ) {
                                      return listObservable.doOnNext( new Action1<List<Edge>>() {
                                          @Override
                                          public void call( List<Edge> edges ) {
                                              final MutationBatch batch = keyspace.prepareMutationBatch();

                                              for ( Edge edge : edges ) {
                                                  logger.info( "Migrating meta for edge {}", edge );
                                                  final MutationBatch edgeBatch =
                                                          migration.to.writeEdge( applicationScope, edge );
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

                                              observer.update( migration.to.getImplementationVersion(),
                                                      String.format( "Currently running.  Rewritten %d edge types",
                                                              newCount ) );
                                          }
                                      } );
                                  }
                              } );
                  }});

        observable.longCount().toBlocking().last();

        return migration.to.getImplementationVersion();

    }




    @Override
    public boolean supports( final int currentVersion ) {
        return currentVersion < edgeMetadataSerializationV2.getImplementationVersion();
    }


    @Override
    public int getMaxVersion() {
        //we only support up to v2 ATM
        return edgeMetadataSerializationV2.getImplementationVersion();
    }
}
