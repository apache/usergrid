/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.cassandra;


import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.apache.usergrid.persistence.CollectionRef;
import org.apache.usergrid.persistence.ConnectedEntityRef;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.IndexBucketLocator.IndexType;
import org.apache.usergrid.persistence.PagingResultsIterator;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.RelationManager;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.RoleRef;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.SimpleCollectionRef;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.SimpleRoleRef;
import org.apache.usergrid.persistence.cassandra.IndexUpdate.IndexEntry;
import org.apache.usergrid.persistence.cassandra.index.ConnectedIndexScanner;
import org.apache.usergrid.persistence.cassandra.index.IndexBucketScanner;
import org.apache.usergrid.persistence.cassandra.index.IndexScanner;
import org.apache.usergrid.persistence.cassandra.index.NoOpIndexScanner;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.geo.CollectionGeoSearch;
import org.apache.usergrid.persistence.geo.ConnectionGeoSearch;
import org.apache.usergrid.persistence.geo.EntityLocationRef;
import org.apache.usergrid.persistence.geo.model.Point;
import org.apache.usergrid.persistence.hector.CountingMutator;
import org.apache.usergrid.persistence.query.ir.AllNode;
import org.apache.usergrid.persistence.query.ir.NameIdentifierNode;
import org.apache.usergrid.persistence.query.ir.QueryNode;
import org.apache.usergrid.persistence.query.ir.QuerySlice;
import org.apache.usergrid.persistence.query.ir.SearchVisitor;
import org.apache.usergrid.persistence.query.ir.WithinNode;
import org.apache.usergrid.persistence.query.ir.result.CollectionResultsLoaderFactory;
import org.apache.usergrid.persistence.query.ir.result.ConnectionIndexSliceParser;
import org.apache.usergrid.persistence.query.ir.result.ConnectionResultsLoaderFactory;
import org.apache.usergrid.persistence.query.ir.result.ConnectionTypesIterator;
import org.apache.usergrid.persistence.query.ir.result.EmptyIterator;
import org.apache.usergrid.persistence.query.ir.result.GeoIterator;
import org.apache.usergrid.persistence.query.ir.result.SliceIterator;
import org.apache.usergrid.persistence.query.ir.result.StaticIdIterator;
import org.apache.usergrid.persistence.query.ir.result.UUIDIndexSliceParser;
import org.apache.usergrid.persistence.schema.CollectionInfo;
import org.apache.usergrid.utils.IndexUtils;
import org.apache.usergrid.utils.MapUtils;

import com.google.common.base.Preconditions;

import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Arrays.asList;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import org.apache.usergrid.persistence.EntityManager;
import static org.apache.usergrid.persistence.Schema.COLLECTION_ROLES;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_COLLECTIONS;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_CONNECTED_ENTITIES;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_CONNECTED_TYPES;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_CONNECTING_ENTITIES;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_CONNECTING_TYPES;
import static org.apache.usergrid.persistence.Schema.INDEX_CONNECTIONS;
import static org.apache.usergrid.persistence.Schema.PROPERTY_COLLECTION_NAME;
import static org.apache.usergrid.persistence.Schema.PROPERTY_INACTIVITY;
import static org.apache.usergrid.persistence.Schema.PROPERTY_ITEM;
import static org.apache.usergrid.persistence.Schema.PROPERTY_ITEM_TYPE;
import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.PROPERTY_TITLE;
import static org.apache.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;
import static org.apache.usergrid.persistence.Schema.TYPE_ENTITY;
import static org.apache.usergrid.persistence.Schema.TYPE_MEMBER;
import static org.apache.usergrid.persistence.Schema.TYPE_ROLE;
import static org.apache.usergrid.persistence.Schema.defaultCollectionName;
import static org.apache.usergrid.persistence.Schema.getDefaultSchema;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_COMPOSITE_DICTIONARIES;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_DICTIONARIES;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_ID_SETS;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX_ENTRIES;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addDeleteToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addInsertToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.batchExecute;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.apache.usergrid.persistence.cassandra.CassandraService.INDEX_ENTRY_LIST_COUNT;
import static org.apache.usergrid.persistence.cassandra.ConnectionRefImpl.CONNECTION_ENTITY_CONNECTION_TYPE;
import static org.apache.usergrid.persistence.cassandra.GeoIndexManager.batchDeleteLocationInConnectionsIndex;
import static org.apache.usergrid.persistence.cassandra.GeoIndexManager.batchRemoveLocationFromCollectionIndex;
import static org.apache.usergrid.persistence.cassandra.GeoIndexManager.batchStoreLocationInCollectionIndex;
import static org.apache.usergrid.persistence.cassandra.GeoIndexManager.batchStoreLocationInConnectionsIndex;
import static org.apache.usergrid.persistence.cassandra.IndexUpdate.indexValueCode;
import static org.apache.usergrid.persistence.cassandra.IndexUpdate.toIndexableValue;
import static org.apache.usergrid.persistence.cassandra.IndexUpdate.validIndexableValue;
import static org.apache.usergrid.utils.ClassUtils.cast;
import static org.apache.usergrid.utils.CompositeUtils.setGreaterThanEqualityFlag;
import static org.apache.usergrid.utils.ConversionUtils.string;
import static org.apache.usergrid.utils.InflectionUtils.singularize;
import static org.apache.usergrid.utils.MapUtils.addMapSet;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMicros;
import static org.apache.usergrid.utils.UUIDUtils.newTimeUUID;

import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.index.query.Query.Level;


public class RelationManagerImpl implements RelationManager {

    private static final Logger logger = LoggerFactory.getLogger( RelationManagerImpl.class );

    private EntityManager em;
    private CassandraService cass;
    private UUID applicationId;
    private EntityRef headEntity;
    private IndexBucketLocator indexBucketLocator;


    public RelationManagerImpl() {
    }


    public RelationManagerImpl init( EntityManager em, CassandraService cass, UUID applicationId,
                                     EntityRef headEntity, IndexBucketLocator indexBucketLocator ) {

        Assert.notNull( em, "Entity manager cannot be null" );
        Assert.notNull( cass, "Cassandra service cannot be null" );
        Assert.notNull( applicationId, "Application Id cannot be null" );
        Assert.notNull( headEntity, "Head entity cannot be null" );
        Assert.notNull( headEntity.getUuid(), "Head entity uuid cannot be null" );
        Assert.notNull( indexBucketLocator, "Index bucket locator cannot be null" );

        this.em = em;
        this.applicationId = applicationId;
        this.cass = cass;
        this.headEntity = headEntity;
        this.indexBucketLocator = indexBucketLocator;

        return this;
    }


