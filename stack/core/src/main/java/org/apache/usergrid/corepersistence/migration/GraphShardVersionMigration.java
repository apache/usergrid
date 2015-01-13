/*
 *
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
 *
 */

package org.apache.usergrid.corepersistence.migration;


import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.corepersistence.rx.AllEntitiesInSystemObservable;
import org.apache.usergrid.corepersistence.rx.EdgesFromSourceObservable;
import org.apache.usergrid.persistence.core.guice.CurrentImpl;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;


/**
 * Migration for migrating graph edges to the new Shards
 */
public class GraphShardVersionMigration implements DataMigration {


    private static final Logger logger = LoggerFactory.getLogger( GraphShardVersionMigration.class );


    private final EdgeMetadataSerialization v2Serialization;

    private final ManagerCache managerCache;
    private final Keyspace keyspace;


    @Inject
    public GraphShardVersionMigration( @CurrentImpl final EdgeMetadataSerialization v2Serialization,
                                       final ManagerCache managerCache, final Keyspace keyspace ) {
        this.v2Serialization = v2Serialization;
        this.managerCache = managerCache;
        this.keyspace = keyspace;
    }


    @Override
    public void migrate( final ProgressObserver observer ) throws Throwable {

        final AtomicLong counter = new AtomicLong();

        AllEntitiesInSystemObservable.getAllEntitiesInSystem( managerCache, 1000 ).flatMap(
                new Func1<AllEntitiesInSystemObservable.ApplicationEntityGroup, Observable<List<Edge>>>() {


                    @Override
                    public Observable<List<Edge>> call(
                            final AllEntitiesInSystemObservable.ApplicationEntityGroup applicationEntityGroup ) {

                        //emit a stream of all ids from this group
                        return Observable.from( applicationEntityGroup.entityIds )
                                         .flatMap( new Func1<Id, Observable<List<Edge>>>() {


                                             //for each id in the group, get it's edges
                                             @Override
                                             public Observable<List<Edge>> call( final Id id ) {
                                                 logger.info( "Migrating edges from node {} in scope {}", id,
                                                         applicationEntityGroup.applicationScope );

                                                 final GraphManager gm = managerCache
                                                         .getGraphManager( applicationEntityGroup.applicationScope );

                                                 //get each edge from this node as a source
                                                 return EdgesFromSourceObservable.edgesFromSource( gm, id )

                                                         //for each edge, re-index it in v2  every 1000 edges or less
                                                         .buffer( 1000 ).doOnNext( new Action1<List<Edge>>() {
                                                             @Override
                                                             public void call( final List<Edge> edges ) {

                                                                 final MutationBatch batch =
                                                                         keyspace.prepareMutationBatch();

                                                                 for ( final Edge edge : edges ) {
                                                                     logger.info( "Migrating meta for edge {}", edge );
                                                                     final MutationBatch edgeBatch = v2Serialization
                                                                             .writeEdge(
                                                                                     applicationEntityGroup
                                                                                             .applicationScope,
                                                                                     edge );
                                                                     batch.mergeShallow( edgeBatch );
                                                                 }

                                                                 try {
                                                                     batch.execute();
                                                                 }
                                                                 catch ( ConnectionException e ) {
                                                                     throw new RuntimeException(
                                                                             "Unable to perform migration", e );
                                                                 }

                                                                 //update the observer so the admin can see it
                                                                 final long newCount =
                                                                         counter.addAndGet( edges.size() );

                                                                 observer.update( getVersion(), String.format(
                                                                         "Currently running.  Rewritten %d edge types",
                                                                         newCount ) );
                                                             }
                                                         } );
                                             }
                                         } );
                    }
                } ).toBlocking().lastOrDefault( null );
        ;
    }


    @Override
    public int getVersion() {
        return Versions.VERSION_2;
    }
}
