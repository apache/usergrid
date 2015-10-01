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

package org.apache.usergrid.corepersistence.pipeline.read.traverse;


import org.apache.usergrid.corepersistence.pipeline.cursor.CursorSerializer;
import org.apache.usergrid.corepersistence.pipeline.read.AbstractPathFilter;
import org.apache.usergrid.corepersistence.pipeline.read.EdgePath;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
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
public abstract class AbstractReadGraphFilter extends AbstractPathFilter<Id, Id, Edge> {

    private final GraphManagerFactory graphManagerFactory;


    /**
     * Create a new instance of our command
     */
    public AbstractReadGraphFilter( final GraphManagerFactory graphManagerFactory ) {
        this.graphManagerFactory = graphManagerFactory;
    }


    @Override
    public Observable<FilterResult<Id>> call( final Observable<FilterResult<Id>> previousIds ) {


        //get the graph manager
        final GraphManager graphManager =
            graphManagerFactory.createEdgeManager( pipelineContext.getApplicationScope() );


        final String edgeName = getEdgeTypeName();
        final EdgeState edgeCursorState = new EdgeState();


        //return all ids that are emitted from this edge
        return previousIds.flatMap( previousFilterValue -> {

            //set our our constant state
            final Optional<Edge> startFromCursor = getSeekValue();
            final Id id = previousFilterValue.getValue();


            final SimpleSearchByEdgeType search =
                new SimpleSearchByEdgeType( id, edgeName, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                    startFromCursor );

            /**
             * TODO, pass a message with pointers to our cursor values to be generated later
             */
            return graphManager.loadEdgesFromSource( search )
                //set the edge state for cursors
                .doOnNext( edge -> edgeCursorState.update( edge ) )

                    //map our id from the target edge  and set our cursor every edge we traverse
                .map( edge -> createFilterResult( edge.getTargetNode(), edgeCursorState.getCursorEdge(),
                    previousFilterValue.getPath() ) );
        } );
    }


    @Override
    protected FilterResult<Id> createFilterResult( final Id emit, final Edge cursorValue,
                                                   final Optional<EdgePath> parent ) {

        //if it's our first pass, there's no cursor to generate
        if(cursorValue == null){
            return new FilterResult<>( emit, parent );
        }

        return super.createFilterResult( emit, cursorValue, parent );
    }


    @Override
    protected CursorSerializer<Edge> getCursorSerializer() {
        return EdgeCursorSerializer.INSTANCE;
    }


    /**
     * Get the edge type name we should use when traversing
     */
    protected abstract String getEdgeTypeName();


    /**
     * Wrapper class. Because edges seek > the last returned, we need to keep our n-1 value. This will be our cursor We
     * always try to seek to the same position as we ended.  Since we don't deal with a persistent read result, if we
     * seek to a value = to our last, we may skip data.
     */
    private final class EdgeState {

        private Edge cursorEdge = null;
        private Edge currentEdge = null;


        /**
         * Update the pointers
         */
        private void update( final Edge newEdge ) {
            cursorEdge = currentEdge;
            currentEdge = newEdge;
        }


        /**
         * Get the edge to use in cursors for resume
         */
        private Edge getCursorEdge() {
            return cursorEdge;
        }
    }
}
