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


import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;

import rx.Observable;


/**
 * Reads edges from both the commit log and the storage.  Edges from the commit log get priority
 */
public interface MergedEdgeReader {


    /**
     * Get the edges from the source for both the commit log and the permanent storage.  Merge them into a single observable
     * and remove duplicates.  Commit log takes priority
     *
     *
     *
     * @param scope
     * @param edgeType
     * @return
     */
    public Observable<MarkedEdge> getEdgesFromSource( final OrganizationScope scope, final SearchByEdgeType edgeType );


    /**
     * Get edges from the source by target time for both the commit log and permanent storage.  Merge them into a single
     * observable without duplicates. Commit log takes priority
     * @param scope
     * @param edgeType
     * @return
     */
    public Observable<MarkedEdge> getEdgesFromSourceByTargetType( final OrganizationScope scope,
                                                                  final SearchByIdType edgeType );



    /**
     * Get the edges to the target for both the commit log and the permanent storage.  Merge them into a single observable
     * and remove duplicates.  Commit log takes priority
     *
     * @param scope
     * @param edgeType
     * @return
     */
    public Observable<MarkedEdge> getEdgesToTarget( final OrganizationScope scope, final SearchByEdgeType edgeType );

    /**
        * Get edges from the source by target time for both the commit log and permanent storage.  Merge them into a single
        * observable without duplicates. Commit log takes priority
        * @param scope
        * @param edgeType
        * @return
        */
    public Observable<MarkedEdge> getEdgesToTargetBySourceType( final OrganizationScope scope,
                                                                final SearchByIdType edgeType );

    /**
     * Get a merged view of the edge versions from both the commit log and the long term storage
     * @param scope
     * @param search
     * @return
     */
    public Observable<MarkedEdge> getEdgeVersions( OrganizationScope scope, SearchByEdge search );

}
