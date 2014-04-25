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

package org.apache.usergrid.persistence.graph.impl.stage;


import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.guice.CommitLog;
import org.apache.usergrid.persistence.graph.guice.PermanentStorage;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.MergedEdgeReader;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ObservableIterator;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * SimpleRepair operation
 */

public class EdgeDeleteRepairImpl implements EdgeDeleteRepair {


    private static final Logger LOG = LoggerFactory.getLogger( EdgeDeleteRepairImpl.class );

    protected final EdgeSerialization commitLogSerialization;
    protected final EdgeSerialization storageSerialization;
    protected final MergedEdgeReader mergedEdgeReader;
    protected final GraphFig graphFig;
    protected final Keyspace keyspace;


    @Inject
    public EdgeDeleteRepairImpl( @CommitLog final EdgeSerialization commitLogSerialization,
                                 @PermanentStorage final EdgeSerialization storageSerialization,
                                 final MergedEdgeReader mergedEdgeReader, final GraphFig graphFig,
                                 final Keyspace keyspace ) {

        Preconditions.checkNotNull( "commitLogSerialization is required", commitLogSerialization );
        Preconditions.checkNotNull( "storageSerialization is required", storageSerialization );
        Preconditions.checkNotNull( "mergedEdgeReader is required", mergedEdgeReader );
        Preconditions.checkNotNull( "graphFig is required", graphFig );
        Preconditions.checkNotNull( "keyspace is required", keyspace );


        this.commitLogSerialization = commitLogSerialization;
        this.storageSerialization = storageSerialization;
        this.mergedEdgeReader = mergedEdgeReader;
        this.graphFig = graphFig;
        this.keyspace = keyspace;
    }


    public Observable<MarkedEdge> repair( final OrganizationScope scope, final Edge edge ) {


        //merge source and target then deal with the distinct values
        return Observable.just( edge ).flatMap( new Func1<Edge, Observable<? extends MarkedEdge>>() {
            @Override
            public Observable<? extends MarkedEdge> call( final Edge edge ) {
                final MutationBatch batch = keyspace.prepareMutationBatch();

                Observable<MarkedEdge> commitLog = seekAndDelete( scope, edge, commitLogSerialization, batch );
                Observable<MarkedEdge> storage = seekAndDelete( scope, edge, storageSerialization, batch );

                return Observable.merge( commitLog, storage ).distinctUntilChanged().doOnCompleted( new Action0() {
                    @Override
                    public void call() {
                        try {
                            batch.execute();
                        }
                        catch ( ConnectionException e ) {
                            throw new RuntimeException( "Could not delete marked edge", e );
                        }
                    }
                } );
            }
        } );
    }


    private Observable<MarkedEdge> seekAndDelete( final OrganizationScope scope, final Edge edge,
                                                  final EdgeSerialization serialization, final MutationBatch batch ) {
        //We read to ensure that we're only removing if it exist in the serialization.  Otherwise we're
        // inserting
        //tombstone bloat
        return getEdgeVersions( scope, edge, serialization ).take( 1 ).map( new Func1<MarkedEdge, MarkedEdge>() {
            @Override
            public MarkedEdge call( final MarkedEdge markedEdge ) {
                //it's been written with this serializer, remove it
                if ( edge.equals( markedEdge ) ) {
                    final MutationBatch commitLog = serialization.deleteEdge( scope, edge );
                    batch.mergeShallow( commitLog );
                }
                return markedEdge;
            }
        } );
    }


    /**
     * Get all edge versions <= the specified max from the source
     */
    private Observable<MarkedEdge> getEdgeVersions( final OrganizationScope scope, final Edge edge,
                                                    final EdgeSerialization serialization ) {

        return Observable.create( new ObservableIterator<MarkedEdge>( "edgeVersions" ) {
            @Override
            protected Iterator<MarkedEdge> getIterator() {

                final SimpleSearchByEdge search =
                        new SimpleSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getTargetNode(),
                                edge.getVersion(), null );

                return serialization.getEdgeVersions( scope, search );
            }
        } ).subscribeOn( Schedulers.io() );
    }
}
