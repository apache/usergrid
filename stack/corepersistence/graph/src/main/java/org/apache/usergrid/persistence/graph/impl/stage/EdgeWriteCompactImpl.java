/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */
package org.apache.usergrid.persistence.graph.impl.stage;


import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.guice.CommitLogEdgeSerialization;
import org.apache.usergrid.persistence.graph.guice.StorageEdgeSerialization;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.functions.Func1;


public class EdgeWriteCompactImpl implements EdgeWriteCompact {

    private static final Logger LOG = LoggerFactory.getLogger( EdgeWriteCompactImpl.class );

    private final EdgeSerialization commitLog;
       private final EdgeSerialization permanentStorage;
       private final Keyspace keyspace;
       private final GraphFig graphFig;


       @Inject
       public EdgeWriteCompactImpl( @CommitLogEdgeSerialization final EdgeSerialization commitLog,
                                 @StorageEdgeSerialization final EdgeSerialization permanentStorage,
                                 final Keyspace keyspace,
                                 final GraphFig graphFig ) {


           Preconditions.checkNotNull( commitLog, "commitLog is required" );
           Preconditions.checkNotNull( permanentStorage, "permanentStorage is required" );
           Preconditions.checkNotNull( keyspace, "keyspace is required" );
           Preconditions.checkNotNull( keyspace, "consistencyFig is required" );


           this.keyspace = keyspace;
           this.commitLog = commitLog;
           this.permanentStorage = permanentStorage;
           this.graphFig = graphFig;
       }

    @Override
    public Observable<Integer> compact( final OrganizationScope scope, final MarkedEdge edge, final UUID timestamp ) {
        final Edge writtenEdge = edge;

              final UUID writeVersion = edge.getVersion();

              return Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeVersions" ) {
                  @Override
                  protected Iterator<MarkedEdge> getIterator() {
                      //get our edge as it exists in the commit log
                      return commitLog.getEdgeVersions( scope,
                              new SimpleSearchByEdge( writtenEdge.getSourceNode(), writtenEdge.getType(),
                                      writtenEdge.getTargetNode(), writeVersion, null ) );
                  }
              } )
                              //buffer them, then execute mutations in batch
                      .buffer( graphFig.getScanPageSize() ).flatMap( new Func1<List<MarkedEdge>, Observable<MarkedEdge>>() {
                          @Override
                          public Observable<MarkedEdge> call( final List<MarkedEdge> markedEdges ) {

                              final MutationBatch storageWriteBatch = keyspace.prepareMutationBatch();
                              final MutationBatch commitlogCleanBatch = keyspace.prepareMutationBatch();


                              for ( MarkedEdge edge : markedEdges ) {

                                  LOG.debug( "Buffering edge {} to permanent storage and removing from commitlog", edge );

                                  //batch the write
                                  storageWriteBatch.mergeShallow( permanentStorage.writeEdge( scope, edge, timestamp ) );

                                  //batch the cleanup
                                  commitlogCleanBatch.mergeShallow( commitLog.deleteEdge( scope, edge, timestamp ) );
                              }


                              //execute the write to permanent storage
                              try {

                                  storageWriteBatch.execute();
                                  LOG.debug( "Storage write executed" );
                              }
                              catch ( ConnectionException e ) {
                                  throw new RuntimeException( "unable to execute mutation", e );
                              }

                              //since our batch has completed, we can now invoke the remove from the commit log

                              try {
                                  commitlogCleanBatch.execute();
                                  LOG.debug( "Commitlog write executed" );
                              }
                              catch ( ConnectionException e ) {
                                  throw new RuntimeException( "unable to execute mutation", e );
                              }

                              return Observable.from( markedEdges );
                          }
                      } ).count();
    }
}
