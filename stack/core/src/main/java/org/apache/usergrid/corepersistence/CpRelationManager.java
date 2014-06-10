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

import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
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

import org.apache.usergrid.persistence.ConnectedEntityRef;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityFactory;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.IndexBucketLocator;

import org.apache.usergrid.persistence.PagingResultsIterator;

import org.apache.usergrid.persistence.RelationManager;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.ConnectionRefImpl;
import org.apache.usergrid.persistence.cassandra.IndexUpdate;
import org.apache.usergrid.persistence.cassandra.QueryProcessorImpl;
import org.apache.usergrid.persistence.cassandra.index.ConnectedIndexScanner;
import org.apache.usergrid.persistence.cassandra.index.IndexBucketScanner;
import org.apache.usergrid.persistence.cassandra.index.IndexScanner;
import org.apache.usergrid.persistence.cassandra.index.NoOpIndexScanner;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.geo.ConnectionGeoSearch;
import org.apache.usergrid.persistence.geo.EntityLocationRef;
import org.apache.usergrid.persistence.geo.model.Point;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.impl.IndexScopeImpl;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Query.Level;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import org.apache.usergrid.persistence.query.ir.AllNode;
import org.apache.usergrid.persistence.query.ir.NameIdentifierNode;
import org.apache.usergrid.persistence.query.ir.QueryNode;
import org.apache.usergrid.persistence.query.ir.QuerySlice;
import org.apache.usergrid.persistence.query.ir.SearchVisitor;
import org.apache.usergrid.persistence.query.ir.WithinNode;
import org.apache.usergrid.persistence.query.ir.result.ConnectionIndexSliceParser;
import org.apache.usergrid.persistence.query.ir.result.ConnectionResultsLoaderFactory;
import org.apache.usergrid.persistence.query.ir.result.ConnectionTypesIterator;
import org.apache.usergrid.persistence.query.ir.result.EmptyIterator;
import org.apache.usergrid.persistence.query.ir.result.GeoIterator;
import org.apache.usergrid.persistence.query.ir.result.SliceIterator;
import org.apache.usergrid.persistence.query.ir.result.StaticIdIterator;

import org.apache.usergrid.persistence.schema.CollectionInfo;
import org.apache.usergrid.utils.IndexUtils;
import org.apache.usergrid.utils.MapUtils;

import com.yammer.metrics.annotation.Metered;
import static java.util.Arrays.asList;
import me.prettyprint.hector.api.Keyspace;

import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.usergrid.persistence.RoleRef;
import static org.apache.usergrid.persistence.Schema.COLLECTION_ROLES;
import rx.Observable;

import static org.apache.usergrid.persistence.Schema.DICTIONARY_CONNECTED_ENTITIES;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_CONNECTED_TYPES;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_CONNECTING_ENTITIES;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_CONNECTING_TYPES;
import static org.apache.usergrid.persistence.Schema.INDEX_CONNECTIONS;
import static org.apache.usergrid.persistence.Schema.PROPERTY_CREATED;
import static org.apache.usergrid.persistence.Schema.PROPERTY_INACTIVITY;
import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.PROPERTY_TITLE;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;
import static org.apache.usergrid.persistence.Schema.TYPE_ENTITY;
import static org.apache.usergrid.persistence.Schema.TYPE_ROLE;
import static org.apache.usergrid.persistence.Schema.getDefaultSchema;
import org.apache.usergrid.persistence.SimpleRoleRef;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_COMPOSITE_DICTIONARIES;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_DICTIONARIES;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX_ENTRIES;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addDeleteToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addInsertToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.batchExecute;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.apache.usergrid.persistence.cassandra.CassandraService.INDEX_ENTRY_LIST_COUNT;
import static org.apache.usergrid.persistence.cassandra.GeoIndexManager.batchDeleteLocationInConnectionsIndex;
import static org.apache.usergrid.persistence.cassandra.GeoIndexManager.batchRemoveLocationFromCollectionIndex;
import static org.apache.usergrid.persistence.cassandra.GeoIndexManager.batchStoreLocationInCollectionIndex;
import static org.apache.usergrid.persistence.cassandra.GeoIndexManager.batchStoreLocationInConnectionsIndex;
import static org.apache.usergrid.persistence.cassandra.IndexUpdate.indexValueCode;
import static org.apache.usergrid.persistence.cassandra.IndexUpdate.toIndexableValue;
import static org.apache.usergrid.persistence.cassandra.IndexUpdate.validIndexableValue;
import static org.apache.usergrid.persistence.cassandra.Serializers.be;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import static org.apache.usergrid.utils.CompositeUtils.setGreaterThanEqualityFlag;
import static org.apache.usergrid.utils.InflectionUtils.singularize;
import static org.apache.usergrid.utils.MapUtils.addMapSet;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMicros;


/**
 * Implement good-old Usergrid RelationManager with the new-fangled Core Persistence API.
 */
public class CpRelationManager implements RelationManager {

    private static final Logger logger = LoggerFactory.getLogger( CpRelationManager.class );

    private static final String ALL_TYPES = "zzzalltypesnzzz";

    private static final String EDGE_COLL_SUFFIX = "zzzcollzzz";

    private static final String EDGE_CONN_SUFFIX = "zzzconnzzz";

    private CpEntityManagerFactory emf;
    
    private CpManagerCache managerCache;

    private EntityManager em;

    private UUID applicationId;

    private EntityRef headEntity;

    private org.apache.usergrid.persistence.model.entity.Entity cpHeadEntity;

    private ApplicationScope applicationScope;
    private CollectionScope headEntityScope;

    private CassandraService cass;

    private IndexBucketLocator indexBucketLocator;



    public CpRelationManager() {}


    public CpRelationManager init( 
        EntityManager em, 
        CpEntityManagerFactory emf, 
        UUID applicationId,
        EntityRef headEntity, 
        IndexBucketLocator indexBucketLocator ) {

        Assert.notNull( em, "Entity manager cannot be null" );
        Assert.notNull( emf, "Entity manager factory cannot be null" );
        Assert.notNull( applicationId, "Application Id cannot be null" );
        Assert.notNull( headEntity, "Head entity cannot be null" );
        Assert.notNull( headEntity.getUuid(), "Head entity uuid cannot be null" );
       
        // TODO: this assert should not be failing
        //Assert.notNull( indexBucketLocator, "indexBucketLocator cannot be null" );

        this.em = em;
        this.emf = emf;
        this.applicationId = applicationId;
        this.headEntity = headEntity;
        this.managerCache = emf.getManagerCache();
        this.applicationScope = emf.getApplicationScope(applicationId);

        this.cass = em.getCass(); // TODO: eliminate need for this via Core Persistence
        this.indexBucketLocator = indexBucketLocator; // TODO: this also

        // load the Core Persistence version of the head entity as well
        this.headEntityScope = new CollectionScopeImpl( 
            this.applicationScope.getApplication(), 
            this.applicationScope.getApplication(), 
            CpEntityManager.getCollectionScopeNameFromEntityType( headEntity.getType()));

        EntityCollectionManager ecm = managerCache.getEntityCollectionManager(headEntityScope);
        if ( logger.isDebugEnabled() ) {
            logger.debug( "Loading head entity {}:{} from scope\n   app {}\n   owner {}\n   name {}", 
                new Object[] {
                    headEntity.getType(), 
                    headEntity.getUuid(), 
                    headEntityScope.getApplication(), 
                    headEntityScope.getOwner(),
                    headEntityScope.getName()
            } );
        }
        
        this.cpHeadEntity = ecm.load( new SimpleId( 
            headEntity.getUuid(), headEntity.getType() )).toBlockingObservable().last();

        // commented out because it is possible that CP entity has not been created yet
        Assert.notNull( cpHeadEntity, "cpHeadEntity cannot be null" );

        return this;
    }

    
    static String getEdgeTypeFromCollectionName( String name ) {
        String csn = name + EDGE_COLL_SUFFIX;
        return csn;
    }

    static String getEdgeTypeFromConnectionType( String type ) {
        String csn = type + EDGE_CONN_SUFFIX;
        return csn;
    }


    static boolean isConnectionEdgeType( String type )  {
        return type.endsWith( EDGE_COLL_SUFFIX );
    }

    
    public String getConnectionName( String edgeType ) {
        return edgeType.substring( 0, edgeType.indexOf(EDGE_COLL_SUFFIX));
    }


    @Override
    public Set<String> getCollectionIndexes(String collectionName) throws Exception {
        final Set<String> indexes = new HashSet<String>();

        GraphManager gm = managerCache.getGraphManager(applicationScope);

        Observable<String> types= gm.getEdgeTypesFromSource( 
            new SimpleSearchEdgeType( cpHeadEntity.getId(), null,  null ));

        Iterator<String> iter = types.toBlockingObservable().getIterator();
        while ( iter.hasNext() ) {
            indexes.add( iter.next() );
        }
        return indexes;
    }


    @Override
    public Map<String, Map<UUID, Set<String>>> getOwners() throws Exception {

        Map<EntityRef, Set<String>> containerEntities = getContainingCollections();

        Map<String, Map<UUID, Set<String>>> owners = 
                new LinkedHashMap<String, Map<UUID, Set<String>>>();

        for ( EntityRef owner : containerEntities.keySet() ) {
            Set<String> collections = containerEntities.get( owner );
            for ( String collection : collections ) {
                MapUtils.addMapMapSet( owners, owner.getType(), owner.getUuid(), collection );
            }
        }

        return owners;
    }


    private Map<EntityRef, Set<String>> getContainingCollections() {
        return getContainingCollections( -1, null );
    }


