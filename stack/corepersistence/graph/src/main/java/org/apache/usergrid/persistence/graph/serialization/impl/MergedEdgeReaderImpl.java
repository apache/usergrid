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
package org.apache.usergrid.persistence.graph.serialization.impl;


import java.util.Comparator;
import java.util.Iterator;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.rx.OrderedMerge;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.guice.CommitLogEdgeSerialization;
import org.apache.usergrid.persistence.graph.guice.StorageEdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.uuid.UUIDComparator;
import com.google.inject.Singleton;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Utility class for creating observable edges
 */
@Singleton
public class MergedEdgeReaderImpl implements MergedEdgeReader {

    private static final Logger LOG = LoggerFactory.getLogger( MergedEdgeReaderImpl.class );


    private static final Log RX_LOG = new Log();

    private static final EdgeMapper MAPPER = new EdgeMapper();

    private static final EdgeKeySelector EDGE_KEY_SELECTOR = new EdgeKeySelector();

    private final EdgeSerialization commitLogSerialization;
    private final EdgeSerialization permanentSerialization;
    private final GraphFig graphFig;


    @Inject
    public MergedEdgeReaderImpl( @CommitLogEdgeSerialization final EdgeSerialization commitLogSerialization,
                                 @StorageEdgeSerialization final EdgeSerialization permanentSerialization,
                                 final GraphFig graphFig ) {
        this.commitLogSerialization = commitLogSerialization;
        this.permanentSerialization = permanentSerialization;
        this.graphFig = graphFig;
    }


