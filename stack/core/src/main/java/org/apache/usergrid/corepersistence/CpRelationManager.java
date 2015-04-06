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

import org.apache.usergrid.corepersistence.results.CollectionResultsLoaderFactoryImpl;
import org.apache.usergrid.corepersistence.results.ConnectionResultsLoaderFactoryImpl;
import org.apache.usergrid.corepersistence.results.ElasticSearchQueryExecutor;
import org.apache.usergrid.corepersistence.results.QueryExecutor;
import org.apache.usergrid.corepersistence.util.CpEntityMapUtils;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.ConnectedEntityRef;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.PagingResultsIterator;
import org.apache.usergrid.persistence.RelationManager;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.RoleRef;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.SimpleRoleRef;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.ConnectionRefImpl;
import org.apache.usergrid.persistence.cassandra.IndexUpdate;
import org.apache.usergrid.persistence.cassandra.QueryProcessorImpl;
import org.apache.usergrid.persistence.cassandra.index.ConnectedIndexScanner;
import org.apache.usergrid.persistence.cassandra.index.IndexBucketScanner;
import org.apache.usergrid.persistence.cassandra.index.IndexScanner;
import org.apache.usergrid.persistence.cassandra.index.NoOpIndexScanner;
import org.apache.usergrid.persistence.core.future.BetterFuture;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.geo.ConnectionGeoSearch;
import org.apache.usergrid.persistence.geo.EntityLocationRef;
import org.apache.usergrid.persistence.geo.model.Point;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.SearchTypes;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Query.Level;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
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
import org.apache.usergrid.utils.UUIDUtils;

import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;

import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import static java.util.Arrays.asList;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.createId;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.generateScopeFromSource;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.generateScopeFromCollection;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.generateScopeFromConnection;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.getNameFromEdgeType;
import static org.apache.usergrid.persistence.Schema.COLLECTION_ROLES;
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
import static org.apache.usergrid.utils.ClassUtils.cast;
import static org.apache.usergrid.utils.CompositeUtils.setGreaterThanEqualityFlag;
import static org.apache.usergrid.utils.InflectionUtils.singularize;
import static org.apache.usergrid.utils.MapUtils.addMapSet;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMicros;


/**
 * Implement good-old Usergrid RelationManager with the new-fangled Core Persistence API.
 */
public class CpRelationManager implements RelationManager {

    private static final Logger logger = LoggerFactory.getLogger( CpRelationManager.class );


    private CpEntityManagerFactory emf;

    private ManagerCache managerCache;

    private EntityManager em;

    private UUID applicationId;

    private EntityRef headEntity;

    private org.apache.usergrid.persistence.model.entity.Entity cpHeadEntity;

    private ApplicationScope applicationScope;

    private CassandraService cass;

    private IndexBucketLocator indexBucketLocator;

    private MetricsFactory metricsFactory;
    private Timer updateCollectionTimer;
    private Timer createConnectionTimer;
    private Timer cassConnectionDelete;
    private Timer esDeleteConnectionTimer;

    public CpRelationManager() {}


    public CpRelationManager init(
        EntityManager em,
            CpEntityManagerFactory emf,
            UUID applicationId,
            EntityRef headEntity,
            IndexBucketLocator indexBucketLocator,
            MetricsFactory metricsFactory) {

        Assert.notNull(em, "Entity manager cannot be null");
        Assert.notNull(emf, "Entity manager factory cannot be null");
        Assert.notNull(applicationId, "Application Id cannot be null");
        Assert.notNull(headEntity, "Head entity cannot be null");
        Assert.notNull(headEntity.getUuid(), "Head entity uuid cannot be null");
        // TODO: this assert should not be failing
        //Assert.notNull( indexBucketLocator, "indexBucketLocator cannot be null" );
        this.em = em;
        this.emf = emf;
        this.applicationId = applicationId;
        this.headEntity = headEntity;
        this.managerCache = emf.getManagerCache();
        this.applicationScope = CpNamingUtils.getApplicationScope(applicationId);
        this.cass = em.getCass(); // TODO: eliminate need for this via Core Persistence
        this.indexBucketLocator = indexBucketLocator; // TODO: this also
        this.metricsFactory = metricsFactory;
        this.updateCollectionTimer = metricsFactory
            .getTimer(CpRelationManager.class, "relation.manager.es.update.collection");
        this.createConnectionTimer = metricsFactory
            .getTimer(CpRelationManager.class, "relation.manager.es.create.connection.timer");
        this.cassConnectionDelete = metricsFactory
            .getTimer(CpRelationManager.class, "relation.manager.cassandra.delete.connection.batch.timer");
        this.esDeleteConnectionTimer = metricsFactory.getTimer(CpRelationManager.class,
            "relation.manager.es.delete.connection.batch.timer");

        if (logger.isDebugEnabled()) {
            logger.debug("Loading head entity {}:{} from app {}",
                new Object[]{
                    headEntity.getType(),
                    headEntity.getUuid(),
                    applicationScope
                });
        }

        Id entityId = new SimpleId(headEntity.getUuid(), headEntity.getType());

        this.cpHeadEntity = ((CpEntityManager) em).load(entityId);

        // commented out because it is possible that CP entity has not been created yet
        Assert.notNull(cpHeadEntity, "cpHeadEntity cannot be null for app id " + applicationScope.getApplication().getUuid());

        return this;
    }


    @Override
    public Set<String> getCollectionIndexes( String collectionName ) throws Exception {
        final Set<String> indexes = new HashSet<String>();

        GraphManager gm = managerCache.getGraphManager(applicationScope);

        String edgeTypePrefix = CpNamingUtils.getEdgeTypeFromCollectionName( collectionName );

        logger.debug("getCollectionIndexes(): Searching for edge type prefix {} to target {}:{}",
            new Object[] {
                edgeTypePrefix, cpHeadEntity.getId().getType(), cpHeadEntity.getId().getUuid()
        });

        Observable<String> types= gm.getEdgeTypesFromSource(
            new SimpleSearchEdgeType( cpHeadEntity.getId(), edgeTypePrefix, null ) );

        Iterator<String> iter = types.toBlocking().getIterator();
        while ( iter.hasNext() ) {
            indexes.add( iter.next() );
        }
        return indexes;
    }


