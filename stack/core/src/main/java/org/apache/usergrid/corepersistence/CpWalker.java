/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.corepersistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Takes a visitor to all collections and entities.
 */
public class CpWalker {

    private static final Logger logger = LoggerFactory.getLogger( CpWalker.class );



    /**
     * Wait the set amount of time between successive writes.
     * @param
     */
    public CpWalker(){

    }


    public void walkCollections(final CpEntityManager em, final EntityRef start,
        String collectionName, boolean reverse, final CpVisitor visitor) throws Exception {

        doWalkCollections(
            em, collectionName, reverse, new SimpleId( start.getUuid(), start.getType() ), visitor );
    }


    private void doWalkCollections(
            final CpEntityManager em,
            final String collectionName,
            final boolean reverse,
            final Id applicationId,
            final CpVisitor visitor ) {

        final ApplicationScope applicationScope = em.getApplicationScope();

        final GraphManager gm = em.getManagerCache().getGraphManager( applicationScope );

        logger.debug( "Loading edges types from {}:{}\n   scope {}:{}",
            new Object[] {
                applicationId.getType(),
                applicationId.getUuid(),
                applicationScope.getApplication().getType(),
                applicationScope.getApplication().getUuid()
            } );

        final SearchByEdgeType.Order order;
        if ( reverse ) {
            order = SearchByEdgeType.Order.ASCENDING;
        } else {
            order = SearchByEdgeType.Order.DESCENDING;
        }

        final String edgeType;
        if ( collectionName == null ) {
            // only search edge types that end with collections suffix
            edgeType = CpNamingUtils.EDGE_COLL_SUFFIX;

        } else {
            // only search edges to one collection
            edgeType = CpNamingUtils.getEdgeTypeFromCollectionName( collectionName );
        }

        Observable<String> edgeTypes = gm.getEdgeTypesFromSource(
            new SimpleSearchEdgeType( applicationId, edgeType, null ) );

        edgeTypes.flatMap( new Func1<String, Observable<Edge>>() {
            @Override
            public Observable<Edge> call( final String edgeType ) {

                logger.debug( "Loading edges of type {} from node {}", edgeType, applicationId );

                return gm.loadEdgesFromSource(  new SimpleSearchByEdgeType(
                    applicationId, edgeType, Long.MAX_VALUE, order , null ) );

            }

        } ).parallel( new Func1<Observable<Edge>, Observable<Edge>>() {

            @Override
            public Observable<Edge> call( final Observable<Edge> edgeObservable ) { // process edges in parallel
                return edgeObservable.doOnNext( new Action1<Edge>() { // visit and update then entity

                    @Override
                    public void call( Edge edge ) {

                        logger.info( "Re-indexing edge {}", edge );

                        EntityRef targetNodeEntityRef = new SimpleEntityRef(
                            edge.getTargetNode().getType(),
                            edge.getTargetNode().getUuid() );

                        Entity entity;
                        try {
                            entity = em.get( targetNodeEntityRef );
                        }
                        catch ( Exception ex ) {
                            logger.error( "Error getting sourceEntity {}:{}, continuing",
                                targetNodeEntityRef.getType(),
                                targetNodeEntityRef.getUuid() );
                            return;
                        }

                        String collName = CpNamingUtils.getCollectionName( edge.getType() );
                        visitor.visitCollectionEntry( em, collName, entity );
                    }
                } );
            }
        }, Schedulers.io() )

        // wait for it to complete
        .toBlocking().lastOrDefault( null ); // end foreach on edges
    }
}
