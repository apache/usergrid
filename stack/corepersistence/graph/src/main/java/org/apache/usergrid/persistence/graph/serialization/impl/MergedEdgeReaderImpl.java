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

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.guice.CommitLog;
import org.apache.usergrid.persistence.graph.guice.PermanentStorage;
import org.apache.usergrid.persistence.graph.serialization.CassandraConfig;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ObservableIterator;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.OrderedMerge;

import com.google.inject.Singleton;

import rx.Observable;


/**
 * Utility class for creating observable edges
 */
@Singleton
public class MergedEdgeReaderImpl implements MergedEdgeReader {

    private final EdgeSerialization commitLogSerialization;
    private final EdgeSerialization permanentSerialization;
    private final CassandraConfig cassandraConfig;


    @Inject
    public MergedEdgeReaderImpl( @CommitLog final EdgeSerialization commitLogSerialization,
                                 @PermanentStorage final EdgeSerialization permanentSerialization,
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
                } );


        Observable<MarkedEdge> permanent =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesFromSourceStorage" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return permanentSerialization.getEdgesFromSource( scope, edgeType );
                    }
                } );

        return OrderedMerge
                .orderedMerge( EdgeComparator.INSTANCE, cassandraConfig.getScanPageSize() / 2, commitLog, permanent )
                .distinctUntilChanged();
    }


    public Observable<MarkedEdge> getEdgesFromSourceByTargetType( final OrganizationScope scope,
                                                                  final SearchByIdType edgeType ) {

        Observable<MarkedEdge> commitLog =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesFromSourceByTargetTypeCommitLog" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return commitLogSerialization.getEdgesFromSourceByTargetType( scope, edgeType );
                    }
                } );


        Observable<MarkedEdge> permanent =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesFromSourceByTargetTypeStorage" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return permanentSerialization.getEdgesFromSourceByTargetType( scope, edgeType );
                    }
                } );

        return OrderedMerge
                .orderedMerge( EdgeComparator.INSTANCE, cassandraConfig.getScanPageSize() / 2, commitLog, permanent )
                .distinctUntilChanged();
    }


    public Observable<MarkedEdge> getEdgesToTarget( final OrganizationScope scope, final SearchByEdgeType edgeType ) {
        Observable<MarkedEdge> commitLog =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesToTargetCommitLog" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return commitLogSerialization.getEdgesToTarget( scope, edgeType );
                    }
                } );


        Observable<MarkedEdge> permanent =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesToTargetTypeStorage" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return permanentSerialization.getEdgesToTarget( scope, edgeType );
                    }
                } );

        return OrderedMerge
                .orderedMerge( EdgeComparator.INSTANCE, cassandraConfig.getScanPageSize() / 2, commitLog, permanent )
                .distinctUntilChanged();
    }


    public Observable<MarkedEdge> getEdgesToTargetBySourceType( final OrganizationScope scope,
                                                                final SearchByIdType edgeType ) {
        Observable<MarkedEdge> commitLog =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesToTargetBySourceTypeCommitLog" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return commitLogSerialization.getEdgesToTargetBySourceType( scope, edgeType );
                    }
                } );


        Observable<MarkedEdge> permanent =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgesToTargetBySourceTypeStorage" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return permanentSerialization.getEdgesToTargetBySourceType( scope, edgeType );
                    }
                } );

        return OrderedMerge
                .orderedMerge( EdgeComparator.INSTANCE, cassandraConfig.getScanPageSize() / 2, commitLog, permanent )
                .distinctUntilChanged();
    }


    @Override
    public Observable<MarkedEdge> getEdgeVersions( final OrganizationScope scope, final SearchByEdge search ) {
        Observable<MarkedEdge> commitLog =
                Observable.create( new ObservableIterator<MarkedEdge>( "ggetEdgeVersionsCommitLog" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return commitLogSerialization.getEdgeVersions( scope, search );
                    }
                } );


        Observable<MarkedEdge> permanent =
                Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeVersionsStorage" ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return permanentSerialization.getEdgeVersions( scope, search );
                    }
                } );

        return OrderedMerge
                .orderedMerge( EdgeComparator.INSTANCE, cassandraConfig.getScanPageSize() / 2, commitLog, permanent )
                .distinctUntilChanged();
    }


    private static final class EdgeComparator implements Comparator<MarkedEdge> {

        private static final EdgeComparator INSTANCE = new EdgeComparator();


        @Override
        public int compare( final MarkedEdge o1, final MarkedEdge o2 ) {

            int compare = o1.getSourceNode().compareTo( o2.getSourceNode() );

            if ( compare != 0 ) {
                return compare;
            }

            compare = o1.getType().compareTo( o2.getType() );

            if ( compare != 0 ) {
                return compare;
            }

            compare = o1.getTargetNode().compareTo( o2.getTargetNode() );

            if ( compare != 0 ) {
                return compare;
            }

            if ( o1.isDeleted() ) {
                //if o2 is deleted it's returned
                if ( o2.isDeleted() ) {
                    return 0;
                }

                //o2 is not deleted, so it should be "less" to be emitted first
                return -1;
            }

            //o2 is deleted and o1 is not
            if ( o2.isDeleted() ) {
                return 1;
            }

            return 0;
        }
    }
}