    @Override
    public Map<String, Map<UUID, Set<String>>> getOwners() throws Exception {

        // TODO: do we need to restrict this to edges prefixed with owns?
        //Map<EntityRef, Set<String>> containerEntities = getContainers(-1, "owns", null);
        Map<EntityRef, Set<String>> containerEntities = getContainers();

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
              .flatMap( new Func1<String, Observable<Edge>>() {
                  @Override
                  public Observable<Edge> call( final String edgeType ) {
                      return gm.loadEdgesToTarget(
                          new SimpleSearchByEdgeType( cpHeadEntity.getId(), edgeType, Long.MAX_VALUE,
                              SearchByEdgeType.Order.DESCENDING, null ) );

                  }
              } );

        //if our limit is set, take them.  Note this logic is still borked, we can't possibly fit everything in memmory
        if ( limit > -1 ) {
            edges = edges.take( limit );
        }


        return edges.collect( () -> new LinkedHashMap<EntityRef, Set<String>>(), ( entityRefSetMap, edge) -> {
                if ( fromEntityType != null && !fromEntityType.equals( edge.getSourceNode().getType() ) ) {
                    logger.debug( "Ignoring edge from entity type {}", edge.getSourceNode().getType() );
                    return;
                }

                final EntityRef eref =
                    new SimpleEntityRef( edge.getSourceNode().getType(), edge.getSourceNode().getUuid() );

                String name = getNameFromEdgeType(edge.getType());
                addMapSet( entityRefSetMap, eref, name );
            }
         ).toBlocking().last();
    }


    public void updateContainingCollectionAndCollectionIndexes(
            final org.apache.usergrid.persistence.model.entity.Entity cpEntity ) {


        final GraphManager gm = managerCache.getGraphManager( applicationScope );

        Iterator<String> edgeTypesToTarget = gm.getEdgeTypesToTarget( new SimpleSearchEdgeType(
            cpHeadEntity.getId(), null, null) ).toBlocking().getIterator();

        logger.debug("updateContainingCollectionsAndCollections(): "
                + "Searched for edges to target {}:{}\n   in scope {}\n   found: {}",
            new Object[] {
                cpHeadEntity.getId().getType(),
                cpHeadEntity.getId().getUuid(),
                applicationScope.getApplication(),
                edgeTypesToTarget.hasNext()
        });

        // loop through all types of edge to target


        final ApplicationEntityIndex ei = managerCache.getEntityIndex(applicationScope);

        final EntityIndexBatch entityIndexBatch = ei.createBatch();

        final int count = gm.getEdgeTypesToTarget(
            new SimpleSearchEdgeType( cpHeadEntity.getId(), null, null ) )

                // for each edge type, emit all the edges of that type
                .flatMap( new Func1<String, Observable<Edge>>() {
                    @Override
                    public Observable<Edge> call( final String etype ) {
                        return gm.loadEdgesToTarget( new SimpleSearchByEdgeType(
                            cpHeadEntity.getId(), etype, Long.MAX_VALUE,
                            SearchByEdgeType.Order.DESCENDING, null ) );
                    }
                } )

                //for each edge we receive index and add to the batch
                .doOnNext( new Action1<Edge>() {
                    @Override
                    public void call( final Edge edge ) {


                        // reindex the entity in the source entity's collection or connection index

                        IndexScope indexScope = generateScopeFromSource(edge);


                        entityIndexBatch.index( indexScope, cpEntity );

                        // reindex the entity in the source entity's all-types index

                        //TODO REMOVE INDEX CODE
                        //                        indexScope = new IndexScopeImpl( new SimpleId(
                        //                            sourceEntity.getUuid(), sourceEntity.getType() ), CpNamingUtils
                        // .ALL_TYPES, entityType );
                        //
                        //                        entityIndexBatch.index( indexScope, cpEntity );
                    }
                } ).count().toBlocking().lastOrDefault( 0 );

        //Adding graphite metrics
        Timer.Context timeElasticIndexBatch = updateCollectionTimer.time();
        entityIndexBatch.execute();
        timeElasticIndexBatch.stop();

        logger.debug( "updateContainingCollectionsAndCollections() updated {} indexes", count );
    }


    @Override
    public boolean isConnectionMember( String connectionType, EntityRef entity ) throws Exception {

        Id entityId = new SimpleId( entity.getUuid(), entity.getType() );

        String edgeType = CpNamingUtils.getEdgeTypeFromConnectionType( connectionType );

        logger.debug("isConnectionMember(): Checking for edge type {} from {}:{} to {}:{}",
            new Object[] {
                edgeType,
                headEntity.getType(), headEntity.getUuid(),
                entity.getType(), entity.getUuid() });

        GraphManager gm = managerCache.getGraphManager( applicationScope );
        Observable<Edge> edges = gm.loadEdgeVersions( new SimpleSearchByEdge(
            new SimpleId( headEntity.getUuid(), headEntity.getType() ),
            edgeType,
            entityId,
            Long.MAX_VALUE,
            SearchByEdgeType.Order.DESCENDING,
            null ) );

        return edges.toBlocking().firstOrDefault( null ) != null;
    }


