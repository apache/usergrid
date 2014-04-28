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


import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.consistency.MessageListener;
import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.guice.CommitLogEdgeSerialization;
import org.apache.usergrid.persistence.graph.guice.NodeDelete;
import org.apache.usergrid.persistence.graph.guice.StorageEdgeSerialization;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeMetaRepair;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.MergedEdgeReader;
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
public class NodeDeleteListener implements MessageListener<EdgeEvent<Id>, Integer> {


    private static final Logger LOG = LoggerFactory.getLogger( NodeDeleteListener.class );

    private final NodeSerialization nodeSerialization;
    private final EdgeSerialization commitLogSerialization;
    private final EdgeSerialization storageSerialization;
    private final MergedEdgeReader mergedEdgeReader;
    private final EdgeMetadataSerialization edgeMetadataSerialization;
    private final EdgeMetaRepair edgeMetaRepair;
    private final GraphFig graphFig;
    protected final Keyspace keyspace;


    /**
     * Wire the serialization dependencies
     */
    @Inject
    public NodeDeleteListener( final NodeSerialization nodeSerialization,
                               final EdgeMetadataSerialization edgeMetadataSerialization,
                               final EdgeMetaRepair edgeMetaRepair, final GraphFig graphFig,
                               @NodeDelete final AsyncProcessor nodeDelete,
                               @CommitLogEdgeSerialization final EdgeSerialization commitLogSerialization,
                               @StorageEdgeSerialization final EdgeSerialization storageSerialization,
                               final MergedEdgeReader mergedEdgeReader, final Keyspace keyspace ) {


        this.nodeSerialization = nodeSerialization;
        this.commitLogSerialization = commitLogSerialization;
        this.storageSerialization = storageSerialization;
        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.edgeMetaRepair = edgeMetaRepair;
        this.graphFig = graphFig;
        this.mergedEdgeReader = mergedEdgeReader;
        this.keyspace = keyspace;

        nodeDelete.addListener( this );
    }


    /**
     * Removes this node from the graph.
     *
     * @param edgeEvent The edge event that was fired.
     *
     * @return An observable that emits the total number of edges that have been removed with this node both as the
     *         target and source
     */
    @Override
    public Observable<Integer> receive( final EdgeEvent<Id> edgeEvent ) {

        final Id node = edgeEvent.getData();
        final OrganizationScope scope = edgeEvent.getOrganizationScope();
        final UUID version = edgeEvent.getVersion();


        return Observable.from( node ).subscribeOn( Schedulers.io() ).map( new Func1<Id, Optional<UUID>>() {
            @Override
            public Optional<UUID> call( final Id id ) {
                return nodeSerialization.getMaxVersion( scope, node );
            }
        } ) //only continue if it's present.  Otherwise stop
                .takeWhile( new Func1<Optional<UUID>, Boolean>() {

                    @Override
                    public Boolean call( final Optional<UUID> uuidOptional ) {

                        LOG.debug( "Node with id {} has max version of {}", node, uuidOptional.orNull() );

                        return uuidOptional.isPresent();
                    }
                } )

                        //delete source and targets in parallel and merge them into a single observable
                .flatMap( new Func1<Optional<UUID>, Observable<MarkedEdge>>() {
                    @Override
                    public Observable<MarkedEdge> call( final Optional<UUID> uuidOptional ) {

                        /**
                         * Delete from the commit log and storage concurrently
                         */
                        Observable<MarkedEdge> commitLogRemovals =
                                doDeletes( commitLogSerialization, new NodeMutator() {
                                    @Override
                                    public MutationBatch doDeleteMutation( final OrganizationScope scope,
                                                                           final MarkedEdge edge ) {
                                        final MutationBatch commitBatch =
                                                commitLogSerialization.deleteEdge( scope, edge );

                                        final MutationBatch storageBatch =
                                                storageSerialization.deleteEdge( scope, edge );

                                        commitBatch.mergeShallow( storageBatch );

                                        return commitBatch;
                                    }
                                }, node, scope, version );

                        Observable<MarkedEdge> storageRemovals = doDeletes( storageSerialization, new NodeMutator() {
                            @Override
                            public MutationBatch doDeleteMutation( final OrganizationScope scope,
                                                                   final MarkedEdge edge ) {

                                final MutationBatch storageBatch = storageSerialization.deleteEdge( scope, edge );

                                return storageBatch;
                            }
                        }, node, scope, version );

                        return Observable.merge( commitLogRemovals, storageRemovals );
                    }
                } )


                .count()
                        //if nothing is ever emitted, emit 0 so that we know no operations took place. Finally remove
                        // the
                        // target node in the mark
                .defaultIfEmpty( 0 ).doOnCompleted( new Action0() {
                    @Override
                    public void call() {
                        try {
                            nodeSerialization.delete( scope, node, version ).execute();
                        }
                        catch ( ConnectionException e ) {
                            throw new RuntimeException( "Unable to delete marked graph node " + node, e );
                        }
                    }
                } );
    }


