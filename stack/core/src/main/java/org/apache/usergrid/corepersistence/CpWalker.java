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

import com.google.common.base.Optional;

import rx.Observable;
import rx.schedulers.Schedulers;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.getNameFromEdgeType;


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

        if(start != null) {
            doWalkCollections(
                em, collectionName, reverse, new SimpleId(start.getUuid(), start.getType()), visitor);
        }
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
            edgeType = CpNamingUtils.EDGE_COLL_PREFIX;

        } else {
            // only search edges to one collection
            edgeType = CpNamingUtils.getEdgeTypeFromCollectionName( collectionName );
        }

        Observable<Edge> edges = gm.getEdgeTypesFromSource(
                    new SimpleSearchEdgeType( applicationId, edgeType, null ) ).flatMap( emittedEdgeType -> {

            logger.debug( "Loading edges of type {} from node {}", edgeType, applicationId );

            return gm.loadEdgesFromSource(
                new SimpleSearchByEdgeType( applicationId, emittedEdgeType, Long.MAX_VALUE, order, Optional.absent() ) );
        } ).flatMap( edge -> {
            //run each edge through it's own scheduler, up to 100 at a time
            return Observable.just( edge ).doOnNext( edgeValue -> {
                logger.info( "Re-indexing edge {}", edgeValue );

                EntityRef targetNodeEntityRef =
                    new SimpleEntityRef( edgeValue.getTargetNode().getType(), edgeValue.getTargetNode().getUuid() );

                Entity entity;
                try {
                    entity = em.get( targetNodeEntityRef );
                }
                catch ( Exception ex ) {
                    logger.error( "Error getting sourceEntity {}:{}, continuing", targetNodeEntityRef.getType(),
                        targetNodeEntityRef.getUuid() );
                    return;
                }
                if ( entity == null ) {
                    return;
                }
                String collName = getNameFromEdgeType( edgeValue.getType() );
                visitor.visitCollectionEntry( em, collName, entity );
            } ).subscribeOn( Schedulers.io() );
        }, 100 );

        // wait for it to complete
        edges.toBlocking().lastOrDefault( null ); // end foreach on edges
    }
}