    @SuppressWarnings( "unchecked" )
    @Override
    public boolean isCollectionMember( String collName, EntityRef entity ) throws Exception {

        Id entityId = new SimpleId( entity.getUuid(), entity.getType() );

        String edgeType = CpNamingUtils.getEdgeTypeFromCollectionName( collName );

        logger.debug("isCollectionMember(): Checking for edge type {} from {}:{} to {}:{}",
            new Object[] {
                edgeType,
                headEntity.getType(), headEntity.getUuid(),
                entity.getType(), entity.getUuid() });

        GraphManager gm = managerCache.getGraphManager( applicationScope );
        Observable<Edge> edges = gm.loadEdgeVersions( new SimpleSearchByEdge(
            new SimpleId( headEntity.getUuid(), headEntity.getType() ),
            edgeType,
            entityId,
            Long.MAX_VALUE,
            SearchByEdgeType.Order.DESCENDING,
            null ) );

        return edges.toBlocking().firstOrDefault( null ) != null;
    }


    private boolean moreThanOneInboundConnection( EntityRef target, String connectionType ) {

        Id targetId = new SimpleId( target.getUuid(), target.getType() );

        GraphManager gm = managerCache.getGraphManager( applicationScope );

        Observable<Edge> edgesToTarget = gm.loadEdgesToTarget( new SimpleSearchByEdgeType(
            targetId,
            CpNamingUtils.getEdgeTypeFromConnectionType( connectionType ),
            System.currentTimeMillis(),
            SearchByEdgeType.Order.DESCENDING,
            null ) ); // last

        Iterator<Edge> iterator = edgesToTarget.toBlocking().getIterator();
        int count = 0;
        while ( iterator.hasNext() ) {
            iterator.next();
            if ( count++ > 1 ) {
                return true;
            }
        }
        return false;
    }


    private boolean moreThanOneOutboundConnection( EntityRef source, String connectionType ) {

        Id sourceId = new SimpleId( source.getUuid(), source.getType() );

        GraphManager gm = managerCache.getGraphManager( applicationScope );

        Observable<Edge> edgesFromSource = gm.loadEdgesFromSource( new SimpleSearchByEdgeType(
            sourceId,
            CpNamingUtils.getEdgeTypeFromConnectionType( connectionType ),
            System.currentTimeMillis(),
            SearchByEdgeType.Order.DESCENDING,
            null ) ); // last

        int count = edgesFromSource.take( 2 ).count().toBlocking().last();

        return count > 1;
    }


    @Override
    public Set<String> getCollections() throws Exception {

        final Set<String> indexes = new HashSet<String>();

        GraphManager gm = managerCache.getGraphManager( applicationScope );

        Observable<String> str = gm.getEdgeTypesFromSource(
                new SimpleSearchEdgeType( cpHeadEntity.getId(), null, null ) );

        Iterator<String> iter = str.toBlocking().getIterator();
        while ( iter.hasNext() ) {
            String edgeType = iter.next();
            indexes.add( getNameFromEdgeType( edgeType ) );
        }

        return indexes;
    }


    @Override
    public Results getCollection( String collectionName,
            UUID startResult,
            int count,
            Level resultsLevel,
            boolean reversed ) throws Exception {

        Query query = Query.fromQL( "select *" );
        query.setLimit( count );
        query.setReversed( reversed );

        if ( startResult != null ) {
            query.addGreaterThanEqualFilter( "created", startResult.timestamp() );
        }

        return searchCollection( collectionName, query );
    }


    @Override
    public Results getCollection( String collName, Query query, Level level ) throws Exception {

        return searchCollection( collName, query );
    }


    // add to a named collection of the head entity
    @Override
    public Entity addToCollection( String collName, EntityRef itemRef ) throws Exception {

        CollectionInfo collection =
                getDefaultSchema().getCollection( headEntity.getType(), collName );
        if ( ( collection != null ) && !collection.getType().equals( itemRef.getType() ) ) {
            return null;
        }

        return addToCollection( collName, itemRef,
                ( collection != null && collection.getLinkedCollection() != null ) );
    }


    public Entity addToCollection( String collName, EntityRef itemRef, boolean connectBack )
            throws Exception {

        Id entityId = new SimpleId( itemRef.getUuid(), itemRef.getType() );
        org.apache.usergrid.persistence.model.entity.Entity memberEntity =
            ((CpEntityManager)em).load( entityId );

        return addToCollection(collName, itemRef, memberEntity, connectBack);
    }


    public Entity addToCollection(final String collName, final EntityRef itemRef,
            final org.apache.usergrid.persistence.model.entity.Entity memberEntity, final boolean connectBack )
        throws Exception {

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
            logger.debug( "Loaded member entity {}:{} from   app {}\n   "
                + " data {}",
                new Object[] {
                    itemRef.getType(),
                    itemRef.getUuid(),applicationScope,
                    CpEntityMapUtils.toMap( memberEntity )
                } );
        }

        String edgeType = CpNamingUtils.getEdgeTypeFromCollectionName( collName );

        UUID timeStampUuid = memberEntity.getId().getUuid() != null
                && UUIDUtils.isTimeBased( memberEntity.getId().getUuid() )
                ?  memberEntity.getId().getUuid() : UUIDUtils.newTimeUUID();

        long uuidHash = UUIDUtils.getUUIDLong( timeStampUuid );

        // create graph edge connection from head entity to member entity
        Edge edge = new SimpleEdge( cpHeadEntity.getId(), edgeType, memberEntity.getId(), uuidHash );
        GraphManager gm = managerCache.getGraphManager( applicationScope );
        gm.writeEdge( edge ).toBlocking().last();


        if(logger.isDebugEnabled()) {
            logger.debug( "Wrote edgeType {}\n   from {}:{}\n   to {}:{}\n   scope {}:{}", new Object[] {
                edgeType, cpHeadEntity.getId().getType(), cpHeadEntity.getId().getUuid(), memberEntity.getId().getType(),
                memberEntity.getId().getUuid(), applicationScope.getApplication().getType(),
                applicationScope.getApplication().getUuid()
            } );
        }

        ( ( CpEntityManager ) em ).indexEntityIntoCollection( cpHeadEntity, memberEntity, collName );

        if(logger.isDebugEnabled()) {
            logger.debug( "Added entity {}:{} to collection {}", new Object[] {
                itemRef.getUuid().toString(), itemRef.getType(), collName
            } );
        }
        //        logger.debug("With head entity scope is {}:{}:{}", new Object[] {
        //            headEntityScope.getApplication().toString(),
        //            headEntityScope.getOwner().toString(),
        //            headEntityScope.getName()});

