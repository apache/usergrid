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


import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ObservableIterator;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.uuid.UUIDComparator;
import com.fasterxml.uuid.impl.UUIDUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.functions.Func2;


/**
 * SimpleRepair operation
 *
 */
@Singleton
public abstract class AbstractEdgeRepair  {


    private static final Logger LOG = LoggerFactory.getLogger(AbstractEdgeRepair.class);

    protected final EdgeSerialization edgeSerialization;
    protected final GraphFig graphFig;
    protected final Keyspace keyspace;
    protected final Scheduler scheduler;


    @Inject
    public AbstractEdgeRepair( final EdgeSerialization edgeSerialization, final GraphFig graphFig,
                               final Keyspace keyspace, final Scheduler scheduler ) {
        this.edgeSerialization = edgeSerialization;
        this.graphFig = graphFig;
        this.keyspace = keyspace;
        this.scheduler = scheduler;
    }



    public Observable<MarkedEdge> repair( final OrganizationScope scope, final Edge edge ) {

        final UUID maxVersion = edge.getVersion();

        //get source edges
        Observable<MarkedEdge> sourceEdges = getEdgeVersionsFromSource( scope, edge );

        //get target edges
        Observable<MarkedEdge> targetEdges = getEdgeVersionsToTarget( scope, edge );



        //merge source and target then deal with the distinct values
        return Observable.merge( sourceEdges, targetEdges ).filter( getFilter( maxVersion ) ).distinctUntilChanged().buffer( graphFig.getScanPageSize() )
                         .flatMap( new Func1<List<MarkedEdge>, Observable<MarkedEdge>>() {
                             @Override
                             public Observable<MarkedEdge> call( final List<MarkedEdge> markedEdges ) {
                                 final MutationBatch batch = keyspace.prepareMutationBatch();

                                 for ( MarkedEdge edge : markedEdges ) {
                                     LOG.debug( "Deleting edge {}" , edge );
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
     * A filter used to filter out edges appropriately for the max version
     *
     * @param maxVersion the max version to use
     * @return The filter to use in the max version
     */
    protected abstract Func1<MarkedEdge, Boolean> getFilter(final UUID maxVersion);

    /**
     * Get all edge versions <= the specified max from the source
     */
    private Observable<MarkedEdge> getEdgeVersionsFromSource( final OrganizationScope scope, final Edge edge ) {

        return Observable.create( new ObservableIterator<MarkedEdge>() {
            @Override
            protected Iterator<MarkedEdge> getIterator() {

                final SimpleSearchByEdge search = getSearchByEdge(edge);

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

                final SimpleSearchByEdge search = getSearchByEdge(edge);

                return edgeSerialization.getEdgeToTarget( scope, search );
            }
        } ).subscribeOn( scheduler );
    }


    /**
     * Construct the search params for the edge
     * @param edge
     * @return
     */
    private SimpleSearchByEdge getSearchByEdge(final Edge edge){
        return new SimpleSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getTargetNode(),
                                        edge.getVersion(), null );
    }
}