    private Map<EntityRef, Set<String>> getContainingCollections( int limit, String containingEntityType ) {

        Map<EntityRef, Set<String>> results = new LinkedHashMap<EntityRef, Set<String>>();

        GraphManager gm = managerCache.getGraphManager(applicationScope);

        Iterator<String> edgeTypes = gm.getEdgeTypesToTarget( new SimpleSearchEdgeType( 
            cpHeadEntity.getId(), null, null) ).toBlockingObservable().getIterator();

        while ( edgeTypes.hasNext() ) {

            String edgeType = edgeTypes.next();

            Observable<Edge> edges = gm.loadEdgesToTarget( new SimpleSearchByEdgeType(
                cpHeadEntity.getId(), edgeType, Long.MAX_VALUE, null ));

            Iterator<Edge> iter = edges.toBlockingObservable().getIterator();
            while ( iter.hasNext() ) {
                Edge edge = iter.next();

                if ( !isConnectionEdgeType( edge.getType()) ) {
                    continue;
                }

                EntityRef eref = new SimpleEntityRef( 
                    edge.getSourceNode().getType(), edge.getSourceNode().getUuid() );

                if ( containingEntityType != null && !containingEntityType.equals( eref.getType() )) {
                    continue;
                }

                String connectionName = null;
                if ( isConnectionEdgeType( edge.getType() )) {
                    connectionName = getConnectionName( edge.getType() );
                }
                addMapSet( results, eref, connectionName );
            }

            if ( limit > 0 && results.keySet().size() >= limit ) {
                break;
            }
        }

        EntityRef applicationRef = new SimpleEntityRef( TYPE_APPLICATION, applicationId );
        if ( !results.containsKey( applicationRef ) ) {

            if ( containingEntityType != null && !containingEntityType.equals( applicationRef.getType())) {
                addMapSet( results, applicationRef, 
                    CpEntityManager.getCollectionScopeNameFromEntityType( headEntity.getType() ) );
            }
        }
        return results;
    }


    @SuppressWarnings("unchecked")
    @Metered(group = "core", name = "RelationManager_isOwner")
    @Override
    public boolean isCollectionMember(String collName, EntityRef entity) throws Exception {

        Id entityId = new SimpleId( entity.getUuid(), entity.getType() );

        GraphManager gm = managerCache.getGraphManager(applicationScope);
        Observable<Edge> edges = gm.loadEdgeVersions( 
            new SimpleSearchByEdge(
                new SimpleId(headEntity.getUuid(), headEntity.getType()), 
                getEdgeTypeFromCollectionName( collName ),  
                entityId, 
                Long.MAX_VALUE,
                null));

        return edges.toBlockingObservable().firstOrDefault(null) != null;
    }


   private boolean moreThanOneInboundConnection( 
           EntityRef target, String connectionType ) {

        Id targetId = new SimpleId( target.getUuid(), target.getType() );

        GraphManager gm = managerCache.getGraphManager(applicationScope);

        Observable<Edge> edgesToTarget = gm.loadEdgesToTarget( new SimpleSearchByEdgeType(
            targetId,
            CpRelationManager.getEdgeTypeFromConnectionType( connectionType ),
            System.currentTimeMillis(),
            null)); // last

        Iterator<Edge> iterator = edgesToTarget.toBlockingObservable().getIterator();
        int count = 0;
        while ( iterator.hasNext() ) {
            iterator.next();
            if ( count++ > 1 ) { 
                return true;
            }
        } 
        return false;
   } 

   private boolean moreThanOneOutboundConnection( 
           EntityRef source, String connectionType ) {

        Id sourceId = new SimpleId( source.getUuid(), source.getType() );

        GraphManager gm = managerCache.getGraphManager(applicationScope);

        Observable<Edge> edgesFromSource = gm.loadEdgesFromSource(new SimpleSearchByEdgeType(
            sourceId,
            CpRelationManager.getEdgeTypeFromConnectionType( connectionType ),
            System.currentTimeMillis(),
            null)); // last
        
        Iterator<Edge> iterator = edgesFromSource.toBlockingObservable().getIterator();
        int count = 0;
        while ( iterator.hasNext() ) {
            iterator.next();
            if ( count++ > 1 ) { 
                return true;
            }
        } 
        return false;
   } 


    @Override
    public boolean isConnectionMember(String connectionName, EntityRef entity) throws Exception {

        Id entityId = new SimpleId( entity.getUuid(), entity.getType() );

        GraphManager gm = managerCache.getGraphManager(applicationScope);
        Observable<Edge> edges = gm.loadEdgeVersions( 
            new SimpleSearchByEdge(
                new SimpleId(headEntity.getUuid(), headEntity.getType()), 
                getEdgeTypeFromConnectionType( connectionName ),  
                entityId, 
                Long.MAX_VALUE,
                null));

        return edges.toBlockingObservable().firstOrDefault(null) != null;
    }

    @Override
    public Set<String> getCollections() throws Exception {

        Map<String, CollectionInfo> collections = 
                getDefaultSchema().getCollections( headEntity.getType() );
        if ( collections == null ) {
            return null;
        }

        return collections.keySet();
    }

    @Override
    public Results getCollection(String collectionName, UUID startResult, int count, 
            Level resultsLevel, boolean reversed) throws Exception {

        // TODO: how to set Query startResult?

        Query query = Query.fromQL("select *");
        query.setLimit(count);
        query.setReversed(reversed);

        return searchCollection(collectionName, query);
    }

    @Override
    public Results getCollection(
            String collName, Query query, Level level) throws Exception {

        return searchCollection(collName, query);
    }


    // add to a named collection of the head entity
    @Override
    public Entity addToCollection(String collName, EntityRef itemRef) throws Exception {
       
        CollectionInfo collection = getDefaultSchema().getCollection( headEntity.getType(), collName );
        if ( ( collection != null ) && !collection.getType().equals( itemRef.getType() ) ) {
            return null;
        }

        return addToCollection( collName, itemRef, collection.getLinkedCollection() != null );
    }

    public Entity addToCollection(String collName, EntityRef itemRef, boolean connectBack ) throws Exception {

        Entity itemEntity = em.get( itemRef );

        if ( itemEntity == null ) {
            return null;
        }

        CollectionInfo collection = getDefaultSchema().getCollection( headEntity.getType(), collName );
        if ( ( collection != null ) && !collection.getType().equals( itemRef.getType() ) ) {
            return null;
        }


        // load the new member entity to be added to the collection from its default scope
        CollectionScope memberScope = new CollectionScopeImpl( 
            applicationScope.getApplication(), 
            applicationScope.getApplication(), 
            CpEntityManager.getCollectionScopeNameFromEntityType( itemRef.getType()));
        EntityCollectionManager memberMgr = managerCache.getEntityCollectionManager(memberScope);

        if ( logger.isDebugEnabled() ) {
            logger.debug("Loading member entity {}:{} from scope\n   app {}\n   owner {}\n   name {}", 
                new Object[] { 
                    itemRef.getType(), 
                    itemRef.getUuid(), 
                    memberScope.getApplication(), 
                    memberScope.getOwner(), 
                    memberScope.getName() 
            });
        }

        org.apache.usergrid.persistence.model.entity.Entity memberEntity = memberMgr.load(
            new SimpleId( itemRef.getUuid(), itemRef.getType() )).toBlockingObservable().last();

        if ( memberEntity == null ) {
            throw new RuntimeException("Unable to load entity uuid=" 
                + itemRef.getUuid() + " type=" + itemRef.getType());
        }

        // create graph edge connection from head entity to member entity
        Edge edge = new SimpleEdge(
            cpHeadEntity.getId(), 
            getEdgeTypeFromCollectionName( collName ), 
            memberEntity.getId(), 
            memberEntity.getId().getUuid().timestamp() );
        GraphManager gm = managerCache.getGraphManager(applicationScope);
        gm.writeEdge(edge).toBlockingObservable().last();

        // index member into entity connection | type scope
        IndexScope collectionIndexScope = new IndexScopeImpl(
            applicationScope.getApplication(), 
            cpHeadEntity.getId(), 
            CpEntityManager.getCollectionScopeNameFromCollectionName( collName ));
        EntityIndex collectionIndex = managerCache.getEntityIndex(collectionIndexScope);
        collectionIndex.index( memberEntity );

        // index member into entity connection | all-types scope
        IndexScope allCollectionsIndexScope = new IndexScopeImpl(
            applicationScope.getApplication(), 
            cpHeadEntity.getId(), 
            ALL_TYPES);
        EntityIndex allCollectionIndex = managerCache.getEntityIndex(allCollectionsIndexScope);
        allCollectionIndex.index( memberEntity );

        logger.debug("Added entity {}:{} to collection {}", new String[] { 
            itemRef.getUuid().toString(), itemRef.getType(), collName }); 

        logger.debug("With head entity scope is {}:{}:{}", new String[] { 
            headEntityScope.getApplication().toString(), 
            headEntityScope.getOwner().toString(),
            headEntityScope.getName()}); 

        if ( connectBack ) {
            getRelationManager( itemEntity )
                    .addToCollection( collection.getLinkedCollection(), headEntity, false );
        }

        return itemEntity;
    }


    @Override
    public Entity addToCollections(List<EntityRef> owners, String collName) throws Exception {

        // TODO: this addToCollections() implementation seems wrong.
        for ( EntityRef eref : owners ) {
            addToCollection( collName, eref ); 
        }

        return null;
    }


