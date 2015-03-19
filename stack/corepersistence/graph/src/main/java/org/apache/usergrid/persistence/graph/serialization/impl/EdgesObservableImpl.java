/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.graph.serialization.impl;

import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.graph.serialization.EdgesObservable;
import org.apache.usergrid.persistence.model.entity.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

/**
 * Emits the edges that are edges from the specified source node
 */
public class EdgesObservableImpl implements EdgesObservable {

    private static final Logger logger = LoggerFactory.getLogger(EdgesObservableImpl.class);
    public EdgesObservableImpl(){

    }

    /**
     * Get all edges from the source
     */
    @Override
    public  Observable<Edge> edgesFromSource( final GraphManager gm, final Id sourceNode){
        Observable<String> edgeTypes = gm.getEdgeTypesFromSource( new SimpleSearchEdgeType( sourceNode, null, null ) );

        return edgeTypes.flatMap( new Func1<String, Observable<Edge>>() {
            @Override
            public Observable<Edge> call( final String edgeType ) {

                logger.debug( "Loading edges of edgeType {} from {}", edgeType, sourceNode );

                return gm.loadEdgesFromSource( new SimpleSearchByEdgeType( sourceNode, edgeType, Long.MAX_VALUE,
                    SearchByEdgeType.Order.DESCENDING, null ) );
            }
        } );
    }
    /**
     * Get all edges from the source
     */
    @Override
    public  Observable<Edge> edgesToTarget(final GraphManager gm,  final Id targetNode) {
        Observable<String> edgeTypes = gm.getEdgeTypesToTarget( new SimpleSearchEdgeType( targetNode, null, null ) );

        return edgeTypes.flatMap( new Func1<String, Observable<Edge>>() {
            @Override
            public Observable<Edge> call( final String edgeType ) {

                logger.debug( "Loading edges of edgeType {} to {}", edgeType, targetNode);

                return gm.loadEdgesToTarget( new SimpleSearchByEdgeType( targetNode, edgeType, Long.MAX_VALUE,
                    SearchByEdgeType.Order.DESCENDING, null ) );
            }
        } );
    }
}
