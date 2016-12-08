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


import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.index.CollectionSettings;
import org.apache.usergrid.corepersistence.index.CollectionSettingsFactory;
import org.apache.usergrid.corepersistence.index.CollectionSettingsScopeImpl;
import org.apache.usergrid.corepersistence.pipeline.read.ResultsPage;
import org.apache.usergrid.corepersistence.results.ConnectionRefQueryExecutor;
import org.apache.usergrid.corepersistence.results.EntityQueryExecutor;
import org.apache.usergrid.corepersistence.results.IdQueryExecutor;
import org.apache.usergrid.corepersistence.service.CollectionSearch;
import org.apache.usergrid.corepersistence.service.CollectionService;
import org.apache.usergrid.corepersistence.service.ConnectionSearch;
import org.apache.usergrid.corepersistence.service.ConnectionService;
import org.apache.usergrid.corepersistence.util.CpEntityMapUtils;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.Query.Level;
import org.apache.usergrid.persistence.cassandra.ConnectionRefImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.graph.*;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.schema.CollectionInfo;
import org.apache.usergrid.utils.InflectionUtils;
import org.apache.usergrid.utils.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import rx.Observable;

import java.util.*;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.*;
import static org.apache.usergrid.persistence.Schema.*;
import static org.apache.usergrid.utils.ClassUtils.cast;
import static org.apache.usergrid.utils.InflectionUtils.singularize;
import static org.apache.usergrid.utils.MapUtils.addMapSet;


/**
 * Implement good-old Usergrid RelationManager with the new-fangled Core Persistence API.
 */
public class CpRelationManager implements RelationManager {

    private static final Logger logger = LoggerFactory.getLogger( CpRelationManager.class );
    private final EntityManagerFig entityManagerFig;

    private ManagerCache managerCache;

    private EntityManager em;

    private UUID applicationId;

    private EntityRef headEntity;

    private org.apache.usergrid.persistence.model.entity.Entity cpHeadEntity;

    private final ApplicationScope applicationScope;

    private final AsyncEventService indexService;

    private final CollectionSettingsFactory collectionSettingsFactory;


    private final CollectionService collectionService;
    private final ConnectionService connectionService;


