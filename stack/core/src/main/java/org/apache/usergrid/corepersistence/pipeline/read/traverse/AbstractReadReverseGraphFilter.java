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


import org.apache.usergrid.corepersistence.asyncevents.AsyncEventQueueType;
import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.asyncevents.EventBuilder;
import org.apache.usergrid.corepersistence.asyncevents.EventBuilderImpl;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.persistence.index.impl.IndexOperationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.pipeline.cursor.CursorSerializer;
import org.apache.usergrid.corepersistence.pipeline.read.AbstractPathFilter;
import org.apache.usergrid.corepersistence.pipeline.read.EdgePath;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;

import rx.Observable;
import rx.functions.Func1;


/**
 * Command for reading graph edges in reverse order.
 */
public abstract class AbstractReadReverseGraphFilter extends AbstractPathFilter<Id, Id, MarkedEdge> {

    private static final Logger logger = LoggerFactory.getLogger( AbstractReadGraphFilter.class );

    private final GraphManagerFactory graphManagerFactory;
    private final RxTaskScheduler rxTaskScheduler;
    private final EventBuilder eventBuilder;
    private final AsyncEventService asyncEventService;


    /**
     * Create a new instance of our command
     */
    public AbstractReadReverseGraphFilter( final GraphManagerFactory graphManagerFactory,
                                    final RxTaskScheduler rxTaskScheduler,
                                    final EventBuilder eventBuilder,
                                    final AsyncEventService asyncEventService ) {
        this.graphManagerFactory = graphManagerFactory;
        this.rxTaskScheduler = rxTaskScheduler;
        this.eventBuilder = eventBuilder;
        this.asyncEventService = asyncEventService;
    }


    @Override
    public Observable<FilterResult<Id>> call( final Observable<FilterResult<Id>> previousIds ) {


        final ApplicationScope applicationScope = pipelineContext.getApplicationScope();

        //get the graph manager
        final GraphManager graphManager =
            graphManagerFactory.createEdgeManager( applicationScope );


        final String edgeName = getEdgeTypeName();
        final EdgeState edgeCursorState = new EdgeState();


        //return all ids that are emitted from this edge
        return previousIds.flatMap( previousFilterValue -> {

            //set our our constant state
            final Optional<MarkedEdge> startFromCursor = getSeekValue();
            final Id id = previousFilterValue.getValue();


            final Optional<Edge> typeWrapper = Optional.fromNullable(startFromCursor.orNull());

            /**
             * We do not want to filter.  This is intentional DO NOT REMOVE!!!
             *
             * We want to fire events on these edges if they exist, the delete was missed.
             */
            final SimpleSearchByEdgeType search =
                new SimpleSearchByEdgeType( id, edgeName, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                    typeWrapper, false );

            /**
             * TODO, pass a message with pointers to our cursor values to be generated later
             */
            return graphManager.loadEdgesToTarget( search ).filter(markedEdge -> {

                final boolean isDeleted = markedEdge.isDeleted();
                final boolean isSourceNodeDeleted = markedEdge.isSourceNodeDelete();
                final boolean isTargetNodeDelete = markedEdge.isTargetNodeDeleted();


                if (isDeleted) {

                    logger.info("Edge {} is deleted when seeking, deleting the edge", markedEdge);
                    final IndexOperationMessage indexOperationMessage = eventBuilder.buildDeleteEdge(applicationScope, markedEdge);
                    asyncEventService.queueIndexOperationMessage(indexOperationMessage, AsyncEventQueueType.DELETE);

                }

                if (isSourceNodeDeleted) {

                    final Id sourceNodeId = markedEdge.getSourceNode();
                    logger.info("Edge {} has a deleted source node, deleting the entity for id {}", markedEdge, sourceNodeId);

                    final IndexOperationMessage indexOperationMessage = eventBuilder.buildEntityDelete(applicationScope, sourceNodeId);
                    asyncEventService.queueIndexOperationMessage(indexOperationMessage, AsyncEventQueueType.DELETE);

                }

                if (isTargetNodeDelete) {

                    final Id targetNodeId = markedEdge.getTargetNode();
                    logger.info("Edge {} has a deleted target node, deleting the entity for id {}", markedEdge, targetNodeId);

                    final IndexOperationMessage indexOperationMessage = eventBuilder.buildEntityDelete(applicationScope, targetNodeId);
                    asyncEventService.queueIndexOperationMessage(indexOperationMessage, AsyncEventQueueType.DELETE);

                }


                //filter if any of them are marked
                return !isDeleted && !isSourceNodeDeleted && !isTargetNodeDelete;


            })  // any non-deleted edges should be de-duped here so the results are unique
                .distinct( new EdgeDistinctKey() )
                //set the edge state for cursors
                .doOnNext( edge -> {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Seeking over edge {}", edge);
                    }
                    edgeCursorState.update( edge );
                } )

                //map our id from the target edge  and set our cursor every edge we traverse
                .map( edge -> createFilterResult( edge.getSourceNode(), edgeCursorState.getCursorEdge(),
                    previousFilterValue.getPath() ) );
        } );
    }


    @Override
    protected FilterResult<Id> createFilterResult( final Id emit, final MarkedEdge cursorValue,
                                                   final Optional<EdgePath> parent ) {

        //if it's our first pass, there's no cursor to generate
        if(cursorValue == null){
            return new FilterResult<>( emit, parent );
        }

        return super.createFilterResult( emit, cursorValue, parent );
    }


    @Override
    protected CursorSerializer<MarkedEdge> getCursorSerializer() {
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

        private MarkedEdge cursorEdge = null;
        private MarkedEdge currentEdge = null;


        /**
         * Update the pointers
         */
        private void update( final MarkedEdge newEdge ) {
            cursorEdge = currentEdge;
            currentEdge = newEdge;
        }


        /**
         * Get the edge to use in cursors for resume
         */
        private MarkedEdge getCursorEdge() {
            return cursorEdge;
        }
    }

    private Observable.Transformer<IndexOperationMessage, IndexOperationMessage> applyCollector(AsyncEventQueueType queueType) {

        return observable -> observable
            .collect(() -> new IndexOperationMessage(), (collector, single) -> collector.ingest(single))
            .filter(msg -> !msg.isEmpty())
            .doOnNext(indexOperation -> {
                asyncEventService.queueIndexOperationMessage(indexOperation, queueType);
            });

    }

    /**
     *  Return a key that Rx can use for determining a distinct edge.  Build a string containing the UUID
     *  of the source and target nodes, with the type to ensure uniqueness rather than the int sum of the hash codes.
     *  Edge timestamp is specifically left out as edges with the same source,target,type but different timestamps
     *  are considered duplicates.
     */
    private class EdgeDistinctKey implements Func1<Edge,String> {

        @Override
        public String call(Edge edge) {

            return buildDistinctKey(edge.getSourceNode().getUuid().toString(), edge.getTargetNode().getUuid().toString(),
                edge.getType().toLowerCase());
        }
    }

    protected static String buildDistinctKey(final String sourceNode, final String targetNode, final String type){

        final String DISTINCT_KEY_SEPARATOR = ":";
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder
            .append(sourceNode)
            .append(DISTINCT_KEY_SEPARATOR)
            .append(targetNode)
            .append(DISTINCT_KEY_SEPARATOR)
            .append(type);

        return stringBuilder.toString();

    }

}
