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


import org.apache.usergrid.corepersistence.pipeline.cursor.CursorSerializer;
import org.apache.usergrid.corepersistence.pipeline.read.AbstractSeekingFilter;
import org.apache.usergrid.corepersistence.pipeline.read.Filter;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;

import rx.Observable;


/**
 * Command for reading graph edges
 */
public abstract class AbstractReadGraphFilter extends AbstractSeekingFilter<Id, Id, Edge> implements Filter<Id, Id> {

    private final GraphManagerFactory graphManagerFactory;


    /**
     * Create a new instance of our command
     */
    public AbstractReadGraphFilter( final GraphManagerFactory graphManagerFactory ) {
        this.graphManagerFactory = graphManagerFactory;
    }


    @Override
    public Observable<Id> call( final Observable<Id> observable ) {

        //get the graph manager
        final GraphManager graphManager =
            graphManagerFactory.createEdgeManager( pipelineContext.getApplicationScope() );


        final String edgeName = getEdgeTypeName();


        //return all ids that are emitted from this edge
        return observable.flatMap( id -> {

            //set our our constant state
            final Optional<Edge> startFromCursor = getSeekValue();


            final SimpleSearchByEdgeType search =
                new SimpleSearchByEdgeType( id, edgeName, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                    startFromCursor );

            /**
             * TODO, pass a message with pointers to our cursor values to be generated later
             */
            return graphManager.loadEdgesFromSource( search )
                //set our cursor every edge we traverse
                .doOnNext( edge -> setCursor( edge ) )
                    //map our id from the target edge
                .map( edge -> edge.getTargetNode() );
        } );
    }


    @Override
    protected CursorSerializer<Edge> getCursorSerializer() {
        return EdgeCursorSerializer.INSTANCE;
    }


    /**
     * Get the edge type name we should use when traversing
     */
    protected abstract String getEdgeTypeName();
}