        if ( connectBack && collection != null && collection.getLinkedCollection() != null ) {
            getRelationManager( itemEntity ).addToCollection(
                    collection.getLinkedCollection(), headEntity, cpHeadEntity, false );
            getRelationManager( itemEntity ).addToCollection(
                    collection.getLinkedCollection(), headEntity, false );
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

        CollectionInfo collection = getDefaultSchema().getCollection( headEntity.getType(), collName );
        if ( ( collection != null ) && !collection.getType().equals( itemType ) ) {
            return null;
        }

        properties = getDefaultSchema().cleanUpdatedProperties( itemType, properties, true );

        Entity itemEntity = em.create( itemType, properties );

        if ( itemEntity != null ) {

            addToCollection( collName, itemEntity );

            if ( collection != null && collection.getLinkedCollection() != null ) {
                getRelationManager( getHeadEntity() )
                        .addToCollection( collection.getLinkedCollection(), itemEntity );
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
            logger.debug( "Loading entity to remove from collection "
                + "{}:{} from app {}\n",
                new Object[] {
                    itemRef.getType(),
                    itemRef.getUuid(),
                    applicationScope
               });
        }

        Id entityId = new SimpleId( itemRef.getUuid(), itemRef.getType() );
        org.apache.usergrid.persistence.model.entity.Entity memberEntity =
            ((CpEntityManager)em).load( entityId );

        final ApplicationEntityIndex ei = managerCache.getEntityIndex( applicationScope );
        final EntityIndexBatch batch = ei.createBatch();

        // remove item from collection index
        IndexScope indexScope = generateScopeFromCollection( cpHeadEntity.getId(), collName );

        batch.deindex( indexScope, memberEntity );

        // remove collection from item index
        IndexScope itemScope = generateScopeFromCollection( memberEntity.getId(),
            Schema.defaultCollectionName( cpHeadEntity.getId().getType() ) );


        batch.deindex( itemScope, cpHeadEntity );

        batch.execute();

        // remove edge from collection to item
        GraphManager gm = managerCache.getGraphManager( applicationScope );
        Edge collectionToItemEdge = new SimpleEdge(
                cpHeadEntity.getId(),
                CpNamingUtils.getEdgeTypeFromCollectionName( collName ),
                memberEntity.getId(), UUIDUtils.getUUIDLong( memberEntity.getId().getUuid() ) );
        gm.deleteEdge( collectionToItemEdge ).toBlocking().last();

        // remove edge from item to collection
        Edge itemToCollectionEdge = new SimpleEdge(
                memberEntity.getId(),
                CpNamingUtils.getEdgeTypeFromCollectionName(
                        Schema.defaultCollectionName( cpHeadEntity.getId().getType() ) ),
                cpHeadEntity.getId(),
                UUIDUtils.getUUIDLong( cpHeadEntity.getId().getUuid() ) );

        gm.deleteEdge( itemToCollectionEdge ).toBlocking().last();

        // special handling for roles collection of a group
        if ( headEntity.getType().equals( Group.ENTITY_TYPE ) ) {

            if ( collName.equals( COLLECTION_ROLES ) ) {
                String path = ( String ) ( ( Entity ) itemRef ).getMetadata( "path" );

                if ( path.startsWith( "/roles/" ) ) {

                    Entity itemEntity = em.get( new SimpleEntityRef( memberEntity.getId().getType(),
                            memberEntity.getId().getUuid() ) );

                    RoleRef roleRef = SimpleRoleRef.forRoleEntity( itemEntity );
                    em.deleteRole( roleRef.getApplicationRoleName() );
                }
            }
        }
    }

    @Override
    public void copyRelationships(String srcRelationName, EntityRef dstEntityRef,
            String dstRelationName) throws Exception {

        headEntity = em.validate( headEntity );
        dstEntityRef = em.validate( dstEntityRef );

        CollectionInfo srcCollection =
                getDefaultSchema().getCollection( headEntity.getType(), srcRelationName );

        CollectionInfo dstCollection =
                getDefaultSchema().getCollection( dstEntityRef.getType(), dstRelationName );

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

        CollectionInfo collection =
            getDefaultSchema().getCollection( headEntity.getType(), collName );

        if ( collection == null ) {
            throw new RuntimeException( "Cannot find collection-info for '" + collName
                    + "' of " + headEntity.getType() + ":" + headEntity .getUuid() );
        }

        final IndexScope indexScope = generateScopeFromCollection( cpHeadEntity.getId(), collName );

        final ApplicationEntityIndex ei = managerCache.getEntityIndex( applicationScope );

        final SearchTypes types = SearchTypes.fromTypes( collection.getType() );

        logger.debug( "Searching scope {}:{}",

            indexScope.getOwner().toString(), indexScope.getName() );

        query.setEntityType( collection.getType() );
        query = adjustQuery( query );


        final CollectionResultsLoaderFactoryImpl resultsLoaderFactory = new CollectionResultsLoaderFactoryImpl( managerCache );


        //execute the query and return our next result
        final QueryExecutor executor = new ElasticSearchQueryExecutor( resultsLoaderFactory, ei, applicationScope, indexScope, types, query );

        return executor.next();
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
            logger.debug("createConnection(): "
                + "Indexing connection type '{}'\n   from source {}:{}]\n"
                + "   to target {}:{}\n   app {}",
                new Object[] {
                    connectionType,
                    headEntity.getType(),
                    headEntity.getUuid(),
                    connectedEntityRef.getType(),
                    connectedEntityRef.getUuid(),
                    applicationScope
            });
        }

        Id entityId = new SimpleId( connectedEntityRef.getUuid(), connectedEntityRef.getType());
        org.apache.usergrid.persistence.model.entity.Entity targetEntity =
            ((CpEntityManager)em).load( entityId );

        String edgeType = CpNamingUtils.getEdgeTypeFromConnectionType( connectionType );

        // create graph edge connection from head entity to member entity
        Edge edge = new SimpleEdge(
                cpHeadEntity.getId(), edgeType, targetEntity.getId(), System.currentTimeMillis() );

        GraphManager gm = managerCache.getGraphManager( applicationScope );
        gm.writeEdge( edge ).toBlocking().last();

        ApplicationEntityIndex ei = managerCache.getEntityIndex( applicationScope );
        EntityIndexBatch batch = ei.createBatch();

        // Index the new connection in app|source|type context
        IndexScope indexScope = generateScopeFromConnection( cpHeadEntity.getId(), connectionType );

        batch.index( indexScope, targetEntity );

        // Index the new connection in app|scope|all-types context
        //TODO REMOVE INDEX CODE
//        IndexScope allTypesIndexScope = new IndexScopeImpl( cpHeadEntity.getId(), CpNamingUtils.ALL_TYPES, entityType );
//        batch.index( allTypesIndexScope, targetEntity );


        BetterFuture future = batch.execute();

        Keyspace ko = cass.getApplicationKeyspace( applicationId );
        Mutator<ByteBuffer> m = createMutator( ko, be );
        batchUpdateEntityConnection( m, false, connection, UUIDGenerator.newTimeUUID() );
        //Added Graphite Metrics
        Timer.Context timeElasticIndexBatch = createConnectionTimer.time();
        batchExecute( m, CassandraService.RETRY_COUNT );
        timeElasticIndexBatch.stop();


        return connection;
    }


