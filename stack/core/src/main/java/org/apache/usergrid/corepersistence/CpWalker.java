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

import java.util.Stack;

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityRef;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;


/**
 * Takes a visitor to all collections and entities.
 */
public class CpWalker {

    private static final Logger logger = LoggerFactory.getLogger( CpWalker.class );

    private long writeDelayMs = 100;


    public void walkCollections( final CpEntityManager em, final EntityRef start, 
            final CpVisitor visitor ) throws Exception {

        Stack stack = new Stack();
        Id appId = new SimpleId( em.getApplicationId(), TYPE_APPLICATION );
        stack.push( appId );

        doWalkCollections(em, new SimpleId(start.getUuid(), start.getType()), visitor, new Stack());
    }


    private void doWalkCollections( final CpEntityManager em, final Id start, 
            final CpVisitor visitor, final Stack stack ) {

        final Id fromEntityId = new SimpleId( start.getUuid(), start.getType() );

        final ApplicationScope applicationScope = em.getApplicationScope();

        final GraphManager gm = em.getManagerCache().getGraphManager(applicationScope);

        logger.debug("Loading edges types from {}:{}\n   scope {}:{}",
            new Object[] { start.getType(), start.getUuid(),
                applicationScope.getApplication().getType(),
                applicationScope.getApplication().getUuid()
            });

        Observable<String> edgeTypes = gm.getEdgeTypesFromSource( 
                new SimpleSearchEdgeType( fromEntityId, null , null ));

        edgeTypes.forEach( new Action1<String>() {

            @Override
            public void call( final String edgeType ) {

                try {
                    Thread.sleep( writeDelayMs );
                } catch ( InterruptedException ignored ) {}

                logger.debug("Loading edges of edgeType {} from {}:{}\n   scope {}:{}",
                    new Object[] { edgeType, start.getType(), start.getUuid(),
                        applicationScope.getApplication().getType(),
                        applicationScope.getApplication().getUuid()
                });


                Observable<String> edgeTypes = gm.getEdgeTypesFromSource( new SimpleSearchEdgeType(fromEntityId, CpNamingUtils.EDGE_COLL_SUFFIX, null ));



                Observable<Edge> edges = gm.loadEdgesFromSource( new SimpleSearchByEdgeType( 
                        fromEntityId, edgeType, Long.MAX_VALUE, 
                        SearchByEdgeType.Order.DESCENDING, null ));

                edges.forEach( new Action1<Edge>() {

                    @Override
                    public void call( Edge edge ) {

                        EntityRef sourceEntityRef = new SimpleEntityRef( 
                            edge.getSourceNode().getType(), edge.getSourceNode().getUuid());
                        Entity sourceEntity;
                        try {
                            sourceEntity = em.get( sourceEntityRef );
                        } catch (Exception ex) {
                            logger.error( "Error getting sourceEntity {}:{}, continuing", 
                                    sourceEntityRef.getType(), sourceEntityRef.getUuid());
                            return;
                        }

                        EntityRef targetEntityRef = new SimpleEntityRef( 
                            edge.getTargetNode().getType(), edge.getTargetNode().getUuid());
                        Entity targetEntity;
                        try {
                            targetEntity = em.get( targetEntityRef );
                        } catch (Exception ex) {
                            logger.error( "Error getting sourceEntity {}:{}, continuing", 
                                    sourceEntityRef.getType(), sourceEntityRef.getUuid());
                            return;
                        }
                            
                        if ( CpNamingUtils.isCollectionEdgeType( edge.getType() )) {

                            String collName = CpNamingUtils.getCollectionName( edgeType );

                            visitor.visitCollectionEntry( 
                                    em, collName, sourceEntity, targetEntity );

                            // recursion
                            if ( !stack.contains( targetEntity.getUuid() )) {
                                stack.push( targetEntity.getUuid() );
                                doWalkCollections( em, edge.getSourceNode(), visitor, stack );
                                stack.pop(); 
                            }

                        } else {

                            String collName = CpNamingUtils.getConnectionType(edgeType);

                            visitor.visitConnectionEntry( 
                                    em, collName, sourceEntity, targetEntity );

                            // recursion
                            if ( !stack.contains( targetEntity.getUuid() )) {
                                stack.push( targetEntity.getUuid() );
                                doWalkCollections( em, edge.getTargetNode(), visitor, stack );
                                stack.pop(); 
                            }
                        }
                    }

                }); // end foreach on edges

            }

        }); // end foreach on edgeTypes

    }
    
}