    @Override
    @Metered(group = "core", name = "RelationManager_createItemInCollection")
    public Entity createItemInCollection(
        String collName, String itemType, Map<String, Object> properties) throws Exception {

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

        else if ( headEntity.getType().equals( Group.ENTITY_TYPE ) 
                && ( collName.equals( COLLECTION_ROLES ) ) ) {
            UUID groupId = headEntity.getUuid();
            String roleName = ( String ) properties.get( PROPERTY_NAME );
            return em.createGroupRole( groupId, roleName, ( Long ) properties.get( PROPERTY_INACTIVITY ) );
        }

        CollectionInfo collection = getDefaultSchema().getCollection(headEntity.getType(),collName);
        if ( ( collection != null ) && !collection.getType().equals( itemType ) ) {
            return null;
        }

        properties = getDefaultSchema().cleanUpdatedProperties( itemType, properties, true );

        Entity itemEntity = em.create( itemType, properties );

        if ( itemEntity != null ) {

            addToCollection( collName, itemEntity );

            if ( collection.getLinkedCollection() != null ) {
                getRelationManager(  getHeadEntity() )
                    .addToCollection( collection.getLinkedCollection(),itemEntity);
            }
        }

        return itemEntity;  
    }

    @Override
    public void removeFromCollection(String collName, EntityRef itemRef) throws Exception {

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
        CollectionScope memberScope = new CollectionScopeImpl( 
            this.applicationScope.getApplication(), 
            this.applicationScope.getApplication(), 
            CpEntityManager.getCollectionScopeNameFromEntityType( itemRef.getType() ));
        EntityCollectionManager memberMgr = managerCache.getEntityCollectionManager(memberScope);

        if ( logger.isDebugEnabled() ) {
            logger.debug("Loading entity to remove from collection "
                    + "{}:{} from scope\n   app {}\n   owner {}\n   name {}", 
                new Object[] { 
                    itemRef.getType(), 
                    itemRef.getUuid(), 
                    memberScope.getApplication(), 
                    memberScope.getOwner(), 
                    memberScope.getName() 
            });
        }

        org.apache.usergrid.persistence.model.entity.Entity memberEntity = memberMgr.load(
            new SimpleId( itemRef.getUuid(), itemRef.getType() )).toBlockingObservable().last();

        IndexScope indexScope = new IndexScopeImpl(
            applicationScope.getApplication(), 
            cpHeadEntity.getId(), 
            CpEntityManager.getCollectionScopeNameFromCollectionName( collName ));

        // remove from collection index
        EntityIndex ei = managerCache.getEntityIndex(indexScope);
        ei.deindex( memberEntity );

        // remove collection edge
        Edge edge = new SimpleEdge( 
            cpHeadEntity.getId(),
            getEdgeTypeFromCollectionName( collName ), 
            memberEntity.getId(), 
            memberEntity.getId().getUuid().timestamp() );
        GraphManager gm = managerCache.getGraphManager(applicationScope);
        gm.deleteEdge(edge).toBlockingObservable().last();

        // special handling for roles collection of a group
        if ( headEntity.getType().equals( Group.ENTITY_TYPE ) ) {
            if ( collName.equals( COLLECTION_ROLES ) ) {
                String path = (String)( (Entity)itemRef ).getMetadata( "path" );

                if ( path.startsWith( "/roles/" ) ) {

                    Entity itemEntity = em.get( new SimpleEntityRef( 
                        memberEntity.getId().getType(), memberEntity.getId().getUuid() ) );

                    RoleRef roleRef = SimpleRoleRef.forRoleEntity( itemEntity );
                    em.deleteRole( roleRef.getApplicationRoleName() );
                }
            }
        }
    }


