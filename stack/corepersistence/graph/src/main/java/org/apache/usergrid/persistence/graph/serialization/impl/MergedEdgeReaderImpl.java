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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.rx.OrderedMerge;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.guice.CommitLogEdgeSerialization;
import org.apache.usergrid.persistence.graph.guice.StorageEdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.CassandraConfig;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;

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


    public static final Log RX_LOG = new Log();

    private final EdgeSerialization commitLogSerialization;
    private final EdgeSerialization permanentSerialization;
    private final CassandraConfig cassandraConfig;




    @Inject
    public MergedEdgeReaderImpl( @CommitLogEdgeSerialization final EdgeSerialization commitLogSerialization,
                                 @StorageEdgeSerialization final EdgeSerialization permanentSerialization,
                                 final CassandraConfig cassandraConfig ) {
        this.commitLogSerialization = commitLogSerialization;
        this.permanentSerialization = permanentSerialization;
        this.cassandraConfig = cassandraConfig;
    }


    /**
     * Get the edges from the source for both the commit log and the permanent storage.  Merge them into a single
     * observable
     */
    public Observable<MarkedEdge> getEdgesFromSource( final OrganizationScope scope, final SearchByEdgeType edgeType ) {
        Observable<MarkedEdge> commitLog =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesFromSourceCommitLog" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return commitLogSerialization.getEdgesFromSource( scope, edgeType );
                    }
                } ).subscribeOn( Schedulers.io() );


        Observable<MarkedEdge> permanent =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesFromSourceStorage" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return permanentSerialization.getEdgesFromSource( scope, edgeType );
                    }
                } ).subscribeOn( Schedulers.io() );

        return OrderedMerge
                .orderedMerge( SourceEdgeComparator.INSTANCE, cassandraConfig.getScanPageSize() / 2, commitLog,
                        permanent ).distinctUntilChanged().doOnNext( RX_LOG);
    }


    public Observable<MarkedEdge> getEdgesFromSourceByTargetType( final OrganizationScope scope,
                                                                  final SearchByIdType edgeType ) {

        Observable<MarkedEdge> commitLog =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesFromSourceByTargetTypeCommitLog" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return commitLogSerialization.getEdgesFromSourceByTargetType( scope, edgeType );
                    }
                } ).subscribeOn( Schedulers.io() );


        Observable<MarkedEdge> permanent =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesFromSourceByTargetTypeStorage" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return permanentSerialization.getEdgesFromSourceByTargetType( scope, edgeType );
                    }
                } ).subscribeOn( Schedulers.io() );

        return OrderedMerge
                .orderedMerge( SourceEdgeComparator.INSTANCE, cassandraConfig.getScanPageSize() / 2, commitLog,
                        permanent ).distinctUntilChanged().doOnNext( RX_LOG);
    }


    public Observable<MarkedEdge> getEdgesToTarget( final OrganizationScope scope, final SearchByEdgeType edgeType ) {
        Observable<MarkedEdge> commitLog =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesToTargetCommitLog" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return commitLogSerialization.getEdgesToTarget( scope, edgeType );
                    }
                } ).subscribeOn( Schedulers.io() );


        Observable<MarkedEdge> permanent =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesToTargetTypeStorage" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return permanentSerialization.getEdgesToTarget( scope, edgeType );
                    }
                } ).subscribeOn( Schedulers.io() );

        return OrderedMerge
                .orderedMerge( TargetEdgeComparator.INSTANCE, cassandraConfig.getScanPageSize() / 2, commitLog,
                        permanent ).distinctUntilChanged().doOnNext( RX_LOG);
    }


    public Observable<MarkedEdge> getEdgesToTargetBySourceType( final OrganizationScope scope,
                                                                final SearchByIdType edgeType ) {
        Observable<MarkedEdge> commitLog =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesToTargetBySourceTypeCommitLog" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return commitLogSerialization.getEdgesToTargetBySourceType( scope, edgeType );
                    }
                } ).subscribeOn( Schedulers.io() );


        Observable<MarkedEdge> permanent =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesToTargetBySourceTypeStorage" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return permanentSerialization.getEdgesToTargetBySourceType( scope, edgeType );
                    }
                } ).subscribeOn( Schedulers.io() );

        return OrderedMerge
                .orderedMerge( TargetEdgeComparator.INSTANCE, cassandraConfig.getScanPageSize() / 2, commitLog,
                        permanent ).distinctUntilChanged().doOnNext( RX_LOG);
    }


    @Override
    public Observable<MarkedEdge> getEdgeVersions( final OrganizationScope scope, final SearchByEdge search ) {
        Observable<MarkedEdge> commitLog =
                Observable.create( new ObservableIterator<MarkedEdge>( "ggetEdgeVersionsCommitLog" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return commitLogSerialization.getEdgeVersions( scope, search );
                    }
                } ).subscribeOn( Schedulers.io() );


        Observable<MarkedEdge> permanent =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeVersionsStorage" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return permanentSerialization.getEdgeVersions( scope, search );
                    }
                } ).subscribeOn( Schedulers.io() );

        return OrderedMerge
                .orderedMerge( EdgeVersionComparator.INSTANCE, cassandraConfig.getScanPageSize() / 2, commitLog, permanent )
                .distinctUntilChanged().doOnNext( RX_LOG);
    }


    /**
     * Edge comparator for comparing edgs.  Should order them by timeuuid, then non seek node, then deleted = true
     * first
     */
    private static abstract class EdgeComparator implements Comparator<MarkedEdge> {


        @Override
        public int compare( final MarkedEdge o1, final MarkedEdge o2 ) {


            int compare = compareVersions( o1, o2 );

            if ( compare != 0 ) {
                return compare;
            }

            compare = compareId( o1, o2 );

            if ( compare != 0 ) {
                return compare;
            }

            return compareMarks( o1, o2 );
        }


        protected abstract int compareId( final MarkedEdge o1, final MarkedEdge o2 );
    }


    /**
     * Performs comparisons for read from source edges
     */
    public static final class SourceEdgeComparator extends EdgeComparator {

        public static final SourceEdgeComparator INSTANCE = new SourceEdgeComparator();


        @Override
        protected int compareId( final MarkedEdge o1, final MarkedEdge o2 ) {
            return o1.getTargetNode().compareTo( o2.getTargetNode() );
        }
    }


    /**
     * Performs comparisons for read from source edges
     */
    public static final class TargetEdgeComparator extends EdgeComparator {

        public static final TargetEdgeComparator INSTANCE = new TargetEdgeComparator();


        @Override
        protected int compareId( final MarkedEdge o1, final MarkedEdge o2 ) {
            return o1.getSourceNode().compareTo( o2.getSourceNode() );
        }
    }


    /**
     * Assumes that all edges are the same and only differ by version and marked
     */
    public static final class EdgeVersionComparator implements Comparator<MarkedEdge> {

        public static final EdgeVersionComparator INSTANCE = new EdgeVersionComparator();

        @Override
        public int compare( final MarkedEdge o1, final MarkedEdge o2 ) {
            int compare = compareVersions(o1, o2);

            if ( compare != 0 ) {
                return compare;
            }

            return compareMarks( o1, o2 );
        }
    }


    /**
     * Compare versions of the two edges.  The highest version will be considered "less" than a lower version since we
     * want descending ordering
     * @param o1
     * @param o2
     * @return
     */
    public static int compareVersions(final MarkedEdge o1, final MarkedEdge o2){
        return UUIDComparator.staticCompare( o1.getVersion(), o2.getVersion() );
    }
    /**
     * Compare the marks.  Since a marked of deleted is higher priority, it becomes a less than respnse
     */
    public static int compareMarks( final MarkedEdge o1, final MarkedEdge o2 ) {
        if ( o1.isDeleted() ) {
            //if o2 is deleted they're both deleted and equa
            if ( o2.isDeleted() ) {
                return 0;
            }

            //o2 is not deleted, so o1 should be "less" to be emitted first
            return 1;
        }

        //o2 is deleted and o1 is not
        if ( o2.isDeleted() ) {
            return -1;
        }

        return 0;
    }

    private static class Log implements Action1<MarkedEdge>{



        @Override
        public void call( final MarkedEdge markedEdge ) {
            LOG.debug( "Emitting edge {}", markedEdge );
        }
    }
}
