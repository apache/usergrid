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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.pipeline.PipelineBuilderFactory;
import org.apache.usergrid.corepersistence.pipeline.read.ReadPipelineBuilder;
import org.apache.usergrid.corepersistence.pipeline.read.ResultsPage;
import org.apache.usergrid.corepersistence.results.ObservableQueryExecutor;
import org.apache.usergrid.corepersistence.util.CpEntityMapUtils;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.ConnectedEntityRef;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Query.Level;
import org.apache.usergrid.persistence.RelationManager;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.RoleRef;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.SimpleRoleRef;
import org.apache.usergrid.persistence.cassandra.ConnectionRefImpl;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.schema.CollectionInfo;
import org.apache.usergrid.utils.MapUtils;

import com.codahale.metrics.Timer;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import rx.Observable;
import rx.functions.Func1;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.createCollectionEdge;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.createCollectionSearchEdge;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.createConnectionSearchByEdge;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.createSearchEdgeFromSource;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.generateScopeFromSource;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.getNameFromEdgeType;
import static org.apache.usergrid.persistence.Schema.COLLECTION_ROLES;
import static org.apache.usergrid.persistence.Schema.PROPERTY_INACTIVITY;
import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.PROPERTY_TITLE;
import static org.apache.usergrid.persistence.Schema.TYPE_ENTITY;
import static org.apache.usergrid.persistence.Schema.TYPE_ROLE;
import static org.apache.usergrid.persistence.Schema.getDefaultSchema;
import static org.apache.usergrid.utils.ClassUtils.cast;
import static org.apache.usergrid.utils.InflectionUtils.singularize;
import static org.apache.usergrid.utils.MapUtils.addMapSet;


/**
 * Implement good-old Usergrid RelationManager with the new-fangled Core Persistence API.
 */
public class CpRelationManager implements RelationManager {

    private static final Logger logger = LoggerFactory.getLogger( CpRelationManager.class );

    private ManagerCache managerCache;
    private final PipelineBuilderFactory pipelineBuilderFactory;

    private EntityManager em;

    private UUID applicationId;

    private EntityRef headEntity;

    private org.apache.usergrid.persistence.model.entity.Entity cpHeadEntity;

    private final ApplicationScope applicationScope;

    private final AsyncEventService indexService;

    private MetricsFactory metricsFactory;
    private Timer updateCollectionTimer;


