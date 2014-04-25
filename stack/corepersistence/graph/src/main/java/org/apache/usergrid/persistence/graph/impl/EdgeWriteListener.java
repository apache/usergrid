/*
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
 */
package org.apache.usergrid.persistence.graph.impl;


import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.consistency.MessageListener;
import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.guice.CommitLogEdgeSerialization;
import org.apache.usergrid.persistence.graph.guice.EdgeWrite;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.fasterxml.uuid.UUIDComparator;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.functions.Func1;


/**
 * Construct the asynchronous delete operation from the listener
 */
@Singleton
public class EdgeWriteListener implements MessageListener<EdgeEvent<Edge>, EdgeEvent<Edge>> {


    private final EdgeSerialization commitLog;
    private final EdgeSerialization permanentStorage;
    private final Keyspace keyspace;
    private final GraphFig graphFig;


    @Inject
    public EdgeWriteListener( @CommitLogEdgeSerialization final EdgeSerialization commitLog,
                              @CommitLogEdgeSerialization final EdgeSerialization permanentStorage, final Keyspace keyspace,
                              @EdgeWrite final AsyncProcessor edgeWrite, final GraphFig graphFig ) {


        Preconditions.checkNotNull( commitLog, "commitLog is required" );
        Preconditions.checkNotNull( permanentStorage, "permanentStorage is required" );
        Preconditions.checkNotNull( edgeWrite, "edgeWrite is required" );
        Preconditions.checkNotNull( keyspace, "keyspace is required" );
        Preconditions.checkNotNull( keyspace, "graphFig is required" );


        this.keyspace = keyspace;
        this.commitLog = commitLog;
        this.permanentStorage = permanentStorage;
        this.graphFig = graphFig;

        edgeWrite.addListener( this );
    }


    @Override
    public Observable<EdgeEvent<Edge>> receive( final EdgeEvent<Edge> write ) {

        final Edge writtenEdge = write.getData();
        final OrganizationScope scope = write.getOrganizationScope();
        final UUID now = UUIDGenerator.newTimeUUID();

        return Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeVersions" ) {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                //get our edge as it exists in the commit log
                return commitLog.getEdgeVersions( scope,
                        new SimpleSearchByEdge( writtenEdge.getSourceNode(), writtenEdge.getType(),
                                writtenEdge.getTargetNode(), now, null ) );
            }
        } )
                //only process until we get to an edge <= this version or complete.
                .takeWhile( new Func1<MarkedEdge, Boolean>() {
                    @Override
                    public Boolean call( final MarkedEdge markedEdge ) {
                        return UUIDComparator.staticCompare( writtenEdge.getVersion(), markedEdge.getVersion() ) < 0;
                    }
                } )

                        //buffer them, then execute mutations in batch
                .buffer( graphFig.getScanPageSize() ).map( new Func1<List<MarkedEdge>, EdgeEvent<Edge>>() {
                    @Override
                    public EdgeEvent<Edge> call( final List<MarkedEdge> markedEdges ) {


                        final MutationBatch storageWriteBatch = keyspace.prepareMutationBatch();
                        final MutationBatch commitlogCleanBatch = keyspace.prepareMutationBatch();


                        for ( MarkedEdge edge : markedEdges ) {

                            //batch the write
                            storageWriteBatch.mergeShallow( permanentStorage.writeEdge( scope, edge ) );

                            //batch the cleanup
                            commitlogCleanBatch.mergeShallow( commitLog.deleteEdge( scope, edge ) );
                        }


                        //execute the write to permanent storage
                        try {
                            storageWriteBatch.execute();
                        }
                        catch ( ConnectionException e ) {
                            throw new RuntimeException( "unable to execute mutation", e );
                        }

                        //since our batch has completed, we can now invoke the remove from the commit log

                        try {
                            commitlogCleanBatch.execute();
                        }
                        catch ( ConnectionException e ) {
                            throw new RuntimeException( "unable to execute mutation", e );
                        }

                        return write;
                    }
                } );
    }
}