    private RelationManagerImpl getRelationManager( EntityRef headEntity ) {
        RelationManagerImpl rmi = new RelationManagerImpl();
        rmi.init( em, cass, applicationId, headEntity, indexBucketLocator );
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


    /**
     * Batch update collection index.
     *
     * @param indexUpdate The update to apply
     * @param owner The entity that is the owner context of this entity update.  Can either be an application, or
     * another entity
     * @param collectionName the collection name
     *
     * @return The indexUpdate with batch mutations
     *
     * @throws Exception the exception
     */
    public IndexUpdate batchUpdateCollectionIndex( IndexUpdate indexUpdate, EntityRef owner, String collectionName )
            throws Exception {

        logger.debug( "batchUpdateCollectionIndex" );

        Entity indexedEntity = indexUpdate.getEntity();

        String bucketId = indexBucketLocator
                .getBucket( applicationId, IndexType.COLLECTION, indexedEntity.getUuid(), indexedEntity.getType(),
                        indexUpdate.getEntryName() );

        // the root name without the bucket
        // entity_id,collection_name,prop_name,
        Object index_name = null;
        // entity_id,collection_name,prop_name, bucketId
        Object index_key = null;

        // entity_id,collection_name,collected_entity_id,prop_name

        for ( IndexEntry entry : indexUpdate.getPrevEntries() ) {

            if ( entry.getValue() != null ) {

                index_name = key( owner.getUuid(), collectionName, entry.getPath() );

                index_key = key( index_name, bucketId );

                addDeleteToMutator( indexUpdate.getBatch(), ENTITY_INDEX, index_key, entry.getIndexComposite(),
                        indexUpdate.getTimestamp() );

                if ( "location.coordinates".equals( entry.getPath() ) ) {
                    EntityLocationRef loc = new EntityLocationRef( indexUpdate.getEntity(), entry.getTimestampUuid(),
                            entry.getValue().toString() );
                    batchRemoveLocationFromCollectionIndex( indexUpdate.getBatch(), indexBucketLocator, applicationId,
                            index_name, loc );
                }
            }
            else {
                logger.error( "Unexpected condition - deserialized property value is null" );
            }
        }

        if ( ( indexUpdate.getNewEntries().size() > 0 ) && ( !indexUpdate.isMultiValue() || ( indexUpdate.isMultiValue()
                && !indexUpdate.isRemoveListEntry() ) ) ) {

            for ( IndexEntry indexEntry : indexUpdate.getNewEntries() ) {

                // byte valueCode = indexEntry.getValueCode();

                index_name = key( owner.getUuid(), collectionName, indexEntry.getPath() );

                index_key = key( index_name, bucketId );

                // int i = 0;

                addInsertToMutator( indexUpdate.getBatch(), ENTITY_INDEX, index_key, indexEntry.getIndexComposite(),
                        null, indexUpdate.getTimestamp() );

                if ( "location.coordinates".equals( indexEntry.getPath() ) ) {
                    EntityLocationRef loc =
                            new EntityLocationRef( indexUpdate.getEntity(), indexEntry.getTimestampUuid(),
                                    indexEntry.getValue().toString() );
                    batchStoreLocationInCollectionIndex( indexUpdate.getBatch(), indexBucketLocator, applicationId,
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


    @Override
    public Set<String> getCollectionIndexes( String collectionName ) throws Exception {

        // TODO TN, read all buckets here
        List<HColumn<String, String>> results =
                cass.getAllColumns( cass.getApplicationKeyspace( applicationId ), ENTITY_DICTIONARIES,
                        key( headEntity.getUuid(), collectionName, Schema.DICTIONARY_INDEXES ), Serializers.se, Serializers.se );
        Set<String> indexes = new TreeSet<String>();
        if ( results != null ) {
            for ( HColumn<String, String> column : results ) {
                String propertyName = column.getName();
                if ( !propertyName.endsWith( ".keywords" ) ) {
                    indexes.add( column.getName() );
                }
            }
        }
        return indexes;
    }


    public Map<EntityRef, Set<String>> getContainingCollections() throws Exception {
        Map<EntityRef, Set<String>> results = new LinkedHashMap<EntityRef, Set<String>>();

        Keyspace ko = cass.getApplicationKeyspace( applicationId );

        // TODO TN get all buckets here

        List<HColumn<DynamicComposite, ByteBuffer>> containers = cass.getAllColumns( ko, ENTITY_COMPOSITE_DICTIONARIES,
                key( headEntity.getUuid(), Schema.DICTIONARY_CONTAINER_ENTITIES ), Serializers.dce, Serializers.be );
        if ( containers != null ) {
            for ( HColumn<DynamicComposite, ByteBuffer> container : containers ) {
                DynamicComposite composite = container.getName();
                if ( composite != null ) {
                    String ownerType = ( String ) composite.get( 0 );
                    String collectionName = ( String ) composite.get( 1 );
                    UUID ownerId = ( UUID ) composite.get( 2 );
                    addMapSet( results, new SimpleEntityRef( ownerType, ownerId ), collectionName );
                    if ( logger.isDebugEnabled() ) {
                        logger.debug( " {} ( {} ) is in collection {} ( {} ).", new Object[] {
                                headEntity.getType(), headEntity.getUuid(), ownerType, collectionName, ownerId
                        } );
                    }
                }
            }
        }
        EntityRef applicationRef = new SimpleEntityRef( TYPE_APPLICATION, applicationId );
        if ( !results.containsKey( applicationRef ) ) {
            addMapSet( results, applicationRef, defaultCollectionName( headEntity.getType() ) );
        }
        return results;
    }


    @SuppressWarnings("unchecked")
    public void batchCreateCollectionMembership( Mutator<ByteBuffer> batch, EntityRef ownerRef, String collectionName,
                                                 EntityRef itemRef, EntityRef membershipRef, UUID timestampUuid )
            throws Exception {

        long timestamp = getTimestampInMicros( timestampUuid );

        if ( membershipRef == null ) {
            membershipRef = new SimpleCollectionRef( ownerRef, collectionName, itemRef );
        }

        Map<String, Object> properties = new TreeMap<String, Object>( CASE_INSENSITIVE_ORDER );
        properties.put( PROPERTY_TYPE, membershipRef.getType() );
        properties.put( PROPERTY_COLLECTION_NAME, collectionName );
        properties.put( PROPERTY_ITEM, itemRef.getUuid() );
        properties.put( PROPERTY_ITEM_TYPE, itemRef.getType() );

        em.batchCreate( batch, membershipRef.getType(), null, properties, membershipRef.getUuid(), timestampUuid );

        addInsertToMutator( batch, ENTITY_COMPOSITE_DICTIONARIES,
                key( membershipRef.getUuid(), Schema.DICTIONARY_CONTAINER_ENTITIES ),
                asList( ownerRef.getType(), collectionName, ownerRef.getUuid() ), membershipRef.getUuid(), timestamp );
    }


    /**
     * Batch add to collection.
     *
     * @param batch the batch
     * @param collectionName the collection name
     * @param entity The entity to add to the batch
     * @param timestampUuid The timestamp of this update in a time uuid
     *
     * @return batch
     *
     * @throws Exception the exception
     */
    public Mutator<ByteBuffer> batchAddToCollection( Mutator<ByteBuffer> batch, String collectionName, Entity entity,
                                                     UUID timestampUuid ) throws Exception {
        List<UUID> ids = new ArrayList<UUID>( 1 );
        ids.add( headEntity.getUuid() );
        return batchAddToCollections( batch, headEntity.getType(), ids, collectionName, entity, timestampUuid );
    }


    @SuppressWarnings("unchecked")
    public Mutator<ByteBuffer> batchAddToCollections( Mutator<ByteBuffer> batch, String ownerType, List<UUID> ownerIds,
                                                      String collectionName, Entity entity, UUID timestampUuid )
            throws Exception {

        long timestamp = getTimestampInMicros( timestampUuid );

        if ( Schema.isAssociatedEntityType( entity.getType() ) ) {
            logger.error( "Cant add an extended type to any collection", new Throwable() );
            return batch;
        }

        Map<UUID, CollectionRef> membershipRefs = new LinkedHashMap<UUID, CollectionRef>();

        for ( UUID ownerId : ownerIds ) {

            CollectionRef membershipRef =
                    new SimpleCollectionRef( new SimpleEntityRef( ownerType, ownerId ), collectionName, entity );

            membershipRefs.put( ownerId, membershipRef );

            // get the bucket this entityId needs to be inserted into
            String bucketId = indexBucketLocator
                    .getBucket( applicationId, IndexType.COLLECTION, entity.getUuid(), collectionName );

            Object collections_key = key( ownerId, Schema.DICTIONARY_COLLECTIONS, collectionName, bucketId );

            // Insert in main collection

            addInsertToMutator( batch, ENTITY_ID_SETS, collections_key, entity.getUuid(), membershipRef.getUuid(),
                    timestamp );

            addInsertToMutator( batch, ENTITY_COMPOSITE_DICTIONARIES,
                    key( entity.getUuid(), Schema.DICTIONARY_CONTAINER_ENTITIES ),
                    asList( ownerType, collectionName, ownerId ), membershipRef.getUuid(), timestamp );
        }


        Schema schema = getDefaultSchema();

        // Add property indexes
        for ( String propertyName : entity.getProperties().keySet() ) {
            boolean indexed_property = schema.isPropertyIndexed( entity.getType(), propertyName );
            if ( indexed_property ) {
                boolean collection_indexes_property =
                        schema.isPropertyIndexedInCollection( ownerType, collectionName, propertyName );
                boolean item_schema_has_property = schema.hasProperty( entity.getType(), propertyName );
                boolean fulltext_indexed = schema.isPropertyFulltextIndexed( entity.getType(), propertyName );
                if ( collection_indexes_property || !item_schema_has_property ) {
                    Object propertyValue = entity.getProperty( propertyName );
                    IndexUpdate indexUpdate =
                            batchStartIndexUpdate( batch, entity, propertyName, propertyValue, timestampUuid,
                                    item_schema_has_property, false, false, fulltext_indexed, true );
                    for ( UUID ownerId : ownerIds ) {
                        EntityRef owner = new SimpleEntityRef( ownerType, ownerId );
                        batchUpdateCollectionIndex( indexUpdate, owner, collectionName );
                    }
                }
            }
        }

        // Add set property indexes

        Set<String> dictionaryNames = em.getDictionaryNames( entity );

        for ( String dictionaryName : dictionaryNames ) {
            boolean has_dictionary = schema.hasDictionary( entity.getType(), dictionaryName );
            boolean dictionary_indexed =
                    schema.isDictionaryIndexedInCollection( ownerType, collectionName, dictionaryName );

            if ( dictionary_indexed || !has_dictionary ) {
                Set<Object> elementValues = em.getDictionaryAsSet( entity, dictionaryName );
                for ( Object elementValue : elementValues ) {
                    IndexUpdate indexUpdate =
                            batchStartIndexUpdate( batch, entity, dictionaryName, elementValue, timestampUuid,
                                    has_dictionary, true, false, false, true );
                    for ( UUID ownerId : ownerIds ) {
                        EntityRef owner = new SimpleEntityRef( ownerType, ownerId );
                        batchUpdateCollectionIndex( indexUpdate, owner, collectionName );
                    }
                }
            }
        }

        for ( UUID ownerId : ownerIds ) {
            EntityRef owner = new SimpleEntityRef( ownerType, ownerId );
            batchCreateCollectionMembership( batch, owner, collectionName, entity, membershipRefs.get( ownerId ),
                    timestampUuid );
        }

        return batch;
    }


    /**
     * Batch remove from collection.
     * <p/>
     * * Batch add to collection.
     *
     * @param batch the batch
     * @param collectionName the collection name
     * @param entity The entity to add to the batch
     * @param timestampUuid The timestamp of this update in a time uuid
     *
     * @return The mutation with the delete operations added
     *
     * @throws Exception the exception
     */
    public Mutator<ByteBuffer> batchRemoveFromCollection( Mutator<ByteBuffer> batch, String collectionName,
                                                          Entity entity, UUID timestampUuid ) throws Exception {
        return this.batchRemoveFromCollection( batch, collectionName, entity, false, timestampUuid );
    }


    @SuppressWarnings("unchecked")
    public Mutator<ByteBuffer> batchRemoveFromCollection( Mutator<ByteBuffer> batch, String collectionName,
                                                          Entity entity, boolean force, UUID timestampUuid )
            throws Exception {

        long timestamp = getTimestampInMicros( timestampUuid );

        if ( !force && headEntity.getUuid().equals( applicationId ) ) {
            // Can't remove entities from root collections
            return batch;
        }

        Object collections_key = key( headEntity.getUuid(), Schema.DICTIONARY_COLLECTIONS, collectionName,
                indexBucketLocator.getBucket( applicationId, IndexType.COLLECTION, entity.getUuid(), collectionName ) );

        // Remove property indexes

        Schema schema = getDefaultSchema();
        for ( String propertyName : entity.getProperties().keySet() ) {
            boolean collection_indexes_property =
                    schema.isPropertyIndexedInCollection( headEntity.getType(), collectionName, propertyName );
            boolean item_schema_has_property = schema.hasProperty( entity.getType(), propertyName );
            boolean fulltext_indexed = schema.isPropertyFulltextIndexed( entity.getType(), propertyName );
            if ( collection_indexes_property || !item_schema_has_property ) {
                IndexUpdate indexUpdate = batchStartIndexUpdate( batch, entity, propertyName, null, timestampUuid,
                        item_schema_has_property, false, false, fulltext_indexed );
                batchUpdateCollectionIndex( indexUpdate, headEntity, collectionName );
            }
        }

        // Remove set indexes

        Set<String> dictionaryNames = em.getDictionaryNames( entity );

        for ( String dictionaryName : dictionaryNames ) {
            boolean has_dictionary = schema.hasDictionary( entity.getType(), dictionaryName );
            boolean dictionary_indexed =
                    schema.isDictionaryIndexedInCollection( headEntity.getType(), collectionName, dictionaryName );

            if ( dictionary_indexed || !has_dictionary ) {
                Set<Object> elementValues = em.getDictionaryAsSet( entity, dictionaryName );
                for ( Object elementValue : elementValues ) {
                    IndexUpdate indexUpdate =
                            batchStartIndexUpdate( batch, entity, dictionaryName, elementValue, timestampUuid,
                                    has_dictionary, true, true, false );
                    batchUpdateCollectionIndex( indexUpdate, headEntity, collectionName );
                }
            }
        }

        // Delete actual property

        addDeleteToMutator( batch, ENTITY_ID_SETS, collections_key, entity.getUuid(), timestamp );

        addDeleteToMutator( batch, ENTITY_COMPOSITE_DICTIONARIES,
                key( entity.getUuid(), Schema.DICTIONARY_CONTAINER_ENTITIES ),
                asList( headEntity.getType(), collectionName, headEntity.getUuid() ), timestamp );

        if ( !headEntity.getType().equalsIgnoreCase( TYPE_APPLICATION ) && !Schema
                .isAssociatedEntityType( entity.getType() ) ) {

            CollectionRef cref = new SimpleCollectionRef( headEntity, collectionName, entity );
            em.delete( new SimpleEntityRef( cref.getType(), cref.getUuid() ) );
        }

        return batch;
    }


    public Mutator<ByteBuffer> batchDeleteConnectionIndexEntries( IndexUpdate indexUpdate, IndexEntry entry,
                                                                  ConnectionRefImpl connection, UUID[] index_keys )
            throws Exception {

        // entity_id,prop_name
        Object property_index_key = key( index_keys[ConnectionRefImpl.ALL], INDEX_CONNECTIONS, entry.getPath(),
                indexBucketLocator.getBucket( applicationId, IndexType.CONNECTION, index_keys[ConnectionRefImpl.ALL],
                        entry.getPath() ) );

        // entity_id,entity_type,prop_name
        Object entity_type_prop_index_key =
                key( index_keys[ConnectionRefImpl.BY_ENTITY_TYPE], INDEX_CONNECTIONS, entry.getPath(),
                        indexBucketLocator.getBucket( applicationId, IndexType.CONNECTION,
                                index_keys[ConnectionRefImpl.BY_ENTITY_TYPE], entry.getPath() ) );

        // entity_id,connection_type,prop_name
        Object connection_type_prop_index_key =
                key( index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], INDEX_CONNECTIONS, entry.getPath(),
                        indexBucketLocator.getBucket( applicationId, IndexType.CONNECTION,
                                index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], entry.getPath() ) );

        // entity_id,connection_type,entity_type,prop_name
        Object connection_type_and_entity_type_prop_index_key =
                key( index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], INDEX_CONNECTIONS, entry.getPath(),
                        indexBucketLocator.getBucket( applicationId, IndexType.CONNECTION,
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


    public Mutator<ByteBuffer> batchAddConnectionIndexEntries( IndexUpdate indexUpdate, IndexEntry entry,
                                                               ConnectionRefImpl connection, UUID[] index_keys ) {

        // entity_id,prop_name
        Object property_index_key = key( index_keys[ConnectionRefImpl.ALL], INDEX_CONNECTIONS, entry.getPath(),
                indexBucketLocator.getBucket( applicationId, IndexType.CONNECTION, index_keys[ConnectionRefImpl.ALL],
                        entry.getPath() ) );

        // entity_id,entity_type,prop_name
        Object entity_type_prop_index_key =
                key( index_keys[ConnectionRefImpl.BY_ENTITY_TYPE], INDEX_CONNECTIONS, entry.getPath(),
                        indexBucketLocator.getBucket( applicationId, IndexType.CONNECTION,
                                index_keys[ConnectionRefImpl.BY_ENTITY_TYPE], entry.getPath() ) );

        // entity_id,connection_type,prop_name
        Object connection_type_prop_index_key =
                key( index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], INDEX_CONNECTIONS, entry.getPath(),
                        indexBucketLocator.getBucket( applicationId, IndexType.CONNECTION,
                                index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], entry.getPath() ) );

        // entity_id,connection_type,entity_type,prop_name
        Object connection_type_and_entity_type_prop_index_key =
                key( index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], INDEX_CONNECTIONS, entry.getPath(),
                        indexBucketLocator.getBucket( applicationId, IndexType.CONNECTION,
                                index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], entry.getPath() ) );

        // composite(property_value,connected_entity_id,connection_type,entity_type,entry_timestamp)
        addInsertToMutator( indexUpdate.getBatch(), ENTITY_INDEX, property_index_key,
                entry.getIndexComposite( connection.getConnectedEntityId(), connection.getConnectionType(),
                        connection.getConnectedEntityType() ), connection.getUuid(), indexUpdate.getTimestamp() );

        // composite(property_value,connected_entity_id,connection_type,entry_timestamp)
        addInsertToMutator( indexUpdate.getBatch(), ENTITY_INDEX, entity_type_prop_index_key,
                entry.getIndexComposite( connection.getConnectedEntityId(), connection.getConnectionType() ),
                connection.getUuid(), indexUpdate.getTimestamp() );

        // composite(property_value,connected_entity_id,entity_type,entry_timestamp)
        addInsertToMutator( indexUpdate.getBatch(), ENTITY_INDEX, connection_type_prop_index_key,
                entry.getIndexComposite( connection.getConnectedEntityId(), connection.getConnectedEntityType() ),
                connection.getUuid(), indexUpdate.getTimestamp() );

        // composite(property_value,connected_entity_id,entry_timestamp)
        addInsertToMutator( indexUpdate.getBatch(), ENTITY_INDEX, connection_type_and_entity_type_prop_index_key,
                entry.getIndexComposite( connection.getConnectedEntityId() ), connection.getUuid(),
                indexUpdate.getTimestamp() );

        return indexUpdate.getBatch();
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
    public IndexUpdate batchUpdateConnectionIndex( IndexUpdate indexUpdate, ConnectionRefImpl connection )
            throws Exception {

        // UUID connection_id = connection.getUuid();

        UUID[] index_keys = connection.getIndexIds();

        // Delete all matching entries from entry list
        for ( IndexEntry entry : indexUpdate.getPrevEntries() ) {

            if ( entry.getValue() != null ) {

                batchDeleteConnectionIndexEntries( indexUpdate, entry, connection, index_keys );

                if ( "location.coordinates".equals( entry.getPath() ) ) {
                    EntityLocationRef loc = new EntityLocationRef( indexUpdate.getEntity(), entry.getTimestampUuid(),
                            entry.getValue().toString() );
                    batchDeleteLocationInConnectionsIndex( indexUpdate.getBatch(), indexBucketLocator, applicationId,
                            index_keys, entry.getPath(), loc );
                }
            }
            else {
                logger.error( "Unexpected condition - deserialized property value is null" );
            }
        }

        if ( ( indexUpdate.getNewEntries().size() > 0 ) && ( !indexUpdate.isMultiValue() || ( indexUpdate.isMultiValue()
                && !indexUpdate.isRemoveListEntry() ) ) ) {

            for ( IndexEntry indexEntry : indexUpdate.getNewEntries() ) {

                batchAddConnectionIndexEntries( indexUpdate, indexEntry, connection, index_keys );

                if ( "location.coordinates".equals( indexEntry.getPath() ) ) {
                    EntityLocationRef loc =
                            new EntityLocationRef( indexUpdate.getEntity(), indexEntry.getTimestampUuid(),
                                    indexEntry.getValue().toString() );
                    batchStoreLocationInConnectionsIndex( indexUpdate.getBatch(), indexBucketLocator, applicationId,
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
                    key( connection.getConnectingIndexId(), Schema.DICTIONARY_INDEXES ), index, null,
                    indexUpdate.getTimestamp() );
        }

        return indexUpdate;
    }


    public Set<String> getConnectionIndexes( ConnectionRefImpl connection ) throws Exception {
        List<HColumn<String, String>> results =
                cass.getAllColumns( cass.getApplicationKeyspace( applicationId ), ENTITY_DICTIONARIES,
                        key( connection.getConnectingIndexId(), Schema.DICTIONARY_INDEXES ), Serializers.se, Serializers.se );
        Set<String> indexes = new TreeSet<String>();
        if ( results != null ) {
            for ( HColumn<String, String> column : results ) {
                String propertyName = column.getName();
                if ( !propertyName.endsWith( ".keywords" ) ) {
                    indexes.add( column.getName() );
                }
            }
        }
        return indexes;
    }


    /**
     * Batch update backward connections property indexes.
     *
     * @param indexUpdate The update to run for incoming connections
     *
     * @return The index update to run
     *
     * @throws Exception the exception
     */
    public IndexUpdate batchUpdateBackwardConnectionsPropertyIndexes( IndexUpdate indexUpdate ) throws Exception {

        logger.debug( "batchUpdateBackwordConnectionsPropertyIndexes" );

        boolean entitySchemaHasProperty = indexUpdate.isSchemaHasProperty();

        if ( entitySchemaHasProperty ) {
            if ( !getDefaultSchema()
                    .isPropertyIndexed( indexUpdate.getEntity().getType(), indexUpdate.getEntryName() ) ) {
                return indexUpdate;
            }
        }


        return doBackwardConnectionsUpdate( indexUpdate );
    }


    /**
     * Search each reverse connection type in the graph for connections.  If one is found, update the index
     * appropriately
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

            PagingResultsIterator itr = getReversedConnectionsIterator( targetEntity, connectionType );

            for ( Object connection : itr ) {

                final ConnectedEntityRef sourceEntity = ( ConnectedEntityRef ) connection;

                //we need to create a connection ref from the source entity (found via reverse edge) to the entity
                // we're about to update.  This is the index that needs updated
                final ConnectionRefImpl connectionRef =
                        new ConnectionRefImpl( sourceEntity, connectionType, indexUpdate.getEntity() );

                batchUpdateConnectionIndex( indexUpdate, connectionRef );
            }
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
    private PagingResultsIterator getReversedConnectionsIterator( EntityRef targetEntity, String connectionType )
            throws Exception {
        return new PagingResultsIterator( getConnectingEntities( targetEntity, connectionType, null, Level.REFS ) );
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
    public IndexUpdate batchUpdateBackwardConnectionsDictionaryIndexes( IndexUpdate indexUpdate ) throws Exception {

        logger.debug( "batchUpdateBackwardConnectionsListIndexes" );

        boolean entityHasDictionary = getDefaultSchema()
                .isDictionaryIndexedInConnections( indexUpdate.getEntity().getType(), indexUpdate.getEntryName() );

        if ( !entityHasDictionary ) {
            return indexUpdate;
        }


        return doBackwardConnectionsUpdate( indexUpdate );
    }


    @SuppressWarnings("unchecked")
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
                    asList( connection.getConnectedEntityId(), connection.getConnectedEntityType() ), timestamp );

            addDeleteToMutator( batch, ENTITY_COMPOSITE_DICTIONARIES,
                    key( connection.getConnectedEntityId(), DICTIONARY_CONNECTING_ENTITIES,
                            connection.getConnectionType() ),
                    asList( connection.getConnectingEntityId(), connection.getConnectingEntityType() ), timestamp );

            // delete the connection path if there will be no connections left

            boolean delete = true;

            //check out outbound edges of the given type.  If we have more than the 1 specified,
            // we shouldn't delete the connection types from our outbound index
            PagingResultsIterator itr = new PagingResultsIterator(
                    getConnectedEntities( connection.getConnectingEntity(), connection.getConnectionType(), null,
                            Level.REFS ) );

            ConnectedEntityRef c;

            while ( itr.hasNext() ) {
                c = ( ConnectedEntityRef ) itr.next();

                if ( !connection.getConnectedEntityId().equals( c.getUuid() ) ) {
                    delete = false;
                    break;
                }


                //        c = (ConnectionRef) itr.next();
                //        if (c.getConnectedEntity().getConnectionType().equals(connection.getConnectedEntity()
                // .getConnectionType()) &&!c.getConnectedEntity().getUuid().equals(connection.getConnectedEntity()
                // .getUuid())) {
                //            delete = false;
                //            break;
                //        }

            }
            //      for (ConnectionRefImpl c : getConnectionsWithEntity(connection.getConnectingEntityId())) {
            //        if (c.getConnectedEntity().getConnectionType().equals(connection.getConnectedEntity()
            // .getConnectionType())) {
            //          if (!c.getConnectedEntity().getUuid().equals(connection.getConnectedEntity().getUuid())) {
            //            delete = false;
            //            break;
            //          }
            //        }
            //      }
            if ( delete ) {
                addDeleteToMutator( batch, ENTITY_DICTIONARIES,
                        key( connection.getConnectingEntityId(), DICTIONARY_CONNECTED_TYPES ),
                        connection.getConnectionType(), timestamp );
            }

            // delete the connection path if there will be no connections left
            delete = true;


            //check out inbound edges of the given type.  If we have more than the 1 specified,
            // we shouldn't delete the connection types from our outbound index
            itr = new PagingResultsIterator(
                    getConnectingEntities( connection.getConnectingEntity(), connection.getConnectionType(), null,
                            Level.REFS ) );

            while ( itr.hasNext() ) {
                c = ( ConnectedEntityRef ) itr.next();

                if ( !connection.getConnectedEntityId().equals( c.getUuid() ) ) {
                    delete = false;
                    break;
                }
                //        if (c.getConnectedEntity().getConnectionType().equals(connection.getConnectedEntity()
                // .getConnectionType()) && !c.getConnectingEntity().getUuid().equals(connection.getConnectingEntity
                // ().getUuid())) {
                //            delete = false;
                //            break;
                //        }

            }

            //      for (ConnectionRefImpl c : getConnectionsWithEntity(connection.getConnectedEntityId())) {
            //        if (c.getConnectedEntity().getConnectionType().equals(connection.getConnectedEntity()
            // .getConnectionType())) {
            //          if (!c.getConnectingEntity().getUuid().equals(connection.getConnectingEntity().getUuid())) {
            //            delete = false;
            //            break;
            //          }
            //        }
            //      }
            if ( delete ) {
                addDeleteToMutator( batch, ENTITY_DICTIONARIES,
                        key( connection.getConnectedEntityId(), DICTIONARY_CONNECTING_TYPES ),
                        connection.getConnectionType(), timestamp );
            }
        }
        else {
            addInsertToMutator( batch, ENTITY_COMPOSITE_DICTIONARIES,
                    key( connection.getConnectingEntityId(), DICTIONARY_CONNECTED_ENTITIES,
                            connection.getConnectionType() ),
                    asList( connection.getConnectedEntityId(), connection.getConnectedEntityType() ), timestamp,
                    timestamp );

            addInsertToMutator( batch, ENTITY_COMPOSITE_DICTIONARIES,
                    key( connection.getConnectedEntityId(), DICTIONARY_CONNECTING_ENTITIES,
                            connection.getConnectionType() ),
                    asList( connection.getConnectingEntityId(), connection.getConnectingEntityType() ), timestamp,
                    timestamp );

            // Add connection type to connections set
            addInsertToMutator( batch, ENTITY_DICTIONARIES,
                    key( connection.getConnectingEntityId(), DICTIONARY_CONNECTED_TYPES ),
                    connection.getConnectionType(), null, timestamp );

            // Add connection type to connections set
            addInsertToMutator( batch, ENTITY_DICTIONARIES,
                    key( connection.getConnectedEntityId(), DICTIONARY_CONNECTING_TYPES ),
                    connection.getConnectionType(), null, timestamp );
        }

        // Add property indexes

        // Iterate though all the properties of the connected entity

        Schema schema = getDefaultSchema();
        for ( String propertyName : connectedEntity.getProperties().keySet() ) {
            Object propertyValue = connectedEntity.getProperties().get( propertyName );

            boolean indexed = schema.isPropertyIndexed( connectedEntity.getType(), propertyName );

            boolean connection_indexes_property = schema.isPropertyIndexed( connectedEntity.getType(), propertyName );
            boolean item_schema_has_property = schema.hasProperty( connectedEntity.getType(), propertyName );
            boolean fulltext_indexed = schema.isPropertyFulltextIndexed( connectedEntity.getType(), propertyName );
            // For each property, if the schema says it's indexed, update its
            // index

            if ( indexed && ( connection_indexes_property || !item_schema_has_property ) ) {
                IndexUpdate indexUpdate =
                        batchStartIndexUpdate( batch, connectedEntity, propertyName, disconnect ? null : propertyValue,
                                timestampUuid, item_schema_has_property, false, false, fulltext_indexed );
                batchUpdateConnectionIndex( indexUpdate, connection );
            }
        }

        // Add indexes for the connected entity's list properties

        // Get the names of the list properties in the connected entity
        Set<String> dictionaryNames = em.getDictionaryNames( connectedEntity );

        // For each list property, get the values in the list and
        // update the index with those values

        for ( String dictionaryName : dictionaryNames ) {
            boolean has_dictionary = schema.hasDictionary( connectedEntity.getType(), dictionaryName );
            boolean dictionary_indexed =
                    schema.isDictionaryIndexedInConnections( connectedEntity.getType(), dictionaryName );

            if ( dictionary_indexed || !has_dictionary ) {
                Set<Object> elementValues = em.getDictionaryAsSet( connectedEntity, dictionaryName );
                for ( Object elementValue : elementValues ) {
                    IndexUpdate indexUpdate =
                            batchStartIndexUpdate( batch, connectedEntity, dictionaryName, elementValue, timestampUuid,
                                    has_dictionary, true, disconnect, false );
                    batchUpdateConnectionIndex( indexUpdate, connection );
                }
            }
        }

        return batch;
    }


    public void updateEntityConnection( boolean disconnect, ConnectionRefImpl connection ) throws Exception {

        UUID timestampUuid = newTimeUUID();
        Mutator<ByteBuffer> batch = CountingMutator.createFlushingMutator( cass.getApplicationKeyspace( applicationId ), Serializers.be );

        // Make or break the connection

        batchUpdateEntityConnection( batch, disconnect, connection, timestampUuid );

        // Make or break a connection from the connecting entity
        // to the connection itself

        ConnectionRefImpl loopback = connection.getConnectionToConnectionEntity();
        if ( !disconnect ) {
            em.insertEntity( new SimpleEntityRef(
                    CONNECTION_ENTITY_CONNECTION_TYPE, loopback.getConnectedEntityId() ) );
        }

        batchUpdateEntityConnection( batch, disconnect, loopback, timestampUuid );

        batchExecute( batch, CassandraService.RETRY_COUNT );
    }


    public void batchDisconnect( Mutator<ByteBuffer> batch, UUID timestampUuid ) throws Exception {


        PagingResultsIterator itr =
                new PagingResultsIterator( getConnectingEntities( headEntity, null, null, Level.REFS ) );

        ConnectionRefImpl connection = null;

        while ( itr.hasNext() ) {
            Object itrObj = itr.next();
            if ( itrObj instanceof ConnectionRefImpl ) {
                connection = (ConnectionRefImpl) itrObj;
            }
            else if ( itrObj instanceof SimpleEntityRef ) {
                connection = new ConnectionRefImpl( (SimpleEntityRef) itrObj );
            }
            else if ( itrObj instanceof EntityRef ) {
                    connection = new ConnectionRefImpl( new SimpleEntityRef((EntityRef) itr.next()));
            }
            else if ( itrObj instanceof UUID ) {
                    connection = new ConnectionRefImpl( new SimpleEntityRef((UUID)itr.next()));
            }

            batchUpdateEntityConnection( batch, true, connection, timestampUuid );
        }
    }


    public IndexUpdate batchStartIndexUpdate( Mutator<ByteBuffer> batch, Entity entity, String entryName,
                                              Object entryValue, UUID timestampUuid, boolean schemaHasProperty,
                                              boolean isMultiValue, boolean removeListEntry, boolean fulltextIndexed )
            throws Exception {
        return batchStartIndexUpdate( batch, entity, entryName, entryValue, timestampUuid, schemaHasProperty,
                isMultiValue, removeListEntry, fulltextIndexed, false );
    }


    public IndexUpdate batchStartIndexUpdate( Mutator<ByteBuffer> batch, Entity entity, String entryName,
                                              Object entryValue, UUID timestampUuid, boolean schemaHasProperty,
                                              boolean isMultiValue, boolean removeListEntry, boolean fulltextIndexed,
                                              boolean skipRead ) throws Exception {

        long timestamp = getTimestampInMicros( timestampUuid );

        IndexUpdate indexUpdate =
                new IndexUpdate( batch, entity, entryName, entryValue, schemaHasProperty, isMultiValue, removeListEntry,
                        timestampUuid );

        // entryName = entryName.toLowerCase();

        // entity_id,connection_type,connected_entity_id,prop_name

        if ( !skipRead ) {

            List<HColumn<ByteBuffer, ByteBuffer>> entries = null;

            if ( isMultiValue && validIndexableValue( entryValue ) ) {
                entries = cass.getColumns( cass.getApplicationKeyspace( applicationId ), ENTITY_INDEX_ENTRIES,
                        entity.getUuid(),
                        new DynamicComposite( entryName, indexValueCode( entryValue ), toIndexableValue( entryValue ) ),
                        setGreaterThanEqualityFlag( new DynamicComposite( entryName, indexValueCode( entryValue ),
                                toIndexableValue( entryValue ) ) ), INDEX_ENTRY_LIST_COUNT, false );
            }
            else {
                entries = cass.getColumns( cass.getApplicationKeyspace( applicationId ), ENTITY_INDEX_ENTRIES,
                        entity.getUuid(), new DynamicComposite( entryName ),
                        setGreaterThanEqualityFlag( new DynamicComposite( entryName ) ), INDEX_ENTRY_LIST_COUNT,
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
                DynamicComposite composite = DynamicComposite.fromByteBuffer( entry.getName().duplicate() );
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

                    indexUpdate.addPrevEntry( entryPath, prev_value, prev_timestamp, entry.getName().duplicate() );

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

            List<Map.Entry<String, Object>> list = IndexUtils.getKeyValueList( entryName, entryValue, fulltextIndexed );

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
                    indexUpdate.addNewEntry( indexEntry.getKey(), toIndexableValue( indexEntry.getValue() ) );
                }
            }

            if ( isMultiValue ) {
                addInsertToMutator( batch, ENTITY_INDEX_ENTRIES, entity.getUuid(),
                        asList( entryName, indexValueCode( entryValue ), toIndexableValue( entryValue ),
                                indexUpdate.getTimestampUuid() ), null, timestamp );
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
                            asList( entryName, code, val, indexUpdate.getTimestampUuid(), name ), null, timestamp );

                    indexUpdate.addIndex( indexEntry.getKey() );
                }
            }

            indexUpdate.addIndex( entryName );
        }

        return indexUpdate;
    }


    public void batchUpdatePropertyIndexes( Mutator<ByteBuffer> batch, String propertyName, Object propertyValue,
                                            boolean entitySchemaHasProperty, boolean noRead, UUID timestampUuid )
            throws Exception {

        Entity entity = getHeadEntity();

        UUID associatedId = null;
        String associatedType = null;

        if ( Schema.isAssociatedEntityType( entity.getType() ) ) {
            Object item = entity.getProperty( PROPERTY_ITEM );
            if ( ( item instanceof UUID ) && ( entity.getProperty( PROPERTY_COLLECTION_NAME ) instanceof String ) ) {
                associatedId = ( UUID ) item;
                associatedType = string( entity.getProperty( PROPERTY_ITEM_TYPE ) );
                String entryName = TYPE_MEMBER + "." + propertyName;
                if ( logger.isDebugEnabled() ) {
                    logger.debug( "Extended property {} ( {} ).{} indexed as {} ({})." + entryName, new Object[] {
                            entity.getType(), entity.getUuid(), propertyName, associatedType, associatedId
                    } );
                }
                propertyName = entryName;
            }
        }

        IndexUpdate indexUpdate = batchStartIndexUpdate( batch, entity, propertyName, propertyValue, timestampUuid,
                entitySchemaHasProperty, false, false,
                getDefaultSchema().isPropertyFulltextIndexed( entity.getType(), propertyName ), noRead );

        // Update collections

        String effectiveType = entity.getType();
        if ( associatedType != null ) {
            indexUpdate.setAssociatedId( associatedId );
            effectiveType = associatedType;
        }

        Map<String, Set<CollectionInfo>> containers = getDefaultSchema().getContainers( effectiveType );
        if ( containers != null ) {

            Map<EntityRef, Set<String>> containerEntities = null;
            if ( noRead ) {
                containerEntities = new LinkedHashMap<EntityRef, Set<String>>();
                EntityRef applicationRef = new SimpleEntityRef( TYPE_APPLICATION, applicationId );
                addMapSet( containerEntities, applicationRef, defaultCollectionName( entity.getType() ) );
            }
            else {
                containerEntities = getContainingCollections();
            }

            for ( EntityRef containerEntity : containerEntities.keySet() ) {
                if ( containerEntity.getType().equals( TYPE_APPLICATION ) && Schema
                        .isAssociatedEntityType( entity.getType() ) ) {
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

        if ( !noRead ) {
            batchUpdateBackwardConnectionsPropertyIndexes( indexUpdate );
        }

        /**
         * We've updated the properties, add the deletes to the ledger
         *
         */

        for ( IndexEntry entry : indexUpdate.getPrevEntries() ) {
            addDeleteToMutator( batch, ENTITY_INDEX_ENTRIES, entity.getUuid(), entry.getLedgerColumn(),
                    indexUpdate.getTimestamp() );
        }
    }


    public void batchUpdateSetIndexes( Mutator<ByteBuffer> batch, String setName, Object elementValue,
                                       boolean removeFromSet, UUID timestampUuid ) throws Exception {

        Entity entity = getHeadEntity();

        elementValue = getDefaultSchema().validateEntitySetValue( entity.getType(), setName, elementValue );

        IndexUpdate indexUpdate =
                batchStartIndexUpdate( batch, entity, setName, elementValue, timestampUuid, true, true, removeFromSet,
                        false );

        // Update collections
        Map<String, Set<CollectionInfo>> containers =
                getDefaultSchema().getContainersIndexingDictionary( entity.getType(), setName );

        if ( containers != null ) {
            Map<EntityRef, Set<String>> containerEntities = getContainingCollections();
            for ( EntityRef containerEntity : containerEntities.keySet() ) {
                if ( containerEntity.getType().equals( TYPE_APPLICATION ) && Schema
                        .isAssociatedEntityType( entity.getType() ) ) {
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


    private IndexScanner searchIndex( Object indexKey, QuerySlice slice, int pageSize ) throws Exception {

        DynamicComposite[] range = slice.getRange();

        Object keyPrefix = key( indexKey, slice.getPropertyName() );

        IndexScanner scanner =
                new IndexBucketScanner( cass, indexBucketLocator, ENTITY_INDEX, applicationId, IndexType.CONNECTION,
                        keyPrefix, range[0], range[1], slice.isReversed(), pageSize, slice.hasCursor(), slice.getPropertyName() );

        return scanner;
    }


    /**
     * Search the collection index using all the buckets for the given collection
     *
     * @param indexKey The index key to read
     * @param slice Slice set in the query
     * @param collectionName The name of the collection to search
     * @param pageSize The page size to load when iterating
     */
    private IndexScanner searchIndexBuckets( Object indexKey, QuerySlice slice, String collectionName, int pageSize )
            throws Exception {

        DynamicComposite[] range = slice.getRange();

        Object keyPrefix = key( indexKey, slice.getPropertyName() );

        IndexScanner scanner =
                new IndexBucketScanner( cass, indexBucketLocator, ENTITY_INDEX, applicationId, IndexType.COLLECTION,
                        keyPrefix, range[0], range[1], slice.isReversed(), pageSize, slice.hasCursor(), collectionName );

        return scanner;
    }


    @SuppressWarnings("unchecked")
    @Override
    public boolean isCollectionMember( String collectionName, EntityRef entity ) throws Exception {

        Keyspace ko = cass.getApplicationKeyspace( applicationId );

        ByteBuffer col = DynamicComposite
                .toByteBuffer( asList( this.headEntity.getType(), collectionName, headEntity.getUuid() ) );

        HColumn<ByteBuffer, ByteBuffer> result = cass.getColumn( ko, ENTITY_COMPOSITE_DICTIONARIES,
                key( entity.getUuid(), Schema.DICTIONARY_CONTAINER_ENTITIES ), col, Serializers.be, Serializers.be );

        return result != null;
    }


    /** @param connectionName The name of hte connection */
    public boolean isConnectionMember( String connectionName, EntityRef entity ) throws Exception {
        Keyspace ko = cass.getApplicationKeyspace( applicationId );

        Object key = key( this.headEntity.getUuid(), DICTIONARY_CONNECTED_ENTITIES, connectionName );

        DynamicComposite start = new DynamicComposite( entity.getUuid() );

        List<HColumn<ByteBuffer, ByteBuffer>> cols =
                cass.getColumns( ko, ENTITY_COMPOSITE_DICTIONARIES, key, start, null, 1, false );

        if ( cols == null || cols.size() == 0 ) {
            return false;
        }

        UUID returnedUUID = ( UUID ) DynamicComposite.fromByteBuffer( cols.get( 0 ).getName() ).get( 0 );

        return entity.getUuid().equals( returnedUUID );


        //    addDeleteToMutator(batch, ENTITY_COMPOSITE_DICTIONARIES,
        //        key(connection.getConnectedEntityId(), DICTIONARY_CONNECTING_ENTITIES,
        // connection.getConnectionType()),
        //        asList(connection.getConnectingEntityId(), connection.getConnectingEntityType()), timestamp);
        //
        //
        //    ConnectionRefImpl ref = new ConnectionRefImpl(this.headEntity, connectionName, entity);
        //
        //
        //
        //
        //
        //
        //    HColumn<String, UUID> col = cass.getColumn(ko, ENTITY_CONNECTIONS, ref.getUuid(),
        //        ConnectionRefImpl.CONNECTED_ENTITY_ID, se, ue);
        //
        //
        //    getConnectedEntities(this.headEntity, connectionName, )
        //
        //    return col != null && entity.getUuid().equals(col.getValue());
    }


    @Override
    public Map<String, Map<UUID, Set<String>>> getOwners() throws Exception {
        Map<EntityRef, Set<String>> containerEntities = getContainingCollections();
        Map<String, Map<UUID, Set<String>>> owners = new LinkedHashMap<String, Map<UUID, Set<String>>>();

        for ( EntityRef owner : containerEntities.keySet() ) {
            Set<String> collections = containerEntities.get( owner );
            for ( String collection : collections ) {
                MapUtils.addMapMapSet( owners, owner.getType(), owner.getUuid(), collection );
            }
        }

        return owners;
    }


    @Override
    public Set<String> getCollections() throws Exception {

        Map<String, CollectionInfo> collections = getDefaultSchema().getCollections( headEntity.getType() );
        if ( collections == null ) {
            return null;
        }

        return collections.keySet();
    }


    @Override
    public Results getCollection( String collectionName, UUID startResult, int count, Level resultsLevel,
                                  boolean reversed ) throws Exception {
        // changed intentionally to delegate to search so that behavior is
        // consistent across all index access.

        // TODO T.N fix cursor parsing here so startResult can be used in this
        // context. Needs a bit of refactor
        // for accommodating cursor I/O USERGRID-1750. A bit hacky, but until a
        // furthur refactor this works.

        Query query = new Query().withResultsLevel( resultsLevel ).withReversed( reversed ).withLimit( count )
                                 .withStartResult( startResult );

        return searchCollection( collectionName, query );
    }


    @Override
    public Results getCollection( String collectionName, Query query, Level resultsLevel ) throws Exception {

        // changed intentionally to delegate to search so that behavior is
        // consistent across all index access.

        return searchCollection( collectionName, query );
    }


    @Override
    public Entity addToCollection( String collectionName, EntityRef itemRef ) throws Exception {

        Entity itemEntity = em.get( itemRef );

        if ( itemEntity == null ) {
            return null;
        }

        CollectionInfo collection = getDefaultSchema().getCollection( headEntity.getType(), collectionName );
        if ( ( collection != null ) && !collection.getType().equals( itemRef.getType() ) ) {
            return null;
        }

        UUID timestampUuid = newTimeUUID();
        Mutator<ByteBuffer> batch = CountingMutator.createFlushingMutator(cass.getApplicationKeyspace( applicationId ), Serializers.be );

        batchAddToCollection( batch, collectionName, itemEntity, timestampUuid );

        if ( collection.getLinkedCollection() != null ) {
            getRelationManager( itemEntity )
                    .batchAddToCollection( batch, collection.getLinkedCollection(), getHeadEntity(), timestampUuid );
        }

        batchExecute( batch, CassandraService.RETRY_COUNT );

        return itemEntity;
    }


    @Override
    public Entity addToCollections( List<EntityRef> owners, String collectionName ) throws Exception {

        Entity itemEntity = getHeadEntity();

        Map<String, List<UUID>> collectionsByType = new LinkedHashMap<String, List<UUID>>();
        for ( EntityRef owner : owners ) {
            MapUtils.addMapList( collectionsByType, owner.getType(), owner.getUuid() );
        }

        UUID timestampUuid = newTimeUUID();
        Mutator<ByteBuffer> batch = CountingMutator.createFlushingMutator( cass.getApplicationKeyspace( applicationId ), Serializers.be );

        Schema schema = getDefaultSchema();
        for ( Entry<String, List<UUID>> entry : collectionsByType.entrySet() ) {
            CollectionInfo collection = schema.getCollection( entry.getKey(), collectionName );
            if ( ( collection != null ) && !collection.getType().equals( headEntity.getType() ) ) {
                continue;
            }
            batchAddToCollections( batch, entry.getKey(), entry.getValue(), collectionName, itemEntity, timestampUuid );

            if ( collection.getLinkedCollection() != null ) {
                logger.error(
                        "Bulk add to collections used on a linked collection, linked connection will not be updated" );
            }
        }

        batchExecute( batch, CassandraService.RETRY_COUNT );

        return null;
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
            UUID timestampUuid = newTimeUUID();
            Mutator<ByteBuffer> batch = CountingMutator.createFlushingMutator( cass.getApplicationKeyspace( applicationId ), Serializers.be );

            batchAddToCollection( batch, collectionName, itemEntity, timestampUuid );

            if ( collection.getLinkedCollection() != null ) {
                getRelationManager( itemEntity )
                        .batchAddToCollection( batch, collection.getLinkedCollection(), getHeadEntity(),
                                timestampUuid );
            }

            batchExecute( batch, CassandraService.RETRY_COUNT );
        }

        return itemEntity;
    }


    @Override
    public void removeFromCollection( String collectionName, EntityRef itemRef ) throws Exception {

        if ( headEntity.getUuid().equals( applicationId ) ) {
            if ( collectionName.equals( COLLECTION_ROLES ) ) {
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

        Entity itemEntity = em.get( itemRef );

        if ( itemEntity == null ) {
            return;
        }

        UUID timestampUuid = newTimeUUID();
        Mutator<ByteBuffer> batch = CountingMutator.createFlushingMutator( cass.getApplicationKeyspace( applicationId ), Serializers.be );

        batchRemoveFromCollection( batch, collectionName, itemEntity, timestampUuid );

        CollectionInfo collection = getDefaultSchema().getCollection( headEntity.getType(), collectionName );
        if ( ( collection != null ) && ( collection.getLinkedCollection() != null ) ) {
            getRelationManager( itemEntity )
                    .batchRemoveFromCollection( batch, collection.getLinkedCollection(), getHeadEntity(),
                            timestampUuid );
        }

        batchExecute( batch, CassandraService.RETRY_COUNT );

        if ( headEntity.getType().equals( Group.ENTITY_TYPE ) ) {
            if ( collectionName.equals( COLLECTION_ROLES ) ) {
                String path = ( String ) ( ( Entity ) itemRef ).getMetadata( "path" );
                if ( path.startsWith( "/roles/" ) ) {
                    RoleRef roleRef = SimpleRoleRef.forRoleEntity( itemEntity );
                    em.deleteRole( roleRef.getApplicationRoleName() );
                }
            }
        }
    }


    public void batchRemoveFromContainers( Mutator<ByteBuffer> m, UUID timestampUuid ) throws Exception {
        Entity entity = getHeadEntity();
        // find all the containing collections
        Map<EntityRef, Set<String>> containers = getContainingCollections();
        if ( containers != null ) {
            for ( Entry<EntityRef, Set<String>> container : containers.entrySet() ) {
                for ( String collectionName : container.getValue() ) {
                    getRelationManager( container.getKey() )
                            .batchRemoveFromCollection( m, collectionName, entity, true, timestampUuid );
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
    public Results searchCollection( String collectionName, Query query ) throws Exception {

        if ( query == null ) {
            query = new Query();
        }

        headEntity = em.validate( headEntity );

        CollectionInfo collection = getDefaultSchema().getCollection( headEntity.getType(), collectionName );

        query.setEntityType( collection.getType() );

        final CollectionResultsLoaderFactory factory = new CollectionResultsLoaderFactory();

        // we have something to search with, visit our tree and evaluate the
        // results
        QueryProcessorImpl qp = new QueryProcessorImpl( query, collection, em, factory );
        SearchCollectionVisitor visitor = new SearchCollectionVisitor( qp );

        return qp.getResults( visitor );
    }


    @Override
    public ConnectionRef createConnection( ConnectionRef connection ) throws Exception {
        ConnectionRefImpl connectionImpl = new ConnectionRefImpl( connection );

        updateEntityConnection( false, connectionImpl );

        return connection;
    }


    @Override
    public ConnectionRef createConnection( String connectionType, EntityRef connectedEntityRef ) throws Exception {

        headEntity = em.validate( headEntity );
        connectedEntityRef = em.validate( connectedEntityRef );

        ConnectionRefImpl connection = new ConnectionRefImpl( headEntity, connectionType, connectedEntityRef );

        updateEntityConnection( false, connection );

        return connection;
    }


    @Override
    public ConnectionRef createConnection( String pairedConnectionType, EntityRef pairedEntity, String connectionType,
                                           EntityRef connectedEntityRef ) throws Exception {

        ConnectionRefImpl connection =
                new ConnectionRefImpl( headEntity, new ConnectedEntityRefImpl( pairedConnectionType, pairedEntity ),
                        new ConnectedEntityRefImpl( connectionType, connectedEntityRef ) );

        updateEntityConnection( false, connection );

        return connection;
    }


    @Override
    public ConnectionRef createConnection( ConnectedEntityRef... connections ) throws Exception {

        ConnectionRefImpl connection = new ConnectionRefImpl( headEntity, connections );

        updateEntityConnection( false, connection );

        return connection;
    }


    @Override
    public ConnectionRef connectionRef( String connectionType, EntityRef connectedEntityRef ) throws Exception {

        ConnectionRef connection = new ConnectionRefImpl( headEntity, connectionType, connectedEntityRef );

        return connection;
    }


    @Override
    public ConnectionRef connectionRef( String pairedConnectionType, EntityRef pairedEntity, String connectionType,
                                        EntityRef connectedEntityRef ) throws Exception {

        ConnectionRef connection =
                new ConnectionRefImpl( headEntity, new ConnectedEntityRefImpl( pairedConnectionType, pairedEntity ),
                        new ConnectedEntityRefImpl( connectionType, connectedEntityRef ) );

        return connection;
    }


    @Override
    public ConnectionRef connectionRef( ConnectedEntityRef... connections ) {

        ConnectionRef connection = new ConnectionRefImpl( headEntity, connections );

        return connection;
    }


    @Override
    public void deleteConnection( ConnectionRef connectionRef ) throws Exception {
        updateEntityConnection( true, new ConnectionRefImpl( connectionRef ) );
    }


    @Override
    public Set<String> getConnectionTypes( UUID connectedEntityId ) throws Exception {
        // Add connection type to connections set
        //    addInsertToMutator(batch, ENTITY_DICTIONARIES,
        //        key(connection.getConnectingEntityId(), DICTIONARY_CONNECTED_TYPES),
        // connection.getConnectionType(), null,
        //        timestamp);
        //
        //    // Add connection type to connections set
        //    addInsertToMutator(batch, ENTITY_DICTIONARIES,
        //        key(connection.getConnectedEntityId(), DICTIONARY_CONNECTING_TYPES),
        // connection.getConnectionType(), null,
        //        timestamp);
        //
        //
        //    Object key = key(connectedEntityId, DICTIONARY_CONNECTED_TYPES);

        Set<String> connections = cast( em.getDictionaryAsSet( new SimpleEntityRef( connectedEntityId ),
                Schema.DICTIONARY_CONNECTED_TYPES ) );

        return connections;

        //    Set<String> connection_types = new TreeSet<String>(CASE_INSENSITIVE_ORDER);
        //
        //    //TODO T.N. get this from the dictionary
        //    List<ConnectionRefImpl> connections = getConnections(new ConnectionRefImpl(headEntity,
        // new ConnectedEntityRefImpl(
        //        NULL_ID), new ConnectedEntityRefImpl(connectedEntityId)), false);
        //
        //    for (ConnectionRefImpl connection : connections) {
        //      if ((connection.getConnectionType() != null) && (connection.getFirstPairedConnectedEntityId() ==
        // null)) {
        //        connection_types.add(connection.getConnectionType());
        //      }
        //    }
        //
        //    return connection_types;
    }


    // <<<<<<< HEAD
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
    public Results getConnectedEntities( String connectionType, String connectedEntityType, Level resultsLevel )
            throws Exception {


        return getConnectedEntities( headEntity, connectionType, connectedEntityType, resultsLevel );
    }


    /**
     * Get all edges that are from the sourceEntity
     *
     * @param sourceEntity The source entity to search edges in
     * @param connectionType The type of connection.  If not specified, all connections are returned
     * @param connectedEntityType The connected entity type, if not specified all types are returned
     * @param resultsLevel The results level to return
     */
    private Results getConnectedEntities( EntityRef sourceEntity, String connectionType, String connectedEntityType,
                                          Level resultsLevel ) throws Exception {
        Query query = new Query();
        query.setResultsLevel( resultsLevel );

        ConnectionRefImpl connectionRef =
                new ConnectionRefImpl( sourceEntity, connectionType, new SimpleEntityRef( connectedEntityType, null ) );
        //        EntityRef connectedEntity) {
        //    ConnectionRefImpl connectionRef = new ConnectionRefImpl(new ConnectedEntityRefImpl(connectionType,
        // connectedEntityType, null, true), sourceEntity );


        final ConnectionResultsLoaderFactory factory = new ConnectionResultsLoaderFactory( connectionRef );

        QueryProcessorImpl qp = new QueryProcessorImpl( query, null, em, factory );
        SearchConnectionVisitor visitor = new SearchConnectionVisitor( qp, connectionRef, true );

        return qp.getResults( visitor );
    }


    @Override
    public Results getConnectingEntities( String connectionType, String connectedEntityType,
                                          Level resultsLevel ) throws Exception {

        return getConnectingEntities(connectionType, connectedEntityType, resultsLevel, 0 );
    }


    @Override
    public Results getConnectingEntities(String connectionType,
    		String entityType, Level level, int count) throws Exception {
		return getConnectingEntities(headEntity, connectionType, entityType, level, count );
	}


	/**
     * Get all edges that are to the targetEntity
     *
     * @param targetEntity The target entity to search edges in
     * @param connectionType The type of connection.  If not specified, all connections are returned
     * @param connectedEntityType The connected entity type, if not specified all types are returned
     * @param count result limit
     */
	private Results getConnectingEntities(EntityRef targetEntity,
			String connectionType, String connectedEntityType, Level level, int count) throws Exception {
        Query query = new Query();
        query.setResultsLevel( level );
        query.setLimit(count);

        final ConnectionRefImpl connectionRef =
                new ConnectionRefImpl( new SimpleEntityRef( connectedEntityType, null ), connectionType, targetEntity );
        final ConnectionResultsLoaderFactory factory = new ConnectionResultsLoaderFactory( connectionRef );

        QueryProcessorImpl qp = new QueryProcessorImpl( query, null, em, factory );
        SearchConnectionVisitor visitor = new SearchConnectionVisitor( qp, connectionRef, false );

        return qp.getResults( visitor );
	}


	/**
     * Get all edges that are to the targetEntity
     *
     * @param targetEntity The target entity to search edges in
     * @param connectionType The type of connection.  If not specified, all connections are returned
     * @param connectedEntityType The connected entity type, if not specified all types are returned
     * @param resultsLevel The results level to return
     */
    private Results getConnectingEntities( EntityRef targetEntity, String connectionType, String connectedEntityType,
                                           Level resultsLevel ) throws Exception {
    	return getConnectingEntities(targetEntity, connectionType, connectedEntityType, resultsLevel, 0);
    }


    @Override
    public Results searchConnectedEntities( Query query ) throws Exception {

        Preconditions.checkNotNull(query, "Query must not be null");


        final String connectedEntityType = query.getEntityType();
        final String connectionType = query.getConnectionType();

        Preconditions.checkNotNull( connectedEntityType, "entityType must not be null" );
        Preconditions.checkNotNull( connectionType, "connectionType must not be null" );

        headEntity = em.validate( headEntity );

        ConnectionRefImpl connectionRef =
                new ConnectionRefImpl( headEntity, connectionType, new SimpleEntityRef( connectedEntityType, null ) );

        final ConnectionResultsLoaderFactory factory = new ConnectionResultsLoaderFactory( connectionRef );

        QueryProcessorImpl qp = new QueryProcessorImpl( query, null, em, factory );
        SearchConnectionVisitor visitor = new SearchConnectionVisitor( qp, connectionRef, true );

        return qp.getResults( visitor );
    }


    @Override
    public Set<String> getConnectionIndexes( String connectionType ) throws Exception {
        return getConnectionIndexes( new ConnectionRefImpl( headEntity, connectionType, null ) );
    }


    private static final UUIDIndexSliceParser UUID_PARSER = new UUIDIndexSliceParser();


    /**
     * Simple search visitor that performs all the joining
     *
     * @author tnine
     */
    private class SearchCollectionVisitor extends SearchVisitor {

        private final CollectionInfo collection;


        /**
         * @param queryProcessor
         */
        public SearchCollectionVisitor( QueryProcessorImpl queryProcessor ) {
            super( queryProcessor );
            this.collection = queryProcessor.getCollectionInfo();
        }


        /* (non-Javadoc)
     * @see org.apache.usergrid.persistence.query.ir.SearchVisitor#secondaryIndexScan(org.apache.usergrid.persistence.query.ir
     * .QueryNode, org.apache.usergrid.persistence.query.ir.QuerySlice)
     */
        @Override
        protected IndexScanner secondaryIndexScan( QueryNode node, QuerySlice slice ) throws Exception {
            // NOTE we explicitly do not append the slice value here. This
            // is done in the searchIndex method below
            Object indexKey = key( headEntity.getUuid(), collection.getName() );

            // update the cursor and order before we perform the slice
            // operation. Should be done after subkeying since this can
            // change the hash value of the slice
            queryProcessor.applyCursorAndSort( slice );

            IndexScanner columns = null;

            // nothing left to search for this range
            if ( slice.isComplete() ) {
                columns = new NoOpIndexScanner();
            }
            // perform the search
            else {
                columns = searchIndexBuckets( indexKey, slice, collection.getName(),
                        queryProcessor.getPageSizeHint( node ) );
            }

            return columns;
        }


        public void visit( AllNode node ) throws Exception {

            String collectionName = collection.getName();

            QuerySlice slice = node.getSlice();

            queryProcessor.applyCursorAndSort( slice );

            UUID startId = null;

            if ( slice.hasCursor() ) {
                startId = UUID_PARSER.parse( slice.getCursor() ).getUUID();
            }


            IndexScanner indexScanner = cass.getIdList( cass.getApplicationKeyspace( applicationId ),
                    key( headEntity.getUuid(), DICTIONARY_COLLECTIONS, collectionName ), startId, null,
                    queryProcessor.getPageSizeHint( node ), query.isReversed(), indexBucketLocator, applicationId,
                    collectionName, node.isForceKeepFirst() );

            this.results.push( new SliceIterator( slice, indexScanner, UUID_PARSER ) );
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
                    new CollectionGeoSearch( em, indexBucketLocator, cass, headEntity, collection.getName() ),
                    query.getLimit(), slice, node.getPropertyName(),
                    new Point( node.getLattitude(), node.getLongitude() ), node.getDistance() );

            results.push( itr );
        }


        @Override
        public void visit( NameIdentifierNode nameIdentifierNode ) throws Exception {
            EntityRef ref = em.getAlias(
                    headEntity, collection.getType(), nameIdentifierNode.getName() );

            if ( ref == null ) {
                this.results.push( new EmptyIterator() );
                return;
            }

            this.results.push( new StaticIdIterator( ref.getUuid() ) );
        }
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

            GeoIterator itr =
                    new GeoIterator( new ConnectionGeoSearch( em, indexBucketLocator, cass, connection.getIndexId() ),
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


            boolean skipFirst = node.isForceKeepFirst() ? false : slice.hasCursor();

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
                connectionTypes = new ConnectionTypesIterator( cass, applicationId, entityIdToUse, outgoing, size );
            }

            IndexScanner connectionScanner =
                    new ConnectedIndexScanner( cass, dictionaryType, applicationId, entityIdToUse, connectionTypes,
                            start, slice.isReversed(), size, skipFirst );

            this.results.push( new SliceIterator( slice, connectionScanner, connectionParser ) );
        }


        @Override
        public void visit( NameIdentifierNode nameIdentifierNode ) throws Exception {
            //TODO T.N. USERGRID-1919 actually validate this is connected
            EntityRef ref = em.getAlias( new SimpleEntityRef(Application.ENTITY_TYPE, applicationId),
                    connection.getConnectedEntityType(), nameIdentifierNode.getName() );

            if ( ref == null ) {
                this.results.push( new EmptyIterator() );
                return;
            }

            this.results.push( new StaticIdIterator( ref.getUuid() ) );
        }
    }
}