    public CpRelationManager( final MetricsFactory metricsFactory, final ManagerCache managerCache,
                              final PipelineBuilderFactory pipelineBuilderFactory, final AsyncEventService indexService,
                              final EntityManager em, final UUID applicationId, final EntityRef headEntity ) {


        Assert.notNull( em, "Entity manager cannot be null" );
        Assert.notNull( applicationId, "Application Id cannot be null" );
        Assert.notNull( headEntity, "Head entity cannot be null" );
        Assert.notNull( headEntity.getUuid(), "Head entity uuid cannot be null" );
        Assert.notNull( indexService, "indexService cannot be null" );

        // TODO: this assert should not be failing
        //Assert.notNull( indexBucketLocator, "indexBucketLocator cannot be null" );
        this.em = em;
        this.applicationId = applicationId;
        this.headEntity = headEntity;
        this.managerCache = managerCache;
        this.applicationScope = CpNamingUtils.getApplicationScope( applicationId );
        this.pipelineBuilderFactory = pipelineBuilderFactory;

        this.metricsFactory = metricsFactory;
        this.updateCollectionTimer =
            metricsFactory.getTimer( CpRelationManager.class, "relation.manager.es.update.collection" );

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Loading head entity {}:{} from app {}", new Object[] {
                headEntity.getType(), headEntity.getUuid(), applicationScope
            } );
        }

        Id entityId = new SimpleId( headEntity.getUuid(), headEntity.getType() );

        this.cpHeadEntity = ( ( CpEntityManager ) em ).load( entityId );

        // commented out because it is possible that CP entity has not been created yet
        Assert.notNull( cpHeadEntity, String
            .format( "cpHeadEntity cannot be null for entity id %s, app id %s", entityId.getUuid(), applicationId ) );

        this.indexService = indexService;
    }


    @Override
    public Set<String> getCollectionIndexes( String collectionName ) throws Exception {
        GraphManager gm = managerCache.getGraphManager( applicationScope );

        String edgeTypePrefix = CpNamingUtils.getEdgeTypeFromCollectionName(collectionName);

        logger.debug( "getCollectionIndexes(): Searching for edge type prefix {} to target {}:{}", new Object[] {
            edgeTypePrefix, cpHeadEntity.getId().getType(), cpHeadEntity.getId().getUuid()
        } );

        Observable<Set<String>> types =
            gm.getEdgeTypesFromSource( new SimpleSearchEdgeType( cpHeadEntity.getId(), edgeTypePrefix, null ) )
              .collect( () -> new HashSet<>(), ( set, type ) -> set.add( type ) );


        return types.toBlocking().last();
    }


    @Override
    public Map<String, Map<UUID, Set<String>>> getOwners() throws Exception {

        // TODO: do we need to restrict this to edges prefixed with owns?
        //Map<EntityRef, Set<String>> containerEntities = getContainers(-1, "owns", null);
        Map<EntityRef, Set<String>> containerEntities = getContainers();

        Map<String, Map<UUID, Set<String>>> owners = new LinkedHashMap<String, Map<UUID, Set<String>>>();

        for ( EntityRef owner : containerEntities.keySet() ) {
            Set<String> collections = containerEntities.get( owner );
            for ( String collection : collections ) {
                MapUtils.addMapMapSet( owners, owner.getType(), owner.getUuid(), collection );
            }
        }

        return owners;
    }


    private Map<EntityRef, Set<String>> getContainers() {
        return getContainers(-1, null, null);
    }


    /**
     * Gets containing collections and/or connections depending on the edge type you pass in
     *
     * @param limit Max number to return
     * @param edgeType Edge type, edge type prefix or null to allow any edge type
     * @param fromEntityType Only consider edges from entities of this type
     */
    Map<EntityRef, Set<String>> getContainers( final int limit, final String edgeType, final String fromEntityType ) {

        final GraphManager gm = managerCache.getGraphManager( applicationScope );

        Observable<Edge> edges =
            gm.getEdgeTypesToTarget( new SimpleSearchEdgeType( cpHeadEntity.getId(), edgeType, null ) )
              .flatMap(new Func1<String, Observable<Edge>>() {
                  @Override
                  public Observable<Edge> call(final String edgeType) {
                      return gm.loadEdgesToTarget(
                          new SimpleSearchByEdgeType(cpHeadEntity.getId(), edgeType, Long.MAX_VALUE,
                              SearchByEdgeType.Order.DESCENDING, Optional.<Edge>absent()));
                  }
              });

        //if our limit is set, take them.  Note this logic is still borked, we can't possibly fit everything in memmory
        if ( limit > -1 ) {
            edges = edges.take( limit );
        }


        return edges.collect( () -> new LinkedHashMap<EntityRef, Set<String>>(), ( entityRefSetMap, edge ) -> {
            if ( fromEntityType != null && !fromEntityType.equals( edge.getSourceNode().getType() ) ) {
                logger.debug( "Ignoring edge from entity type {}", edge.getSourceNode().getType() );
                return;
            }

            final EntityRef eref =
                new SimpleEntityRef( edge.getSourceNode().getType(), edge.getSourceNode().getUuid() );

            String name = getNameFromEdgeType( edge.getType() );
            addMapSet( entityRefSetMap, eref, name );
        } ).toBlocking().last();
    }


    @Override
    public boolean isConnectionMember( String connectionType, EntityRef entity ) throws Exception {

        Id entityId = new SimpleId( entity.getUuid(), entity.getType() );

        String edgeType = CpNamingUtils.getEdgeTypeFromConnectionType(connectionType);

        logger.debug( "isConnectionMember(): Checking for edge type {} from {}:{} to {}:{}", new Object[] {
            edgeType, headEntity.getType(), headEntity.getUuid(), entity.getType(), entity.getUuid()
        } );

        GraphManager gm = managerCache.getGraphManager( applicationScope );
        Observable<Edge> edges = gm.loadEdgeVersions(
            new SimpleSearchByEdge( new SimpleId( headEntity.getUuid(), headEntity.getType() ), edgeType, entityId,
                Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING, null ) );

        return edges.toBlocking().firstOrDefault( null ) != null;
    }


    @SuppressWarnings( "unchecked" )
    @Override
    public boolean isCollectionMember( String collName, EntityRef entity ) throws Exception {

        Id entityId = new SimpleId( entity.getUuid(), entity.getType() );

        String edgeType = CpNamingUtils.getEdgeTypeFromCollectionName(collName);

        logger.debug( "isCollectionMember(): Checking for edge type {} from {}:{} to {}:{}", new Object[] {
            edgeType, headEntity.getType(), headEntity.getUuid(), entity.getType(), entity.getUuid()
        } );

        GraphManager gm = managerCache.getGraphManager(applicationScope);
        Observable<Edge> edges = gm.loadEdgeVersions(
            new SimpleSearchByEdge( new SimpleId( headEntity.getUuid(), headEntity.getType() ), edgeType, entityId,
                Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING, Optional.<Edge>absent() ) );

        return edges.toBlocking().firstOrDefault( null ) != null;
    }


    @Override
    public Set<String> getCollections() throws Exception {

        final Set<String> indexes = new HashSet<String>();

        GraphManager gm = managerCache.getGraphManager( applicationScope );

        Observable<String> str =
            gm.getEdgeTypesFromSource( new SimpleSearchEdgeType( cpHeadEntity.getId(), null, null ) );

        Iterator<String> iter = str.toBlocking().getIterator();
        while ( iter.hasNext() ) {
            String edgeType = iter.next();
            indexes.add( getNameFromEdgeType( edgeType ) );
        }

        return indexes;
    }


    @Override
    public Results getCollection( String collectionName, UUID startResult, int count, Level resultsLevel,
                                  boolean reversed ) throws Exception {


        final String ql;

        if ( startResult != null ) {
            ql = "select * where created > " + startResult.timestamp();
        }
        else {
            ql = "select *";
        }

        Query query = Query.fromQL( ql );
        query.setLimit( count );
        query.setReversed( reversed );

        return searchCollection( collectionName, query );
    }


    @Override
    public Results getCollection( String collName, Query query, Level level ) throws Exception {

        return searchCollection( collName, query );
    }


    // add to a named collection of the head entity
    @Override
    public Entity addToCollection( String collName, EntityRef itemRef ) throws Exception {

        CollectionInfo collection = getDefaultSchema().getCollection( headEntity.getType(), collName );
        if ( ( collection != null ) && !collection.getType().equals( itemRef.getType() ) ) {
            return null;
        }

        return addToCollection( collName, itemRef, ( collection != null && collection.getLinkedCollection() != null ) );
    }


    public Entity addToCollection( String collName, EntityRef itemRef, boolean connectBack ) throws Exception {

        Id entityId = new SimpleId( itemRef.getUuid(), itemRef.getType() );
        org.apache.usergrid.persistence.model.entity.Entity memberEntity = ( ( CpEntityManager ) em ).load( entityId );

        return addToCollection( collName, itemRef, memberEntity, connectBack );
    }


    public Entity addToCollection( final String collName, final EntityRef itemRef,
                                   final org.apache.usergrid.persistence.model.entity.Entity memberEntity,
                                   final boolean connectBack ) throws Exception {

        // don't fetch entity if we've already got one
        final Entity itemEntity;
        if ( itemRef instanceof Entity ) {
            itemEntity = ( Entity ) itemRef;
        }
        else {
            itemEntity = em.get( itemRef );
        }

        if ( itemEntity == null ) {
            return null;
        }

        CollectionInfo collection = getDefaultSchema().getCollection( headEntity.getType(), collName );
        if ( ( collection != null ) && !collection.getType().equals( itemRef.getType() ) ) {
            return null;
        }


        if ( memberEntity == null ) {
            throw new RuntimeException(
                "Unable to load entity uuid=" + itemRef.getUuid() + " type=" + itemRef.getType() );
        }

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Loaded member entity {}:{} from   app {}\n   " + " data {}", new Object[] {
                itemRef.getType(), itemRef.getUuid(), applicationScope, CpEntityMapUtils.toMap( memberEntity )
            } );
        }


        // create graph edge connection from head entity to member entity
        final Edge edge = createCollectionEdge( cpHeadEntity.getId(), collName, memberEntity.getId() );
        GraphManager gm = managerCache.getGraphManager( applicationScope );
        gm.writeEdge( edge ).toBlocking().last();


        //perform indexing

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Wrote edge {}", edge );
        }

        indexService.queueNewEdge(applicationScope, memberEntity, edge);


        if ( logger.isDebugEnabled() ) {
            logger.debug( "Added entity {}:{} to collection {}", new Object[] {
                itemRef.getUuid().toString(), itemRef.getType(), collName
            } );
        }


        return itemEntity;
    }


    @Override
    public Entity addToCollections( List<EntityRef> owners, String collName ) throws Exception {

        // TODO: this addToCollections() implementation seems wrong.
        for ( EntityRef eref : owners ) {
            addToCollection( collName, eref );
        }

        return null;
    }


    @Override
    public Entity createItemInCollection( String collName, String itemType, Map<String, Object> properties )
        throws Exception {

        if ( headEntity.getUuid().equals( applicationId ) ) {
            if ( itemType.equals( TYPE_ENTITY ) ) {
                itemType = singularize( collName );
            }

            if ( itemType.equals( TYPE_ROLE ) ) {
                Long inactivity = ( Long ) properties.get( PROPERTY_INACTIVITY );
                if ( inactivity == null ) {
                    inactivity = 0L;
                }
                return em.createRole( ( String ) properties.get( PROPERTY_NAME ),
                    ( String ) properties.get( PROPERTY_TITLE ), inactivity );
            }
            return em.create( itemType, properties );
        }

        else if ( headEntity.getType().equals( Group.ENTITY_TYPE ) && ( collName.equals( COLLECTION_ROLES ) ) ) {
            UUID groupId = headEntity.getUuid();
            String roleName = ( String ) properties.get( PROPERTY_NAME );
            return em.createGroupRole( groupId, roleName, ( Long ) properties.get( PROPERTY_INACTIVITY ) );
        }

        CollectionInfo collection = getDefaultSchema().getCollection( headEntity.getType(), collName );
        if ( ( collection != null ) && !collection.getType().equals( itemType ) ) {
            return null;
        }

        properties = getDefaultSchema().cleanUpdatedProperties( itemType, properties, true );

        Entity itemEntity = em.create( itemType, properties );

        if ( itemEntity != null ) {

            addToCollection( collName, itemEntity );

            if ( collection != null && collection.getLinkedCollection() != null ) {
                Id itemEntityId = new SimpleId( itemEntity.getUuid(), itemEntity.getType() );
                final Edge edge = createCollectionEdge( cpHeadEntity.getId(), collName, itemEntityId );

                GraphManager gm = managerCache.getGraphManager( applicationScope );
                gm.writeEdge( edge );
            }
        }

        return itemEntity;
    }


    @Override
    public void removeFromCollection( String collName, EntityRef itemRef ) throws Exception {

        // special handling for roles collection of the application
        if ( headEntity.getUuid().equals( applicationId ) ) {
            if ( collName.equals( COLLECTION_ROLES ) ) {
                Entity itemEntity = em.get( itemRef );
                if ( itemEntity != null ) {
                    RoleRef roleRef = SimpleRoleRef.forRoleEntity( itemEntity );
                    em.deleteRole( roleRef.getApplicationRoleName() );
                    return;
                }
                em.delete( itemEntity );
                return;
            }
            em.delete( itemRef );
            return;
        }

        // load the entity to be removed to the collection


        if ( logger.isDebugEnabled() ) {
            logger.debug( "Loading entity to remove from collection " + "{}:{} from app {}\n", new Object[] {
                itemRef.getType(), itemRef.getUuid(), applicationScope
            } );
        }

        Id entityId = new SimpleId( itemRef.getUuid(), itemRef.getType() );
        org.apache.usergrid.persistence.model.entity.Entity memberEntity = ( ( CpEntityManager ) em ).load( entityId );


        // remove edge from collection to item
        GraphManager gm = managerCache.getGraphManager( applicationScope );


        //run our delete
        final Edge collectionToItemEdge = createCollectionEdge( cpHeadEntity.getId(), collName, memberEntity.getId() );
        gm.markEdge( collectionToItemEdge ).toBlocking().last();


        /**
         * Remove from the index
         *
         */

        final ApplicationEntityIndex ei = managerCache.getEntityIndex( applicationScope );
        final EntityIndexBatch batch = ei.createBatch();

        // remove item from collection index
        SearchEdge indexScope = createCollectionSearchEdge( cpHeadEntity.getId(), collName );

        batch.deindex( indexScope, memberEntity );


        batch.execute();


        // special handling for roles collection of a group
        if ( headEntity.getType().equals( Group.ENTITY_TYPE ) ) {

            if ( collName.equals( COLLECTION_ROLES ) ) {
                String path = ( String ) ( ( Entity ) itemRef ).getMetadata( "path" );

                if ( path.startsWith( "/roles/" ) ) {

                    Entity itemEntity =
                        em.get( new SimpleEntityRef( memberEntity.getId().getType(), memberEntity.getId().getUuid() ) );

                    RoleRef roleRef = SimpleRoleRef.forRoleEntity( itemEntity );
                    em.deleteRole( roleRef.getApplicationRoleName() );
                }
            }
        }
    }


    @Override
    public void copyRelationships( String srcRelationName, EntityRef dstEntityRef, String dstRelationName )
        throws Exception {

        headEntity = em.validate( headEntity );
        dstEntityRef = em.validate( dstEntityRef );

        CollectionInfo srcCollection = getDefaultSchema().getCollection( headEntity.getType(), srcRelationName );

        CollectionInfo dstCollection = getDefaultSchema().getCollection( dstEntityRef.getType(), dstRelationName );

        Results results = null;
        do {
            if ( srcCollection != null ) {
                results = em.getCollection( headEntity, srcRelationName, null, 5000, Level.REFS, false );
            }
            else {
                results = em.getConnectedEntities( headEntity, srcRelationName, null, Level.REFS );
            }

            if ( ( results != null ) && ( results.size() > 0 ) ) {
                List<EntityRef> refs = results.getRefs();
                for ( EntityRef ref : refs ) {
                    if ( dstCollection != null ) {
                        em.addToCollection( dstEntityRef, dstRelationName, ref );
                    }
                    else {
                        em.createConnection( dstEntityRef, dstRelationName, ref );
                    }
                }
            }
        }
        while ( ( results != null ) && ( results.hasMoreResults() ) );
    }


    @Override
    public Results searchCollection( String collName, Query query ) throws Exception {

        if ( query == null ) {
            query = new Query();
            query.setCollection( collName );
        }

        headEntity = em.validate( headEntity );

        CollectionInfo collection = getDefaultSchema().getCollection( headEntity.getType(), collName );

        if ( collection == null ) {
            throw new RuntimeException(
                "Cannot find collection-info for '" + collName + "' of " + headEntity.getType() + ":" + headEntity
                    .getUuid() );
        }


        query.setEntityType( collection.getType() );
        query = adjustQuery( query );



        final ReadPipelineBuilder readPipelineBuilder =
            pipelineBuilderFactory.createReadPipelineBuilder(applicationScope);

        //set our fields applicable to both operations
        readPipelineBuilder.withCursor(query.getCursor());
        readPipelineBuilder.withLimit(query.getLimit());

        //TODO, this should be removed when the CP relation manager is removed
        readPipelineBuilder.setStartId( cpHeadEntity.getId() );

        if ( query.isGraphSearch() ) {
            readPipelineBuilder.getCollection( collName );
        }
        else {
            final String entityType = collection.getType();

            readPipelineBuilder.getCollectionWithQuery( collName, entityType, query.getQl().get() );
        }


        final Observable<ResultsPage> resultsObservable = readPipelineBuilder.execute();

        return new ObservableQueryExecutor( resultsObservable ).next();
    }


    @Override
    public ConnectionRef createConnection( ConnectionRef connection ) throws Exception {

        return createConnection( connection.getConnectionType(), connection.getConnectedEntity() );
    }


    @Override
    public ConnectionRef createConnection( String connectionType, EntityRef connectedEntityRef ) throws Exception {

        headEntity = em.validate( headEntity );
        connectedEntityRef = em.validate( connectedEntityRef );

        ConnectionRefImpl connection = new ConnectionRefImpl( headEntity, connectionType, connectedEntityRef );


        if ( logger.isDebugEnabled() ) {
            logger.debug( "createConnection(): " + "Indexing connection type '{}'\n   from source {}:{}]\n"
                + "   to target {}:{}\n   app {}", new Object[] {
                connectionType, headEntity.getType(), headEntity.getUuid(), connectedEntityRef.getType(),
                connectedEntityRef.getUuid(), applicationScope
            } );
        }

        Id entityId = new SimpleId( connectedEntityRef.getUuid(), connectedEntityRef.getType() );
        org.apache.usergrid.persistence.model.entity.Entity targetEntity = ( ( CpEntityManager ) em ).load( entityId );

        String edgeType = CpNamingUtils.getEdgeTypeFromConnectionType( connectionType );

        // create graph edge connection from head entity to member entity
        Edge edge = new SimpleEdge( cpHeadEntity.getId(), edgeType, targetEntity.getId(), System.currentTimeMillis() );

        GraphManager gm = managerCache.getGraphManager( applicationScope );
        gm.writeEdge( edge ).toBlocking().last();

        ApplicationEntityIndex ei = managerCache.getEntityIndex( applicationScope );
        EntityIndexBatch batch = ei.createBatch();

        // Index the new connection in app|source|type context
        IndexEdge edgeIndex = generateScopeFromSource( edge );

        batch.index( edgeIndex, targetEntity );

        batch.execute();

        // Index the new connection in app|scope|all-types context
        //TODO REMOVE INDEX CODE
        //        IndexScope allTypesIndexScope = new IndexScopeImpl( cpHeadEntity.getId(), CpNamingUtils.ALL_TYPES,
        // entityType );
        //        batch.index( allTypesIndexScope, targetEntity );


        return connection;
    }


    @Override
    public ConnectionRef createConnection( String pairedConnectionType, EntityRef pairedEntity, String connectionType,
                                           EntityRef connectedEntityRef ) throws Exception {

        throw new UnsupportedOperationException( "Paired connections not supported" );
    }


    @Override
    public ConnectionRef createConnection( ConnectedEntityRef... connections ) throws Exception {

        throw new UnsupportedOperationException( "Paired connections not supported" );
    }


    @Override
    public ConnectionRef connectionRef( String connectionType, EntityRef connectedEntityRef ) throws Exception {

        ConnectionRef connection = new ConnectionRefImpl( headEntity, connectionType, connectedEntityRef );

        return connection;
    }


    @Override
    public ConnectionRef connectionRef( String pairedConnectionType, EntityRef pairedEntity, String connectionType,
                                        EntityRef connectedEntityRef ) throws Exception {

        throw new UnsupportedOperationException( "Paired connections not supported" );
    }


    @Override
    public ConnectionRef connectionRef( ConnectedEntityRef... connections ) {

        throw new UnsupportedOperationException( "Paired connections not supported" );
    }


    @Override
    public void deleteConnection( ConnectionRef connectionRef ) throws Exception {

        // First, clean up the dictionary records of the connection
        EntityRef connectingEntityRef = connectionRef.getConnectingEntity();  // source
        EntityRef connectedEntityRef = connectionRef.getConnectedEntity();  // target

        String connectionType = connectionRef.getConnectedEntity().getConnectionType();


        if ( logger.isDebugEnabled() ) {
            logger.debug( "Deleting connection '{}' from source {}:{} \n   to target {}:{}", new Object[] {
                connectionType, connectingEntityRef.getType(), connectingEntityRef.getUuid(),
                connectedEntityRef.getType(), connectedEntityRef.getUuid()
            } );
        }

        Id entityId = new SimpleId( connectedEntityRef.getUuid(), connectedEntityRef.getType() );
        org.apache.usergrid.persistence.model.entity.Entity targetEntity = ( ( CpEntityManager ) em ).load( entityId );

        // Delete graph edge connection from head entity to member entity
        Edge edge = new SimpleEdge( new SimpleId( connectingEntityRef.getUuid(), connectingEntityRef.getType() ),
            connectionType, targetEntity.getId(), System.currentTimeMillis() );


        GraphManager gm = managerCache.getGraphManager( applicationScope );


        final Id sourceId = new SimpleId( connectingEntityRef.getUuid(), connectingEntityRef.getType() );

        final SearchByEdge search = createConnectionSearchByEdge( sourceId, connectionType, targetEntity.getId() );

        //delete all the edges
        final Edge lastEdge =
            gm.loadEdgeVersions( search ).flatMap( returnedEdge -> gm.markEdge( returnedEdge ) ).toBlocking()
              .lastOrDefault( null );

        if ( lastEdge != null ) {
            final ApplicationEntityIndex ei = managerCache.getEntityIndex( applicationScope );
            final EntityIndexBatch batch = ei.createBatch();

            SearchEdge indexScope = createSearchEdgeFromSource( lastEdge );
            batch.deindex( indexScope, targetEntity );
        }
    }


    @Override
    public Set<String> getConnectionTypes( UUID connectedEntityId ) throws Exception {
        throw new UnsupportedOperationException( "Cannot specify entity by UUID alone." );
    }


    @Override
    public Set<String> getConnectionTypes() throws Exception {
        return getConnectionTypes( false );
    }


    @Override
    public Set<String> getConnectionTypes( boolean filterConnection ) throws Exception {
        Set<String> connections = cast( em.getDictionaryAsSet( headEntity, Schema.DICTIONARY_CONNECTED_TYPES ) );

        if ( connections == null ) {
            return null;
        }
        if ( filterConnection && ( connections.size() > 0 ) ) {
            connections.remove( "connection" );
        }
        return connections;
    }


    @Override
    public Results getConnectedEntities( String connectionType, String connectedEntityType, Level level )
        throws Exception {

        //until this is refactored properly, we will delegate to a search by query
        Results raw = null;

        Preconditions.checkNotNull( connectionType, "connectionType cannot be null" );

        Query query = new Query();
        query.setConnectionType( connectionType );
        query.setEntityType( connectedEntityType );
        query.setResultsLevel( level );

        return searchConnectedEntities( query );
    }


    @Override
    public Results getConnectingEntities( String connType, String fromEntityType, Level resultsLevel )
        throws Exception {

        return getConnectingEntities( connType, fromEntityType, resultsLevel, -1 );
    }


    @Override
    public Results getConnectingEntities( String connType, String fromEntityType, Level level, int count )
        throws Exception {

        // looking for edges to the head entity
        String edgeType = CpNamingUtils.getEdgeTypeFromConnectionType( connType );

        Map<EntityRef, Set<String>> containers = getContainers( count, edgeType, fromEntityType );

        if ( Level.REFS.equals( level ) ) {
            List<EntityRef> refList = new ArrayList<EntityRef>( containers.keySet() );
            return Results.fromRefList( refList );
        }

        if ( Level.IDS.equals( level ) ) {
            // TODO: someday this should return a list of Core Persistence Ids
            List<UUID> idList = new ArrayList<UUID>();
            for ( EntityRef ref : containers.keySet() ) {
                idList.add( ref.getUuid() );
            }
            return Results.fromIdList( idList );
        }

        List<Entity> entities = new ArrayList<Entity>();
        for ( EntityRef ref : containers.keySet() ) {
            Entity entity = em.get( ref );
            logger.debug( "   Found connecting entity: " + entity.getProperties() );
            entities.add( entity );
        }
        return Results.fromEntities( entities );
    }


    @Override
    public Results searchConnectedEntities( Query query ) throws Exception {

        Preconditions.checkNotNull( query, "query cannot be null" );

        final String connection = query.getConnectionType();

        Preconditions.checkNotNull( connection, "connection must be specified" );

        headEntity = em.validate( headEntity );


        query = adjustQuery( query );

        final String entityType = query.getEntityType();


        final ReadPipelineBuilder readPipelineBuilder =
            pipelineBuilderFactory.createReadPipelineBuilder(applicationScope);

        //set our fields applicable to both operations
        readPipelineBuilder.withCursor( query.getCursor() );
        readPipelineBuilder.withLimit(query.getLimit());

        //TODO, this should be removed when the CP relation manager is removed
        readPipelineBuilder.setStartId( cpHeadEntity.getId() );

        if ( query.isGraphSearch() ) {
            readPipelineBuilder.getConnection( connection );
        }
        else {
            readPipelineBuilder.getConnectionWithQuery( connection, Optional.fromNullable( entityType ),
                query.getQl().get() );
        }


        final Observable<ResultsPage> resultsObservable = readPipelineBuilder.execute();

        return new ObservableQueryExecutor( resultsObservable ).next();
    }


    private Query adjustQuery( Query query ) {

        // handle the select by identifier case
        if ( query.getQl().isPresent() ) {
            return query;
        }

        // a name alias or email alias was specified
        if ( query.containsSingleNameOrEmailIdentifier() ) {

            Identifier ident = query.getSingleIdentifier();

            // an email was specified.  An edge case that only applies to users.
            // This is fulgy to put here, but required.
            if ( query.getEntityType().equals( User.ENTITY_TYPE ) && ident.isEmail() ) {

                final String newQuery = "select * where email='" + query.getSingleNameOrEmailIdentifier() + "'";

                query.setQl( newQuery );
            }

            // use the ident with the default alias. could be an email
            else {

                final String newQuery = "select * where name='" + query.getSingleNameOrEmailIdentifier() + "'";
                query.setQl( newQuery );
            }
        }
        else if ( query.containsSingleUuidIdentifier() ) {

            //TODO, this shouldn't even come from ES, it should look up the entity directly
            final String newQuery = "select * where uuid=" + query.getSingleUuidIdentifier() + "";
            query.setQl( newQuery );
        }


        //TODO T.N. not sure if we still need this.  If we do then we ned to modify our query interface.

        //        final String ql = query.getQl();
        //
        //        if ( query.isReversed()  && ( StringUtils.isEmpty( ql ) || !(ql.contains( "order by " )) )) {
        //
        //
        //            final String sortQueryString =
        //
        //            SortPredicate
        //            Query.SortPredicate desc =
        //                new Query.SortPredicate( PROPERTY_CREATED, Query.SortDirection.DESCENDING );
        //
        //            try {
        //                query.addSort( desc );
        //            }
        //            catch ( Exception e ) {
        //                logger.warn( "Attempted to reverse sort order already set", PROPERTY_CREATED );
        //            }
        //        }

        return query;
    }


    @Override
    public Set<String> getConnectionIndexes( String connectionType ) throws Exception {
        throw new UnsupportedOperationException( "Not supported yet." );
    }


    /** side effect: converts headEntity into an Entity if it is an EntityRef! */
    private Entity getHeadEntity() throws Exception {
        Entity entity = null;
        if ( headEntity instanceof Entity ) {
            entity = ( Entity ) headEntity;
        }
        else {
            entity = em.get( headEntity );
            headEntity = entity;
        }
        return entity;
    }
}
