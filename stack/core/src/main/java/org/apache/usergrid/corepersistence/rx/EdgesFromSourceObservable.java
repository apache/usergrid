/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.rx;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;
import rx.functions.Func1;


/**
 * Emits the id of all nodes that are target nodes for the given source node
 */
public class EdgesFromSourceObservable {

    private static final Logger logger = LoggerFactory.getLogger( EdgesFromSourceObservable.class );


    /**
     * Get all edges from the source
     */
    public static Observable<Edge> edgesFromSource( final ApplicationScope applicationScope, final Id sourceNode,
                                                    final GraphManager gm ) {
        final Id applicationId = applicationScope.getApplication();
        //only search edge types that start with collections


        Observable<String> edgeTypes = gm.getEdgeTypesFromSource( new SimpleSearchEdgeType( sourceNode, null, null ) );

        return edgeTypes.flatMap( new Func1<String, Observable<Edge>>() {
            @Override
            public Observable<Edge> call( final String edgeType ) {

                logger.debug( "Loading edges of edgeType {} from {}\n   scope {}",
                        new Object[] { edgeType, sourceNode, applicationScope } );

                return gm.loadEdgesFromSource( new SimpleSearchByEdgeType( applicationId, edgeType, Long.MAX_VALUE,
                        SearchByEdgeType.Order.DESCENDING, null ) );
            }
        } );
    }
}