    @Override
    public void copyRelationships(String srcRelationName, EntityRef dstEntityRef, 
            String dstRelationName) throws Exception {

        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results searchCollection(String collName, Query query) throws Exception {

        if ( query == null ) {
            query = new Query();
        }

        headEntity = em.validate( headEntity );

        CollectionInfo collection = 
            getDefaultSchema().getCollection( headEntity.getType(), collName );

        IndexScope indexScope = new IndexScopeImpl(
            applicationScope.getApplication(), 
            cpHeadEntity.getId(), 
            CpEntityManager.getCollectionScopeNameFromCollectionName( collName ));
        EntityIndex ei = managerCache.getEntityIndex(indexScope);
      
        logger.debug("Searching scope {}:{}:{}",
            new String[] { 
                indexScope.getApplication().toString(), 
                indexScope.getOwner().toString(),
                indexScope.getName() }); 

        query.setEntityType( collection.getType() );
        query = adjustQuery( query );

        CandidateResults crs = ei.search( query );

        return buildResults( query, crs, collName );
    }


    @Override
    @Metered(group = "core", name = "RelationManager_createConnection_connection_ref")
    public ConnectionRef createConnection( ConnectionRef connection ) throws Exception {
        
        return createConnection( connection.getConnectionType(), connection.getConnectedEntity() );
    }


    @Override
    @Metered(group = "core", name = "RelationManager_createConnection_connectionType")
    public ConnectionRef createConnection( 
            String connectionType, EntityRef connectedEntityRef ) throws Exception {

        headEntity = em.validate( headEntity );
        connectedEntityRef = em.validate( connectedEntityRef );

        ConnectionRefImpl connection = new ConnectionRefImpl( 
            headEntity, connectionType, connectedEntityRef );

        CollectionScope targetScope = new CollectionScopeImpl( 
            applicationScope.getApplication(), 
            applicationScope.getApplication(), 
            CpEntityManager.getCollectionScopeNameFromEntityType( connectedEntityRef.getType() ));

        EntityCollectionManager targetEcm = managerCache.getEntityCollectionManager(targetScope);

        if ( logger.isDebugEnabled() ) {
            logger.debug("Creating connection '{}' from source {}:{}]\n"
                    + "   to target {}:{}"
                    + "   from scope\n      app {}\n      owner {}\n      name {}", 
                new Object[] { 
                    connectionType,
                    headEntity.getType(), 
                    headEntity.getUuid(), 
                    connectedEntityRef.getType(), 
                    connectedEntityRef.getUuid(), 
                    targetScope.getApplication(), 
                    targetScope.getOwner(), 
                    targetScope.getName() 
            });
        }

        org.apache.usergrid.persistence.model.entity.Entity targetEntity = targetEcm.load(
            new SimpleId( connectedEntityRef.getUuid(), connectedEntityRef.getType() ))
                .toBlockingObservable().last();

        // create graph edge connection from head entity to member entity
        Edge edge = new SimpleEdge( 
            cpHeadEntity.getId(), 
            getEdgeTypeFromConnectionType( connectionType ),
            targetEntity.getId(), 
            System.currentTimeMillis() );
        GraphManager gm = managerCache.getGraphManager(applicationScope);
        gm.writeEdge(edge).toBlockingObservable().last();

        // Index the new connection in app|source|type context
        IndexScope indexScope = new IndexScopeImpl(
            applicationScope.getApplication(), 
            cpHeadEntity.getId(), 
            CpEntityManager.getConnectionScopeName( connectedEntityRef.getType(), connectionType ));
        EntityIndex ei = managerCache.getEntityIndex(indexScope);
        ei.index( targetEntity );

        // Index the new connection in app|source|type context
        IndexScope allTypesIndexScope = new IndexScopeImpl(
            applicationScope.getApplication(), 
            cpHeadEntity.getId(), 
            ALL_TYPES);
        EntityIndex aei = managerCache.getEntityIndex(allTypesIndexScope);
        aei.index( targetEntity );

        Keyspace ko = cass.getApplicationKeyspace( applicationId );
        Mutator<ByteBuffer> m = createMutator( ko, be );
        batchUpdateEntityConnection( m, false, connection, UUIDGenerator.newTimeUUID() );
        batchExecute( m, CassandraService.RETRY_COUNT );

        return connection;
    }

    
    @SuppressWarnings("unchecked")
    @Metered(group = "core", name = "CpRelationManager_batchUpdateEntityConnection")
    public Mutator<ByteBuffer> batchUpdateEntityConnection( Mutator<ByteBuffer> batch, 
        boolean disconnect, ConnectionRefImpl connection, UUID timestampUuid ) throws Exception {

        long timestamp = getTimestampInMicros( timestampUuid );

        Entity connectedEntity = em.get( new SimpleEntityRef( 
                connection.getConnectedEntityType(), connection.getConnectedEntityId()) );

        if ( connectedEntity == null ) {
            return batch;
        }

        // Create connection for requested params

        if ( disconnect ) {
            
            addDeleteToMutator( batch, ENTITY_COMPOSITE_DICTIONARIES,
                key( connection.getConnectingEntityId(), DICTIONARY_CONNECTED_ENTITIES,
                    connection.getConnectionType() ),
                asList( connection.getConnectedEntityId(), 
                        connection.getConnectedEntityType() ), timestamp );

            addDeleteToMutator( batch, ENTITY_COMPOSITE_DICTIONARIES,
                key( connection.getConnectedEntityId(), DICTIONARY_CONNECTING_ENTITIES,
                    connection.getConnectionType() ),
                asList( connection.getConnectingEntityId(), 
                        connection.getConnectingEntityType() ), timestamp );

            // delete the connection path if there will be no connections left

            // check out outbound edges of the given type.  If we have more than the 1 specified,
            // we shouldn't delete the connection types from our outbound index
            if ( !moreThanOneOutboundConnection( 
                connection.getConnectingEntity(), connection.getConnectionType() ) ) {

                addDeleteToMutator( batch, ENTITY_DICTIONARIES,
                    key( connection.getConnectingEntityId(), DICTIONARY_CONNECTED_TYPES ),
                    connection.getConnectionType(), timestamp );
            }

            //check out inbound edges of the given type.  If we have more than the 1 specified,
            // we shouldn't delete the connection types from our outbound index
            if ( !moreThanOneInboundConnection( 
               connection.getConnectingEntity(), connection.getConnectionType() ) ) {

                addDeleteToMutator( batch, ENTITY_DICTIONARIES,
                        key( connection.getConnectedEntityId(), DICTIONARY_CONNECTING_TYPES ),
                        connection.getConnectionType(), timestamp );
            }

        } else {

            addInsertToMutator( batch, ENTITY_COMPOSITE_DICTIONARIES,
                key( connection.getConnectingEntityId(), DICTIONARY_CONNECTED_ENTITIES,
                    connection.getConnectionType() ),
                asList( connection.getConnectedEntityId(), connection.getConnectedEntityType() ), 
                    timestamp, timestamp );

            addInsertToMutator( batch, ENTITY_COMPOSITE_DICTIONARIES,
                key( connection.getConnectedEntityId(), DICTIONARY_CONNECTING_ENTITIES,
                    connection.getConnectionType() ),
                asList( connection.getConnectingEntityId(), connection.getConnectingEntityType() ), 
                    timestamp, timestamp );

            // Add connection type to connections set
            addInsertToMutator( batch, ENTITY_DICTIONARIES,
                key( connection.getConnectingEntityId(), DICTIONARY_CONNECTED_TYPES ),
                connection.getConnectionType(), null, timestamp );

            // Add connection type to connections set
            addInsertToMutator( batch, ENTITY_DICTIONARIES,
                key( connection.getConnectedEntityId(), DICTIONARY_CONNECTING_TYPES ),
                connection.getConnectionType(), null, timestamp );
        }

        // Add indexes for the connected entity's list properties

        // Get the names of the list properties in the connected entity
        Set<String> dictionaryNames = em.getDictionaryNames( connectedEntity );

        // For each list property, get the values in the list and
        // update the index with those values

        Schema schema = getDefaultSchema();

        for ( String dictionaryName : dictionaryNames ) {
            boolean has_dictionary = schema.hasDictionary( connectedEntity.getType(), dictionaryName );
            boolean dictionary_indexed = schema.isDictionaryIndexedInConnections( 
                connectedEntity.getType(), dictionaryName );

            if ( dictionary_indexed || !has_dictionary ) {
                Set<Object> elementValues = em.getDictionaryAsSet( connectedEntity, dictionaryName );
                for ( Object elementValue : elementValues ) {
                    IndexUpdate indexUpdate =
                        batchStartIndexUpdate( batch, connectedEntity, dictionaryName, 
                            elementValue, timestampUuid, has_dictionary, true, disconnect, false );
                    batchUpdateConnectionIndex( indexUpdate, connection );
                }
            }
        }

        return batch;
    }


    @Override
    @Metered(group = "core", name = "RelationManager_createConnection_paired_connection_type")
    public ConnectionRef createConnection( 
            String pairedConnectionType, EntityRef pairedEntity, String connectionType,
            EntityRef connectedEntityRef ) throws Exception {
        
        throw new UnsupportedOperationException("Paired connections not supported"); 
    }


    @Override
    @Metered(group = "core", name = "RelationManager_createConnection_connected_entity_ref")
    public ConnectionRef createConnection( ConnectedEntityRef... connections ) throws Exception {

        throw new UnsupportedOperationException("Paired connections not supported"); 
    }

    @Override
    public ConnectionRef connectionRef(
            String connectionType, 
            EntityRef connectedEntityRef) throws Exception {

        ConnectionRef connection = new ConnectionRefImpl( 
                headEntity, connectionType, connectedEntityRef );

        return connection;
    }

    @Override
    public ConnectionRef connectionRef(String pairedConnectionType, EntityRef pairedEntity, 
            String connectionType, EntityRef connectedEntityRef) throws Exception {

        throw new UnsupportedOperationException("Paired connections not supported"); 
    }

    @Override
    public ConnectionRef connectionRef(ConnectedEntityRef... connections) {

        throw new UnsupportedOperationException("Paired connections not supported"); 
    }

    @Override
    public void deleteConnection(ConnectionRef connectionRef) throws Exception {
       
        // First, clean up the dictionary records of the connection
        Keyspace ko = cass.getApplicationKeyspace( applicationId );
        Mutator<ByteBuffer> m = createMutator( ko, be );
        batchUpdateEntityConnection( 
            m, true, (ConnectionRefImpl)connectionRef, UUIDGenerator.newTimeUUID() );
        batchExecute( m, CassandraService.RETRY_COUNT );

        EntityRef connectingEntityRef = connectionRef.getConnectingEntity();  // source
        EntityRef connectedEntityRef = connectionRef.getConnectedEntity();  // target

        String connectionType = connectionRef.getConnectedEntity().getConnectionType();
        
        CollectionScope targetScope = new CollectionScopeImpl( 
            applicationScope.getApplication(), 
            applicationScope.getApplication(), 
            CpEntityManager.getCollectionScopeNameFromEntityType( connectedEntityRef.getType() ));

        EntityCollectionManager targetEcm = managerCache.getEntityCollectionManager(targetScope);

        if ( logger.isDebugEnabled() ) {
            logger.debug("Deleting connection '{}' from source {}:{} \n   to target {}:{}",
                new Object[] { 
                    connectionType,
                    connectingEntityRef.getType(), 
                    connectingEntityRef.getUuid(), 
                    connectedEntityRef.getType(), 
                    connectedEntityRef.getUuid()
            });
        }

        org.apache.usergrid.persistence.model.entity.Entity targetEntity = targetEcm.load(
            new SimpleId( connectedEntityRef.getUuid(), connectedEntityRef.getType() ))
                .toBlockingObservable().last();

        // Delete graph edge connection from head entity to member entity
        Edge edge = new SimpleEdge( 
            new SimpleId( connectingEntityRef.getUuid(), connectingEntityRef.getType() ),
            connectionType,
            targetEntity.getId(), 
            System.currentTimeMillis() );
        GraphManager gm = managerCache.getGraphManager(applicationScope);
        gm.deleteEdge(edge).toBlockingObservable().last();

        // Deindex the connection in app|source|type context
        IndexScope indexScope = new IndexScopeImpl(
            applicationScope.getApplication(), 
            new SimpleId( connectingEntityRef.getUuid(), connectingEntityRef.getType() ),
            CpEntityManager.getConnectionScopeName( targetEntity.getId().getType(), connectionType ));
        EntityIndex ei = managerCache.getEntityIndex( indexScope );
        ei.deindex( targetEntity );

        // Deindex the connection in app|source|type context
        IndexScope allTypesIndexScope = new IndexScopeImpl(
            applicationScope.getApplication(), 
            new SimpleId( connectingEntityRef.getUuid(), connectingEntityRef.getType() ),
            ALL_TYPES);
        EntityIndex aei = managerCache.getEntityIndex(allTypesIndexScope);
        aei.deindex( targetEntity );

    }


    @Override
    public Set<String> getConnectionTypes(UUID connectedEntityId) throws Exception {
        throw new UnsupportedOperationException("Cannot specify entity by UUID alone."); 
    }

    @Override
    public Set<String> getConnectionTypes() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getConnectionTypes(boolean filterConnection) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }


    @Override
    public Results getConnectedEntities( 
        String connectionType, String connectedEntityType, Level level) throws Exception {

        Results raw = null;

        Query query = new Query();
        query.setConnectionType(connectionType);
        query.setEntityType(connectedEntityType);

        if ( connectionType == null ) {
            raw = searchConnectedEntities( query );

        } else {

            headEntity = em.validate( headEntity );

            IndexScope indexScope = new IndexScopeImpl(
                applicationScope.getApplication(), 
                cpHeadEntity.getId(), 
                CpEntityManager.getConnectionScopeName( connectedEntityType, connectionType ));
            EntityIndex ei = managerCache.getEntityIndex(indexScope);
        
            logger.debug("Searching connected entities from scope {}:{}:{}", new String[] { 
                indexScope.getApplication().toString(), 
                indexScope.getOwner().toString(),
                indexScope.getName()}); 

            query = adjustQuery( query );
            CandidateResults crs = ei.search( query );

            raw = buildResults( query , crs, query.getConnectionType() );
        }

        if ( Level.REFS.equals(level ) ) {
            List<EntityRef> refList = new ArrayList<EntityRef>( raw.getEntities() );
            return Results.fromRefList( refList );
        } 

        if ( Level.IDS.equals(level ) ) {
            // TODO: someday this should return a list of Core Persistence Ids
            List<UUID> idList = new ArrayList<UUID>();
            for ( EntityRef ref : raw.getEntities() ) {
                idList.add( ref.getUuid() );
            }
            return Results.fromIdList( idList );
        }

        List<Entity> entities = new ArrayList<Entity>();
        for ( EntityRef ref : raw.getEntities() ) {
            Entity entity = em.get( ref );
            entities.add( entity );
        }
        
        return Results.fromEntities( entities );
    }


    @Override
    public Results getConnectingEntities(String connectionType, String connectedEntityType, 
            Level resultsLevel) throws Exception {

        return getConnectingEntities( connectionType, connectedEntityType, resultsLevel, -1 );
    }

    @Override
    public Results getConnectingEntities(
            String connectionType, String entityType, Level level, int count) throws Exception {

        Map<EntityRef, Set<String>> containers = getContainingCollections( count, entityType );

        if ( Level.REFS.equals(level ) ) {
            List<EntityRef> refList = new ArrayList<EntityRef>( containers.keySet() );
            return Results.fromRefList( refList );
        } 

        if ( Level.IDS.equals(level ) ) {
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
            logger.debug("   Found connecting entity: " + entity.getProperties());
            entities.add( entity );
        }
        return Results.fromEntities( entities );
    }


