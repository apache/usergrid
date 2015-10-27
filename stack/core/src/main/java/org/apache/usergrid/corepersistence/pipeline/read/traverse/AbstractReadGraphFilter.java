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



/**
 * Command for reading graph edges
 */
public abstract class AbstractReadGraphFilter extends AbstractPathFilter<Id, Id, MarkedEdge> {

    private static final Logger logger = LoggerFactory.getLogger( AbstractReadGraphFilter.class );

    private final GraphManagerFactory graphManagerFactory;
    private final RxTaskScheduler rxTaskScheduler;
    private final EventBuilder eventBuilder;
    private final AsyncEventService asyncEventService;


    /**
     * Create a new instance of our command
     */
    public AbstractReadGraphFilter( final GraphManagerFactory graphManagerFactory,
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
            return graphManager.loadEdgesFromSource( search ).filter(markedEdge -> {

                final boolean isDeleted = markedEdge.isDeleted();
                final boolean isSourceNodeDeleted = markedEdge.isSourceNodeDelete();
                final boolean isTargetNodeDelete = markedEdge.isTargetNodeDeleted();


                if (isDeleted) {

                    logger.trace("Edge {} is deleted, deleting the edge", markedEdge);
                    final Observable<IndexOperationMessage> indexMessageObservable = eventBuilder.buildDeleteEdge(applicationScope, markedEdge);

                    indexMessageObservable
                        .compose(applyCollector())
                        .subscribeOn(rxTaskScheduler.getAsyncIOScheduler())
                        .subscribe();

                }

                if (isSourceNodeDeleted) {

                    final Id sourceNodeId = markedEdge.getSourceNode();
                    logger.trace("Edge {} has a deleted source node, deleting the entity for id {}", markedEdge, sourceNodeId);

                    final EventBuilderImpl.EntityDeleteResults
                        entityDeleteResults = eventBuilder.buildEntityDelete(applicationScope, sourceNodeId);

                    entityDeleteResults.getIndexObservable()
                        .compose(applyCollector())
                        .subscribeOn(rxTaskScheduler.getAsyncIOScheduler())
                        .subscribe();

                    Observable.merge(entityDeleteResults.getEntitiesDeleted(),
                        entityDeleteResults.getCompactedNode())
                        .subscribeOn(rxTaskScheduler.getAsyncIOScheduler()).
                        subscribe();

                }

                if (isTargetNodeDelete) {

                    final Id targetNodeId = markedEdge.getTargetNode();
                    logger.trace("Edge {} has a deleted target node, deleting the entity for id {}", markedEdge, targetNodeId);

                    final EventBuilderImpl.EntityDeleteResults
                        entityDeleteResults = eventBuilder.buildEntityDelete(applicationScope, targetNodeId);

                    entityDeleteResults.getIndexObservable()
                        .compose(applyCollector())
                        .subscribeOn(rxTaskScheduler.getAsyncIOScheduler())
                        .subscribe();

                    Observable.merge(entityDeleteResults.getEntitiesDeleted(),
                        entityDeleteResults.getCompactedNode())
                        .subscribeOn(rxTaskScheduler.getAsyncIOScheduler()).
                        subscribe();

                }


                //filter if any of them are marked
                return !isDeleted && !isSourceNodeDeleted && !isTargetNodeDelete;


            })
                //set the edge state for cursors
                .doOnNext( edge -> {
                    logger.trace( "Seeking over edge {}", edge );
                    edgeCursorState.update( edge );
                } )

                    //map our id from the target edge  and set our cursor every edge we traverse
                .map( edge -> createFilterResult( edge.getTargetNode(), edgeCursorState.getCursorEdge(),
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

    private Observable.Transformer<IndexOperationMessage, IndexOperationMessage> applyCollector() {

        return observable -> observable
            .filter((IndexOperationMessage msg) -> !msg.isEmpty())
            .collect(() -> new IndexOperationMessage(), (collector, single) -> collector.ingest(single))
            .doOnNext(indexOperation -> {
                asyncEventService.queueIndexOperationMessage(indexOperation);
            });

    }

}