    /**
     * Do the deletes
     */
    private Observable<MarkedEdge> doDeletes( final EdgeSerialization serialization, final NodeMutator mutator,
                                              final Id node, final OrganizationScope scope, final UUID version ) {
        /**
         * Note that while we're processing, returned edges could be moved from the commit log to storage.  As a result,
         * we need to issue a delete with the same version as the node delete on both commit log and storage for
         * results from the commit log.  This
         * ensures that the edge is removed from both, regardless of another thread or nodes' processing state.
         *
         */

        //get all edges pointing to the target node and buffer then into groups for deletion
        Observable<MarkedEdge> targetEdges =
                getEdgesTypesToTarget( scope, new SimpleSearchEdgeType( node, null ) ).subscribeOn( Schedulers.io() )
                        .flatMap( new Func1<String, Observable<MarkedEdge>>() {
                            @Override
                            public Observable<MarkedEdge> call( final String edgeType ) {
                                return getEdgesToTarget( serialization, scope,
                                        new SimpleSearchByEdgeType( node, edgeType, version, null ) );
                            }
                        } );


        //get all edges pointing to the source node and buffer them into groups for deletion
        Observable<MarkedEdge> sourceEdges =
                getEdgesTypesFromSource( scope, new SimpleSearchEdgeType( node, null ) ).subscribeOn( Schedulers.io() )
                        .flatMap( new Func1<String, Observable<MarkedEdge>>() {
                            @Override
                            public Observable<MarkedEdge> call( final String edgeType ) {
                                return getEdgesFromSource( serialization, scope,
                                        new SimpleSearchByEdgeType( node, edgeType, version, null ) );
                            }
                        } );

        //merge both source and target into 1 observable.  We'll need to check them all regardless of order
        return Observable.merge( targetEdges, sourceEdges )

                //buffer and delete marked edges in our buffer size so we're making less trips to cassandra
                .buffer( graphFig.getScanPageSize() ).flatMap( new Func1<List<MarkedEdge>, Observable<MarkedEdge>>() {
                    @Override
                    public Observable<MarkedEdge> call( final List<MarkedEdge> markedEdges ) {

                        LOG.debug( "Batching {} edges for node {} for deletion", markedEdges.size(), node );

                        final MutationBatch batch = keyspace.prepareMutationBatch();

                        Set<TargetPair> sourceNodes = new HashSet<TargetPair>( markedEdges.size() );
                        Set<TargetPair> targetNodes = new HashSet<TargetPair>( markedEdges.size() );

                        for ( MarkedEdge edge : markedEdges ) {

                            //delete the newest edge <= the version on the node delete
                            final MutationBatch delete = mutator.doDeleteMutation( scope, edge );

                            batch.mergeShallow( delete );

                            sourceNodes.add( new TargetPair( edge.getSourceNode(), edge.getType() ) );
                            targetNodes.add( new TargetPair( edge.getTargetNode(), edge.getType() ) );
                        }

                        try {
                            batch.execute();
                        }
                        catch ( ConnectionException e ) {
                            throw new RuntimeException( "Unable to delete edges", e );
                        }

                        //now  delete meta data


                        //delete both the source and target meta data in parallel for the edge we deleted in the
                        // previous step
                        //if nothing else is using them.  We purposefully do not schedule them on a new scheduler
                        //we want them running on the i/o thread from the Observable emitting all the edges

                        //
                        LOG.debug( "About to audit {} source types", sourceNodes.size() );

                        Observable<Integer> sourceMetaCleanup =
                                Observable.from( sourceNodes ).flatMap( new Func1<TargetPair, Observable<Integer>>() {
                                    @Override
                                    public Observable<Integer> call( final TargetPair targetPair ) {
                                        return edgeMetaRepair
                                                .repairSources( scope, targetPair.id, targetPair.edgeType, version );
                                    }
                                } ).last();


                        LOG.debug( "About to audit {} target types", targetNodes.size() );

                        Observable<Integer> targetMetaCleanup =
                                Observable.from( targetNodes ).flatMap( new Func1<TargetPair, Observable<Integer>>() {
                                    @Override
                                    public Observable<Integer> call( final TargetPair targetPair ) {
                                        return edgeMetaRepair
                                                .repairTargets( scope, targetPair.id, targetPair.edgeType, version );
                                    }
                                } ).last();


                        //run both the source/target edge type cleanup, then proceed
                        return Observable.merge( sourceMetaCleanup, targetMetaCleanup ).last()
                                         .flatMap( new Func1<Integer, Observable<MarkedEdge>>() {
                                             @Override
                                             public Observable<MarkedEdge> call( final Integer integer ) {
                                                 return Observable.from( markedEdges );
                                             }
                                         } );
                    }
                } );
    }


    /**
     * Interface to define mutation delete callbacks
     */
    private static interface NodeMutator {

        /**
         * Perform the delete mutation on the marked edge
         */
        public MutationBatch doDeleteMutation( final OrganizationScope scope, MarkedEdge edge );
    }


    /**
     * Get all existing edge types to the target node
     */
    private Observable<String> getEdgesTypesToTarget( final OrganizationScope scope, final SearchEdgeType search ) {

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
    private Observable<String> getEdgesTypesFromSource( final OrganizationScope scope, final SearchEdgeType search ) {

        return Observable.create( new ObservableIterator<String>( "getEdgeTypesFromSource" ) {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getEdgeTypesFromSource( scope, search );
            }
        } );
    }


    /**
     * Load all edges pointing to this target
     */
    private Observable<MarkedEdge> getEdgesToTarget( final EdgeSerialization edgeSerialization,
                                                     final OrganizationScope scope, final SearchByEdgeType search ) {

        return Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesToTarget" ) {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return edgeSerialization.getEdgesToTarget( scope, search );
            }
        } );
    }


    /**
     * Load all edges pointing to this target
     */
    private Observable<MarkedEdge> getEdgesFromSource( final EdgeSerialization edgeSerialization,
                                                       final OrganizationScope scope, final SearchByEdgeType search ) {

        return Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesFromSource" ) {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return edgeSerialization.getEdgesFromSource( scope, search );
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
