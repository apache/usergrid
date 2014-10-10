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
import java.util.logging.Level;

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.EntityRef;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
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

                Observable<Edge> edges = gm.loadEdgesFromSource( new SimpleSearchByEdgeType( 
                        fromEntityId, edgeType, Long.MAX_VALUE, 
                        SearchByEdgeType.Order.DESCENDING, null ));

                edges.forEach( new Action1<Edge>() {

                    @Override
                    public void call( Edge edge ) {

                        if ( CpNamingUtils.isCollectionEdgeType( edge.getType() )) {

                            String collName = CpNamingUtils.getCollectionName( edgeType );
                            String memberType = edge.getTargetNode().getType();

                            CollectionScope collScope = new CollectionScopeImpl(
                                applicationScope.getApplication(),
                                applicationScope.getApplication(),
                                CpNamingUtils.getCollectionScopeNameFromEntityType( start.getType()));
                            EntityCollectionManager collMgr = 
                                em.getManagerCache().getEntityCollectionManager(collScope);

                            org.apache.usergrid.persistence.model.entity.Entity collEntity = 
                                collMgr.load( edge.getSourceNode() ).toBlockingObservable().last();

                            if (collEntity == null) {
                                logger.warn("(Empty collection?) Failed to load collection entity "
                                        + "{}:{} from scope\n   app {}\n   owner {}\n   name {}",
                                        new Object[]{
                                            edge.getSourceNode().getType(), 
                                            edge.getSourceNode().getUuid(),
                                            collScope.getApplication(),
                                            collScope.getOwner(),
                                            collScope.getName()
                                        });
                                return;
                            }

                            CollectionScope memberScope = new CollectionScopeImpl(
                                applicationScope.getApplication(),
                                applicationScope.getApplication(),
                                CpNamingUtils.getCollectionScopeNameFromEntityType( memberType ));
                            EntityCollectionManager memberMgr = 
                                em.getManagerCache().getEntityCollectionManager(memberScope);

                            org.apache.usergrid.persistence.model.entity.Entity memberEntity = 
                                memberMgr.load( edge.getTargetNode()).toBlockingObservable().last();

                            if (memberEntity == null) {
                                logger.warn("(Empty collection?) Failed to load member entity "
                                        + "{}:{} from scope\n   app {}\n   owner {}\n   name {}",
                                        new Object[]{
                                            edge.getTargetNode().getType(), 
                                            edge.getTargetNode().getUuid(),
                                            memberScope.getApplication(),
                                            memberScope.getOwner(),
                                            memberScope.getName()
                                        });
                                return;
                            }

                            visitor.visitCollectionEntry( 
                                    memberMgr, collName, collEntity, memberEntity );

                            // recursion
                            if ( !stack.contains( memberEntity.getId() )) {
                                stack.push( memberEntity.getId() );
                                doWalkCollections( em, memberEntity.getId(), visitor, stack );
                                stack.pop(); 
                            }


                        } else if ( CpNamingUtils.isConnectionEdgeType( edge.getType() )) {

                            String connType = CpNamingUtils.getConnectionType( edgeType );
                            String targetEntityType = edge.getTargetNode().getType();
                            String sourceEntityType = start.getType();

                            CollectionScope sourceScope = new CollectionScopeImpl(
                                applicationScope.getApplication(),
                                applicationScope.getApplication(),
                                CpNamingUtils.getCollectionScopeNameFromEntityType(sourceEntityType));
                            EntityCollectionManager sourceEcm = 
                                em.getManagerCache().getEntityCollectionManager(sourceScope);

                            org.apache.usergrid.persistence.model.entity.Entity sourceEntity = 
                                sourceEcm.load( edge.getSourceNode() ).toBlockingObservable().last();

                            if (sourceEntity == null) {
                                logger.warn("(Empty connection?) Failed to load source entity "
                                        + "{}:{} from scope\n   app {}\n   owner {}\n   name {}", 
                                        new Object[]{
                                            edge.getSourceNode().getType(), 
                                            edge.getSourceNode().getUuid(),
                                            sourceScope.getApplication(),
                                            sourceScope.getOwner(),
                                            sourceScope.getName()
                                        });
                                return;
                            }

                            CollectionScope targetScope = new CollectionScopeImpl(
                                applicationScope.getApplication(),
                                applicationScope.getApplication(),
                                CpNamingUtils.getCollectionScopeNameFromEntityType(targetEntityType));
                            EntityCollectionManager targetEcm = 
                                em.getManagerCache().getEntityCollectionManager(targetScope);

                            org.apache.usergrid.persistence.model.entity.Entity targetEntity = 
                                targetEcm.load( edge.getTargetNode() ).toBlockingObservable().last();

                            if (targetEntity == null) {
                                logger.warn("(Empty connection?) Failed to load target entity "
                                        + "{}:{} from scope\n   app {}\n   owner {}\n   name {}",
                                        new Object[]{
                                            edge.getTargetNode().getType(), 
                                            edge.getTargetNode().getUuid(),
                                            targetScope.getApplication(),
                                            targetScope.getOwner(),
                                            targetScope.getName()
                                        });
                                return;
                            }

                            visitor.visitConnectionEntry( 
                                    targetEcm, connType, sourceEntity, targetEntity );

                            // recursion
                            if ( !stack.contains( targetEntity.getId() )) {
                                stack.push( targetEntity.getId() );
                                doWalkCollections( em, targetEntity.getId(), visitor, stack );
                                stack.pop(); 
                            }
                        }
                    }

                }); // end foreach on edges

            }

        }); // end foreach on edgeTypes

    }
    
}