    @SuppressWarnings( "unchecked" )
    public Mutator<ByteBuffer> batchUpdateEntityConnection(
            Mutator<ByteBuffer> batch,
            boolean disconnect,
            ConnectionRefImpl conn,
            UUID timestampUuid ) throws Exception {

        long timestamp = getTimestampInMicros( timestampUuid );

        Entity connectedEntity = em.get(new SimpleEntityRef(
                conn.getConnectedEntityType(), conn.getConnectedEntityId() ) );

        if ( connectedEntity == null ) {
            return batch;
        }

        // Create connection for requested params

        if ( disconnect ) {

            addDeleteToMutator(batch, ENTITY_COMPOSITE_DICTIONARIES,
                key(conn.getConnectingEntityId(), DICTIONARY_CONNECTED_ENTITIES,
                        conn.getConnectionType() ),
                asList(conn.getConnectedEntityId(), conn.getConnectedEntityType() ), timestamp );

            addDeleteToMutator(batch, ENTITY_COMPOSITE_DICTIONARIES,
                key(conn.getConnectedEntityId(), DICTIONARY_CONNECTING_ENTITIES,
                        conn.getConnectionType() ),
                asList(conn.getConnectingEntityId(), conn.getConnectingEntityType() ), timestamp );

            // delete the connection path if there will be no connections left

            // check out outbound edges of the given type.  If we have more than the 1 specified,
            // we shouldn't delete the connection types from our outbound index
            if ( !moreThanOneOutboundConnection(conn.getConnectingEntity(), conn.getConnectionType() ) ) {

                addDeleteToMutator(batch, ENTITY_DICTIONARIES,
                        key(conn.getConnectingEntityId(), DICTIONARY_CONNECTED_TYPES ),
                        conn.getConnectionType(), timestamp );
            }

            //check out inbound edges of the given type.  If we have more than the 1 specified,
            // we shouldn't delete the connection types from our outbound index
            if ( !moreThanOneInboundConnection(conn.getConnectingEntity(), conn.getConnectionType() ) ) {

                addDeleteToMutator(batch, ENTITY_DICTIONARIES,
                    key(conn.getConnectedEntityId(), DICTIONARY_CONNECTING_TYPES ),
                    conn.getConnectionType(), timestamp );
        }
        }
        else {

            addInsertToMutator(batch, ENTITY_COMPOSITE_DICTIONARIES,
                    key(conn.getConnectingEntityId(), DICTIONARY_CONNECTED_ENTITIES,
                            conn.getConnectionType() ),
                    asList(conn.getConnectedEntityId(), conn.getConnectedEntityType() ), timestamp,
                    timestamp );

            addInsertToMutator(batch, ENTITY_COMPOSITE_DICTIONARIES,
                    key(conn.getConnectedEntityId(), DICTIONARY_CONNECTING_ENTITIES,
                            conn.getConnectionType() ),
                    asList(conn.getConnectingEntityId(), conn.getConnectingEntityType() ), timestamp,
                    timestamp );

            // Add connection type to connections set
            addInsertToMutator(batch, ENTITY_DICTIONARIES,
                    key(conn.getConnectingEntityId(), DICTIONARY_CONNECTED_TYPES ),
                    conn.getConnectionType(), null, timestamp );

            // Add connection type to connections set
            addInsertToMutator(batch, ENTITY_DICTIONARIES,
                    key(conn.getConnectedEntityId(), DICTIONARY_CONNECTING_TYPES ),
                    conn.getConnectionType(), null, timestamp );
        }

        // Add indexes for the connected entity's list properties

        // Get the names of the list properties in the connected entity
        Set<String> dictionaryNames = em.getDictionaryNames( connectedEntity );

        // For each list property, get the values in the list and
        // update the index with those values

        Schema schema = getDefaultSchema();

        for ( String dictionaryName : dictionaryNames ) {

            boolean has_dictionary = schema.hasDictionary(
                    connectedEntity.getType(), dictionaryName );

            boolean dictionary_indexed = schema.isDictionaryIndexedInConnections(
                    connectedEntity.getType(), dictionaryName );

            if ( dictionary_indexed || !has_dictionary ) {
                Set<Object> elementValues = em.getDictionaryAsSet( connectedEntity, dictionaryName );
                for ( Object elementValue : elementValues ) {
                    IndexUpdate indexUpdate = batchStartIndexUpdate(
                            batch, connectedEntity, dictionaryName, elementValue,
                            timestampUuid, has_dictionary, true, disconnect, false );
                    batchUpdateConnectionIndex(indexUpdate, conn );
                }
            }
        }

        return batch;
    }