    @Override
    public Results searchConnectedEntities( Query query ) throws Exception {

        if ( query == null ) {
            query = new Query();
        }

        headEntity = em.validate( headEntity );

        if ( query.getEntityType() == null ) {

            // search across all types of collections of the head-entity
            IndexScope indexScope = new IndexScopeImpl(
                applicationScope.getApplication(), 
                cpHeadEntity.getId(), 
                ALL_TYPES);
            EntityIndex ei = managerCache.getEntityIndex(indexScope);
        
            logger.debug("Searching connections from the all-types scope {}:{}:{}", new String[] { 
                indexScope.getApplication().toString(), 
                indexScope.getOwner().toString(),
                indexScope.getName()}); 

            query = adjustQuery( query );
            CandidateResults crs = ei.search( query );

            return buildConnectionResults(query , crs, query.getConnectionType() );
        }

        IndexScope indexScope = new IndexScopeImpl(
            applicationScope.getApplication(), 
            cpHeadEntity.getId(), 
            CpEntityManager.getConnectionScopeName( 
                    query.getEntityType(), query.getConnectionType() ));
        EntityIndex ei = managerCache.getEntityIndex(indexScope);
    
        logger.debug("Searching connections from the '{}' scope {}:{}:{}", new String[] { 
            indexScope.getApplication().toString(), 
            indexScope.getOwner().toString(),
            indexScope.getName()}); 

        query = adjustQuery( query );
        CandidateResults crs = ei.search( query );

        return buildConnectionResults(query , crs, query.getConnectionType() );
    }


    private Query adjustQuery( Query query ) {

        // handle the select by identifier case
        if ( query.getRootOperand() == null ) {

            // a name alias or email alias was specified
            if ( query.containsSingleNameOrEmailIdentifier() ) {

                Identifier ident = query.getSingleIdentifier();

                // an email was specified.  An edge case that only applies to users.  
                // This is fulgy to put here, but required.
                if ( query.getEntityType().equals( User.ENTITY_TYPE ) && ident.isEmail() ) {

                    Query newQuery = Query.fromQL(
                        "select * where email='" + query.getSingleNameOrEmailIdentifier()+ "'");
                    query.setRootOperand( newQuery.getRootOperand() );
                }

                // use the ident with the default alias. could be an email
                else {

                    Query newQuery = Query.fromQL(
                        "select * where name='" + query.getSingleNameOrEmailIdentifier()+ "'");
                    query.setRootOperand( newQuery.getRootOperand() );
                }

            } else if ( query.containsSingleUuidIdentifier() ) {

                Query newQuery = Query.fromQL(
                        "select * where uuid='" + query.getSingleUuidIdentifier() + "'");
                query.setRootOperand( newQuery.getRootOperand() );
            }
        }

        if ( query.isReversed() ) {

            Query.SortPredicate newsp = new Query.SortPredicate( 
                PROPERTY_CREATED, Query.SortDirection.DESCENDING );
            query.addSort( newsp ); 
        }

        // reverse chrono order by default
        if ( query.getSortPredicates().isEmpty() ) {

            Query.SortPredicate newsp = new Query.SortPredicate( 
                PROPERTY_CREATED, Query.SortDirection.ASCENDING);
            query.addSort( newsp ); 
        }


        return query;
    }