    /**
     * Get the edges from the source for both the commit log and the permanent storage.  Merge them into a single
     * observable
     */
    public Observable<MarkedEdge> getEdgesFromSource( final OrganizationScope scope, final SearchByEdgeType edgeType ) {
        Observable<SourceAwareMarkedEdge> commitLog =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesFromSourceCommitLog" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return commitLogSerialization.getEdgesFromSource( scope, edgeType );
                    }
                } ).subscribeOn( Schedulers.io() ).map( new Func1<MarkedEdge, SourceAwareMarkedEdge>() {
                    @Override
                    public SourceAwareMarkedEdge call( final MarkedEdge markedEdge ) {
                        return new SourceAwareMarkedEdge( markedEdge, commitLogSerialization );
                    }
                } );


        Observable<SourceAwareMarkedEdge> permanent =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesFromSourceStorage" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return permanentSerialization.getEdgesFromSource( scope, edgeType );
                    }
                } ).subscribeOn( Schedulers.io() ).subscribeOn( Schedulers.io() )
                          .map( new Func1<MarkedEdge, SourceAwareMarkedEdge>() {
                              @Override
                              public SourceAwareMarkedEdge call( final MarkedEdge markedEdge ) {
                                  return new SourceAwareMarkedEdge( markedEdge, permanentSerialization );
                              }
                          } );

        return OrderedMerge
                .orderedMerge( new SourceEdgeComparator( commitLogSerialization ), graphFig.getScanPageSize() / 2,
                        commitLog, permanent ).map( MAPPER ).distinctUntilChanged(EDGE_KEY_SELECTOR).doOnNext( RX_LOG );
    }


    public Observable<MarkedEdge> getEdgesFromSourceByTargetType( final OrganizationScope scope,
                                                                  final SearchByIdType edgeType ) {

        Observable<SourceAwareMarkedEdge> commitLog =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesFromSourceByTargetTypeCommitLog" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return commitLogSerialization.getEdgesFromSourceByTargetType( scope, edgeType );
                    }
                } ).subscribeOn( Schedulers.io() ).map( new Func1<MarkedEdge, SourceAwareMarkedEdge>() {
                    @Override
                    public SourceAwareMarkedEdge call( final MarkedEdge markedEdge ) {
                        return new SourceAwareMarkedEdge( markedEdge, commitLogSerialization );
                    }
                } );


        Observable<SourceAwareMarkedEdge> permanent =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesFromSourceByTargetTypeStorage" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return permanentSerialization.getEdgesFromSourceByTargetType( scope, edgeType );
                    }
                } ).subscribeOn( Schedulers.io() ).subscribeOn( Schedulers.io() )
                          .map( new Func1<MarkedEdge, SourceAwareMarkedEdge>() {
                              @Override
                              public SourceAwareMarkedEdge call( final MarkedEdge markedEdge ) {
                                  return new SourceAwareMarkedEdge( markedEdge, permanentSerialization );
                              }
                          } );

        return OrderedMerge
                .orderedMerge( new SourceEdgeComparator( commitLogSerialization ), graphFig.getScanPageSize() / 2,
                        commitLog, permanent ).map( MAPPER ).distinctUntilChanged(EDGE_KEY_SELECTOR).doOnNext( RX_LOG );
    }


    public Observable<MarkedEdge> getEdgesToTarget( final OrganizationScope scope, final SearchByEdgeType edgeType ) {
        Observable<SourceAwareMarkedEdge> commitLog =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesToTargetCommitLog" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return commitLogSerialization.getEdgesToTarget( scope, edgeType );
                    }
                } ).subscribeOn( Schedulers.io() ).map( new Func1<MarkedEdge, SourceAwareMarkedEdge>() {
                    @Override
                    public SourceAwareMarkedEdge call( final MarkedEdge markedEdge ) {
                        return new SourceAwareMarkedEdge( markedEdge, commitLogSerialization );
                    }
                } );


        Observable<SourceAwareMarkedEdge> permanent =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesToTargetTypeStorage" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return permanentSerialization.getEdgesToTarget( scope, edgeType );
                    }
                } ).subscribeOn( Schedulers.io() ).subscribeOn( Schedulers.io() )
                          .map( new Func1<MarkedEdge, SourceAwareMarkedEdge>() {
                              @Override
                              public SourceAwareMarkedEdge call( final MarkedEdge markedEdge ) {
                                  return new SourceAwareMarkedEdge( markedEdge, permanentSerialization );
                              }
                          } );

        return OrderedMerge
                .orderedMerge( new SourceEdgeComparator( commitLogSerialization ), graphFig.getScanPageSize() / 2,
                        commitLog, permanent ).map( MAPPER ).distinctUntilChanged(EDGE_KEY_SELECTOR).doOnNext( RX_LOG );
    }


    public Observable<MarkedEdge> getEdgesToTargetBySourceType( final OrganizationScope scope,
                                                                final SearchByIdType edgeType ) {
        Observable<SourceAwareMarkedEdge> commitLog =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesToTargetBySourceTypeCommitLog" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return commitLogSerialization.getEdgesToTargetBySourceType( scope, edgeType );
                    }
                } ).subscribeOn( Schedulers.io() ).map( new Func1<MarkedEdge, SourceAwareMarkedEdge>() {
                    @Override
                    public SourceAwareMarkedEdge call( final MarkedEdge markedEdge ) {
                        return new SourceAwareMarkedEdge( markedEdge, commitLogSerialization );
                    }
                } );


        Observable<SourceAwareMarkedEdge> permanent =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesToTargetBySourceTypeStorage" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return permanentSerialization.getEdgesToTargetBySourceType( scope, edgeType );
                    }
                } ).subscribeOn( Schedulers.io() ).subscribeOn( Schedulers.io() )
                          .map( new Func1<MarkedEdge, SourceAwareMarkedEdge>() {
                              @Override
                              public SourceAwareMarkedEdge call( final MarkedEdge markedEdge ) {
                                  return new SourceAwareMarkedEdge( markedEdge, permanentSerialization );
                              }
                          } );

        return OrderedMerge
                .orderedMerge( new SourceEdgeComparator( commitLogSerialization ), graphFig.getScanPageSize() / 2,
                        commitLog, permanent ).map( MAPPER ).distinctUntilChanged(EDGE_KEY_SELECTOR).doOnNext( RX_LOG );
    }


    @Override
    public Observable<MarkedEdge> getEdgeVersions( final OrganizationScope scope, final SearchByEdge search ) {
        Observable<SourceAwareMarkedEdge> commitLog =
                Observable.create( new ObservableIterator<MarkedEdge>( "ggetEdgeVersionsCommitLog" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return commitLogSerialization.getEdgeVersions( scope, search );
                    }
                } ).subscribeOn( Schedulers.io() ).map( new Func1<MarkedEdge, SourceAwareMarkedEdge>() {
                    @Override
                    public SourceAwareMarkedEdge call( final MarkedEdge markedEdge ) {
                        return new SourceAwareMarkedEdge( markedEdge, commitLogSerialization );
                    }
                } );


        Observable<SourceAwareMarkedEdge> permanent =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeVersionsStorage" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return permanentSerialization.getEdgeVersions( scope, search );
                    }
                } ).subscribeOn( Schedulers.io() ).map( new Func1<MarkedEdge, SourceAwareMarkedEdge>() {
                    @Override
                    public SourceAwareMarkedEdge call( final MarkedEdge markedEdge ) {
                        return new SourceAwareMarkedEdge( markedEdge, permanentSerialization );
                    }
                } );

        return OrderedMerge
                .orderedMerge( new EdgeVersionComparator( commitLogSerialization ), graphFig.getScanPageSize() / 2,
                        commitLog, permanent ).map( MAPPER ).distinctUntilChanged(EDGE_KEY_SELECTOR).doOnNext( RX_LOG );
    }


    /**
     * Edge comparator for comparing edgs.  Should order them by timeuuid, then non seek node, then deleted = true
     * first
     */
    private static abstract class EdgeComparator implements Comparator<SourceAwareMarkedEdge> {


        private final EdgeSerialization commitLog;


        protected EdgeComparator( final EdgeSerialization commitLog ) {
            this.commitLog = commitLog;
        }


        @Override
        public int compare( final SourceAwareMarkedEdge o1, final SourceAwareMarkedEdge o2 ) {


            int compare = compareVersions( o1.edge, o2.edge );

            if ( compare != 0 ) {
                return compare;
            }

            compare = compareId( o1.edge, o2.edge );

            if ( compare != 0 ) {
                return compare;
            }

            //if it comes from the commit log, it's always greater if all the fields are the same.  The commit log
            //is always the most recent source of data
            if ( o1.edgeSerialization == commitLog ) {
                if ( o2.edgeSerialization == commitLog ) {
                    return 0;
                }

                return 1;
            }

            return -1;
        }


        protected abstract int compareId( final MarkedEdge o1, final MarkedEdge o2 );
    }


    /**
     * Performs comparisons for read from source edges
     */
    public static final class SourceEdgeComparator extends EdgeComparator {


        protected SourceEdgeComparator( final EdgeSerialization commitLog ) {
            super( commitLog );
        }


        @Override
        protected int compareId( final MarkedEdge o1, final MarkedEdge o2 ) {
            return o1.getTargetNode().compareTo( o2.getTargetNode() );
        }
    }


    /**
     * Performs comparisons for read from source edges
     */
    public static final class TargetEdgeComparator extends EdgeComparator {


        protected TargetEdgeComparator( final EdgeSerialization commitLog ) {
            super( commitLog );
        }


        @Override
        protected int compareId( final MarkedEdge o1, final MarkedEdge o2 ) {
            return o1.getSourceNode().compareTo( o2.getSourceNode() );
        }
    }


    /**
     * Assumes that all edges are the same and only differ by version and marked
     */
    public static final class EdgeVersionComparator implements Comparator<SourceAwareMarkedEdge> {

        protected final EdgeSerialization commitLog;


        public EdgeVersionComparator( final EdgeSerialization commitLog ) {this.commitLog = commitLog;}


        @Override
        public int compare( final SourceAwareMarkedEdge o1, final SourceAwareMarkedEdge o2 ) {
            int compare = compareVersions( o1.edge, o2.edge );

            if ( compare != 0 ) {
                return compare;
            }

            //if it comes from the commit log, it's always greater
            if ( o1.edgeSerialization == commitLog ) {
                return 1;
            }

            return -1;
        }
    }


    /**
     * Compare versions of the two edges.  The highest version will be considered "less" than a lower version since we
     * want descending ordering
     */
    public static int compareVersions( final MarkedEdge o1, final MarkedEdge o2 ) {
        return UUIDComparator.staticCompare( o1.getVersion(), o2.getVersion() );
    }


    private static class Log implements Action1<MarkedEdge> {


        @Override
        public void call( final MarkedEdge markedEdge ) {
            LOG.debug( "Emitting edge {}", markedEdge );
        }
    }


    private static class EdgeMapper implements Func1<SourceAwareMarkedEdge, MarkedEdge> {


        @Override
        public MarkedEdge call( final SourceAwareMarkedEdge sourceAwareMarkedEdge ) {
            return sourceAwareMarkedEdge.edge;
        }
    }


    /**
     * Wrapper that allows us to select inputs based on source if the edges are equal
     */
    public static class SourceAwareMarkedEdge {

        public final MarkedEdge edge;
        public final EdgeSerialization edgeSerialization;


        public SourceAwareMarkedEdge( final MarkedEdge edge, final EdgeSerialization edgeSerialization ) {
            this.edge = edge;
            this.edgeSerialization = edgeSerialization;
        }
    }


    /**
     * Function to select keys
     */
    private static class EdgeKeySelector implements Func1<MarkedEdge, EdgeKey> {
        @Override
        public EdgeKey call( final MarkedEdge markedEdge ) {
            return new EdgeKey( markedEdge.getSourceNode(), markedEdge.getType(), markedEdge.getTargetNode(),
                    markedEdge.getVersion() );
        }
    }


    /**
     * Class that represents edge keys
     */
    private static class EdgeKey {
        private final Id sourceNode;
        private final String type;
        private final Id targetNode;
        private final UUID version;


        private EdgeKey( final Id sourceNode, final String type, final Id targetNode, final UUID version ) {
            this.sourceNode = sourceNode;
            this.type = type;
            this.targetNode = targetNode;
            this.version = version;
        }


        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) {
                return true;
            }
            if ( !( o instanceof EdgeKey ) ) {
                return false;
            }

            final EdgeKey edgeKey = ( EdgeKey ) o;

            if ( !sourceNode.equals( edgeKey.sourceNode ) ) {
                return false;
            }
            if ( !targetNode.equals( edgeKey.targetNode ) ) {
                return false;
            }
            if ( !type.equals( edgeKey.type ) ) {
                return false;
            }
            if ( !version.equals( edgeKey.version ) ) {
                return false;
            }

            return true;
        }


        @Override
        public int hashCode() {
            int result = sourceNode.hashCode();
            result = 31 * result + type.hashCode();
            result = 31 * result + targetNode.hashCode();
            result = 31 * result + version.hashCode();
            return result;
        }
    }
}