    @Override
    public ConnectionRef createConnection(
            String pairedConnectionType,
            EntityRef pairedEntity,
            String connectionType,
            EntityRef connectedEntityRef ) throws Exception {

        throw new UnsupportedOperationException( "Paired connections not supported" );
    }


    @Override
    public ConnectionRef createConnection( ConnectedEntityRef... connections ) throws Exception {

        throw new UnsupportedOperationException( "Paired connections not supported" );
    }


    @Override
    public ConnectionRef connectionRef(
            String connectionType, EntityRef connectedEntityRef ) throws Exception {

        ConnectionRef connection = new ConnectionRefImpl( headEntity, connectionType, connectedEntityRef );

        return connection;
    }


    @Override
    public ConnectionRef connectionRef(
            String pairedConnectionType,
            EntityRef pairedEntity,
            String connectionType,
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
        Keyspace ko = cass.getApplicationKeyspace( applicationId );
        Mutator<ByteBuffer> m = createMutator( ko, be );
        batchUpdateEntityConnection(
                m, true, ( ConnectionRefImpl ) connectionRef, UUIDGenerator.newTimeUUID() );

        //Added Graphite Metrics
        Timer.Context timeDeleteConnections = cassConnectionDelete.time();
        batchExecute( m, CassandraService.RETRY_COUNT );
        timeDeleteConnections.stop();

        EntityRef connectingEntityRef = connectionRef.getConnectingEntity();  // source
        EntityRef connectedEntityRef = connectionRef.getConnectedEntity();  // target

        String connectionType = connectionRef.getConnectedEntity().getConnectionType();


        if ( logger.isDebugEnabled() ) {
            logger.debug( "Deleting connection '{}' from source {}:{} \n   to target {}:{}",
                new Object[] {
                    connectionType,
                    connectingEntityRef.getType(),
                    connectingEntityRef.getUuid(),
                    connectedEntityRef.getType(),
                    connectedEntityRef.getUuid()
                });
        }

        Id entityId = new SimpleId( connectedEntityRef.getUuid(), connectedEntityRef.getType() );
        org.apache.usergrid.persistence.model.entity.Entity targetEntity =
            ((CpEntityManager)em).load( entityId );

        // Delete graph edge connection from head entity to member entity
        Edge edge = new SimpleEdge(
            new SimpleId( connectingEntityRef.getUuid(),
                connectingEntityRef.getType() ),
                connectionType,
                targetEntity.getId(),
                System.currentTimeMillis() );

        GraphManager gm = managerCache.getGraphManager( applicationScope );
        gm.deleteEdge( edge ).toBlocking().last();

        final ApplicationEntityIndex ei = managerCache.getEntityIndex( applicationScope );
        final EntityIndexBatch batch = ei.createBatch();

        // Deindex the connection in app|source|type context
        final Id cpId =  createId( connectingEntityRef );
        IndexScope indexScope = generateScopeFromConnection( cpId, connectionType );
        batch.deindex( indexScope, targetEntity );

        // Deindex the connection in app|source|type context
        //TODO REMOVE INDEX CODE
//        IndexScope allTypesIndexScope = new IndexScopeImpl(
//            new SimpleId( connectingEntityRef.getUuid(),
//                connectingEntityRef.getType() ),
//                CpNamingUtils.ALL_TYPES, entityType );
//
//        batch.deindex( allTypesIndexScope, targetEntity );

        //Added Graphite Metrics
        Timer.Context timeDeleteConnection = esDeleteConnectionTimer.time();
        batch.execute();
        timeDeleteConnection.stop();

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
        Set<String> connections = cast(
                em.getDictionaryAsSet( headEntity, Schema.DICTIONARY_CONNECTED_TYPES ) );

        if ( connections == null ) {
            return null;
        }
        if ( filterConnection && ( connections.size() > 0 ) ) {
            connections.remove( "connection" );
        }
        return connections;
    }