    @Override
    public Set<String> getConnectionIndexes(String connectionType) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    
    private CpRelationManager getRelationManager( EntityRef headEntity ) {
        CpRelationManager rmi = new CpRelationManager();
        rmi.init( em, emf, applicationId, headEntity, null);
        return rmi;
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


    private Results buildConnectionResults(
        Query query, CandidateResults crs, String connectionType ) {
       
        if ( query.getLevel().equals( Level.ALL_PROPERTIES )) {
            return buildResults( query, crs, connectionType );
        }

        final EntityRef sourceRef = 
                new SimpleEntityRef( headEntity.getType(), headEntity.getUuid() );

        List<ConnectionRef> refs = new ArrayList<ConnectionRef>( crs.size() );

        for ( CandidateResult cr : crs ) {

            SimpleEntityRef targetRef = 
                    new SimpleEntityRef( cr.getId().getType(), cr.getId().getUuid() );

            final ConnectionRef ref = new ConnectionRefImpl( sourceRef, connectionType, targetRef );

            refs.add( ref );
        }

        return Results.fromConnections( refs );
    }
    

    private Results buildResults(Query query, CandidateResults crs, String collName ) {

        Results results = null;

        if ( query.getLevel().equals( Level.IDS )) {
            
            // TODO: replace this with List<Id> someday
            List<UUID> ids = new ArrayList<UUID>();
            Iterator<CandidateResult> iter = crs.iterator();
            while ( iter.hasNext() ) {
                ids.add( iter.next().getId().getUuid() );
            }
            results = Results.fromIdList( ids );

        } else if ( query.getLevel().equals( Level.REFS )) {

            if ( crs.size() == 1 ) {
                CandidateResult cr = crs.iterator().next();
                results = Results.fromRef( 
                    new SimpleEntityRef( cr.getId().getType(), cr.getId().getUuid()));

            } else {

                List<EntityRef> entityRefs = new ArrayList<EntityRef>();
                Iterator<CandidateResult> iter = crs.iterator();
                while ( iter.hasNext() ) {
                    Id id = iter.next().getId();
                    entityRefs.add( new SimpleEntityRef( id.getType(), id.getUuid() ));
                } 
                results = Results.fromRefList(entityRefs);
            }

        } else {

            // first, build map of latest versions of entities
            Map<Id, org.apache.usergrid.persistence.model.entity.Entity> latestVersions = 
                new LinkedHashMap<Id, org.apache.usergrid.persistence.model.entity.Entity>();

            Iterator<CandidateResult> iter = crs.iterator();
            while ( iter.hasNext() ) {

                CandidateResult cr = iter.next();

                CollectionScope collScope = new CollectionScopeImpl( 
                    applicationScope.getApplication(), 
                    applicationScope.getApplication(), 
                    CpEntityManager.getCollectionScopeNameFromEntityType( cr.getId().getType() ));
                EntityCollectionManager ecm = managerCache.getEntityCollectionManager(collScope);

                if ( logger.isDebugEnabled() ) {
                    logger.debug("Loading entity {} from scope\n   app {}\n   owner {}\n   name {}", 
                    new Object[] { 
                        cr.getId(),
                        collScope.getApplication(), 
                        collScope.getOwner(), 
                        collScope.getName() 
                    });
                }

                org.apache.usergrid.persistence.model.entity.Entity e =
                    ecm.load( cr.getId() ).toBlockingObservable().last();

                if ( cr.getVersion().compareTo( e.getVersion()) > 0 )  {
                    logger.debug("Stale version uuid:{} type:{} version:{}", 
                        new Object[] {cr.getId().getUuid(), cr.getId().getType(), cr.getVersion()});
                    continue;
                }

                org.apache.usergrid.persistence.model.entity.Entity alreadySeen = 
                    latestVersions.get( e.getId() ); 
                if ( alreadySeen == null ) { // never seen it, so add to map
                    latestVersions.put( e.getId(), e);

                } else {
                    // we seen this id before, only add entity if newer version
                    if ( e.getVersion().compareTo( alreadySeen.getVersion() ) > 0 ) {
                        latestVersions.put( e.getId(), e);
                    }
                }
            }

            // now build collection of old-school entities
            List<Entity> entities = new ArrayList<Entity>();
            for ( Id id : latestVersions.keySet() ) {

                org.apache.usergrid.persistence.model.entity.Entity e =
                    latestVersions.get( id );

                Entity entity = EntityFactory.newEntity(
                    e.getId().getUuid(), e.getField("type").getValue().toString() );

                Map<String, Object> entityMap = CpEntityMapUtils.toMap( e );
                entity.addProperties( entityMap ); 
                entities.add( entity );
            }

            if ( entities.size() == 1 ) {
                results = Results.fromEntity( entities.get(0));
            } else {
                results = Results.fromEntities( entities );
            }
        }

        results.setCursor( crs.getCursor() );
        results.setQueryProcessor( new CpQueryProcessor(em, query, headEntity, collName) );

        return results;
    }

    @Override
    public void batchUpdateSetIndexes( Mutator<ByteBuffer> batch, String setName, Object elementValue,
                                       boolean removeFromSet, UUID timestampUuid ) throws Exception {

        Entity entity = getHeadEntity();

        elementValue = getDefaultSchema().validateEntitySetValue( 
                entity.getType(), setName, elementValue );

        IndexUpdate indexUpdate = batchStartIndexUpdate( batch, entity, setName, elementValue, 
                timestampUuid, true, true, removeFromSet, false );

        // Update collections
        Map<String, Set<CollectionInfo>> containers =
                getDefaultSchema().getContainersIndexingDictionary( entity.getType(), setName );

        if ( containers != null ) {
            Map<EntityRef, Set<String>> containerEntities = getContainingCollections();
            for ( EntityRef containerEntity : containerEntities.keySet() ) {
                if ( containerEntity.getType().equals( TYPE_APPLICATION ) && Schema
                        .isAssociatedEntityType( entity.getType() ) ) {
                    logger.debug( "Extended properties for {} not indexed by application", 
                            entity.getType() );
                    continue;
                }
                Set<String> collectionNames = containerEntities.get( containerEntity );
                Set<CollectionInfo> collections = containers.get( containerEntity.getType() );

                if ( collections != null ) {

                    for ( CollectionInfo collection : collections ) {
                        if ( collectionNames.contains( collection.getName() ) ) {
                            batchUpdateCollectionIndex( 
                                    indexUpdate, containerEntity, collection.getName() );
                        }
                    }
                }
            }
        }

        batchUpdateBackwardConnectionsDictionaryIndexes( indexUpdate );
    }

    /**
     * Batch update collection index.
     *
     * @param indexUpdate The update to apply
     * @param owner The entity that is the owner context of this entity update.  
     * Can either be an application, or
     * another entity
     * @param collectionName the collection name
     *
     * @return The indexUpdate with batch mutations
     *
     * @throws Exception the exception
     */
    @Metered(group = "core", name = "RelationManager_batchUpdateCollectionIndex")
    public IndexUpdate batchUpdateCollectionIndex( 
            IndexUpdate indexUpdate, EntityRef owner, String collectionName )
            throws Exception {

        logger.debug( "batchUpdateCollectionIndex" );

        Entity indexedEntity = indexUpdate.getEntity();

        String bucketId = indexBucketLocator .getBucket( applicationId, 
                IndexBucketLocator.IndexType.COLLECTION, indexedEntity.getUuid(), 
                indexedEntity.getType(), indexUpdate.getEntryName() );

        // the root name without the bucket
        // entity_id,collection_name,prop_name,
        Object index_name = null;
        // entity_id,collection_name,prop_name, bucketId
        Object index_key = null;

        // entity_id,collection_name,collected_entity_id,prop_name

        for ( IndexUpdate.IndexEntry entry : indexUpdate.getPrevEntries() ) {

            if ( entry.getValue() != null ) {

                index_name = key( owner.getUuid(), collectionName, entry.getPath() );

                index_key = key( index_name, bucketId );

                addDeleteToMutator( indexUpdate.getBatch(), ENTITY_INDEX, index_key, 
                        entry.getIndexComposite(), indexUpdate.getTimestamp() );

                if ( "location.coordinates".equals( entry.getPath() ) ) {
                    EntityLocationRef loc = new EntityLocationRef( indexUpdate.getEntity(), 
                            entry.getTimestampUuid(), entry.getValue().toString() );
                    batchRemoveLocationFromCollectionIndex( indexUpdate.getBatch(), 
                            indexBucketLocator, applicationId, index_name, loc );
                }
            }
            else {
                logger.error( "Unexpected condition - deserialized property value is null" );
            }
        }

        if ( ( indexUpdate.getNewEntries().size() > 0 ) 
                && ( !indexUpdate.isMultiValue() || ( indexUpdate.isMultiValue()
                && !indexUpdate.isRemoveListEntry() ) ) ) {

            for ( IndexUpdate.IndexEntry indexEntry : indexUpdate.getNewEntries() ) {

                // byte valueCode = indexEntry.getValueCode();

                index_name = key( owner.getUuid(), collectionName, indexEntry.getPath() );

                index_key = key( index_name, bucketId );

                // int i = 0;

                addInsertToMutator( indexUpdate.getBatch(), ENTITY_INDEX, index_key, 
                        indexEntry.getIndexComposite(), null, indexUpdate.getTimestamp() );

                if ( "location.coordinates".equals( indexEntry.getPath() ) ) {
                    EntityLocationRef loc = new EntityLocationRef( 
                            indexUpdate.getEntity(), indexEntry.getTimestampUuid(),
                            indexEntry.getValue().toString() );
                    batchStoreLocationInCollectionIndex( indexUpdate.getBatch(), 
                            indexBucketLocator, applicationId,
                            index_name, indexedEntity.getUuid(), loc );
                }

                // i++;
            }
        }

        for ( String index : indexUpdate.getIndexesSet() ) {
            addInsertToMutator( indexUpdate.getBatch(), ENTITY_DICTIONARIES,
                    key( owner.getUuid(), collectionName, Schema.DICTIONARY_INDEXES ), index, null,
                    indexUpdate.getTimestamp() );
        }

        return indexUpdate;
    }


    public IndexUpdate batchStartIndexUpdate( 
            Mutator<ByteBuffer> batch, Entity entity, String entryName,
            Object entryValue, UUID timestampUuid, boolean schemaHasProperty,
             boolean isMultiValue, boolean removeListEntry, boolean fulltextIndexed )
            throws Exception {
        return batchStartIndexUpdate( batch, entity, entryName, entryValue, timestampUuid, 
                schemaHasProperty, isMultiValue, removeListEntry, fulltextIndexed, false );
    }


    @Metered(group = "core", name = "RelationManager_batchStartIndexUpdate")
    public IndexUpdate batchStartIndexUpdate( 
        Mutator<ByteBuffer> batch, Entity entity, String entryName,
        Object entryValue, UUID timestampUuid, boolean schemaHasProperty,
        boolean isMultiValue, boolean removeListEntry, boolean fulltextIndexed,
        boolean skipRead ) throws Exception {

        long timestamp = getTimestampInMicros( timestampUuid );

        IndexUpdate indexUpdate = new IndexUpdate( batch, entity, entryName, entryValue, 
                schemaHasProperty, isMultiValue, removeListEntry, timestampUuid );

        // entryName = entryName.toLowerCase();

        // entity_id,connection_type,connected_entity_id,prop_name

        if ( !skipRead ) {

            List<HColumn<ByteBuffer, ByteBuffer>> entries = null;

            if ( isMultiValue && validIndexableValue( entryValue ) ) {
                entries = cass.getColumns( 
                    cass.getApplicationKeyspace( applicationId ), ENTITY_INDEX_ENTRIES,
                        entity.getUuid(), new DynamicComposite( 
                            entryName, indexValueCode( entryValue), toIndexableValue( entryValue )),
                        setGreaterThanEqualityFlag( 
                            new DynamicComposite( entryName, indexValueCode( entryValue ),
                        toIndexableValue( entryValue ) ) ), INDEX_ENTRY_LIST_COUNT, false );
            }
            else {
                entries = cass.getColumns( 
                    cass.getApplicationKeyspace( applicationId ), ENTITY_INDEX_ENTRIES,
                        entity.getUuid(), 
                        new DynamicComposite( entryName ),
                        setGreaterThanEqualityFlag( 
                            new DynamicComposite( entryName ) ), INDEX_ENTRY_LIST_COUNT, false );
            }

            if ( logger.isDebugEnabled() ) {
                logger.debug( "Found {} previous index entries for {} of entity {}", new Object[] {
                        entries.size(), entryName, entity.getUuid()
                } );
            }

            // Delete all matching entries from entry list
            for ( HColumn<ByteBuffer, ByteBuffer> entry : entries ) {
                UUID prev_timestamp = null;
                Object prev_value = null;
                String prev_obj_path = null;

                // new format:
                // composite(entryName,
                // value_code,prev_value,prev_timestamp,prev_obj_path) = null
                DynamicComposite composite = 
                        DynamicComposite.fromByteBuffer( entry.getName().duplicate() );
                prev_value = composite.get( 2 );
                prev_timestamp = ( UUID ) composite.get( 3 );
                if ( composite.size() > 4 ) {
                    prev_obj_path = ( String ) composite.get( 4 );
                }

                if ( prev_value != null ) {

                    String entryPath = entryName;
                    if ( ( prev_obj_path != null ) && ( prev_obj_path.length() > 0 ) ) {
                        entryPath = entryName + "." + prev_obj_path;
                    }

                    indexUpdate.addPrevEntry( 
                            entryPath, prev_value, prev_timestamp, entry.getName().duplicate() );

                    // composite(property_value,connected_entity_id,entry_timestamp)
                    // addDeleteToMutator(batch, ENTITY_INDEX_ENTRIES,
                    // entity.getUuid(), entry.getName(), timestamp);

                }
                else {
                    logger.error( "Unexpected condition - deserialized property value is null" );
                }
            }
        }

        if ( !isMultiValue || ( isMultiValue && !removeListEntry ) ) {

            List<Map.Entry<String, Object>> list = IndexUtils.getKeyValueList( 
                    entryName, entryValue, fulltextIndexed );

            if ( entryName.equalsIgnoreCase( "location" ) && ( entryValue instanceof Map ) ) {
                @SuppressWarnings("rawtypes") double latitude =
                        MapUtils.getDoubleValue( ( Map ) entryValue, "latitude" );
                @SuppressWarnings("rawtypes") double longitude =
                        MapUtils.getDoubleValue( ( Map ) entryValue, "longitude" );
                list.add( new AbstractMap.SimpleEntry<String, Object>( "location.coordinates",
                        latitude + "," + longitude ) );
            }

            for ( Map.Entry<String, Object> indexEntry : list ) {

                if ( validIndexableValue( indexEntry.getValue() ) ) {
                    indexUpdate.addNewEntry( 
                            indexEntry.getKey(), toIndexableValue( indexEntry.getValue() ) );
                }
            }

            if ( isMultiValue ) {
                addInsertToMutator( batch, ENTITY_INDEX_ENTRIES, entity.getUuid(),
                        asList( entryName, 
                            indexValueCode( entryValue ), 
                            toIndexableValue( entryValue ),
                            indexUpdate.getTimestampUuid() ),
                        null, timestamp );
            }
            else {
                // int i = 0;

                for ( Map.Entry<String, Object> indexEntry : list ) {

                    String name = indexEntry.getKey();
                    if ( name.startsWith( entryName + "." ) ) {
                        name = name.substring( entryName.length() + 1 );
                    }
                    else if ( name.startsWith( entryName ) ) {
                        name = name.substring( entryName.length() );
                    }

                    byte code = indexValueCode( indexEntry.getValue() );
                    Object val = toIndexableValue( indexEntry.getValue() );
                    addInsertToMutator( batch, ENTITY_INDEX_ENTRIES, entity.getUuid(),
                            asList( entryName, code, val, indexUpdate.getTimestampUuid(), name ), 
                            null, timestamp );

                    indexUpdate.addIndex( indexEntry.getKey() );
                }
            }

            indexUpdate.addIndex( entryName );
        }

        return indexUpdate;
    }

    /**
     * Batch update backward connections set indexes.
     *
     * @param indexUpdate The index to update in the dictionary
     *
     * @return The index update
     *
     * @throws Exception the exception
     */
    @Metered(group = "core", name = "RelationManager_batchUpdateBackwardConnectionsDictionaryIndexes")
    public IndexUpdate batchUpdateBackwardConnectionsDictionaryIndexes( 
            IndexUpdate indexUpdate ) throws Exception {

        logger.debug( "batchUpdateBackwardConnectionsListIndexes" );

        boolean entityHasDictionary = getDefaultSchema()
                .isDictionaryIndexedInConnections( 
                        indexUpdate.getEntity().getType(), indexUpdate.getEntryName() );

        if ( !entityHasDictionary ) {
            return indexUpdate;
        }


        return doBackwardConnectionsUpdate( indexUpdate );
    }

    /**
     * Search each reverse connection type in the graph for connections.  
     * If one is found, update the index appropriately
     *
     * @param indexUpdate The index update to use
     *
     * @return The updated index update
     */
    private IndexUpdate doBackwardConnectionsUpdate( IndexUpdate indexUpdate ) throws Exception {
        final Entity targetEntity = indexUpdate.getEntity();

        final ConnectionTypesIterator connectionTypes =
            new ConnectionTypesIterator( cass, applicationId, targetEntity.getUuid(), false, 100 );

        for ( String connectionType : connectionTypes ) {

            PagingResultsIterator itr = 
                    getReversedConnectionsIterator( targetEntity, connectionType );

            for ( Object connection : itr ) {

                final ConnectedEntityRef sourceEntity = ( ConnectedEntityRef ) connection;

                //we need to create a connection ref from the source entity (found via reverse edge) 
                // to the entity we're about to update.  This is the index that needs updated
                final ConnectionRefImpl connectionRef =
                    new ConnectionRefImpl( sourceEntity, connectionType, indexUpdate.getEntity() );

                batchUpdateConnectionIndex( indexUpdate, connectionRef );
            }
        }

        return indexUpdate;
    }

    /**
     * Batch update connection index.
     *
     * @param indexUpdate The update operation to perform
     * @param connection The connection to update
     *
     * @return The index with the batch mutation udpated
     *
     * @throws Exception the exception
     */
    @Metered(group = "core", name = "RelationManager_batchUpdateConnectionIndex")
    public IndexUpdate batchUpdateConnectionIndex( 
            IndexUpdate indexUpdate, ConnectionRefImpl connection ) throws Exception {

        // UUID connection_id = connection.getUuid();

        UUID[] index_keys = connection.getIndexIds();

        // Delete all matching entries from entry list
        for ( IndexUpdate.IndexEntry entry : indexUpdate.getPrevEntries() ) {

            if ( entry.getValue() != null ) {

                batchDeleteConnectionIndexEntries( indexUpdate, entry, connection, index_keys );

                if ( "location.coordinates".equals( entry.getPath() ) ) {
                    EntityLocationRef loc = 
                        new EntityLocationRef( indexUpdate.getEntity(), entry.getTimestampUuid(),
                        entry.getValue().toString() );
                    batchDeleteLocationInConnectionsIndex( 
                        indexUpdate.getBatch(), indexBucketLocator, applicationId,
                        index_keys, entry.getPath(), loc );
                }
            }
            else {
                logger.error( "Unexpected condition - deserialized property value is null" );
            }
        }

        if ( ( indexUpdate.getNewEntries().size() > 0 ) 
                && ( !indexUpdate.isMultiValue() || ( indexUpdate.isMultiValue()
                && !indexUpdate.isRemoveListEntry() ) ) ) {

            for ( IndexUpdate.IndexEntry indexEntry : indexUpdate.getNewEntries() ) {

                batchAddConnectionIndexEntries( indexUpdate, indexEntry, connection, index_keys );

                if ( "location.coordinates".equals( indexEntry.getPath() ) ) {
                    EntityLocationRef loc =
                            new EntityLocationRef( 
                                    indexUpdate.getEntity(), 
                                    indexEntry.getTimestampUuid(),
                                    indexEntry.getValue().toString() );
                    batchStoreLocationInConnectionsIndex( 
                            indexUpdate.getBatch(), indexBucketLocator, applicationId,
                            index_keys, indexEntry.getPath(), loc );
                }
            }

      /*
       * addInsertToMutator(batch, EntityCF.SETS, key(connection_id,
       * Schema.INDEXES_SET), indexEntry.getKey(), null, false, timestamp); }
       *
       * addInsertToMutator(batch, EntityCF.SETS, key(connection_id,
       * Schema.INDEXES_SET), entryName, null, false, timestamp);
       */
        }

        for ( String index : indexUpdate.getIndexesSet() ) {
            addInsertToMutator( indexUpdate.getBatch(), ENTITY_DICTIONARIES,
                    key( connection.getConnectingIndexId(), Schema.DICTIONARY_INDEXES), index, null,
                    indexUpdate.getTimestamp() );
        }

        return indexUpdate;
    }

    /**
     * Get a paging results iterator.  Should return an iterator for all results
     *
     * @param targetEntity The target entity search connections from
     *
     * @return connectionType The name of the edges to search
     */
    private PagingResultsIterator getReversedConnectionsIterator( 
            EntityRef targetEntity, String connectionType ) throws Exception {

        return new PagingResultsIterator( 
                getConnectingEntities( targetEntity, connectionType, null, Level.REFS ) );
    }

    /**
     * Get all edges that are to the targetEntity
     *
     * @param targetEntity The target entity to search edges in
     * @param connectionType The type of connection.  If not specified, all connections are returned
     * @param connectedEntityType The connected entity type, if not specified all types are returned
     * @param resultsLevel The results level to return
     */
    private Results getConnectingEntities( 
            EntityRef targetEntity, String connectionType, String connectedEntityType,
            Level resultsLevel ) throws Exception {

        return getConnectingEntities(
                targetEntity, connectionType, connectedEntityType, resultsLevel, 0);
    }

    /**
     * Get all edges that are to the targetEntity
     *
     * @param targetEntity The target entity to search edges in
     * @param connectionType The type of connection.  If not specified, all connections are returned
     * @param connectedEntityType The connected entity type, if not specified all types are returned
     * @param count result limit
     */
    private Results getConnectingEntities( EntityRef targetEntity, String connectionType, 
            String connectedEntityType, Level level, int count) throws Exception {

        Query query = new Query();
        query.setResultsLevel( level );
        query.setLimit(count);

        final ConnectionRefImpl connectionRef =
            new ConnectionRefImpl( new SimpleEntityRef( connectedEntityType, null ), 
            connectionType, targetEntity );
        final ConnectionResultsLoaderFactory factory = new ConnectionResultsLoaderFactory( connectionRef );

        QueryProcessorImpl qp = new QueryProcessorImpl( query, null, em, factory );
        SearchConnectionVisitor visitor = new SearchConnectionVisitor( qp, connectionRef, false );

        return qp.getResults( visitor );
    }

    @Metered(group = "core", name = "RelationManager_batchDeleteConnectionIndexEntries")
    public Mutator<ByteBuffer> batchDeleteConnectionIndexEntries( IndexUpdate indexUpdate, IndexUpdate.IndexEntry entry,
                                                                  ConnectionRefImpl connection, UUID[] index_keys )
            throws Exception {

        // entity_id,prop_name
        Object property_index_key = key( index_keys[ConnectionRefImpl.ALL], INDEX_CONNECTIONS, entry.getPath(),
                indexBucketLocator.getBucket( applicationId, IndexBucketLocator.IndexType.CONNECTION, index_keys[ConnectionRefImpl.ALL],
                        entry.getPath() ) );

        // entity_id,entity_type,prop_name
        Object entity_type_prop_index_key =
                key( index_keys[ConnectionRefImpl.BY_ENTITY_TYPE], INDEX_CONNECTIONS, entry.getPath(),
                        indexBucketLocator.getBucket( applicationId, IndexBucketLocator.IndexType.CONNECTION,
                                index_keys[ConnectionRefImpl.BY_ENTITY_TYPE], entry.getPath() ) );

        // entity_id,connection_type,prop_name
        Object connection_type_prop_index_key =
                key( index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], INDEX_CONNECTIONS, entry.getPath(),
                        indexBucketLocator.getBucket( applicationId, IndexBucketLocator.IndexType.CONNECTION,
                                index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], entry.getPath() ) );

