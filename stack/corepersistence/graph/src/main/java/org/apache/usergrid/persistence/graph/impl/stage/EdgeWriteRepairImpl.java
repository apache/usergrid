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
import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ObservableIterator;

import com.fasterxml.uuid.UUIDComparator;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;


/**
 * SimpleRepair operation
 *
 */
@Singleton
public class EdgeWriteRepairImpl implements EdgeWriteRepair {

    protected final EdgeSerialization edgeSerialization;
    protected final GraphFig graphFig;
    protected final Keyspace keyspace;
    protected final Scheduler scheduler;


    @Inject
    public EdgeWriteRepairImpl( final EdgeSerialization edgeSerialization, final GraphFig graphFig,
                                final Keyspace keyspace, final Scheduler scheduler ) {
        this.edgeSerialization = edgeSerialization;
        this.graphFig = graphFig;
        this.keyspace = keyspace;
        this.scheduler = scheduler;
    }


    @Override
    public Observable<MarkedEdge> repair( final OrganizationScope scope, final Edge edge ) {

        final UUID maxVersion = edge.getVersion();

        //get source edges
        Observable<MarkedEdge> sourceEdges = getEdgeVersionsFromSource( scope, edge );

        //get target edges
        Observable<MarkedEdge> targetEdges = getEdgeVersionsToTarget( scope, edge );


        //merge source and target then deal with the distinct values
        return Observable.merge( sourceEdges, targetEdges ).filter( new Func1<MarkedEdge, Boolean>() {
            /**
             * We only want to return edges < this version so we remove them
             * @param markedEdge
             * @return
             */
            @Override
            public Boolean call( final MarkedEdge markedEdge ) {
                return UUIDComparator.staticCompare( markedEdge.getVersion(), maxVersion ) < 0;
            }
            //buffer the deletes and issue them in a single mutation
        } ).distinctUntilChanged().buffer( graphFig.getScanPageSize() )
                         .flatMap( new Func1<List<MarkedEdge>, Observable<MarkedEdge>>() {
                             @Override
                             public Observable<MarkedEdge> call( final List<MarkedEdge> markedEdges ) {
                                 final MutationBatch batch = keyspace.prepareMutationBatch();

                                 for ( MarkedEdge edge : markedEdges ) {
                                     final MutationBatch delete = edgeSerialization.deleteEdge( scope, edge );

                                     batch.mergeShallow( delete );
                                 }

                                 try {
                                     batch.execute();
                                 }
                                 catch ( ConnectionException e ) {
                                     throw new RuntimeException( "Unable to issue write to cassandra", e );
                                 }

                                 return Observable.from( markedEdges ).subscribeOn( scheduler );
                             }
             } );
    }


    /**
     * Get all edge versions <= the specified max from the source
     */
    private Observable<MarkedEdge> getEdgeVersionsFromSource( final OrganizationScope scope, final Edge edge ) {

        return Observable.create( new ObservableIterator<MarkedEdge>() {
            @Override
            protected Iterator<MarkedEdge> getIterator() {

                final SimpleSearchByEdge search =
                        new SimpleSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getTargetNode(),
                                edge.getVersion(), null );

                return edgeSerialization.getEdgeFromSource( scope, search );
            }
        } ).subscribeOn( scheduler );
    }


    /**
     * Get all edge versions <= the specified max from the source
     */
    private Observable<MarkedEdge> getEdgeVersionsToTarget( final OrganizationScope scope, final Edge edge ) {

        return Observable.create( new ObservableIterator<MarkedEdge>() {
            @Override
            protected Iterator<MarkedEdge> getIterator() {

                final SimpleSearchByEdge search =
                        new SimpleSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getTargetNode(),
                                edge.getVersion(), null );

                return edgeSerialization.getEdgeToTarget( scope, search );
            }
        } ).subscribeOn( scheduler );
    }
}