    @Override
    public Results getConnectedEntities(
            String connectionType, String connectedEntityType, Level level ) throws Exception {

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
    public Results getConnectingEntities(
            String connType, String fromEntityType, Level resultsLevel ) throws Exception {

        return getConnectingEntities( connType, fromEntityType, resultsLevel, -1 );
    }


    @Override
    public Results getConnectingEntities(
            String connType, String fromEntityType, Level level, int count ) throws Exception {

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

        Preconditions.checkNotNull(query, "query cannot be null");

        final String connection = query.getConnectionType();

        Preconditions.checkNotNull( connection, "connection must be specified" );

//        if ( query == null ) {
//            query = new Query();
//        }

        headEntity = em.validate( headEntity );

        final IndexScope indexScope = generateScopeFromConnection( cpHeadEntity.getId(), connection );

        final SearchTypes searchTypes = SearchTypes.fromNullableTypes( query.getEntityType() );

        ApplicationEntityIndex ei = managerCache.getEntityIndex( applicationScope );

        logger.debug( "Searching connections from the scope {}:{} with types {}", new Object[] {
                        indexScope.getOwner().toString(), indexScope.getName(), searchTypes
                } );

        query = adjustQuery( query );

        final ConnectionResultsLoaderFactoryImpl resultsLoaderFactory = new ConnectionResultsLoaderFactoryImpl( managerCache,
            headEntity, connection );

        final QueryExecutor executor = new ElasticSearchQueryExecutor(resultsLoaderFactory, ei, applicationScope, indexScope, searchTypes, query);

        return executor.next();
//        CandidateResults crs = ei.search( indexScope, searchTypes, query );

//        return buildConnectionResults( indexScope, query, crs, connection );
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

                    Query newQuery = Query.fromQL( "select * where email='"
                            + query.getSingleNameOrEmailIdentifier() + "'" );
                    query.setRootOperand( newQuery.getRootOperand() );
                }

                // use the ident with the default alias. could be an email
                else {

                    Query newQuery = Query.fromQL( "select * where name='"
                            + query.getSingleNameOrEmailIdentifier() + "'" );
                    query.setRootOperand( newQuery.getRootOperand() );
                }
            }
            else if ( query.containsSingleUuidIdentifier() ) {

                Query newQuery = Query.fromQL(
                        "select * where uuid='" + query.getSingleUuidIdentifier() + "'" );
                query.setRootOperand( newQuery.getRootOperand() );
            }
        }

        if ( query.isReversed() ) {

            Query.SortPredicate desc =
                new Query.SortPredicate( PROPERTY_CREATED, Query.SortDirection.DESCENDING );

            try {
                query.addSort( desc );
            }
            catch ( Exception e ) {
                logger.warn( "Attempted to reverse sort order already set", PROPERTY_CREATED );
            }
        }

        if ( query.getSortPredicates().isEmpty() ) {

            Query.SortPredicate asc =
                new Query.SortPredicate( PROPERTY_CREATED, Query.SortDirection.ASCENDING);

            query.addSort( asc );
        }

        return query;
    }


    @Override
    public Set<String> getConnectionIndexes( String connectionType ) throws Exception {
        throw new UnsupportedOperationException( "Not supported yet." );
    }


    private CpRelationManager getRelationManager( EntityRef headEntity ) {
        CpRelationManager rmi = new CpRelationManager();
        rmi.init( em, emf, applicationId, headEntity, null, metricsFactory);
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

//
//    private Results buildConnectionResults( final IndexScope indexScope,
//            final Query query, final CandidateResults crs, final String connectionType ) {
//
//        if ( query.getLevel().equals( Level.ALL_PROPERTIES ) ) {
//            return buildResults( indexScope, query, crs, connectionType );
//        }
//
//        final EntityRef sourceRef = new SimpleEntityRef( headEntity.getType(), headEntity.getUuid() );
//
//        List<ConnectionRef> refs = new ArrayList<ConnectionRef>( crs.size() );
//
//        for ( CandidateResult cr : crs ) {
//
//            SimpleEntityRef targetRef =
//                    new SimpleEntityRef( cr.getId().getType(), cr.getId().getUuid() );
//
//            final ConnectionRef ref =
//                    new ConnectionRefImpl( sourceRef, connectionType, targetRef );
//
//            refs.add( ref );
//        }
//
//        return Results.fromConnections( refs );
//    }




    @Override
    public void batchUpdateSetIndexes( Mutator<ByteBuffer> batch, String setName, Object elementValue,
                                       boolean removeFromSet, UUID timestampUuid ) throws Exception {

        Entity entity = getHeadEntity();

        elementValue = getDefaultSchema()
                .validateEntitySetValue( entity.getType(), setName, elementValue );

        IndexUpdate indexUpdate = batchStartIndexUpdate( batch, entity, setName, elementValue,
                timestampUuid, true, true, removeFromSet, false );

        // Update collections

        Map<String, Set<CollectionInfo>> containers =
                getDefaultSchema().getContainersIndexingDictionary( entity.getType(), setName );

        if ( containers != null ) {
            Map<EntityRef, Set<String>> containerEntities = getContainers();
            for ( EntityRef containerEntity : containerEntities.keySet() ) {
                if ( containerEntity.getType().equals( TYPE_APPLICATION )
                        && Schema.isAssociatedEntityType( entity.getType() ) ) {
                    logger.debug( "Extended properties for {} not indexed by application", entity.getType() );
                    continue;
                }
                Set<String> collectionNames = containerEntities.get( containerEntity );
                Set<CollectionInfo> collections = containers.get( containerEntity.getType() );

                if ( collections != null ) {

                    for ( CollectionInfo collection : collections ) {
                        if ( collectionNames.contains( collection.getName() ) ) {
                            batchUpdateCollectionIndex( indexUpdate, containerEntity, collection.getName() );
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
     * @param owner The entity that is the owner context of this entity update. Can either be an
     * application, or another * entity
     * @param collectionName the collection name
     *
     * @return The indexUpdate with batch mutations
     * @throws Exception the exception
     */
    public IndexUpdate batchUpdateCollectionIndex(
            IndexUpdate indexUpdate, EntityRef owner, String collectionName )
            throws Exception {

        logger.debug( "batchUpdateCollectionIndex" );

        Entity indexedEntity = indexUpdate.getEntity();

        String bucketId = indexBucketLocator
                .getBucket( applicationId, IndexBucketLocator.IndexType.COLLECTION, indexedEntity.getUuid(),
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
                && ( !indexUpdate.isMultiValue()
                || ( indexUpdate.isMultiValue() && !indexUpdate.isRemoveListEntry() ) ) ) {

            for ( IndexUpdate.IndexEntry indexEntry : indexUpdate.getNewEntries() ) {

                // byte valueCode = indexEntry.getValueCode();

                index_name = key( owner.getUuid(), collectionName, indexEntry.getPath() );

                index_key = key( index_name, bucketId );

                // int i = 0;

                addInsertToMutator( indexUpdate.getBatch(), ENTITY_INDEX, index_key,
                        indexEntry.getIndexComposite(), null, indexUpdate.getTimestamp() );

                if ( "location.coordinates".equals( indexEntry.getPath() ) ) {
                    EntityLocationRef loc = new EntityLocationRef(
                            indexUpdate.getEntity(),
                            indexEntry.getTimestampUuid(),
                            indexEntry.getValue().toString() );
                    batchStoreLocationInCollectionIndex(
                            indexUpdate.getBatch(),
                            indexBucketLocator,
                            applicationId,
                            index_name,
                            indexedEntity.getUuid(),
                            loc );
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
                    cass.getApplicationKeyspace( applicationId ),
                        ENTITY_INDEX_ENTRIES,
                        entity.getUuid(),
                        new DynamicComposite(
                            entryName,
                            indexValueCode( entryValue ),
                            toIndexableValue( entryValue ) ),
                        setGreaterThanEqualityFlag(
                            new DynamicComposite(
                                entryName, indexValueCode( entryValue ),
                                toIndexableValue( entryValue ) ) ),
                        INDEX_ENTRY_LIST_COUNT,
                        false );
            }
            else {
                entries = cass.getColumns(
                    cass.getApplicationKeyspace( applicationId ),
                    ENTITY_INDEX_ENTRIES,
                    entity.getUuid(),
                    new DynamicComposite( entryName ),
                    setGreaterThanEqualityFlag( new DynamicComposite( entryName ) ),
                    INDEX_ENTRY_LIST_COUNT,
                    false );
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

            List<Map.Entry<String, Object>> list =
                    IndexUtils.getKeyValueList( entryName, entryValue, fulltextIndexed );

            if ( entryName.equalsIgnoreCase( "location" ) && ( entryValue instanceof Map ) ) {
                @SuppressWarnings( "rawtypes" ) double latitude =
                        MapUtils.getDoubleValue( ( Map ) entryValue, "latitude" );
                @SuppressWarnings( "rawtypes" ) double longitude =
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

        logger.debug( "doBackwardConnectionsUpdate" );

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
    public IndexUpdate batchUpdateConnectionIndex(
            IndexUpdate indexUpdate, ConnectionRefImpl connection ) throws Exception {

        logger.debug( "batchUpdateConnectionIndex" );

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
        query.setLimit( count );

        final ConnectionRefImpl connectionRef = new ConnectionRefImpl(
                new SimpleEntityRef( connectedEntityType, null ), connectionType, targetEntity );
        final ConnectionResultsLoaderFactory factory =
                new ConnectionResultsLoaderFactory( connectionRef );

        QueryProcessorImpl qp = new QueryProcessorImpl( query, null, em, factory );
        SearchConnectionVisitor visitor = new SearchConnectionVisitor( qp, connectionRef, false );

        return qp.getResults( visitor );
    }


    public Mutator<ByteBuffer> batchDeleteConnectionIndexEntries(
            IndexUpdate indexUpdate,
            IndexUpdate.IndexEntry entry,
            ConnectionRefImpl connection,
            UUID[] index_keys ) throws Exception {

        logger.debug( "batchDeleteConnectionIndexEntries" );

        // entity_id,prop_name
        Object property_index_key = key( index_keys[ConnectionRefImpl.ALL], INDEX_CONNECTIONS, entry.getPath(),
                indexBucketLocator.getBucket( applicationId, IndexBucketLocator.IndexType.CONNECTION,
                        index_keys[ConnectionRefImpl.ALL], entry.getPath() ) );

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


    public Mutator<ByteBuffer> batchAddConnectionIndexEntries( IndexUpdate indexUpdate, IndexUpdate.IndexEntry entry,
                                                               ConnectionRefImpl conn, UUID[] index_keys ) {

        logger.debug( "batchAddConnectionIndexEntries" );

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

        /** True if we should search from source->target edges.
         * False if we should search from target<-source edges */
        private final boolean outgoing;


        /**
         * @param queryProcessor They query processor to use
         * @param connection The connection refernce
         * @param outgoing The direction to search.  True if we should search from source->target
         * edges.  False if we * should search from target<-source edges
         */
        public SearchConnectionVisitor( QueryProcessorImpl queryProcessor, ConnectionRefImpl connection,
                                        boolean outgoing ) {
            super( queryProcessor );
            this.connection = connection;
            this.outgoing = outgoing;
        }


        /* (non-Javadoc)
     * @see org.apache.usergrid.persistence.query.ir.SearchVisitor#secondaryIndexScan(org.apache.usergrid.persistence
     * .query.ir
     * .QueryNode, org.apache.usergrid.persistence.query.ir.QuerySlice)
     */
        @Override
        protected IndexScanner secondaryIndexScan( QueryNode node, QuerySlice slice ) throws Exception {

            UUID id = ConnectionRefImpl.getIndexId(
                    ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE,
                    headEntity,
                    connection.getConnectionType(),
                    connection.getConnectedEntityType(),
                    new ConnectedEntityRef[0] );

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
                query.getLimit(),
                slice,
                node.getPropertyName(),
                new Point( node.getLattitude(), node.getLongitude() ),
                node.getDistance() );

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

            final ConnectionIndexSliceParser connectionParser = new ConnectionIndexSliceParser( targetType );

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
                    cass,
                    dictionaryType,
                    applicationId,
                    entityIdToUse,
                    connectionTypes,
                    start,
                    slice.isReversed(),
                    size,
                    skipFirst );

            this.results.push( new SliceIterator( slice, connectionScanner, connectionParser ) );
        }


        @Override
        public void visit( NameIdentifierNode nameIdentifierNode ) throws Exception {

            //TODO T.N. USERGRID-1919 actually validate this is connected
            EntityRef ref = em.getAlias( connection.getConnectedEntityType(), nameIdentifierNode.getName() );

            if ( ref == null ) {
                this.results.push( new EmptyIterator() );
                return;
            }

            this.results.push( new StaticIdIterator( ref.getUuid() ) );
        }
    }


    private IndexScanner searchIndex( Object indexKey, QuerySlice slice, int pageSize ) throws Exception {

        DynamicComposite[] range = slice.getRange();

        Object keyPrefix = key( indexKey, slice.getPropertyName() );

        IndexScanner scanner = new IndexBucketScanner(
                cass,
                indexBucketLocator,
                ENTITY_INDEX,
                applicationId,
                IndexBucketLocator.IndexType.CONNECTION,
                keyPrefix,
                range[0],
                range[1],
                slice.isReversed(),
                pageSize,
                slice.hasCursor(),
                slice.getPropertyName() );

        return scanner;
    }
}