        // entity_id,connection_type,entity_type,prop_name
        Object connection_type_and_entity_type_prop_index_key =
                key( index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], INDEX_CONNECTIONS, entry.getPath(),
                        indexBucketLocator.getBucket( applicationId, IndexBucketLocator.IndexType.CONNECTION,
                                index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], entry.getPath() ) );

        // composite(property_value,connected_entity_id,connection_type,entity_type,entry_timestamp)
        addDeleteToMutator( indexUpdate.getBatch(), ENTITY_INDEX, property_index_key,
                entry.getIndexComposite( connection.getConnectedEntityId(), connection.getConnectionType(),
                        connection.getConnectedEntityType() ), indexUpdate.getTimestamp() );

        // composite(property_value,connected_entity_id,connection_type,entry_timestamp)
        addDeleteToMutator( indexUpdate.getBatch(), ENTITY_INDEX, entity_type_prop_index_key,
                entry.getIndexComposite( connection.getConnectedEntityId(), connection.getConnectionType() ),
                indexUpdate.getTimestamp() );

        // composite(property_value,connected_entity_id,entity_type,entry_timestamp)
        addDeleteToMutator( indexUpdate.getBatch(), ENTITY_INDEX, connection_type_prop_index_key,
                entry.getIndexComposite( connection.getConnectedEntityId(), connection.getConnectedEntityType() ),
                indexUpdate.getTimestamp() );

        // composite(property_value,connected_entity_id,entry_timestamp)
        addDeleteToMutator( indexUpdate.getBatch(), ENTITY_INDEX, connection_type_and_entity_type_prop_index_key,
                entry.getIndexComposite( connection.getConnectedEntityId() ), indexUpdate.getTimestamp() );

        return indexUpdate.getBatch();
    }


    @Metered(group = "core", name = "RelationManager_batchAddConnectionIndexEntries")
    public Mutator<ByteBuffer> batchAddConnectionIndexEntries( IndexUpdate indexUpdate, 
        IndexUpdate.IndexEntry entry, ConnectionRefImpl conn, UUID[] index_keys ) {

        // entity_id,prop_name
        Object property_index_key = key( index_keys[ConnectionRefImpl.ALL], 
                INDEX_CONNECTIONS, entry.getPath(),
                indexBucketLocator.getBucket( applicationId, 
                        IndexBucketLocator.IndexType.CONNECTION, index_keys[ConnectionRefImpl.ALL],
                        entry.getPath() ) );

        // entity_id,entity_type,prop_name
        Object entity_type_prop_index_key =
            key( index_keys[ConnectionRefImpl.BY_ENTITY_TYPE], INDEX_CONNECTIONS, entry.getPath(),
                indexBucketLocator.getBucket( applicationId, IndexBucketLocator.IndexType.CONNECTION,
                index_keys[ConnectionRefImpl.BY_ENTITY_TYPE], entry.getPath() ) );

        // entity_id,connection_type,prop_name
        Object connection_type_prop_index_key =
            key( index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], INDEX_CONNECTIONS, entry.getPath(),
                indexBucketLocator.getBucket( applicationId, IndexBucketLocator.IndexType.CONNECTION,
                index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], entry.getPath() ) );

        // entity_id,connection_type,entity_type,prop_name
        Object connection_type_and_entity_type_prop_index_key =
            key( index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], 
                INDEX_CONNECTIONS, entry.getPath(),
                indexBucketLocator.getBucket( applicationId, IndexBucketLocator.IndexType.CONNECTION,
                index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], entry.getPath() ) );

        // composite(property_value,connected_entity_id,connection_type,entity_type,entry_timestamp)
        addInsertToMutator( indexUpdate.getBatch(), ENTITY_INDEX, property_index_key,
            entry.getIndexComposite( conn.getConnectedEntityId(), conn.getConnectionType(),
                conn.getConnectedEntityType() ), conn.getUuid(), indexUpdate.getTimestamp() );

        // composite(property_value,connected_entity_id,connection_type,entry_timestamp)
        addInsertToMutator( indexUpdate.getBatch(), ENTITY_INDEX, entity_type_prop_index_key,
            entry.getIndexComposite( conn.getConnectedEntityId(), conn.getConnectionType() ),
            conn.getUuid(), indexUpdate.getTimestamp() );

        // composite(property_value,connected_entity_id,entity_type,entry_timestamp)
        addInsertToMutator( indexUpdate.getBatch(), ENTITY_INDEX, connection_type_prop_index_key,
            entry.getIndexComposite( conn.getConnectedEntityId(), conn.getConnectedEntityType() ),
            conn.getUuid(), indexUpdate.getTimestamp() );

        // composite(property_value,connected_entity_id,entry_timestamp)
        addInsertToMutator( indexUpdate.getBatch(), ENTITY_INDEX, 
            connection_type_and_entity_type_prop_index_key,
            entry.getIndexComposite( conn.getConnectedEntityId() ), conn.getUuid(),
            indexUpdate.getTimestamp() );

        return indexUpdate.getBatch();
    }


    /**
     * Simple search visitor that performs all the joining
     *
     * @author tnine
     */
    private class SearchConnectionVisitor extends SearchVisitor {

        private final ConnectionRefImpl connection;

        /** True if we should search from source->target edges.  False if we should search from target<-source edges */
        private final boolean outgoing;


        /**
         * @param queryProcessor They query processor to use
         * @param connection The connection refernce
         * @param outgoing The direction to search.  True if we should search from source->target edges.  False if we
         * should search from target<-source edges
         */
        public SearchConnectionVisitor( QueryProcessorImpl queryProcessor, ConnectionRefImpl connection,
                                        boolean outgoing ) {
            super( queryProcessor );
            this.connection = connection;
            this.outgoing = outgoing;
        }


        /* (non-Javadoc)
     * @see org.apache.usergrid.persistence.query.ir.SearchVisitor#secondaryIndexScan(org.apache.usergrid.persistence.query.ir
     * .QueryNode, org.apache.usergrid.persistence.query.ir.QuerySlice)
     */
        @Override
        protected IndexScanner secondaryIndexScan( QueryNode node, QuerySlice slice ) throws Exception {

            UUID id = ConnectionRefImpl.getIndexId( ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE, headEntity,
                    connection.getConnectionType(), connection.getConnectedEntityType(), new ConnectedEntityRef[0] );

            Object key = key( id, INDEX_CONNECTIONS );

            // update the cursor and order before we perform the slice
            // operation
            queryProcessor.applyCursorAndSort( slice );

            IndexScanner columns = null;

            if ( slice.isComplete() ) {
                columns = new NoOpIndexScanner();
            }
            else {
                columns = searchIndex( key, slice, queryProcessor.getPageSizeHint( node ) );
            }

            return columns;
        }


        /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.query.ir.NodeVisitor#visit(org.apache.usergrid.
     * persistence.query.ir.WithinNode)
     */
        @Override
        public void visit( WithinNode node ) throws Exception {

            QuerySlice slice = node.getSlice();

            queryProcessor.applyCursorAndSort( slice );

            GeoIterator itr = new GeoIterator( 
                new ConnectionGeoSearch( em, indexBucketLocator, cass, connection.getIndexId() ),
                query.getLimit(), slice, node.getPropertyName(),
                new Point( node.getLattitude(), node.getLongitude() ), node.getDistance() );

            results.push( itr );
        }


        @Override
        public void visit( AllNode node ) throws Exception {
            QuerySlice slice = node.getSlice();

            queryProcessor.applyCursorAndSort( slice );

            int size = queryProcessor.getPageSizeHint( node );

            ByteBuffer start = null;

            if ( slice.hasCursor() ) {
                start = slice.getCursor();
            }


            boolean skipFirst = !node.isForceKeepFirst() && slice.hasCursor();

            UUID entityIdToUse;

            //change our type depending on which direction we're loading
            String dictionaryType;

            //the target type
            String targetType;

            //this is on the "source" side of the edge
            if ( outgoing ) {
                entityIdToUse = connection.getConnectingEntityId();
                dictionaryType = DICTIONARY_CONNECTED_ENTITIES;
                targetType = connection.getConnectedEntityType();
            }

            //we're on the target side of the edge
            else {
                entityIdToUse = connection.getConnectedEntityId();
                dictionaryType = DICTIONARY_CONNECTING_ENTITIES;
                targetType = connection.getConnectingEntityType();
            }

            final String connectionType = connection.getConnectionType();

            final ConnectionIndexSliceParser connectionParser = 
                    new ConnectionIndexSliceParser( targetType );

            final Iterator<String> connectionTypes;

            //use the provided connection type
            if ( connectionType != null ) {
                connectionTypes = Collections.singleton( connectionType ).iterator();
            }

            //we need to iterate all connection types
            else {
                connectionTypes = new ConnectionTypesIterator( 
                        cass, applicationId, entityIdToUse, outgoing, size );
            }

            IndexScanner connectionScanner = new ConnectedIndexScanner( 
                cass, dictionaryType, applicationId, entityIdToUse, connectionTypes, start, 
                slice.isReversed(), size, skipFirst );

            this.results.push( new SliceIterator( slice, connectionScanner, connectionParser ) );
        }


        @Override
        public void visit( NameIdentifierNode nameIdentifierNode ) throws Exception {

            //TODO T.N. USERGRID-1919 actually validate this is connected
            EntityRef ref = em.getAlias(
                    connection.getConnectedEntityType(),nameIdentifierNode.getName() );

            if ( ref == null ) {
                this.results.push( new EmptyIterator() );
                return;
            }

            this.results.push( new StaticIdIterator( ref.getUuid() ) );
        }
    }

    private IndexScanner searchIndex( 
            Object indexKey, QuerySlice slice, int pageSize ) throws Exception {

        DynamicComposite[] range = slice.getRange();

        Object keyPrefix = key( indexKey, slice.getPropertyName() );

        IndexScanner scanner = new IndexBucketScanner( 
            cass, indexBucketLocator, ENTITY_INDEX, applicationId, 
            IndexBucketLocator.IndexType.CONNECTION, keyPrefix, range[0], range[1], 
            slice.isReversed(), pageSize, slice.hasCursor(), slice.getPropertyName() );

        return scanner;
    }


}
