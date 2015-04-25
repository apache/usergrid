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

package org.apache.usergrid.corepersistence.pipeline.read.graph;


import org.apache.usergrid.corepersistence.pipeline.read.AbstractFilter;
import org.apache.usergrid.corepersistence.pipeline.read.TraverseFilter;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;


/**
 * Command for reading graph edges
 */
public abstract class AbstractReadGraphFilter extends AbstractFilter<Id, Edge>
    implements TraverseFilter {

    private final GraphManagerFactory graphManagerFactory;


    /**
     * Create a new instance of our command
     * @param graphManagerFactory
     */
    public AbstractReadGraphFilter( final GraphManagerFactory graphManagerFactory ) {
        this.graphManagerFactory = graphManagerFactory;
    }


    @Override
    public Observable<Id> call( final Observable<Id> observable ) {

        //get the graph manager
        final GraphManager graphManager = graphManagerFactory.createEdgeManager( applicationScope );

        //set our our constant state
        final Optional<Edge> startFromCursor = getCursor();

        final String edgeName = getEdgeTypeName();


        //return all ids that are emitted from this edge
        return observable.flatMap( new Func1<Id, Observable<Id>>() {

            @Override
            public Observable<Id> call( final Id id ) {

                final SimpleSearchByEdgeType search = new SimpleSearchByEdgeType(id,edgeName, Long.MAX_VALUE,
                    SearchByEdgeType.Order.DESCENDING, startFromCursor   );

                /**
                 * TODO, pass a message with pointers to our cursor values to be generated later
                 */
                return graphManager.loadEdgesFromSource( search ).doOnNext( edge -> setCursor( edge ) ).map(
                    edge -> edge.getTargetNode() );
            }
        } );
    }


    @Override
    protected Class<Edge> getCursorClass() {
        return Edge.class;
    }



    /**
     * Get the edge type name we should use when traversing
     * @return
     */
    protected abstract String getEdgeTypeName();
}