    public CpRelationManager( final ManagerCache managerCache,
                              final AsyncEventService indexService, final CollectionService collectionService,
                              final ConnectionService connectionService,
                              final EntityManager em,
                              final EntityManagerFig entityManagerFig, final UUID applicationId,
                              final CollectionSettingsFactory collectionSettingsFactory,
                              final EntityRef headEntity) {


        Assert.notNull( em, "Entity manager cannot be null" );
        Assert.notNull( applicationId, "Application Id cannot be null" );
        Assert.notNull( headEntity, "Head entity cannot be null" );
        Assert.notNull( headEntity.getUuid(), "Head entity uuid cannot be null" );
        Assert.notNull( indexService, "indexService cannot be null" );
        Assert.notNull( collectionService, "collectionService cannot be null" );
        Assert.notNull( connectionService, "connectionService cannot be null" );

        this.entityManagerFig = entityManagerFig;

        // TODO: this assert should not be failing
        //Assert.notNull( indexBucketLocator, "indexBucketLocator cannot be null" );
        this.em = em;
        this.applicationId = applicationId;
        this.headEntity = headEntity;
        this.managerCache = managerCache;
        this.applicationScope = CpNamingUtils.getApplicationScope( applicationId );

        this.collectionService = collectionService;
        this.connectionService = connectionService;

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Loading head entity {}:{} from app {}",
                headEntity.getType(), headEntity.getUuid(), applicationScope
            );
        }

        Id entityId = new SimpleId( headEntity.getUuid(), headEntity.getType() );

        this.cpHeadEntity = ( ( CpEntityManager ) em ).load( entityId );

        // commented out because it is possible that CP entity has not been created yet
        Assert.notNull( cpHeadEntity, String
            .format( "cpHeadEntity cannot be null for entity id %s, app id %s", entityId.getUuid(), applicationId ) );

        this.indexService = indexService;
        this.collectionSettingsFactory = collectionSettingsFactory;

    }


    @Override
    public Set<String> getCollectionIndexes( String collectionName ) throws Exception {
        GraphManager gm = managerCache.getGraphManager( applicationScope );

        String edgeTypePrefix = CpNamingUtils.getEdgeTypeFromCollectionName( collectionName );

        if (logger.isTraceEnabled()) {
            logger.trace("getCollectionIndexes(): Searching for edge type prefix {} to target {}:{}",
                edgeTypePrefix, cpHeadEntity.getId().getType(), cpHeadEntity.getId().getUuid() );
        }

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
        return getContainers( -1, null, null );
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
              .flatMap( edgeType1 -> gm.loadEdgesToTarget(
                  new SimpleSearchByEdgeType( cpHeadEntity.getId(), edgeType1, Long.MAX_VALUE,
                      SearchByEdgeType.Order.DESCENDING, Optional.<Edge>absent() ) ) );

        //if our limit is set, take them.  Note this logic is still borked, we can't possibly fit everything in memmory
        if ( limit > -1 ) {
            edges = edges.take( limit );
        }


        return edges.collect( () -> new LinkedHashMap<EntityRef, Set<String>>(), ( entityRefSetMap, edge ) -> {
            if ( fromEntityType != null && !fromEntityType.equals( edge.getSourceNode().getType() ) ) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Ignoring edge from entity type {}", edge.getSourceNode().getType());
                }
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


        if (logger.isTraceEnabled()) {
            logger.trace("isConnectionMember(): Checking for edge type {} from {}:{} to {}:{}",
                connectionType, headEntity.getType(), headEntity.getUuid(), entity.getType(), entity.getUuid() );
        }

        GraphManager gm = managerCache.getGraphManager( applicationScope );
        Observable<MarkedEdge> edges = gm.loadEdgeVersions( CpNamingUtils
            .createEdgeFromConnectionType( new SimpleId( headEntity.getUuid(), headEntity.getType() ), connectionType,
                entityId ) );

        return edges.toBlocking().firstOrDefault( null ) != null;
    }


    @SuppressWarnings( "unchecked" )
    @Override
    public boolean isCollectionMember( String collectionName, EntityRef entity ) throws Exception {

        Id entityId = new SimpleId( entity.getUuid(), entity.getType() );

        if (logger.isTraceEnabled()) {
            logger.trace("isCollectionMember(): Checking for edge type {} from {}:{} to {}:{}",
                collectionName, headEntity.getType(), headEntity.getUuid(), entity.getType(), entity.getUuid() );
        }

        GraphManager gm = managerCache.getGraphManager( applicationScope );
        Observable<MarkedEdge> edges = gm.loadEdgeVersions( CpNamingUtils
            .createEdgeFromCollectionName( new SimpleId( headEntity.getUuid(), headEntity.getType() ), collectionName,
                entityId ) );

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


        if (startResult != null ) {

            // UUID timestamp is a different measure than 'created' field on entities
            Calendar uuidEpoch = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            uuidEpoch.clear();
            uuidEpoch.set(1582, 9, 15, 0, 0, 0); // 9 = October
            long epochMillis = uuidEpoch.getTime().getTime();

            long time = (startResult.timestamp() / 10000L) + epochMillis;

            if ( !reversed ) {
                ql = "select * where created > " + time;
            } else {
                ql = "select * where created < " + time;
            }

        } else {
            ql = "select *";
        }

        Query query = Query.fromQL( ql );
        if(query == null ){
            throw new RuntimeException("Unable to get data for collection: "+collectionName);
        }
        query.setLimit( count );
        query.setReversed( reversed );

        return searchCollection( collectionName, query );
    }


    @Override
    public Results getCollection( String collectionName, Query query, Level level ) throws Exception {

        return searchCollection( collectionName, query );
    }


    // add to a named collection of the head entity
    @Override
    public Entity addToCollection( String collectionName, EntityRef itemRef ) throws Exception {

        Preconditions.checkNotNull( itemRef, "itemref is null" );
        CollectionInfo collection = getDefaultSchema().getCollection( headEntity.getType(), collectionName );
        if ( ( collection != null && collection.getType() != null ) && !collection.getType()
                                                                                  .equals( itemRef.getType() ) ) {
            return null;
        }


        Id entityId = new SimpleId( itemRef.getUuid(), itemRef.getType() );
        org.apache.usergrid.persistence.model.entity.Entity memberEntity = ( ( CpEntityManager ) em ).load( entityId );


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


        if ( memberEntity == null ) {
            throw new RuntimeException(
                "Unable to load entity uuid=" + itemRef.getUuid() + " type=" + itemRef.getType() );
        }

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Loaded member entity {}:{} from   app {}\n    data {}",
                itemRef.getType(), itemRef.getUuid(), applicationScope, CpEntityMapUtils.toMap( memberEntity ) );
        }


        // create graph edge connection from head entity to member entity
        final Edge edge = createCollectionEdge( cpHeadEntity.getId(), collectionName, memberEntity.getId() );
        final String linkedCollection = collection.getLinkedCollection();

        GraphManager gm = managerCache.getGraphManager( applicationScope );

        gm.writeEdge( edge ).doOnNext( writtenEdge -> {
            if ( logger.isDebugEnabled() ) {
                logger.debug( "Wrote edge {}", writtenEdge );
            }
        } ).filter( writtenEdge -> linkedCollection != null ).flatMap( writtenEdge -> {
            final String pluralType = InflectionUtils.pluralize( cpHeadEntity.getId().getType() );
            final Edge reverseEdge = createCollectionEdge( memberEntity.getId(), pluralType, cpHeadEntity.getId() );

            //reverse
            return gm.writeEdge( reverseEdge ).doOnNext( reverseEdgeWritten -> {

                if ( !skipIndexingForType( cpHeadEntity.getId().getType() ) ) {

                    indexService.queueNewEdge(applicationScope, cpHeadEntity, reverseEdge);
                }

            } );
        } ).doOnCompleted( () -> {

            if ( !skipIndexingForType( memberEntity.getId().getType() ) ) {
                indexService.queueNewEdge(applicationScope, memberEntity, edge);
            }


            if ( logger.isDebugEnabled() ) {
                logger.debug( "Added entity {}:{} to collection {}",
                    itemRef.getUuid().toString(), itemRef.getType(), collectionName );
            }
        } ).toBlocking().lastOrDefault( null );


        // remove any duplicate edges (keeps the duplicate edge with same timestamp)
        removeDuplicateEdgesAsync(gm, edge);


        if ( logger.isDebugEnabled() ) {
            logger.debug( "Added entity {}:{} to collection {}",
                itemRef.getUuid().toString(), itemRef.getType(), collectionName  );
        }


        return itemEntity;
    }


    @Override
    public Entity createItemInCollection( String collectionName, String itemType, Map<String, Object> properties )
        throws Exception {

        if ( headEntity.getUuid().equals( applicationId ) ) {
            if ( itemType.equals( TYPE_ENTITY ) ) {
                itemType = singularize( collectionName );
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

        else if ( headEntity.getType().equals( Group.ENTITY_TYPE ) && ( collectionName.equals( COLLECTION_ROLES ) ) ) {
            UUID groupId = headEntity.getUuid();
            String roleName = ( String ) properties.get( PROPERTY_NAME );
            return em.createGroupRole( groupId, roleName, ( Long ) properties.get( PROPERTY_INACTIVITY ) );
        }

        CollectionInfo collection = getDefaultSchema().getCollection( headEntity.getType(), collectionName );
        if ( ( collection != null ) && !collection.getType().equals( itemType ) ) {
            return null;
        }

        properties = getDefaultSchema().cleanUpdatedProperties( itemType, properties, true );

        Entity itemEntity = em.create( itemType, properties );

        if ( itemEntity != null ) {

            addToCollection( collectionName, itemEntity );

            if ( collection != null && collection.getLinkedCollection() != null ) {
                Id itemEntityId = new SimpleId( itemEntity.getUuid(), itemEntity.getType() );
                final Edge edge = createCollectionEdge( cpHeadEntity.getId(), collectionName, itemEntityId );

                GraphManager gm = managerCache.getGraphManager( applicationScope );
                gm.writeEdge( edge );
            }
        }

        return itemEntity;
    }


    @Override
    public void removeFromCollection( String collectionName, EntityRef itemRef ) throws Exception {

        if ( headEntity.getUuid().equals( applicationId ) ) {
            if ( collectionName.equals( COLLECTION_ROLES ) ) {
                // special handling for roles collection of the application
                Entity itemEntity = em.get( itemRef );
                if ( itemEntity != null ) {
                    RoleRef roleRef = SimpleRoleRef.forRoleEntity( itemEntity );
                    em.deleteRole(roleRef.getApplicationRoleName(), Optional.fromNullable(itemEntity) );
                    return;
                }
            }
            // handles normal app collection deletes
            em.delete( itemRef );
            return;
        }

        // headEntity is not an application (used for management entities and entity collections like user devices)

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Loading entity to remove from collection {}:{} from app {}\n",
                itemRef.getType(), itemRef.getUuid(), applicationScope );
        }

        Id entityId = new SimpleId( itemRef.getUuid(), itemRef.getType() );


        // this will remove the edges from app->entity(collection)
        removeItemFromCollection(collectionName, itemRef);

        // special handling for roles collection of a group
        if ( headEntity.getType().equals( Group.ENTITY_TYPE ) ) {

            if ( collectionName.equals( COLLECTION_ROLES ) ) {
                String path = ( String ) ( ( Entity ) itemRef ).getMetadata( "path" );

                if ( path.startsWith( "/roles/" ) ) {

                    Entity itemEntity =
                        em.get( new SimpleEntityRef( entityId.getType(), entityId.getUuid() ) );

                    RoleRef roleRef = SimpleRoleRef.forRoleEntity( itemEntity );
                    em.deleteRole( roleRef.getApplicationRoleName(), Optional.fromNullable(itemEntity) );
                }
            }
        }
    }

    @Override
    public void removeItemFromCollection( String collectionName, EntityRef itemRef ) throws Exception {

        Id entityId = new SimpleId( itemRef.getUuid(), itemRef.getType() );

        // remove edge from collection to item
        GraphManager gm = managerCache.getGraphManager( applicationScope );



        // mark the edge versions and take the first for later delete edge queue event ( load is descending )
        final Edge markedSourceEdge = gm.loadEdgeVersions(
            CpNamingUtils.createEdgeFromCollectionName( cpHeadEntity.getId(), collectionName, entityId ) )
            .flatMap(edge -> gm.markEdge(edge)).toBlocking().firstOrDefault(null);


        Edge markedReversedEdge = null;
        CollectionInfo collection = getDefaultSchema().getCollection( headEntity.getType(), collectionName );
        if (collection != null && collection.getLinkedCollection() != null) {
            // delete reverse edges
            final String pluralType = InflectionUtils.pluralize( cpHeadEntity.getId().getType() );
            markedReversedEdge = gm.loadEdgeVersions(
                CpNamingUtils.createEdgeFromCollectionName( entityId, pluralType, cpHeadEntity.getId() ) )
                .flatMap(reverseEdge -> gm.markEdge(reverseEdge)).toBlocking().firstOrDefault(null);
        }


        /**
         * Remove from the index.  This will call gm.deleteEdge which also deletes the reverse edge(s) and de-indexes
         * older versions of the edge(s).
         *
         */
        if( markedSourceEdge != null ) {
            indexService.queueDeleteEdge(applicationScope, markedSourceEdge);
        }
        if( markedReversedEdge != null ){
            indexService.queueDeleteEdge(applicationScope, markedReversedEdge);

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
                results = em.getTargetEntities( headEntity, srcRelationName, null, Level.REFS );
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
    public Results searchCollection( String collectionName, Query query ) throws Exception {

        if ( query == null ) {
            query = new Query();
            query.setCollection( collectionName );
        }

        headEntity = em.validate( headEntity );

        CollectionInfo collection = getDefaultSchema().getCollection( headEntity.getType(), collectionName );

        if ( collection == null ) {
            throw new RuntimeException(
                "Cannot find collection-info for '" + collectionName + "' of " + headEntity.getType() + ":" + headEntity
                    .getUuid() );
        }


        query.setEntityType( collection.getType() );
        final Query toExecute = adjustQuery( query );
        final Optional<String> queryString = query.isGraphSearch()? Optional.<String>absent(): query.getQl();
        final Id ownerId = headEntity.asId();


        if(query.getLevel() == Level.IDS ){

            return new IdQueryExecutor( toExecute.getCursor() ) {
                @Override
                protected Observable<ResultsPage<Id>> buildNewResultsPage(
                    final Optional<String> cursor ) {

                    final CollectionSearch search =
                        new CollectionSearch( applicationScope, ownerId, collectionName, collection.getType(), toExecute.getLimit(),
                            queryString, cursor );

                    return collectionService.searchCollectionIds( search );
                }
            }.next();

        }

        //wire the callback so we can get each page
        return new EntityQueryExecutor( toExecute.getCursor() ) {
            @Override
            protected Observable<ResultsPage<org.apache.usergrid.persistence.model.entity.Entity>> buildNewResultsPage(
                final Optional<String> cursor ) {

                final CollectionSearch search =
                    new CollectionSearch( applicationScope, ownerId, collectionName, collection.getType(), toExecute.getLimit(),
                        queryString, cursor );

                return collectionService.searchCollection( search );
            }
        }.next();
    }


    @Override
    public Results searchCollectionConsistent( String collectionName, Query query, int expectedResults )
        throws Exception {
        Results results;
        long maxLength = entityManagerFig.pollForRecordsTimeout();
        long sleepTime = entityManagerFig.sleep();
        boolean found;
        long current = System.currentTimeMillis(), length = 0;
        do {
            results = searchCollection( collectionName, query );
            length = System.currentTimeMillis() - current;
            found = expectedResults == results.size();
            if ( found ) {
                break;
            }
            logger.info("Sleeping {} ms during searchCollectionConsistent", sleepTime);
            Thread.sleep( sleepTime );
        }
        while ( !found && length <= maxLength );
        logger.info( "Consistent Search finished in {}, results={}, expected={}...dumping stack",
            length, results.size(), expectedResults );
        return results;
    }


    @Override
    public ConnectionRef createConnection( ConnectionRef connection ) throws Exception {

        return createConnection( connection.getConnectionType(), connection.getTargetRefs() );
    }


    @Override
    public ConnectionRef createConnection( String connectionType, EntityRef connectedEntityRef ) throws Exception {

        headEntity = em.validate( headEntity );
        connectedEntityRef = em.validate( connectedEntityRef );

        ConnectionRefImpl connection = new ConnectionRefImpl( headEntity, connectionType, connectedEntityRef );


        if ( logger.isTraceEnabled() ) {
            logger.trace( "createConnection(): Indexing connection type '{}'\n   from source {}:{}]\n   to target {}:{}\n   app {}",
                connectionType, headEntity.getType(), headEntity.getUuid(), connectedEntityRef.getType(),
                connectedEntityRef.getUuid(), applicationScope );
        }

        final Id entityId = new SimpleId( connectedEntityRef.getUuid(), connectedEntityRef.getType() );
        final org.apache.usergrid.persistence.model.entity.Entity targetEntity =
            ( ( CpEntityManager ) em ).load( entityId );

        // create graph edge connection from head entity to member entity
        final Edge edge = createConnectionEdge( cpHeadEntity.getId(), connectionType, targetEntity.getId() );

        final GraphManager gm = managerCache.getGraphManager( applicationScope );


        //write new edge

        gm.writeEdge(edge).toBlocking().lastOrDefault(null); //throw an exception if this fails


        if ( !skipIndexingForType( targetEntity.getId().getType() ) ) {

            indexService.queueNewEdge(applicationScope, targetEntity, edge);
        }

        // remove any duplicate edges (keeps the duplicate edge with same timestamp)
        removeDuplicateEdgesAsync(gm, edge);



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
        EntityRef connectingEntityRef = connectionRef.getSourceRefs();  // source
        EntityRef connectedEntityRef = connectionRef.getTargetRefs();  // target

        String connectionType = connectionRef.getTargetRefs().getConnectionType();


        if ( logger.isTraceEnabled() ) {
            logger.trace( "Deleting connection '{}' from source {}:{} \n   to target {}:{}",
                connectionType, connectingEntityRef.getType(), connectingEntityRef.getUuid(),
                connectedEntityRef.getType(), connectedEntityRef.getUuid() );
        }

        Id entityId = new SimpleId( connectedEntityRef.getUuid(), connectedEntityRef.getType() );
        org.apache.usergrid.persistence.model.entity.Entity targetEntity = ( ( CpEntityManager ) em ).load( entityId );

        GraphManager gm = managerCache.getGraphManager( applicationScope );

        final Id sourceId = new SimpleId( connectingEntityRef.getUuid(), connectingEntityRef.getType() );

        final SearchByEdge search = createConnectionSearchByEdge( sourceId, connectionType, targetEntity.getId() );

        //delete all the edges and queue their processing
        gm.loadEdgeVersions( search ).flatMap( returnedEdge -> gm.markEdge( returnedEdge ) )
          .doOnNext( returnedEdge -> {

              if ( !skipIndexingForType( returnedEdge.getSourceNode().getType() ) || !skipIndexingForType( returnedEdge.getTargetNode().getType() ) ) {

                  indexService.queueDeleteEdge(applicationScope, returnedEdge);
              }

          }).toBlocking()
          .lastOrDefault( null );
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
    public Results getTargetEntities( String connectionType, String connectedEntityType, Level level )
        throws Exception {

        //until this is refactored properly, we will delegate to a search by query
        Results raw = null;

        Preconditions.checkNotNull( connectionType, "connectionType cannot be null" );

        Query query = new Query();
        query.setConnectionType( connectionType );
        query.setEntityType( connectedEntityType );
        query.setResultsLevel( level );

        return searchTargetEntities( query );
    }


    @Override
    public Results getSourceEntities( String connType, String fromEntityType, Level resultsLevel ) throws Exception {

        return getSourceEntities( connType, fromEntityType, resultsLevel, -1 );
    }


    @Override
    public Results getSourceEntities( String connType, String fromEntityType, Level level, int count )
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

            if (logger.isTraceEnabled()) {
                logger.trace("   Found connecting entity: " + entity.getProperties());
            }

            entities.add( entity );
        }
        return Results.fromEntities( entities );
    }


    @Override
    public Results searchTargetEntities( Query query ) throws Exception {

        Preconditions.checkNotNull( query, "query cannot be null" );

        final String connection = query.getConnectionType();

        Preconditions.checkNotNull( connection, "connection must be specified" );

        headEntity = em.validate( headEntity );


        final Query toExecute = adjustQuery( query );

        final Optional<String> entityType = Optional.fromNullable( query.getEntityType() );
        //set startid -- graph | es query filter -- load entities filter (verifies exists) --> results page collector
        // -> 1.0 results

        //  startid -- graph edge load -- entity load (verify) from ids -> results page collector
        // startid -- eq query candiddate -- entity load (verify) from canddiates -> results page collector

        //startid -- graph edge load -- entity id verify --> filter to connection ref --> connection ref collector
        //startid -- eq query candiddate -- candidate id verify --> filter to connection ref --> connection ref
        // collector

        final Id sourceId = headEntity.asId();

        final Optional<String> queryString = query.isGraphSearch()? Optional.<String>absent(): query.getQl();
        final boolean isConnecting = query.isConnecting();

        if ( query.getResultsLevel() == Level.REFS || query.getResultsLevel() == Level.IDS ) {


            return new ConnectionRefQueryExecutor( toExecute.getCursor() ) {
                @Override
                protected Observable<ResultsPage<ConnectionRef>> buildNewResultsPage( final Optional<String> cursor ) {


                //we need the callback so as we get a new cursor, we execute a new search and re-initialize our builders

                    final ConnectionSearch search =
                        new ConnectionSearch( applicationScope, sourceId, entityType, connection, toExecute.getLimit(),
                            queryString, cursor, isConnecting );
                    return connectionService.searchConnectionAsRefs( search );
                }
            }.next();
        }


        return new EntityQueryExecutor( toExecute.getCursor() ) {
            @Override
            protected Observable<ResultsPage<org.apache.usergrid.persistence.model.entity.Entity>> buildNewResultsPage(
                final Optional<String> cursor ) {

                //we need the callback so as we get a new cursor, we execute a new search and re-initialize our builders
                final ConnectionSearch search =
                    new ConnectionSearch( applicationScope, sourceId, entityType, connection, toExecute.getLimit(),
                        queryString, cursor, isConnecting );
                return connectionService.searchConnection( search );
            }
        }.next();
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
            // groups have a special unique identifier
            else if ( query.getEntityType().equals( Group.ENTITY_TYPE ) ){

                final String newQuery = "select * where path='" + query.getSingleNameOrEmailIdentifier() + "'";

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

    private void removeDuplicateEdgesAsync(GraphManager gm, Edge edge){

        //now read all older versions of an edge, and remove them.  Finally calling delete
        final SearchByEdge searchByEdge =
            new SimpleSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getTargetNode(), Long.MAX_VALUE,
                SearchByEdgeType.Order.DESCENDING, Optional.absent() );

        //load our versions, only retain the most recent one
        gm.loadEdgeVersions(searchByEdge).skip(1).flatMap(edgeToDelete -> {
            if (logger.isDebugEnabled()) {
                logger.debug("Duplicate edge. Marking edge {} for deletion", edgeToDelete);
            }
            return gm.markEdge(edgeToDelete );
        }).lastOrDefault(null).doOnNext(lastEdge -> {
            //no op if we hit our default
            if (lastEdge == null) {
                return;
            }
            //don't queue delete b/c that de-indexes, we need to delete the edges only since we have a version still existing to index.
            gm.deleteEdge(lastEdge).toBlocking().lastOrDefault(null); // this should throw an exception
        }).toBlocking().lastOrDefault(null);//this should throw an exception

    }

    private boolean skipIndexingForType( String type ) {

        boolean skipIndexing = false;

        String collectionName = Schema.defaultCollectionName( type );

        CollectionSettings collectionSettings =
            collectionSettingsFactory.
                getInstance( new CollectionSettingsScopeImpl(new SimpleId( applicationId, TYPE_APPLICATION ), collectionName ) );
        Optional<Map<String, Object>> collectionIndexingSchema =
            collectionSettings.getCollectionSettings( collectionName );

        if ( collectionIndexingSchema.isPresent()) {
            Map jsonMapData = collectionIndexingSchema.get();
            final Object fields = jsonMapData.get( "fields" );
            if ( fields != null && fields instanceof String && "none".equalsIgnoreCase( fields.toString())) {
                skipIndexing = true;
            }
        }

        return skipIndexing;
    }

    /**
     * Get the map manager for uuid mapping
     */
    private MapManager getMapManagerForTypes() {
        Id mapOwner = new SimpleId( applicationId, TYPE_APPLICATION );

        final MapScope ms = CpNamingUtils.getEntityTypeMapScope(mapOwner);

        MapManager mm = managerCache.getMapManager( ms );

        return mm;
    }

}
