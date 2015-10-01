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


import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Construct the asynchronous node delete from the q
 */
public class NodeDeleteListenerImpl implements NodeDeleteListener {


    private static final Logger LOG = LoggerFactory.getLogger( NodeDeleteListenerImpl.class );

    private final NodeSerialization nodeSerialization;
    private final EdgeSerialization storageSerialization;
    private final EdgeMetadataSerialization edgeMetadataSerialization;
    private final EdgeMetaRepair edgeMetaRepair;
    private final GraphFig graphFig;
    protected final Keyspace keyspace;


    /**
     * Wire the serialization dependencies
     */
    @Inject
    public NodeDeleteListenerImpl( final NodeSerialization nodeSerialization,
                                   final EdgeMetadataSerialization edgeMetadataSerialization,
                                   final EdgeMetaRepair edgeMetaRepair, final GraphFig graphFig,
                                   final EdgeSerialization storageSerialization,
                                   final Keyspace keyspace ) {


        this.nodeSerialization = nodeSerialization;
        this.storageSerialization = storageSerialization;
        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.edgeMetaRepair = edgeMetaRepair;
        this.graphFig = graphFig;
        this.keyspace = keyspace;
    }


    /**
     * Removes this node from the graph.
     *
     * @param scope The scope of the application
     * @param node The node that was deleted
     * @param timestamp The timestamp of the event
     *
     * @return An observable that emits the total number of edges that have been removed with this node both as the
     *         target and source
     */
    public Observable<Integer> receive( final ApplicationScope scope, final Id node, final UUID timestamp ) {


        return Observable.just( node )

                //delete source and targets in parallel and merge them into a single observable
                .flatMap( new Func1<Id, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call( final Id node ) {

                        final Optional<Long> maxVersion = nodeSerialization.getMaxVersion( scope, node );

                        LOG.debug( "Node with id {} has max version of {}", node, maxVersion.orNull() );


                        if ( !maxVersion.isPresent() ) {
                            return Observable.empty();
                        }


                        //do all the delete, then when done, delete the node
                        return doDeletes( node, scope, maxVersion.get(), timestamp ).count()
                                //if nothing is ever emitted, emit 0 so that we know no operations took place.
                                // Finally remove
                                // the
                                // target node in the mark
                                .doOnCompleted( new Action0() {
                                    @Override
                                    public void call() {
                                        try {
                                            nodeSerialization.delete( scope, node, maxVersion.get()).execute();
                                        }
                                        catch ( ConnectionException e ) {
                                            throw new RuntimeException( "Unable to connect to casandra", e );
                                        }
                                    }
                                } );
                    }
                } ).defaultIfEmpty( 0 );
    }


    /**
     * Do the deletes
     */
    private Observable<MarkedEdge> doDeletes( final Id node, final ApplicationScope scope, final long maxVersion,
                                              final UUID eventTimestamp ) {
        /**
         * Note that while we're processing, returned edges could be moved from the commit log to storage.  As a result,
         * we need to issue a delete with the same version as the node delete on both commit log and storage for
         * results from the commit log.  This
         * ensures that the edge is removed from both, regardless of another thread or nodes' processing state.
         *
         */

        //get all edges pointing to the target node and buffer then into groups for deletion
        Observable<MarkedEdge> targetEdges =
                getEdgesTypesToTarget( scope, new SimpleSearchEdgeType( node, null, null ) )
                        .subscribeOn( Schedulers.io() ).flatMap( edgeType -> Observable.create( new ObservableIterator<MarkedEdge>( "getTargetEdges" ) {
                            @Override
                            protected Iterator<MarkedEdge> getIterator() {
                                return storageSerialization.getEdgesToTarget( scope,
                                        new SimpleSearchByEdgeType( node, edgeType, maxVersion, SearchByEdgeType.Order.DESCENDING,  Optional.<Edge>absent() ) );
                            }
                        } ) );


        //get all edges pointing to the source node and buffer them into groups for deletion
        Observable<MarkedEdge> sourceEdges =
                getEdgesTypesFromSource( scope, new SimpleSearchEdgeType( node, null, null ) )
                        .subscribeOn( Schedulers.io() ).flatMap( edgeType -> Observable.create( new ObservableIterator<MarkedEdge>( "getSourceEdges" ) {
                            @Override
                            protected Iterator<MarkedEdge> getIterator() {
                                return storageSerialization.getEdgesFromSource( scope,
                                        new SimpleSearchByEdgeType( node, edgeType, maxVersion, SearchByEdgeType.Order.DESCENDING,  Optional.<Edge>absent() ) );
                            }
                        } ) );

        //merge both source and target into 1 observable.  We'll need to check them all regardless of order
        return Observable.merge( targetEdges, sourceEdges )

                //buffer and delete marked edges in our buffer size so we're making less trips to cassandra
                .buffer( graphFig.getScanPageSize() ).flatMap( markedEdges -> {

                    LOG.debug( "Batching {} edges for node {} for deletion", markedEdges.size(), node );

                    final MutationBatch batch = keyspace.prepareMutationBatch();

                    Set<TargetPair> sourceNodes = new HashSet<>( markedEdges.size() );
                    Set<TargetPair> targetNodes = new HashSet<>( markedEdges.size() );

                    for ( MarkedEdge edge : markedEdges ) {

                        //delete the newest edge <= the version on the node delete

                        //we use the version specified on the delete purposefully.  If these edges are re-written
                        //at a greater time we want them to exit
                        batch.mergeShallow( storageSerialization.deleteEdge( scope, edge, eventTimestamp ) );

                        sourceNodes.add( new TargetPair( edge.getSourceNode(), edge.getType() ) );
                        targetNodes.add( new TargetPair( edge.getTargetNode(), edge.getType() ) );
                    }

                    try {
                        batch.execute();
                    }
                    catch ( ConnectionException e ) {
                        throw new RuntimeException( "Unable to connect to casandra", e );
                    }

                    //now  delete meta data


                    //delete both the source and target meta data in parallel for the edge we deleted in the
                    // previous step
                    //if nothing else is using them.  We purposefully do not schedule them on a new scheduler
                    //we want them running on the i/o thread from the Observable emitting all the edges

                    //
                    LOG.debug( "About to audit {} source types", sourceNodes.size() );

                    Observable<Integer> sourceMetaCleanup =
                            Observable.from( sourceNodes ).flatMap( targetPair -> edgeMetaRepair
                                    .repairSources( scope, targetPair.id, targetPair.edgeType, maxVersion ) ).last();


                    LOG.debug( "About to audit {} target types", targetNodes.size() );

                    Observable<Integer> targetMetaCleanup =
                            Observable.from( targetNodes ).flatMap( targetPair -> edgeMetaRepair
                                    .repairTargets( scope, targetPair.id, targetPair.edgeType, maxVersion ) ).last();


                    //run both the source/target edge type cleanup, then proceed
                    return Observable.merge( sourceMetaCleanup, targetMetaCleanup ).lastOrDefault( null )
                                     .flatMap( new Func1<Integer, Observable<MarkedEdge>>() {
                                         @Override
                                         public Observable<MarkedEdge> call( final Integer integer ) {
                                             return Observable.from( markedEdges );
                                         }
                                     } );
                } );
    }


    /**
     * Get all existing edge types to the target node
     */
    private Observable<String> getEdgesTypesToTarget( final ApplicationScope scope, final SearchEdgeType search ) {

        return Observable.create( new ObservableIterator<String>( "getEdgeTypesToTarget" ) {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getEdgeTypesToTarget( scope, search );
            }
        } );
    }


    /**
     * Get all existing edge types to the target node
     */
    private Observable<String> getEdgesTypesFromSource( final ApplicationScope scope, final SearchEdgeType search ) {
        return Observable.create( new ObservableIterator<String>( "getEdgeTypesFromSource" ) {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getEdgeTypesFromSource( scope, search );
            }
        } );
    }


    private static class TargetPair {
        protected final Id id;
        protected final String edgeType;


        private TargetPair( final Id id, final String edgeType ) {
            this.id = id;
            this.edgeType = edgeType;
        }


        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) {
                return true;
            }
            if ( o == null || getClass() != o.getClass() ) {
                return false;
            }

            final TargetPair that = ( TargetPair ) o;

            if ( !edgeType.equals( that.edgeType ) ) {
                return false;
            }
            if ( !id.equals( that.id ) ) {
                return false;
            }

            return true;
        }


        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + edgeType.hashCode();
            return result;
        }
    }
}
