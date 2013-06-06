/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence.cassandra;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Arrays.asList;
import static me.prettyprint.hector.api.factory.HFactory.createIndexedSlicesQuery;
import static me.prettyprint.hector.api.factory.HFactory.createMultigetSliceQuery;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.removeEnd;
import static org.usergrid.persistence.Results.fromIdList;
import static org.usergrid.persistence.Results.fromRefList;
import static org.usergrid.persistence.Results.Level.IDS;
import static org.usergrid.persistence.Results.Level.REFS;
import static org.usergrid.persistence.Schema.COLLECTION_ROLES;
import static org.usergrid.persistence.Schema.DICTIONARY_COLLECTIONS;
import static org.usergrid.persistence.Schema.DICTIONARY_CONNECTED_ENTITIES;
import static org.usergrid.persistence.Schema.DICTIONARY_CONNECTED_TYPES;
import static org.usergrid.persistence.Schema.DICTIONARY_CONNECTING_ENTITIES;
import static org.usergrid.persistence.Schema.DICTIONARY_CONNECTING_TYPES;
import static org.usergrid.persistence.Schema.INDEX_CONNECTIONS;
import static org.usergrid.persistence.Schema.PROPERTY_ASSOCIATED;
import static org.usergrid.persistence.Schema.PROPERTY_COLLECTION_NAME;
import static org.usergrid.persistence.Schema.PROPERTY_CONNECTION;
import static org.usergrid.persistence.Schema.PROPERTY_CURSOR;
import static org.usergrid.persistence.Schema.PROPERTY_INACTIVITY;
import static org.usergrid.persistence.Schema.PROPERTY_ITEM;
import static org.usergrid.persistence.Schema.PROPERTY_ITEM_TYPE;
import static org.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.usergrid.persistence.Schema.PROPERTY_TITLE;
import static org.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.usergrid.persistence.Schema.TYPE_APPLICATION;
import static org.usergrid.persistence.Schema.TYPE_CONNECTION;
import static org.usergrid.persistence.Schema.TYPE_ENTITY;
import static org.usergrid.persistence.Schema.TYPE_MEMBER;
import static org.usergrid.persistence.Schema.TYPE_ROLE;
import static org.usergrid.persistence.Schema.defaultCollectionName;
import static org.usergrid.persistence.Schema.getDefaultSchema;
import static org.usergrid.persistence.SimpleEntityRef.ref;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_COMPOSITE_DICTIONARIES;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_CONNECTIONS;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_DICTIONARIES;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_ID_SETS;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX_ENTRIES;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.addDeleteToMutator;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.addInsertToMutator;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.batchExecute;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.usergrid.persistence.cassandra.CassandraService.INDEX_ENTRY_LIST_COUNT;
import static org.usergrid.persistence.cassandra.ConnectionRefImpl.CONNECTION_ENTITY_CONNECTION_TYPE;
import static org.usergrid.persistence.cassandra.GeoIndexManager.batchDeleteLocationInConnectionsIndex;
import static org.usergrid.persistence.cassandra.GeoIndexManager.batchRemoveLocationFromCollectionIndex;
import static org.usergrid.persistence.cassandra.GeoIndexManager.batchStoreLocationInCollectionIndex;
import static org.usergrid.persistence.cassandra.GeoIndexManager.batchStoreLocationInConnectionsIndex;
import static org.usergrid.persistence.cassandra.IndexUpdate.indexValueCode;
import static org.usergrid.persistence.cassandra.IndexUpdate.toIndexableValue;
import static org.usergrid.persistence.cassandra.IndexUpdate.validIndexableValue;
import static org.usergrid.utils.ClassUtils.cast;
import static org.usergrid.utils.CompositeUtils.setEqualityFlag;
import static org.usergrid.utils.CompositeUtils.setGreaterThanEqualityFlag;
import static org.usergrid.utils.ConversionUtils.bytebuffer;
import static org.usergrid.utils.ConversionUtils.bytes;
import static org.usergrid.utils.ConversionUtils.string;
import static org.usergrid.utils.ConversionUtils.uuid;
import static org.usergrid.utils.InflectionUtils.singularize;
import static org.usergrid.utils.MapUtils.addMapSet;
import static org.usergrid.utils.UUIDUtils.getTimestampInMicros;
import static org.usergrid.utils.UUIDUtils.newTimeUUID;

import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.AbstractComposite.ComponentEquality;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.MultigetSliceQuery;
import me.prettyprint.hector.api.query.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.usergrid.persistence.AssociatedEntityRef;
import org.usergrid.persistence.CollectionRef;
import org.usergrid.persistence.ConnectedEntityRef;
import org.usergrid.persistence.ConnectionRef;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.IndexBucketLocator.IndexType;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.RelationManager;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.Results.Level;
import org.usergrid.persistence.RoleRef;
import org.usergrid.persistence.Schema;
import org.usergrid.persistence.SimpleCollectionRef;
import org.usergrid.persistence.SimpleEntityRef;
import org.usergrid.persistence.SimpleRoleRef;
import org.usergrid.persistence.cassandra.GeoIndexManager.EntityLocationRef;
import org.usergrid.persistence.cassandra.IndexUpdate.IndexEntry;
import org.usergrid.persistence.cassandra.index.IndexBucketScanner;
import org.usergrid.persistence.cassandra.index.IndexScanner;
import org.usergrid.persistence.cassandra.index.NoOpIndexScanner;
import org.usergrid.persistence.entities.Group;
import org.usergrid.persistence.query.ir.AllNode;
import org.usergrid.persistence.query.ir.QuerySlice;
import org.usergrid.persistence.query.ir.SearchVisitor;
import org.usergrid.persistence.query.ir.SliceNode;
import org.usergrid.persistence.query.ir.WithinNode;
import org.usergrid.persistence.query.ir.result.CollectionIndexSliceParser;
import org.usergrid.persistence.query.ir.result.EntityResultsLoader;
import org.usergrid.persistence.query.ir.result.GeoIterator;
import org.usergrid.persistence.query.ir.result.IntersectionIterator;
import org.usergrid.persistence.query.ir.result.SliceIterator;
import org.usergrid.persistence.query.ir.result.UUIDIndexSliceParser;
import org.usergrid.persistence.schema.CollectionInfo;
import org.usergrid.utils.IndexUtils;
import org.usergrid.utils.MapUtils;
import org.usergrid.utils.StringUtils;

import com.beoui.geocell.model.Point;
import com.yammer.metrics.annotation.Metered;

public class RelationManagerImpl implements RelationManager {

  private static final Logger logger = LoggerFactory.getLogger(RelationManagerImpl.class);

  private EntityManagerImpl em;
  private CassandraService cass;
  private UUID applicationId;
  private EntityRef headEntity;
  private IndexBucketLocator indexBucketLocator;

  public static final StringSerializer se = new StringSerializer();
  public static final ByteBufferSerializer be = new ByteBufferSerializer();
  public static final UUIDSerializer ue = new UUIDSerializer();
  public static final LongSerializer le = new LongSerializer();
  private static final UUID NULL_ID = new UUID(0, 0);
  /**
   * Max page size when scanning indexes to load at a time
   */
  public static final int PAGE_SIZE = Query.MAX_LIMIT;

  public RelationManagerImpl() {
  }

  public RelationManagerImpl init(EntityManagerImpl em, CassandraService cass, UUID applicationId,
      EntityRef headEntity, IndexBucketLocator indexBucketLocator) {
    this.em = em;
    this.applicationId = applicationId;
    this.cass = cass;
    this.headEntity = headEntity;
    this.indexBucketLocator = indexBucketLocator;

    return this;
  }

  public ApplicationContext getApplicationContext() {
    return em.getApplicationContext();
  }

  RelationManagerImpl getRelationManager(EntityRef headEntity) {
    RelationManagerImpl rmi = new RelationManagerImpl();
    rmi.init(em, cass, applicationId, headEntity, indexBucketLocator);
    return rmi;
    // return applicationContext.getAutowireCapableBeanFactory()
    // .createBean(RelationManagerImpl.class)
    // .init(em, cass, applicationId, headEntity, indexBucketLocator);
  }

  /** side effect: converts headEntity into an Entity if it is an EntityRef! */
  Entity getHeadEntity() throws Exception {
    Entity entity = null;
    if (headEntity instanceof Entity) {
      entity = (Entity) headEntity;
    } else {
      entity = em.get(headEntity);
      headEntity = entity;
    }
    return entity;
  }

  /**
   * Gets the connections.
   * 
   * @param applicationId
   *          the application id
   * @param connection
   *          the connection
   * @return list of connections
   * @throws Exception
   *           the exception
   */
  @SuppressWarnings("deprecation")
  public List<ConnectionRefImpl> getConnections(ConnectionRefImpl connection, boolean includeConnectionEntities)
      throws Exception {
    Keyspace ko = cass.getApplicationKeyspace(applicationId);

    IndexedSlicesQuery<UUID, String, ByteBuffer> q = createIndexedSlicesQuery(ko, ue, se, be);
    q.setColumnFamily(ENTITY_CONNECTIONS.toString());
    q.setColumnNames(ConnectionRefImpl.getColumnNames());
    connection.addIndexExpressionsToQuery(q);
    QueryResult<OrderedRows<UUID, String, ByteBuffer>> r = q.execute();
    OrderedRows<UUID, String, ByteBuffer> rows = r.get();
    List<ConnectionRefImpl> connections = new ArrayList<ConnectionRefImpl>();
    logger.debug("{} indexed connection row(s) retrieved", rows.getCount());
    for (Row<UUID, String, ByteBuffer> row : rows) {
      UUID entityId = row.getKey();

      logger.debug("Indexed connection {} found", entityId.toString());

      ConnectionRefImpl c = ConnectionRefImpl.loadFromColumns(row.getColumnSlice().getColumns());

      String entityType = c.getConnectedEntityType();
      if (!includeConnectionEntities && TYPE_CONNECTION.equalsIgnoreCase(entityType)) {
        logger.debug("Skipping loopback connection {}", entityId.toString());
        continue;
      }
      connections.add(c);
    }

    logger.debug("Returing {} connection(s)", connections.size());
    return connections;
  }

  @SuppressWarnings("deprecation")
  public List<ConnectionRefImpl> getConnectionsWithEntity(UUID participatingEntityId) throws Exception {
    Keyspace ko = cass.getApplicationKeyspace(applicationId);

    List<ConnectionRefImpl> connections = new ArrayList<ConnectionRefImpl>();
    List<String> idColumns = ConnectionRefImpl.getIdColumnNames();

    for (String idColumn : idColumns) {
      IndexedSlicesQuery<UUID, String, ByteBuffer> q = createIndexedSlicesQuery(ko, ue, se, be);
      q.setColumnFamily(ENTITY_CONNECTIONS.toString());
      q.setColumnNames(ConnectionRefImpl.getColumnNames());
      q.addEqualsExpression(idColumn, bytebuffer(participatingEntityId));
      QueryResult<OrderedRows<UUID, String, ByteBuffer>> r = q.execute();
      OrderedRows<UUID, String, ByteBuffer> rows = r.get();
      for (Row<UUID, String, ByteBuffer> row : rows) {
        UUID entityId = row.getKey();

        logger.debug("Indexed Connection {} found", entityId.toString());

        ConnectionRefImpl c = ConnectionRefImpl.loadFromColumns(row.getColumnSlice().getColumns());

        connections.add(c);
      }
    }

    return connections;
  }

  public List<ConnectionRefImpl> getConnections(List<UUID> connectionIds) throws Exception {

    List<ConnectionRefImpl> connections = new ArrayList<ConnectionRefImpl>();

    Map<UUID, ConnectionRefImpl> resultSet = new LinkedHashMap<UUID, ConnectionRefImpl>();

    Keyspace ko = cass.getApplicationKeyspace(applicationId);
    MultigetSliceQuery<UUID, String, ByteBuffer> q = createMultigetSliceQuery(ko, ue, se, be);
    q.setColumnFamily(ENTITY_CONNECTIONS.toString());
    q.setKeys(connectionIds);
    q.setColumnNames(ConnectionRefImpl.getColumnNamesSet().toArray(new String[0]));
    QueryResult<Rows<UUID, String, ByteBuffer>> r = q.execute();
    Rows<UUID, String, ByteBuffer> rows = r.get();

    for (Row<UUID, String, ByteBuffer> row : rows) {
      ConnectionRefImpl connection = ConnectionRefImpl.loadFromColumns(row.getColumnSlice().getColumns());

      resultSet.put(row.getKey(), connection);
    }

    for (UUID connectionId : connectionIds) {
      ConnectionRefImpl connection = resultSet.get(connectionId);
      connections.add(connection);
    }

    return connections;
  }

  /**
   * Gets the cF key for subkey.
   * 
   * @param collection
   *          the collection
   * @param properties
   *          the properties
   * @return row key
   */
  public Object getCFKeyForSubkey(CollectionInfo collection, SliceNode node) {

    // only bother if we have subkeys
    if (!collection.hasSubkeys()) {
      return null;
    }

    Set<String> fields_used = null;
    Object best_key = null;
    int most_keys_matched = 0;

    List<String[]> combos = collection.getSubkeyCombinations();

    for (String[] combo : combos) {

      int keys_matched = 0;
      List<Object> subkey_props = new ArrayList<Object>();
      Set<String> subkey_names = new LinkedHashSet<String>();

      for (String subkey_name : combo) {

        QuerySlice slice = node.getSlice(subkey_name);

        // no slice for this property, or not an equals, skip it
        if (slice == null || !slice.isEquals()) {
          continue;
        }

        Object subkey_value = null;

        if (subkey_name != null) {

          subkey_value = slice.getStart().getValue();

          if (subkey_value != null) {
            keys_matched++;
            subkey_names.add(subkey_name);
          }
        }

        subkey_props.add(subkey_value);
      }

      Object subkey_key = key(subkey_props.toArray());

      if (keys_matched > most_keys_matched) {
        best_key = subkey_key;
        fields_used = subkey_names;
      }
    }

    // Remove any fields that we used in constructing the row key, it's
    // already been joined
    // by adding it to the row key
    if (fields_used != null) {
      for (String field : fields_used) {
        node.removeSlice(field, collection);
      }
    }

    return best_key;
  }

  /**
   * Batch update collection index.
   * 
   * @param batch
   *          the batch
   * @param applicationId
   *          the application id
   * @param ownerType
   *          the owner type
   * @param ownerId
   *          the owner id
   * @param jointOwnerId
   *          the joint owner id
   * @param collectionName
   *          the collection name
   * @param entityType
   *          the entity type
   * @param entityId
   *          the entity id
   * @param entityProperties
   *          the entity properties
   * @param entryName
   *          the entry name
   * @param entryValue
   *          the entry value
   * @param isSet
   *          the is set
   * @param removeSetEntry
   *          the remove set entry
   * @param timestamp
   *          the timestamp
   * @return batch
   * @throws Exception
   *           the exception
   */
  @Metered(group = "core", name = "RelationManager_batchUpdateCollectionIndex")
  public IndexUpdate batchUpdateCollectionIndex(IndexUpdate indexUpdate, EntityRef owner, String collectionName)
      throws Exception {

    logger.debug("batchUpdateCollectionIndex");

    Entity indexedEntity = indexUpdate.getEntity();

    String bucketId = indexBucketLocator.getBucket(applicationId, IndexType.COLLECTION, indexedEntity.getUuid(),
        indexedEntity.getType(), indexUpdate.getEntryName());

    CollectionInfo collection = getDefaultSchema().getCollection(owner.getType(), collectionName);

    // the root name without the bucket
    // entity_id,collection_name,prop_name,
    Object index_name = null;
    // entity_id,collection_name,prop_name, bucketId
    Object index_key = null;

    // entity_id,collection_name,collected_entity_id,prop_name

    for (IndexEntry entry : indexUpdate.getPrevEntries()) {

      if (entry.getValue() != null) {

        index_name = key(owner.getUuid(), collectionName, entry.getPath());

        index_key = key(index_name, bucketId);

        addDeleteToMutator(indexUpdate.getBatch(), ENTITY_INDEX, index_key, entry.getIndexComposite(),
            indexUpdate.getTimestamp());

        if (collection != null) {
          if (collection.hasSubkeys()) {
            List<String[]> combos = collection.getSubkeyCombinations();
            for (String[] combo : combos) {
              List<Object> subkey_props = new ArrayList<Object>();
              for (String subkey_name : combo) {
                Object subkey_value = null;
                if (subkey_name != null) {
                  subkey_value = indexUpdate.getEntity().getProperty(subkey_name);
                }
                subkey_props.add(subkey_value);
              }
              Object subkey_key = key(subkey_props.toArray());

              // entity_id,collection_name,prop_name
              Object index_subkey_key = key(owner.getUuid(), collectionName, subkey_key, entry.getPath());

              addDeleteToMutator(indexUpdate.getBatch(), ENTITY_INDEX, index_subkey_key, entry.getIndexComposite(),
                  indexUpdate.getTimestamp());

            }
          }
        }

        if ("location.coordinates".equals(entry.getPath())) {
          EntityLocationRef loc = new EntityLocationRef(indexUpdate.getEntity(), entry.getTimestampUuid(), entry
              .getValue().toString());
          batchRemoveLocationFromCollectionIndex(indexUpdate.getBatch(), indexBucketLocator, applicationId, index_name,
              loc);
        }

      } else {
        logger.error("Unexpected condition - deserialized property value is null");
      }
    }

    if ((indexUpdate.getNewEntries().size() > 0)
        && (!indexUpdate.isMultiValue() || (indexUpdate.isMultiValue() && !indexUpdate.isRemoveListEntry()))) {

      for (IndexEntry indexEntry : indexUpdate.getNewEntries()) {

        // byte valueCode = indexEntry.getValueCode();

        index_name = key(owner.getUuid(), collectionName, indexEntry.getPath());

        index_key = key(index_name, bucketId);

        // int i = 0;

        addInsertToMutator(indexUpdate.getBatch(), ENTITY_INDEX, index_key, indexEntry.getIndexComposite(), null,
            indexUpdate.getTimestamp());

        // Add subkey indexes

        if (collection != null) {
          if (collection.hasSubkeys()) {
            List<String[]> combos = collection.getSubkeyCombinations();
            for (String[] combo : combos) {
              List<Object> subkey_props = new ArrayList<Object>();
              for (String subkey_name : combo) {
                Object subkey_value = null;
                if (subkey_name != null) {
                  subkey_value = indexUpdate.getEntity().getProperty(subkey_name);
                }
                subkey_props.add(subkey_value);
              }
              Object subkey_key = key(subkey_props.toArray());

              // entity_id,collection_name,prop_name
              Object index_subkey_key = key(owner.getUuid(), collectionName, subkey_key, indexEntry.getPath(), bucketId);

              addInsertToMutator(indexUpdate.getBatch(), ENTITY_INDEX, index_subkey_key,
                  indexEntry.getIndexComposite(), null, indexUpdate.getTimestamp());

            }
          }
        }

        if ("location.coordinates".equals(indexEntry.getPath())) {
          EntityLocationRef loc = new EntityLocationRef(indexUpdate.getEntity(), indexEntry.getTimestampUuid(),
              indexEntry.getValue().toString());
          batchStoreLocationInCollectionIndex(indexUpdate.getBatch(), indexBucketLocator, applicationId, index_name,
              indexedEntity.getUuid(), loc);
        }

        // i++;
      }

    }

    for (String index : indexUpdate.getIndexesSet()) {
      addInsertToMutator(indexUpdate.getBatch(), ENTITY_DICTIONARIES,
          key(owner.getUuid(), collectionName, Schema.DICTIONARY_INDEXES), index, null, indexUpdate.getTimestamp());
    }

    return indexUpdate;
  }

  @Override
  @Metered(group = "core", name = "RelationManager_getCollectionIndexes")
  public Set<String> getCollectionIndexes(String collectionName) throws Exception {

    // TODO TN, read all buckets here
    List<HColumn<String, String>> results = cass.getAllColumns(cass.getApplicationKeyspace(applicationId),
        ENTITY_DICTIONARIES, key(headEntity.getUuid(), collectionName, Schema.DICTIONARY_INDEXES), se, se);
    Set<String> indexes = new TreeSet<String>();
    if (results != null) {
      for (HColumn<String, String> column : results) {
        String propertyName = column.getName();
        if (!propertyName.endsWith(".keywords")) {
          indexes.add(column.getName());
        }
      }
    }
    return indexes;
  }

  public Map<EntityRef, Set<String>> getContainingCollections() throws Exception {
    Map<EntityRef, Set<String>> results = new LinkedHashMap<EntityRef, Set<String>>();

    Keyspace ko = cass.getApplicationKeyspace(applicationId);

    // TODO TN get all buckets here

    List<HColumn<DynamicComposite, ByteBuffer>> containers = cass.getAllColumns(ko, ENTITY_COMPOSITE_DICTIONARIES,
        key(headEntity.getUuid(), Schema.DICTIONARY_CONTAINER_ENTITIES), EntityManagerFactoryImpl.dce, be);
    if (containers != null) {
      for (HColumn<DynamicComposite, ByteBuffer> container : containers) {
        DynamicComposite composite = container.getName();
        if (composite != null) {
          String ownerType = (String) composite.get(0);
          String collectionName = (String) composite.get(1);
          UUID ownerId = (UUID) composite.get(2);
          addMapSet(results, new SimpleEntityRef(ownerType, ownerId), collectionName);
          if (logger.isDebugEnabled()) {
            logger.debug(" {} ( {} ) is in collection {} ( {} ).",
                new Object[] { headEntity.getType(), headEntity.getUuid(), ownerType, collectionName, ownerId });
          }
        }
      }
    }
    EntityRef applicationRef = new SimpleEntityRef(TYPE_APPLICATION, applicationId);
    if (!results.containsKey(applicationRef)) {
      addMapSet(results, applicationRef, defaultCollectionName(headEntity.getType()));
    }
    return results;

  }

  @SuppressWarnings("unchecked")
  public void batchCreateCollectionMembership(Mutator<ByteBuffer> batch, EntityRef ownerRef, String collectionName,
      EntityRef itemRef, EntityRef membershipRef, UUID timestampUuid) throws Exception {

    long timestamp = getTimestampInMicros(timestampUuid);

    if (membershipRef == null) {
      membershipRef = new SimpleCollectionRef(ownerRef, collectionName, itemRef);
    }

    Map<String, Object> properties = new TreeMap<String, Object>(CASE_INSENSITIVE_ORDER);
    properties.put(PROPERTY_TYPE, membershipRef.getType());
    properties.put(PROPERTY_COLLECTION_NAME, collectionName);
    properties.put(PROPERTY_ITEM, itemRef.getUuid());
    properties.put(PROPERTY_ITEM_TYPE, itemRef.getType());

    em.batchCreate(batch, membershipRef.getType(), null, properties, membershipRef.getUuid(), timestampUuid);

    addInsertToMutator(batch, ENTITY_COMPOSITE_DICTIONARIES,
        key(membershipRef.getUuid(), Schema.DICTIONARY_CONTAINER_ENTITIES),
        asList(ownerRef.getType(), collectionName, ownerRef.getUuid()), membershipRef.getUuid(), timestamp);

  }

  /**
   * Batch add to collection.
   * 
   * @param batch
   *          the batch
   * @param applicationId
   *          the application id
   * @param ownerType
   *          the owner type
   * @param ownerId
   *          the owner id
   * @param jointOwnerId
   *          the joint owner id
   * @param collectionName
   *          the collection name
   * @param entityType
   *          the entity type
   * @param entityId
   *          the entity id
   * @param entityProperties
   *          the entity properties
   * @param timestamp
   *          the timestamp
   * @return batch
   * @throws Exception
   *           the exception
   */
  public Mutator<ByteBuffer> batchAddToCollection(Mutator<ByteBuffer> batch, String collectionName, Entity entity,
      UUID timestampUuid) throws Exception {
    List<UUID> ids = new ArrayList<UUID>(1);
    ids.add(headEntity.getUuid());
    return batchAddToCollections(batch, headEntity.getType(), ids, collectionName, entity, timestampUuid);
  }

  @SuppressWarnings("unchecked")
  @Metered(group = "core", name = "RelationManager_batchAddToCollections")
  public Mutator<ByteBuffer> batchAddToCollections(Mutator<ByteBuffer> batch, String ownerType, List<UUID> ownerIds,
      String collectionName, Entity entity, UUID timestampUuid) throws Exception {

    long timestamp = getTimestampInMicros(timestampUuid);

    if (Schema.isAssociatedEntityType(entity.getType())) {
      logger.error("Cant add an extended type to any collection", new Throwable());
      return batch;
    }

    Map<UUID, CollectionRef> membershipRefs = new LinkedHashMap<UUID, CollectionRef>();

    for (UUID ownerId : ownerIds) {

      CollectionRef membershipRef = new SimpleCollectionRef(new SimpleEntityRef(ownerType, ownerId), collectionName,
          entity);

      membershipRefs.put(ownerId, membershipRef);

      // get the bucket this entityId needs to be inserted into
      String bucketId = indexBucketLocator.getBucket(applicationId, IndexType.COLLECTION, entity.getUuid(),
          collectionName);

      Object collections_key = key(ownerId, Schema.DICTIONARY_COLLECTIONS, collectionName, bucketId);

      // Insert in main collection

      addInsertToMutator(batch, ENTITY_ID_SETS, collections_key, entity.getUuid(), membershipRef.getUuid(), timestamp);

      addInsertToMutator(batch, ENTITY_COMPOSITE_DICTIONARIES,
          key(entity.getUuid(), Schema.DICTIONARY_CONTAINER_ENTITIES), asList(ownerType, collectionName, ownerId),
          membershipRef.getUuid(), timestamp);

    }
    // Insert in subkeyed collections
    Schema schema = getDefaultSchema();
    CollectionInfo collection = schema.getCollection(ownerType, collectionName);
    if (collection != null) {
      if (collection.hasSubkeys()) {
        List<String[]> combos = collection.getSubkeyCombinations();
        for (String[] combo : combos) {
          List<Object> subkey_props = new ArrayList<Object>();
          for (String subkey_name : combo) {
            Object subkey_value = null;
            if (subkey_name != null) {
              subkey_value = entity.getProperty(subkey_name);
            }
            subkey_props.add(subkey_value);
          }
          for (UUID ownerId : ownerIds) {
            addInsertToMutator(batch, ENTITY_ID_SETS,
                key(ownerId, Schema.DICTIONARY_COLLECTIONS, collectionName, subkey_props.toArray()), entity.getUuid(),
                membershipRefs.get(ownerId).getUuid(), timestamp);
          }

        }
      }
    }

    // Add property indexes
    for (String propertyName : entity.getProperties().keySet()) {
      boolean indexed_property = schema.isPropertyIndexed(entity.getType(), propertyName);
      if (indexed_property) {
        boolean collection_indexes_property = schema.isPropertyIndexedInCollection(ownerType, collectionName,
            propertyName);
        boolean item_schema_has_property = schema.hasProperty(entity.getType(), propertyName);
        boolean fulltext_indexed = schema.isPropertyFulltextIndexed(entity.getType(), propertyName);
        if (collection_indexes_property || !item_schema_has_property) {
          Object propertyValue = entity.getProperty(propertyName);
          IndexUpdate indexUpdate = batchStartIndexUpdate(batch, entity, propertyName, propertyValue, timestampUuid,
              item_schema_has_property, false, false, fulltext_indexed, true);
          for (UUID ownerId : ownerIds) {
            EntityRef owner = new SimpleEntityRef(ownerType, ownerId);
            batchUpdateCollectionIndex(indexUpdate, owner, collectionName);
          }
        }
      }
    }

    // Add set property indexes

    Set<String> dictionaryNames = em.getDictionaryNames(entity);

    for (String dictionaryName : dictionaryNames) {
      boolean has_dictionary = schema.hasDictionary(entity.getType(), dictionaryName);
      boolean dictionary_indexed = schema.isDictionaryIndexedInCollection(ownerType, collectionName, dictionaryName);

      if (dictionary_indexed || !has_dictionary) {
        Set<Object> elementValues = em.getDictionaryAsSet(entity, dictionaryName);
        for (Object elementValue : elementValues) {
          IndexUpdate indexUpdate = batchStartIndexUpdate(batch, entity, dictionaryName, elementValue, timestampUuid,
              has_dictionary, true, false, false, true);
          for (UUID ownerId : ownerIds) {
            EntityRef owner = new SimpleEntityRef(ownerType, ownerId);
            batchUpdateCollectionIndex(indexUpdate, owner, collectionName);
          }
        }
      }
    }

    for (UUID ownerId : ownerIds) {
      EntityRef owner = new SimpleEntityRef(ownerType, ownerId);
      batchCreateCollectionMembership(batch, owner, collectionName, entity, membershipRefs.get(ownerId), timestampUuid);
    }

    return batch;
  }

  /**
   * Batch remove from collection.
   * 
   * @param batch
   *          the batch
   * @param applicationId
   *          the application id
   * @param ownerType
   *          the owner type
   * @param ownerId
   *          the owner id
   * @param collectionName
   *          the collection name
   * @param entityType
   *          the entity type
   * @param entityId
   *          the entity id
   * @param entityProperties
   *          the entity properties
   * @param timestamp
   *          the timestamp
   * @return batch
   * @throws Exception
   *           the exception
   */
  public Mutator<ByteBuffer> batchRemoveFromCollection(Mutator<ByteBuffer> batch, String collectionName, Entity entity,
      UUID timestampUuid) throws Exception {
    return this.batchRemoveFromCollection(batch, collectionName, entity, false, timestampUuid);
  }

  @SuppressWarnings("unchecked")
  @Metered(group = "core", name = "RelationManager_batchRemoveFromCollection")
  public Mutator<ByteBuffer> batchRemoveFromCollection(Mutator<ByteBuffer> batch, String collectionName, Entity entity,
      boolean force, UUID timestampUuid) throws Exception {

    long timestamp = getTimestampInMicros(timestampUuid);

    if (!force && headEntity.getUuid().equals(applicationId)) {
      // Can't remove entities from root collections
      return batch;
    }

    Object collections_key = key(headEntity.getUuid(), Schema.DICTIONARY_COLLECTIONS, collectionName,
        indexBucketLocator.getBucket(applicationId, IndexType.COLLECTION, entity.getUuid(), collectionName));

    // Remove property indexes

    Schema schema = getDefaultSchema();
    for (String propertyName : entity.getProperties().keySet()) {
      boolean collection_indexes_property = schema.isPropertyIndexedInCollection(headEntity.getType(), collectionName,
          propertyName);
      boolean item_schema_has_property = schema.hasProperty(entity.getType(), propertyName);
      boolean fulltext_indexed = schema.isPropertyFulltextIndexed(entity.getType(), propertyName);
      if (collection_indexes_property || !item_schema_has_property) {
        IndexUpdate indexUpdate = batchStartIndexUpdate(batch, entity, propertyName, null, timestampUuid,
            item_schema_has_property, false, false, fulltext_indexed);
        batchUpdateCollectionIndex(indexUpdate, headEntity, collectionName);
      }
    }

    // Remove set indexes

    Set<String> dictionaryNames = em.getDictionaryNames(entity);

    for (String dictionaryName : dictionaryNames) {
      boolean has_dictionary = schema.hasDictionary(entity.getType(), dictionaryName);
      boolean dictionary_indexed = schema.isDictionaryIndexedInCollection(headEntity.getType(), collectionName,
          dictionaryName);

      if (dictionary_indexed || !has_dictionary) {
        Set<Object> elementValues = em.getDictionaryAsSet(entity, dictionaryName);
        for (Object elementValue : elementValues) {
          IndexUpdate indexUpdate = batchStartIndexUpdate(batch, entity, dictionaryName, elementValue, timestampUuid,
              has_dictionary, true, true, false);
          batchUpdateCollectionIndex(indexUpdate, headEntity, collectionName);
        }
      }
    }

    // Delete actual property

    addDeleteToMutator(batch, ENTITY_ID_SETS, collections_key, entity.getUuid(), timestamp);

    // Delete from subkeyed collections

    CollectionInfo collection = schema.getCollection(headEntity.getType(), collectionName);
    if (collection != null) {
      if (collection.hasSubkeys()) {
        List<String[]> combos = collection.getSubkeyCombinations();
        for (String[] combo : combos) {
          List<Object> subkey_props = new ArrayList<Object>();
          for (String subkey_name : combo) {
            Object subkey_value = null;
            if (subkey_name != null) {
              subkey_value = entity.getProperty(subkey_name);
            }
            subkey_props.add(subkey_value);
          }
          addDeleteToMutator(batch, ENTITY_ID_SETS, key(collections_key, subkey_props.toArray()), entity.getUuid(),
              timestamp);

        }
      }
    }

    addDeleteToMutator(batch, ENTITY_COMPOSITE_DICTIONARIES,
        key(entity.getUuid(), Schema.DICTIONARY_CONTAINER_ENTITIES),
        asList(headEntity.getType(), collectionName, headEntity.getUuid()), timestamp);

    if (!headEntity.getType().equalsIgnoreCase(TYPE_APPLICATION) && !Schema.isAssociatedEntityType(entity.getType())) {
      em.deleteEntity(new SimpleCollectionRef(headEntity, collectionName, entity).getUuid());
    }

    return batch;
  }

  @Metered(group = "core", name = "RelationManager_batchDeleteConnectionIndexEntries")
  public Mutator<ByteBuffer> batchDeleteConnectionIndexEntries(IndexUpdate indexUpdate, IndexEntry entry,
      ConnectionRefImpl connection, UUID[] index_keys) throws Exception {

    // entity_id,prop_name
    Object property_index_key = key(
        index_keys[ConnectionRefImpl.ALL],
        INDEX_CONNECTIONS,
        entry.getPath(),
        indexBucketLocator.getBucket(applicationId, IndexType.CONNECTION, index_keys[ConnectionRefImpl.ALL],
            entry.getPath()));

    // entity_id,entity_type,prop_name
    Object entity_type_prop_index_key = key(index_keys[ConnectionRefImpl.BY_ENTITY_TYPE], INDEX_CONNECTIONS,
        entry.getPath(), indexBucketLocator.getBucket(applicationId, IndexType.CONNECTION,
            index_keys[ConnectionRefImpl.BY_ENTITY_TYPE], entry.getPath()));

    // entity_id,connection_type,prop_name
    Object connection_type_prop_index_key = key(index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], INDEX_CONNECTIONS,
        entry.getPath(), indexBucketLocator.getBucket(applicationId, IndexType.CONNECTION,
            index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], entry.getPath()));

    // entity_id,connection_type,entity_type,prop_name
    Object connection_type_and_entity_type_prop_index_key = key(
        index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], INDEX_CONNECTIONS, entry.getPath(),
        indexBucketLocator.getBucket(applicationId, IndexType.CONNECTION,
            index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], entry.getPath()));

    // composite(property_value,connected_entity_id,connection_type,entity_type,entry_timestamp)
    addDeleteToMutator(
        indexUpdate.getBatch(),
        ENTITY_INDEX,
        property_index_key,
        entry.getIndexComposite(connection.getConnectedEntityId(), connection.getConnectionType(),
            connection.getConnectedEntityType()), indexUpdate.getTimestamp());

    // composite(property_value,connected_entity_id,connection_type,entry_timestamp)
    addDeleteToMutator(indexUpdate.getBatch(), ENTITY_INDEX, entity_type_prop_index_key,
        entry.getIndexComposite(connection.getConnectedEntityId(), connection.getConnectionType()),
        indexUpdate.getTimestamp());

    // composite(property_value,connected_entity_id,entity_type,entry_timestamp)
    addDeleteToMutator(indexUpdate.getBatch(), ENTITY_INDEX, connection_type_prop_index_key,
        entry.getIndexComposite(connection.getConnectedEntityId(), connection.getConnectedEntityType()),
        indexUpdate.getTimestamp());

    // composite(property_value,connected_entity_id,entry_timestamp)
    addDeleteToMutator(indexUpdate.getBatch(), ENTITY_INDEX, connection_type_and_entity_type_prop_index_key,
        entry.getIndexComposite(connection.getConnectedEntityId()), indexUpdate.getTimestamp());

    return indexUpdate.getBatch();
  }

  @Metered(group = "core", name = "RelationManager_batchAddConnectionIndexEntries")
  public Mutator<ByteBuffer> batchAddConnectionIndexEntries(IndexUpdate indexUpdate, IndexEntry entry,
      ConnectionRefImpl connection, UUID[] index_keys) {

    // entity_id,prop_name
    Object property_index_key = key(
        index_keys[ConnectionRefImpl.ALL],
        INDEX_CONNECTIONS,
        entry.getPath(),
        indexBucketLocator.getBucket(applicationId, IndexType.CONNECTION, index_keys[ConnectionRefImpl.ALL],
            entry.getPath()));

    // entity_id,entity_type,prop_name
    Object entity_type_prop_index_key = key(index_keys[ConnectionRefImpl.BY_ENTITY_TYPE], INDEX_CONNECTIONS,
        entry.getPath(), indexBucketLocator.getBucket(applicationId, IndexType.CONNECTION,
            index_keys[ConnectionRefImpl.BY_ENTITY_TYPE], entry.getPath()));

    // entity_id,connection_type,prop_name
    Object connection_type_prop_index_key = key(index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], INDEX_CONNECTIONS,
        entry.getPath(), indexBucketLocator.getBucket(applicationId, IndexType.CONNECTION,
            index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], entry.getPath()));

    // entity_id,connection_type,entity_type,prop_name
    Object connection_type_and_entity_type_prop_index_key = key(
        index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], INDEX_CONNECTIONS, entry.getPath(),
        indexBucketLocator.getBucket(applicationId, IndexType.CONNECTION,
            index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], entry.getPath()));

    // composite(property_value,connected_entity_id,connection_type,entity_type,entry_timestamp)
    addInsertToMutator(
        indexUpdate.getBatch(),
        ENTITY_INDEX,
        property_index_key,
        entry.getIndexComposite(connection.getConnectedEntityId(), connection.getConnectionType(),
            connection.getConnectedEntityType()), connection.getUuid(), indexUpdate.getTimestamp());

    // composite(property_value,connected_entity_id,connection_type,entry_timestamp)
    addInsertToMutator(indexUpdate.getBatch(), ENTITY_INDEX, entity_type_prop_index_key,
        entry.getIndexComposite(connection.getConnectedEntityId(), connection.getConnectionType()),
        connection.getUuid(), indexUpdate.getTimestamp());

    // composite(property_value,connected_entity_id,entity_type,entry_timestamp)
    addInsertToMutator(indexUpdate.getBatch(), ENTITY_INDEX, connection_type_prop_index_key,
        entry.getIndexComposite(connection.getConnectedEntityId(), connection.getConnectedEntityType()),
        connection.getUuid(), indexUpdate.getTimestamp());

    // composite(property_value,connected_entity_id,entry_timestamp)
    addInsertToMutator(indexUpdate.getBatch(), ENTITY_INDEX, connection_type_and_entity_type_prop_index_key,
        entry.getIndexComposite(connection.getConnectedEntityId()), connection.getUuid(), indexUpdate.getTimestamp());

    return indexUpdate.getBatch();
  }

  /**
   * Batch update connection index.
   * 
   * @param batch
   *          the batch
   * @param applicationId
   *          the application id
   * @param connection
   *          the connection
   * @param entryName
   *          the entry name
   * @param entryValue
   *          the entry value
   * @param isSet
   *          the is set
   * @param removeListEntry
   *          the remove set entry
   * @param timestamp
   *          the timestamp
   * @return batch
   * @throws Exception
   *           the exception
   */
  @Metered(group = "core", name = "RelationManager_batchUpdateConnectionIndex")
  public IndexUpdate batchUpdateConnectionIndex(IndexUpdate indexUpdate, ConnectionRefImpl connection) throws Exception {

    // UUID connection_id = connection.getId();

    UUID[] index_keys = connection.getIndexIds();

    // Delete all matching entries from entry list
    for (IndexEntry entry : indexUpdate.getPrevEntries()) {

      if (entry.getValue() != null) {

        batchDeleteConnectionIndexEntries(indexUpdate, entry, connection, index_keys);

        if ("location.coordinates".equals(entry.getPath())) {
          EntityLocationRef loc = new EntityLocationRef(indexUpdate.getEntity(), entry.getTimestampUuid(), entry
              .getValue().toString());
          batchDeleteLocationInConnectionsIndex(indexUpdate.getBatch(), indexBucketLocator, applicationId, index_keys,
              entry.getPath(), loc);
        }

      } else {
        logger.error("Unexpected condition - deserialized property value is null");
      }
    }

    if ((indexUpdate.getNewEntries().size() > 0)
        && (!indexUpdate.isMultiValue() || (indexUpdate.isMultiValue() && !indexUpdate.isRemoveListEntry()))) {

      for (IndexEntry indexEntry : indexUpdate.getNewEntries()) {

        batchAddConnectionIndexEntries(indexUpdate, indexEntry, connection, index_keys);

        if ("location.coordinates".equals(indexEntry.getPath())) {
          EntityLocationRef loc = new EntityLocationRef(indexUpdate.getEntity(), indexEntry.getTimestampUuid(),
              indexEntry.getValue().toString());
          batchStoreLocationInConnectionsIndex(indexUpdate.getBatch(), indexBucketLocator, applicationId, index_keys,
              indexEntry.getPath(), loc);
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

    for (String index : indexUpdate.getIndexesSet()) {
      addInsertToMutator(indexUpdate.getBatch(), ENTITY_DICTIONARIES,
          key(connection.getConnectingIndexId(), Schema.DICTIONARY_INDEXES), index, null, indexUpdate.getTimestamp());
    }

    return indexUpdate;
  }

  public Set<String> getConnectionIndexes(ConnectionRefImpl connection) throws Exception {
    List<HColumn<String, String>> results = cass.getAllColumns(cass.getApplicationKeyspace(applicationId),
        ENTITY_DICTIONARIES, key(connection.getConnectingIndexId(), Schema.DICTIONARY_INDEXES), se, se);
    Set<String> indexes = new TreeSet<String>();
    if (results != null) {
      for (HColumn<String, String> column : results) {
        String propertyName = column.getName();
        if (!propertyName.endsWith(".keywords")) {
          indexes.add(column.getName());
        }
      }
    }
    return indexes;
  }

  /**
   * Batch update backword connections property indexes.
   * 
   * @param batch
   *          the batch
   * @param applicationId
   *          the application id
   * @param itemType
   *          the item type
   * @param itemId
   *          the item id
   * @param propertyName
   *          the property name
   * @param propertyValue
   *          the property value
   * @param timestamp
   *          the timestamp
   * @return batch
   * @throws Exception
   *           the exception
   */
  @Metered(group = "core", name = "RelationManager_batchUpdateBackwardConnectionsPropertyIndexes")
  public IndexUpdate batchUpdateBackwardConnectionsPropertyIndexes(IndexUpdate indexUpdate) throws Exception {

    logger.debug("batchUpdateBackwordConnectionsPropertyIndexes");

    boolean entitySchemaHasProperty = indexUpdate.isSchemaHasProperty();

    if (entitySchemaHasProperty) {
      if (!getDefaultSchema().isPropertyIndexedInConnections(indexUpdate.getEntity().getType(),
          indexUpdate.getEntryName())) {
        return indexUpdate;
      }
    }

    List<ConnectionRefImpl> connections = getConnections(ConnectionRefImpl.toConnectedEntity(indexUpdate.getEntity()),
        true);

    for (ConnectionRefImpl connection : connections) {

      batchUpdateConnectionIndex(indexUpdate, connection);
    }

    return indexUpdate;
  }

  /**
   * Batch update backword connections set indexes.
   * 
   * @param batch
   *          the batch
   * @param applicationId
   *          the application id
   * @param itemType
   *          the item type
   * @param itemId
   *          the item id
   * @param setName
   *          the set name
   * @param property
   *          the property
   * @param elementValue
   *          the set value
   * @param removeFromSet
   *          the remove from set
   * @param timestamp
   *          the timestamp
   * @return batch
   * @throws Exception
   *           the exception
   */
  @Metered(group = "core", name = "RelationManager_batchUpdateBackwardConnectionsDictionaryIndexes")
  public IndexUpdate batchUpdateBackwardConnectionsDictionaryIndexes(IndexUpdate indexUpdate) throws Exception {

    logger.debug("batchUpdateBackwardConnectionsListIndexes");

    boolean entityHasDictionary = getDefaultSchema().isDictionaryIndexedInConnections(
        indexUpdate.getEntity().getType(), indexUpdate.getEntryName());
    if (!entityHasDictionary) {
      return indexUpdate;
    }

    List<ConnectionRefImpl> connections = getConnections(ConnectionRefImpl.toConnectedEntity(indexUpdate.getEntity()),
        true);

    for (ConnectionRefImpl connection : connections) {

      batchUpdateConnectionIndex(indexUpdate, connection);
    }

    return indexUpdate;
  }

  @SuppressWarnings("unchecked")
  @Metered(group = "core", name = "RelationManager_batchUpdateEntityConnection")
  public Mutator<ByteBuffer> batchUpdateEntityConnection(Mutator<ByteBuffer> batch, boolean disconnect,
      ConnectionRefImpl connection, UUID timestampUuid) throws Exception {

    long timestamp = getTimestampInMicros(timestampUuid);

    Entity connectedEntity = em.loadPartialEntity(connection.getConnectedEntityId());
    if (connectedEntity == null) {
      return batch;
    }

    // Create connection for requested params

    Object connection_id = connection.getUuid();
    Map<String, Object> columns = connection.toColumnMap();

    if (disconnect) {
      addDeleteToMutator(batch, ENTITY_CONNECTIONS, connection_id, timestamp, columns.keySet().toArray());

      addDeleteToMutator(batch, ENTITY_COMPOSITE_DICTIONARIES,
          key(connection.getConnectingEntityId(), DICTIONARY_CONNECTED_ENTITIES, connection.getConnectionType()),
          asList(connection.getConnectedEntityId(), connection.getConnectedEntityType()), timestamp);

      addDeleteToMutator(batch, ENTITY_COMPOSITE_DICTIONARIES,
          key(connection.getConnectedEntityId(), DICTIONARY_CONNECTING_ENTITIES, connection.getConnectionType()),
          asList(connection.getConnectingEntityId(), connection.getConnectingEntityType()), timestamp);

      // delete the connection path if there will be no connections left
      boolean delete = true;
      for (ConnectionRefImpl c : getConnectionsWithEntity(connection.getConnectingEntityId())) {
        if (c.getConnectedEntity().getConnectionType().equals(connection.getConnectedEntity().getConnectionType())) {
          if (!c.getConnectedEntity().getUuid().equals(connection.getConnectedEntity().getUuid())) {
            delete = false;
            break;
          }
        }
      }
      if (delete) {
        addDeleteToMutator(batch, ENTITY_DICTIONARIES,
            key(connection.getConnectingEntityId(), DICTIONARY_CONNECTED_TYPES), connection.getConnectionType(),
            timestamp);
      }

      // delete the connection path if there will be no connections left
      delete = true;
      for (ConnectionRefImpl c : getConnectionsWithEntity(connection.getConnectedEntityId())) {
        if (c.getConnectedEntity().getConnectionType().equals(connection.getConnectedEntity().getConnectionType())) {
          if (!c.getConnectingEntity().getUuid().equals(connection.getConnectingEntity().getUuid())) {
            delete = false;
            break;
          }
        }
      }
      if (delete) {
        addDeleteToMutator(batch, ENTITY_DICTIONARIES,
            key(connection.getConnectedEntityId(), DICTIONARY_CONNECTING_TYPES), connection.getConnectionType(),
            timestamp);
      }

    } else {
      addInsertToMutator(batch, ENTITY_CONNECTIONS, connection_id, columns, timestamp);

      addInsertToMutator(batch, ENTITY_COMPOSITE_DICTIONARIES,
          key(connection.getConnectingEntityId(), DICTIONARY_CONNECTED_ENTITIES, connection.getConnectionType()),
          asList(connection.getConnectedEntityId(), connection.getConnectedEntityType()), timestamp, timestamp);

      addInsertToMutator(batch, ENTITY_COMPOSITE_DICTIONARIES,
          key(connection.getConnectedEntityId(), DICTIONARY_CONNECTING_ENTITIES, connection.getConnectionType()),
          asList(connection.getConnectingEntityId(), connection.getConnectingEntityType()), timestamp, timestamp);

      // Add connection type to connections set
      addInsertToMutator(batch, ENTITY_DICTIONARIES,
          key(connection.getConnectingEntityId(), DICTIONARY_CONNECTED_TYPES), connection.getConnectionType(), null,
          timestamp);

      // Add connection type to connections set
      addInsertToMutator(batch, ENTITY_DICTIONARIES,
          key(connection.getConnectedEntityId(), DICTIONARY_CONNECTING_TYPES), connection.getConnectionType(), null,
          timestamp);
    }

    // Add property indexes

    // Iterate though all the properties of the connected entity

    Schema schema = getDefaultSchema();
    for (String propertyName : connectedEntity.getProperties().keySet()) {
      Object propertyValue = connectedEntity.getProperties().get(propertyName);

      boolean indexed = schema.isPropertyIndexed(connectedEntity.getType(), propertyName);

      boolean connection_indexes_property = schema.isPropertyIndexedInConnections(connectedEntity.getType(),
          propertyName);
      boolean item_schema_has_property = schema.hasProperty(connectedEntity.getType(), propertyName);
      boolean fulltext_indexed = schema.isPropertyFulltextIndexed(connectedEntity.getType(), propertyName);
      // For each property, if the schema says it's indexed, update its
      // index

      if (indexed && (connection_indexes_property || !item_schema_has_property)) {
        IndexUpdate indexUpdate = batchStartIndexUpdate(batch, connectedEntity, propertyName, disconnect ? null
            : propertyValue, timestampUuid, item_schema_has_property, false, false, fulltext_indexed);
        batchUpdateConnectionIndex(indexUpdate, connection);
      }
    }

    // Add indexes for the connected entity's list properties

    // Get the names of the list properties in the connected entity
    Set<String> dictionaryNames = em.getDictionaryNames(connectedEntity);

    // For each list property, get the values in the list and
    // update the index with those values

    for (String dictionaryName : dictionaryNames) {
      boolean has_dictionary = schema.hasDictionary(connectedEntity.getType(), dictionaryName);
      boolean dictionary_indexed = schema.isDictionaryIndexedInConnections(connectedEntity.getType(), dictionaryName);

      if (dictionary_indexed || !has_dictionary) {
        Set<Object> elementValues = em.getDictionaryAsSet(connectedEntity, dictionaryName);
        for (Object elementValue : elementValues) {
          IndexUpdate indexUpdate = batchStartIndexUpdate(batch, connectedEntity, dictionaryName, elementValue,
              timestampUuid, has_dictionary, true, disconnect, false);
          batchUpdateConnectionIndex(indexUpdate, connection);
        }
      }
    }

    if (disconnect) {
      cass.deleteRow(cass.getApplicationKeyspace(applicationId), ENTITY_CONNECTIONS, connection_id, timestamp);

      // TODO any more rows to delete?
    }

    return batch;
  }

  public void updateEntityConnection(boolean disconnect, ConnectionRefImpl connection) throws Exception {

    UUID timestampUuid = newTimeUUID();
    Mutator<ByteBuffer> batch = createMutator(cass.getApplicationKeyspace(applicationId), be);

    // Make or break the connection

    batchUpdateEntityConnection(batch, disconnect, connection, timestampUuid);

    // Make or break a connection from the connecting entity
    // to the connection itself

    ConnectionRefImpl loopback = connection.getConnectionToConnectionEntity();
    if (!disconnect) {
      em.insertEntity(CONNECTION_ENTITY_CONNECTION_TYPE, loopback.getConnectedEntityId());
    }

    batchUpdateEntityConnection(batch, disconnect, loopback, timestampUuid);

    batchExecute(batch, CassandraService.RETRY_COUNT);

  }

  @Metered(group = "core", name = "RelationManager_batchDisconnect")
  public void batchDisconnect(Mutator<ByteBuffer> batch, UUID timestampUuid) throws Exception {
    List<ConnectionRefImpl> connections = getConnectionsWithEntity(headEntity.getUuid());
    if (connections != null) {
      for (ConnectionRefImpl connection : connections) {
        batchUpdateEntityConnection(batch, true, connection, timestampUuid);
      }
    }
  }

  public IndexUpdate batchStartIndexUpdate(Mutator<ByteBuffer> batch, Entity entity, String entryName,
      Object entryValue, UUID timestampUuid, boolean schemaHasProperty, boolean isMultiValue, boolean removeListEntry,
      boolean fulltextIndexed) throws Exception {
    return batchStartIndexUpdate(batch, entity, entryName, entryValue, timestampUuid, schemaHasProperty, isMultiValue,
        removeListEntry, fulltextIndexed, false);
  }

  @Metered(group = "core", name = "RelationManager_batchStartIndexUpdate")
  public IndexUpdate batchStartIndexUpdate(Mutator<ByteBuffer> batch, Entity entity, String entryName,
      Object entryValue, UUID timestampUuid, boolean schemaHasProperty, boolean isMultiValue, boolean removeListEntry,
      boolean fulltextIndexed, boolean skipRead) throws Exception {

    long timestamp = getTimestampInMicros(timestampUuid);

    IndexUpdate indexUpdate = new IndexUpdate(batch, entity, entryName, entryValue, schemaHasProperty, isMultiValue,
        removeListEntry, timestampUuid);

    // entryName = entryName.toLowerCase();

    // entity_id,connection_type,connected_entity_id,prop_name

    if (!skipRead) {

      List<HColumn<ByteBuffer, ByteBuffer>> entries = null;

      if (isMultiValue && validIndexableValue(entryValue)) {
        entries = cass.getColumns(cass.getApplicationKeyspace(applicationId), ENTITY_INDEX_ENTRIES, entity.getUuid(),
            new DynamicComposite(entryName, indexValueCode(entryValue), toIndexableValue(entryValue)),
            setGreaterThanEqualityFlag(new DynamicComposite(entryName, indexValueCode(entryValue),
                toIndexableValue(entryValue))), INDEX_ENTRY_LIST_COUNT, false);
      } else {
        entries = cass.getColumns(cass.getApplicationKeyspace(applicationId), ENTITY_INDEX_ENTRIES, entity.getUuid(),
            new DynamicComposite(entryName), setGreaterThanEqualityFlag(new DynamicComposite(entryName)),
            INDEX_ENTRY_LIST_COUNT, false);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Found {} previous index entries for {} of entity {}", new Object[] { entries.size(), entryName,
            entity.getUuid() });
      }

      // Delete all matching entries from entry list
      for (HColumn<ByteBuffer, ByteBuffer> entry : entries) {
        UUID prev_timestamp = null;
        Object prev_value = null;
        String prev_obj_path = null;

        // new format:
        // composite(entryName,
        // value_code,prev_value,prev_timestamp,prev_obj_path) = null
        DynamicComposite composite = DynamicComposite.fromByteBuffer(entry.getName().duplicate());
        prev_value = composite.get(2);
        prev_timestamp = (UUID) composite.get(3);
        if (composite.size() > 4) {
          prev_obj_path = (String) composite.get(4);
        }

        if (prev_value != null) {

          String entryPath = entryName;
          if ((prev_obj_path != null) && (prev_obj_path.length() > 0)) {
            entryPath = entryName + "." + prev_obj_path;
          }

          indexUpdate.addPrevEntry(entryPath, prev_value, prev_timestamp, entry.getName().duplicate());

          // composite(property_value,connected_entity_id,entry_timestamp)
          // addDeleteToMutator(batch, ENTITY_INDEX_ENTRIES,
          // entity.getUuid(), entry.getName(), timestamp);

        } else {
          logger.error("Unexpected condition - deserialized property value is null");
        }
      }
    }

    if (!isMultiValue || (isMultiValue && !removeListEntry)) {

      List<Map.Entry<String, Object>> list = IndexUtils.getKeyValueList(entryName, entryValue, fulltextIndexed);

      if (entryName.equalsIgnoreCase("location") && (entryValue instanceof Map)) {
        @SuppressWarnings("rawtypes")
        double latitude = MapUtils.getDoubleValue((Map) entryValue, "latitude");
        @SuppressWarnings("rawtypes")
        double longitude = MapUtils.getDoubleValue((Map) entryValue, "longitude");
        list.add(new AbstractMap.SimpleEntry<String, Object>("location.coordinates", latitude + "," + longitude));
      }

      for (Map.Entry<String, Object> indexEntry : list) {

        if (validIndexableValue(indexEntry.getValue())) {
          indexUpdate.addNewEntry(indexEntry.getKey(), toIndexableValue(indexEntry.getValue()));
        }

      }

      if (isMultiValue) {
        addInsertToMutator(
            batch,
            ENTITY_INDEX_ENTRIES,
            entity.getUuid(),
            asList(entryName, indexValueCode(entryValue), toIndexableValue(entryValue), indexUpdate.getTimestampUuid()),
            null, timestamp);
      } else {
        // int i = 0;

        for (Map.Entry<String, Object> indexEntry : list) {

          String name = indexEntry.getKey();
          if (name.startsWith(entryName + ".")) {
            name = name.substring(entryName.length() + 1);
          } else if (name.startsWith(entryName)) {
            name = name.substring(entryName.length());
          }

          byte code = indexValueCode(indexEntry.getValue());
          Object val = toIndexableValue(indexEntry.getValue());
          addInsertToMutator(batch, ENTITY_INDEX_ENTRIES, entity.getUuid(),
              asList(entryName, code, val, indexUpdate.getTimestampUuid(), name), null, timestamp);

          indexUpdate.addIndex(indexEntry.getKey());
        }
      }

      indexUpdate.addIndex(entryName);

    }

    return indexUpdate;
  }

  @Metered(group = "core", name = "RelationManager_batchUpdatePropertyIndexes")
  public void batchUpdatePropertyIndexes(Mutator<ByteBuffer> batch, String propertyName, Object propertyValue,
      boolean entitySchemaHasProperty, boolean noRead, UUID timestampUuid) throws Exception {

    Entity entity = getHeadEntity();

    UUID associatedId = null;
    String associatedType = null;

    if (Schema.isAssociatedEntityType(entity.getType())) {
      Object item = entity.getProperty(PROPERTY_ITEM);
      if ((item instanceof UUID) && (entity.getProperty(PROPERTY_COLLECTION_NAME) instanceof String)) {
        associatedId = (UUID) item;
        associatedType = string(entity.getProperty(PROPERTY_ITEM_TYPE));
        String entryName = TYPE_MEMBER + "." + propertyName;
        if (logger.isDebugEnabled()) {
          logger.debug("Extended property {} ( {} ).{} indexed as {} ({})." + entryName,
              new Object[] { entity.getType(), entity.getUuid(), propertyName, associatedType, associatedId });
        }
        propertyName = entryName;
      }
    }

    IndexUpdate indexUpdate = batchStartIndexUpdate(batch, entity, propertyName, propertyValue, timestampUuid,
        entitySchemaHasProperty, false, false,
        getDefaultSchema().isPropertyFulltextIndexed(entity.getType(), propertyName), noRead);

    // Update collections

    String effectiveType = entity.getType();
    if (associatedType != null) {
      indexUpdate.setAssociatedId(associatedId);
      effectiveType = associatedType;
    }

    Map<String, Set<CollectionInfo>> containers = getDefaultSchema().getContainers(effectiveType);
    if (containers != null) {

      Map<EntityRef, Set<String>> containerEntities = null;
      if (noRead) {
        containerEntities = new LinkedHashMap<EntityRef, Set<String>>();
        EntityRef applicationRef = new SimpleEntityRef(TYPE_APPLICATION, applicationId);
        addMapSet(containerEntities, applicationRef, defaultCollectionName(entity.getType()));
      } else {
        containerEntities = getContainingCollections();
      }

      for (EntityRef containerEntity : containerEntities.keySet()) {
        if (containerEntity.getType().equals(TYPE_APPLICATION) && Schema.isAssociatedEntityType(entity.getType())) {
          logger.debug("Extended properties for {} not indexed by application", entity.getType());
          continue;
        }
        Set<String> collectionNames = containerEntities.get(containerEntity);
        Set<CollectionInfo> collections = containers.get(containerEntity.getType());

        if (collections != null) {
          for (CollectionInfo collection : collections) {
            if (collectionNames.contains(collection.getName())) {
              batchUpdateCollectionIndex(indexUpdate, containerEntity, collection.getName());
            }
          }
        }
      }
    }

    if (!noRead) {
      batchUpdateBackwardConnectionsPropertyIndexes(indexUpdate);
    }

    /**
     * We've updated the properties, add the deletes to the ledger
     * 
     */

    for (IndexEntry entry : indexUpdate.getPrevEntries()) {
      addDeleteToMutator(batch, ENTITY_INDEX_ENTRIES, entity.getUuid(), entry.getLedgerColumn(),
          indexUpdate.getTimestamp());
    }
  }

  public void batchUpdateSetIndexes(Mutator<ByteBuffer> batch, String setName, Object elementValue,
      boolean removeFromSet, UUID timestampUuid) throws Exception {

    Entity entity = getHeadEntity();

    elementValue = getDefaultSchema().validateEntitySetValue(entity.getType(), setName, elementValue);

    IndexUpdate indexUpdate = batchStartIndexUpdate(batch, entity, setName, elementValue, timestampUuid, true, true,
        removeFromSet, false);

    // Update collections
    Map<String, Set<CollectionInfo>> containers = getDefaultSchema().getContainersIndexingDictionary(entity.getType(),
        setName);

    if (containers != null) {
      Map<EntityRef, Set<String>> containerEntities = getContainingCollections();
      for (EntityRef containerEntity : containerEntities.keySet()) {
        if (containerEntity.getType().equals(TYPE_APPLICATION) && Schema.isAssociatedEntityType(entity.getType())) {
          logger.debug("Extended properties for {} not indexed by application", entity.getType());
          continue;
        }
        Set<String> collectionNames = containerEntities.get(containerEntity);
        Set<CollectionInfo> collections = containers.get(containerEntity.getType());

        if (collections != null) {

          for (CollectionInfo collection : collections) {
            if (collectionNames.contains(collection.getName())) {

              batchUpdateCollectionIndex(indexUpdate, containerEntity, collection.getName());
            }
          }
        }
      }
    }

    batchUpdateBackwardConnectionsDictionaryIndexes(indexUpdate);

  }

  /**
   * Process index results.
   * 
   * @param results
   *          the results
   * @param compositeResults
   *          the composite results
   * @param compositeOffset
   *          the composite offset
   * @param connectionType
   *          the connection type
   * @param entityType
   *          the entity type
   * @param outputList
   *          the output list
   * @param outputDetails
   *          the output details
   * @throws Exception
   *           the exception
   */
  public Results getIndexResults(IndexScanner scanner, boolean compositeResults, String connectionType,
      String entityType, Level level, int maxResults) throws Exception {

    if (scanner == null) {
      return null;
    }

    Set<UUID> idSet = new LinkedHashSet<UUID>();
    List<UUID> ids = null;
    List<EntityRef> refs = null;
    Map<UUID, Map<String, Object>> metadata = new LinkedHashMap<UUID, Map<String, Object>>();

    if (level.ordinal() > REFS.ordinal()) {
      level = REFS;
    }

    if (compositeResults && (level != IDS)) {
      refs = new ArrayList<EntityRef>();
    } else {
      ids = new ArrayList<UUID>();
    }

    int last = maxResults + 1;

    Iterator<NavigableSet<HColumn<ByteBuffer, ByteBuffer>>> pages = scanner.iterator();

    while (pages.hasNext() && (refs.size() < last || ids.size() < last)) {
      Iterator<HColumn<ByteBuffer, ByteBuffer>> cols = pages.next().iterator();

      for (int i = 0; i < last && cols.hasNext(); i++) {

        HColumn<ByteBuffer, ByteBuffer> result = cols.next();

        UUID connectedEntityId = null;
        String cType = connectionType;
        String eType = entityType;
        UUID associatedEntityId = null;

        if (compositeResults) {
          List<Object> objects = DynamicComposite.fromByteBuffer(result.getName().duplicate());
          connectedEntityId = (UUID) objects.get(2);
          if (refs != null) {
            if ((connectionType == null) || (entityType == null)) {
              if (connectionType != null) {
                eType = StringUtils.ifString(objects.get(3));
              } else if (entityType != null) {
                cType = StringUtils.ifString(objects.get(3));
              } else {
                cType = StringUtils.ifString(objects.get(3));
                eType = StringUtils.ifString(objects.get(4));
              }
            }
          }
        } else {
          connectedEntityId = uuid(result.getName());
        }

        ByteBuffer v = result.getValue();
        if ((v != null) && (v.remaining() >= 16)) {
          associatedEntityId = uuid(result.getValue());
        }

        if ((refs != null) && (eType != null)) {
          if (!idSet.contains(connectedEntityId)) {
            refs.add(new SimpleEntityRef(eType, connectedEntityId));
            idSet.add(connectedEntityId);
          } else {
            logger.error("Duplicate entity uuid (" + connectedEntityId
                + ") found in index results, discarding but index appears inconsistent...");
          }
        }

        if (ids != null) {
          if (!idSet.contains(connectedEntityId)) {
            ids.add(connectedEntityId);
            idSet.add(connectedEntityId);
          } else {
            logger.error(
                "Duplicate entity uuid ({}) found in index results, discarding but index appears inconsistent...",
                connectedEntityId);
          }
        }

        if (cType != null) {
          MapUtils.putMapMap(metadata, connectedEntityId, PROPERTY_CONNECTION, cType);
        }

        String cursor = encodeBase64URLSafeString(bytes(result.getName()));
        if (cursor != null) {
          MapUtils.putMapMap(metadata, connectedEntityId, PROPERTY_CURSOR, cursor);
        }

        if (associatedEntityId != null) {
          MapUtils.putMapMap(metadata, connectedEntityId, PROPERTY_ASSOCIATED, associatedEntityId);
        }

      }

    }

    Results r = null;

    if ((refs != null) && (refs.size() > 0)) {
      r = fromRefList(refs);
    } else if ((ids != null) && (ids.size() > 0)) {
      r = fromIdList(ids);
    } else {
      r = new Results();
    }

    if (metadata.size() > 0) {
      r.setMetadata(metadata);
    }

    return r;
  }

  /**
   * Search index.
   * 
   * @param applicationId
   *          the application id
   * @param indexKey
   *          the index key
   * @param searchName
   *          the search name
   * @param searchStartValue
   *          the search start value
   * @param searchFinishValue
   *          the search finish value
   * @param startResult
   *          the start result
   * @param count
   *          the count
   * @return the list
   * @throws Exception
   *           the exception
   */
  @Metered(group = "core", name = "RelationManager_searchIndex")
  public List<HColumn<ByteBuffer, ByteBuffer>> searchIndex(Object indexKey, String searchName, Object searchStartValue,
      Object searchFinishValue, UUID startResult, String cursor, int count, boolean reversed) throws Exception {

    searchStartValue = toIndexableValue(searchStartValue);
    searchFinishValue = toIndexableValue(searchFinishValue);

    if (NULL_ID.equals(startResult)) {
      startResult = null;
    }

    List<HColumn<ByteBuffer, ByteBuffer>> results = null;

    Object index_key = key(indexKey, searchName);
    Object start = null;
    Object finish = null;

    if ("*".equals(searchStartValue)) {
      if (isNotBlank(cursor)) {
        byte[] cursorBytes = decodeBase64(cursor);
        if (reversed) {
          finish = cursorBytes;
        } else {
          start = cursorBytes;
        }
      }

    } else if (StringUtils.isString(searchStartValue) && StringUtils.isStringOrNull(searchFinishValue)) {
      String strStartValue = searchStartValue.toString().toLowerCase().trim();

      String strFinishValue = strStartValue;
      if (searchFinishValue != null) {
        strFinishValue = searchFinishValue.toString().toLowerCase().trim();
      }

      if (strStartValue.endsWith("*")) {
        strStartValue = removeEnd(strStartValue, "*");
        finish = new DynamicComposite(indexValueCode(""), strStartValue + "\uFFFF");
        if (isBlank(strStartValue)) {
          strStartValue = "\0000";
        }
      } else {
        finish = new DynamicComposite(indexValueCode(""), strFinishValue + "\u0000");
      }

      if (startResult != null) {
        start = new DynamicComposite(indexValueCode(strStartValue), strStartValue, startResult);
      } else {
        start = new DynamicComposite(indexValueCode(strStartValue), strStartValue);
      }

      if (isNotBlank(cursor)) {
        byte[] cursorBytes = decodeBase64(cursor);
        // if (Composite.validate(0, cursorBytes, false)) {
        start = cursorBytes;
        // }
      }

    } else if (StringUtils.isString(searchFinishValue)) {

      String strFinishValue = searchFinishValue.toString().toLowerCase().trim();

      finish = new DynamicComposite(indexValueCode(strFinishValue), strFinishValue);

      if (isNotBlank(cursor)) {
        byte[] cursorBytes = decodeBase64(cursor);
        // if (Composite.validate(0, cursorBytes, false)) {
        start = cursorBytes;
        // }
      }

    } else if (searchStartValue != null) {
      if (searchFinishValue == null) {
        searchFinishValue = searchStartValue;
      }
      if (startResult != null) {
        start = new DynamicComposite(indexValueCode(searchStartValue), searchStartValue, startResult);
      } else {
        start = new DynamicComposite(indexValueCode(searchStartValue), searchStartValue);
      }
      finish = new DynamicComposite(indexValueCode(searchFinishValue), searchFinishValue);
      setEqualityFlag((DynamicComposite) finish, ComponentEquality.GREATER_THAN_EQUAL);

      if (isNotBlank(cursor)) {
        byte[] cursorBytes = decodeBase64(cursor);
        // if (Composite.validate(0, cursorBytes, false)) {
        start = cursorBytes;
        // }
      }

    }

    results = cass.getColumns(cass.getApplicationKeyspace(applicationId), ENTITY_INDEX, index_key, start, finish,
        count, reversed);

    return results;
  }

  private IndexScanner searchIndex(Object indexKey, QuerySlice slice) throws Exception {

    DynamicComposite start = getStart(slice);

    DynamicComposite finish = getFinish(slice);

    if (slice.isReversed() && (start != null) && (finish != null)) {
      DynamicComposite temp = start;
      start = finish;
      finish = temp;
    }

    Object keyPrefix = key(indexKey, slice.getPropertyName());

    IndexScanner scanner = new IndexBucketScanner(cass, indexBucketLocator, ENTITY_INDEX, applicationId,
        IndexType.CONNECTION, keyPrefix, start, finish, slice.isReversed(), Math.min(10, PAGE_SIZE),
        slice.getPropertyName());

    return scanner;

  }

  /**
   * Search the collection index using all the buckets for the given collection
   * 
   * @param indexKey
   * @param slice
   * @param count
   * @param collectionName
   * @return
   * @throws Exception
   */
  private IndexScanner searchIndexBuckets(Object indexKey, QuerySlice slice, String collectionName) throws Exception {

    DynamicComposite start = getStart(slice);

    DynamicComposite finish = getFinish(slice);

    if (slice.isReversed() && (start != null) && (finish != null)) {
      DynamicComposite temp = start;
      start = finish;
      finish = temp;
    }

    Object keyPrefix = key(indexKey, slice.getPropertyName());

    IndexScanner scanner = new IndexBucketScanner(cass, indexBucketLocator, ENTITY_INDEX, applicationId,
        IndexType.COLLECTION, keyPrefix, start, finish, slice.isReversed(), PAGE_SIZE, collectionName);

    return scanner;

  }

  private DynamicComposite getStart(QuerySlice slice) {
    DynamicComposite start = null;

    if (slice.getCursor() != null) {
      start = DynamicComposite.fromByteBuffer(slice.getCursor());
    } else if (slice.getStart() != null) {
      start = new DynamicComposite(slice.getStart().getCode(), slice.getStart().getValue());
      if (!slice.getStart().isInclusive()) {
        setEqualityFlag((DynamicComposite) start, ComponentEquality.GREATER_THAN_EQUAL);
      }
    }

    return start;
  }

  private DynamicComposite getFinish(QuerySlice slice) {
    DynamicComposite finish = null;

    if (slice.getFinish() != null) {
      finish = new DynamicComposite(slice.getFinish().getCode(), slice.getFinish().getValue());
      if (slice.getFinish().isInclusive()) {
        setEqualityFlag((DynamicComposite) finish, ComponentEquality.GREATER_THAN_EQUAL);
      }
    }

    return finish;
  }

  @Override
  @Metered(group = "core", name = "RelationManager_getOwners")
  public Map<String, Map<UUID, Set<String>>> getOwners() throws Exception {
    Map<EntityRef, Set<String>> containerEntities = getContainingCollections();
    Map<String, Map<UUID, Set<String>>> owners = new LinkedHashMap<String, Map<UUID, Set<String>>>();

    for (EntityRef owner : containerEntities.keySet()) {
      Set<String> collections = containerEntities.get(owner);
      for (String collection : collections) {
        MapUtils.addMapMapSet(owners, owner.getType(), owner.getUuid(), collection);
      }
    }

    return owners;
  }

  @Override
  @Metered(group = "core", name = "RelationManager_getCollections")
  public Set<String> getCollections() throws Exception {

    Map<String, CollectionInfo> collections = getDefaultSchema().getCollections(headEntity.getType());
    if (collections == null) {
      return null;
    }

    return collections.keySet();
  }

  @Override
  @Metered(group = "core", name = "RelationManager_getCollection_start_result")
  public Results getCollection(String collectionName, UUID startResult, int count, Results.Level resultsLevel,
      boolean reversed) throws Exception {
    // Entity_Collections
    // Key: entity_id,collection_name
    IndexScanner scanner = cass.getIdList(cass.getApplicationKeyspace(applicationId),
        key(headEntity.getUuid(), DICTIONARY_COLLECTIONS, collectionName), startResult, null, count + 1, reversed,
        indexBucketLocator, applicationId, collectionName);

    List<UUID> ids = getUUIDListFromIdIndex(scanner, count);

    Results results = null;

    if (resultsLevel == Results.Level.IDS) {
      results = Results.fromIdList(ids);
    } else {
      results = Results.fromIdList(ids, getDefaultSchema().getCollectionType(headEntity.getType(), collectionName));
    }

    return em.loadEntities(results, resultsLevel, count);
  }

  @Override
  @Metered(group = "core", name = "RelationManager_getCollectionSize")
  public long getCollectionSize(String collectionName) throws Exception {
    long result = 0;

    for (String bucketId : indexBucketLocator.getBuckets(applicationId, IndexType.COLLECTION, collectionName)) {

      result += cass.countColumns(cass.getApplicationKeyspace(applicationId), ENTITY_ID_SETS,
          key(headEntity.getUuid(), DICTIONARY_COLLECTIONS, collectionName, bucketId));
    }

    return result;
  }

  // @Override
  // public Results getCollection(String collectionName,
  // Map<String, Object> subkeyProperties, UUID startResult, int count,
  // Results.Level resultsLevel, boolean reversed) throws Exception {
  //
  // Entity e = getHeadEntity();
  //
  // CollectionInfo collection = getDefaultSchema().getCollection(
  // e.getType(), collectionName);
  //
  // Object subkey_key = getCFKeyForSubkey(collection, subkeyProperties,
  // null);
  //
  // Map<UUID, UUID> ids = null;
  //
  // if (subkey_key != null) {
  // ids = cass
  // .getIdPairList(
  // cass.getApplicationKeyspace(applicationId),
  // key(e.getUuid(), DICTIONARY_COLLECTIONS,
  // collectionName, subkey_key), startResult,
  // null, count + 1, reversed);
  // } else {
  // ids = cass.getIdPairList(
  // cass.getApplicationKeyspace(applicationId),
  // key(e.getUuid(), DICTIONARY_COLLECTIONS, collectionName),
  // startResult, null, count + 1, reversed);
  // }
  //
  // Results results = Results.fromIdList(new ArrayList<UUID>(ids.keySet()),
  // collection.getType());
  //
  // return em.loadEntities(results, resultsLevel, ids, count);
  // }

  @Override
  @Metered(group = "core", name = "RelationManager_getCollecitonForQuery")
  public Results getCollection(String collectionName, Query query, Results.Level resultsLevel) throws Exception {

    UUID startResult = query.getStartResult();
    String cursor = query.getCursor();
    if (cursor != null) {
      byte[] cursorBytes = decodeBase64(cursor);
      if ((cursorBytes != null) && (cursorBytes.length == 16)) {
        startResult = uuid(cursorBytes);
      }
    }
    int count = query.getLimit();

    Results results = getCollection(collectionName, startResult, count, resultsLevel, query.isReversed());

    if (results != null) {
      results.setQuery(query);
    }

    return results;

  }

  @Override
  @Metered(group = "core", name = "RelationManager_addToCollection")
  public Entity addToCollection(String collectionName, EntityRef itemRef) throws Exception {

    Entity itemEntity = em.get(itemRef);

    if (itemEntity == null) {
      return null;
    }

    CollectionInfo collection = getDefaultSchema().getCollection(headEntity.getType(), collectionName);
    if ((collection != null) && !collection.getType().equals(itemRef.getType())) {
      return null;
    }

    UUID timestampUuid = newTimeUUID();
    Mutator<ByteBuffer> batch = createMutator(cass.getApplicationKeyspace(applicationId), be);

    batchAddToCollection(batch, collectionName, itemEntity, timestampUuid);

    if (collection.getLinkedCollection() != null) {
      getRelationManager(itemEntity).batchAddToCollection(batch, collection.getLinkedCollection(), getHeadEntity(),
          timestampUuid);

    }

    batchExecute(batch, CassandraService.RETRY_COUNT);

    return itemEntity;
  }

  @Override
  @Metered(group = "core", name = "RelationManager_addToCollections")
  public Entity addToCollections(List<EntityRef> owners, String collectionName) throws Exception {

    Entity itemEntity = getHeadEntity();

    Map<String, List<UUID>> collectionsByType = new LinkedHashMap<String, List<UUID>>();
    for (EntityRef owner : owners) {
      MapUtils.addMapList(collectionsByType, owner.getType(), owner.getUuid());
    }

    UUID timestampUuid = newTimeUUID();
    Mutator<ByteBuffer> batch = createMutator(cass.getApplicationKeyspace(applicationId), be);

    Schema schema = getDefaultSchema();
    for (Entry<String, List<UUID>> entry : collectionsByType.entrySet()) {
      CollectionInfo collection = schema.getCollection(entry.getKey(), collectionName);
      if ((collection != null) && !collection.getType().equals(headEntity.getType())) {
        continue;
      }
      batchAddToCollections(batch, entry.getKey(), entry.getValue(), collectionName, itemEntity, timestampUuid);

      if (collection.getLinkedCollection() != null) {
        logger.error("Bulk add to collections used on a linked collection, linked connection will not be updated");
      }
    }

    batchExecute(batch, CassandraService.RETRY_COUNT);

    return null;
  }

  @Override
  @Metered(group = "core", name = "RelationManager_createItemInCollection")
  public Entity createItemInCollection(String collectionName, String itemType, Map<String, Object> properties)
      throws Exception {

    if (headEntity.getUuid().equals(applicationId)) {
      if (itemType.equals(TYPE_ENTITY)) {
        itemType = singularize(collectionName);
      }
      if (itemType.equals(TYPE_ROLE)) {
        Long inactivity = (Long) properties.get(PROPERTY_INACTIVITY);
        if (inactivity == null)
          inactivity = 0L;
        return em.createRole((String) properties.get(PROPERTY_NAME), (String) properties.get(PROPERTY_TITLE),
            inactivity);
      }
      return em.create(itemType, properties);
    } else if (headEntity.getType().equals(Group.ENTITY_TYPE) && (collectionName.equals(COLLECTION_ROLES))) {
      UUID groupId = headEntity.getUuid();
      String roleName = (String) properties.get(PROPERTY_NAME);
      return em.createGroupRole(groupId, roleName, (Long) properties.get(PROPERTY_INACTIVITY));
    }

    CollectionInfo collection = getDefaultSchema().getCollection(headEntity.getType(), collectionName);
    if ((collection != null) && !collection.getType().equals(itemType)) {
      return null;
    }

    properties = getDefaultSchema().cleanUpdatedProperties(itemType, properties, true);

    Entity itemEntity = em.create(itemType, properties);

    if (itemEntity != null) {
      UUID timestampUuid = newTimeUUID();
      Mutator<ByteBuffer> batch = createMutator(cass.getApplicationKeyspace(applicationId), be);

      batchAddToCollection(batch, collectionName, itemEntity, timestampUuid);

      if (collection.getLinkedCollection() != null) {
        getRelationManager(itemEntity).batchAddToCollection(batch, collection.getLinkedCollection(), getHeadEntity(),
            timestampUuid);

      }

      batchExecute(batch, CassandraService.RETRY_COUNT);

    }

    return itemEntity;
  }

  @Override
  @Metered(group = "core", name = "RelationManager_removeFromCollection")
  public void removeFromCollection(String collectionName, EntityRef itemRef) throws Exception {

    if (headEntity.getUuid().equals(applicationId)) {
      if (collectionName.equals(COLLECTION_ROLES)) {
        Entity itemEntity = em.get(itemRef);
        if (itemEntity != null) {
          RoleRef roleRef = SimpleRoleRef.forRoleEntity(itemEntity);
          em.deleteRole(roleRef.getApplicationRoleName());
          return;
        }
        em.delete(itemEntity);
        return;
      }
      em.delete(itemRef);
      return;
    }

    Entity itemEntity = em.get(itemRef);

    if (itemEntity == null) {
      return;
    }

    UUID timestampUuid = newTimeUUID();
    Mutator<ByteBuffer> batch = createMutator(cass.getApplicationKeyspace(applicationId), be);

    batchRemoveFromCollection(batch, collectionName, itemEntity, timestampUuid);

    CollectionInfo collection = getDefaultSchema().getCollection(headEntity.getType(), collectionName);
    if ((collection != null) && (collection.getLinkedCollection() != null)) {
      getRelationManager(itemEntity).batchRemoveFromCollection(batch, collection.getLinkedCollection(),
          getHeadEntity(), timestampUuid);
    }

    batchExecute(batch, CassandraService.RETRY_COUNT);

    if (headEntity.getType().equals(Group.ENTITY_TYPE)) {
      if (collectionName.equals(COLLECTION_ROLES)) {
        String path = (String) ((Entity) itemRef).getMetadata("path");
        if (path.startsWith("/roles/")) {
          RoleRef roleRef = SimpleRoleRef.forRoleEntity(itemEntity);
          em.deleteRole(roleRef.getApplicationRoleName());
        }
      }
    }
  }

  @Metered(group = "core", name = "RelationManager_batchRemoveFromContainers")
  public void batchRemoveFromContainers(Mutator<ByteBuffer> m, UUID timestampUuid) throws Exception {
    Entity entity = getHeadEntity();
    // find all the containing collections
    Map<EntityRef, Set<String>> containers = getContainingCollections();
    if (containers != null) {
      for (Entry<EntityRef, Set<String>> container : containers.entrySet()) {
        for (String collectionName : container.getValue()) {
          getRelationManager(container.getKey()).batchRemoveFromCollection(m, collectionName, entity, true,
              timestampUuid);
        }
      }
    }
  }

  @Override
  @Metered(group = "core", name = "RelationManager_copyRelationships")
  public void copyRelationships(String srcRelationName, EntityRef dstEntityRef, String dstRelationName)
      throws Exception {

    headEntity = em.validate(headEntity);
    dstEntityRef = em.validate(dstEntityRef);

    CollectionInfo srcCollection = getDefaultSchema().getCollection(headEntity.getType(), srcRelationName);

    CollectionInfo dstCollection = getDefaultSchema().getCollection(dstEntityRef.getType(), dstRelationName);

    Results results = null;
    do {
      if (srcCollection != null) {
        results = em.getCollection(headEntity, srcRelationName, null, 5000, Level.REFS, false);
      } else {
        results = em.getConnectedEntities(headEntity.getUuid(), srcRelationName, null, Level.REFS);
      }

      if ((results != null) && (results.size() > 0)) {
        List<EntityRef> refs = results.getRefs();
        for (EntityRef ref : refs) {
          if (dstCollection != null) {
            em.addToCollection(dstEntityRef, dstRelationName, ref);
          } else {
            em.createConnection(dstEntityRef, dstRelationName, ref);
          }
        }
      }
    } while ((results != null) && (results.hasMoreResults()));

  }

  @Override
  @Metered(group = "core", name = "RelationManager_searchCollection")
  public Results searchCollection(String collectionName, Query query) throws Exception {

    if (query == null) {
      query = new Query();
    }

    headEntity = em.validate(headEntity);

    CollectionInfo collection = getDefaultSchema().getCollection(headEntity.getType(), collectionName);

    query.setEntityType(collection.getType());

    // we have something to search with, visit our tree and evaluate the
    // results

    QueryProcessor qp = new QueryProcessor(query, collection);
    SearchCollectionVisitor visitor = new SearchCollectionVisitor(query, qp, collection);

    return qp.getResults(em, visitor, new EntityResultsLoader(em));
  }

  private List<UUID> getUUIDListFromIdIndex(IndexScanner scanner, int size) {
    SliceIterator<UUID> iter = new SliceIterator<UUID>(scanner, null, UUID_PARSER);

    List<UUID> ids = new ArrayList<UUID>(size);

    while (iter.hasNext() && ids.size() < size) {
      ids.addAll(iter.next());
    }

    return ids;
  }

  @Override
  @Metered(group = "core", name = "RelationManager_createConnection_connection_ref")
  public ConnectionRef createConnection(ConnectionRef connection) throws Exception {
    ConnectionRefImpl connectionImpl = new ConnectionRefImpl(connection);

    updateEntityConnection(false, connectionImpl);

    return connection;
  }

  @Override
  @Metered(group = "core", name = "RelationManager_createConnection_connectionType")
  public ConnectionRef createConnection(String connectionType, EntityRef connectedEntityRef) throws Exception {

    headEntity = em.validate(headEntity);
    connectedEntityRef = em.validate(connectedEntityRef);

    ConnectionRefImpl connection = new ConnectionRefImpl(headEntity, connectionType, connectedEntityRef);

    updateEntityConnection(false, connection);

    return connection;
  }

  @Override
  @Metered(group = "core", name = "RelationManager_createConnection_paired_connection_type")
  public ConnectionRef createConnection(String pairedConnectionType, EntityRef pairedEntity, String connectionType,
      EntityRef connectedEntityRef) throws Exception {

    ConnectionRefImpl connection = new ConnectionRefImpl(headEntity, new ConnectedEntityRefImpl(pairedConnectionType,
        pairedEntity), new ConnectedEntityRefImpl(connectionType, connectedEntityRef));

    updateEntityConnection(false, connection);

    return connection;
  }

  @Override
  @Metered(group = "core", name = "RelationManager_createConnection_connected_entity_ref")
  public ConnectionRef createConnection(ConnectedEntityRef... connections) throws Exception {

    ConnectionRefImpl connection = new ConnectionRefImpl(headEntity, connections);

    updateEntityConnection(false, connection);

    return connection;
  }

  @Override
  @Metered(group = "core", name = "RelationManager_connectionRef_type_entity")
  public ConnectionRef connectionRef(String connectionType, EntityRef connectedEntityRef) throws Exception {

    ConnectionRef connection = new ConnectionRefImpl(headEntity, connectionType, connectedEntityRef);

    return connection;
  }

  @Override
  @Metered(group = "core", name = "RelationManager_connectionRef_entity_to_entity")
  public ConnectionRef connectionRef(String pairedConnectionType, EntityRef pairedEntity, String connectionType,
      EntityRef connectedEntityRef) throws Exception {

    ConnectionRef connection = new ConnectionRefImpl(headEntity, new ConnectedEntityRefImpl(pairedConnectionType,
        pairedEntity), new ConnectedEntityRefImpl(connectionType, connectedEntityRef));

    return connection;

  }

  @Override
  @Metered(group = "core", name = "RelationManager_connectionRef_connections")
  public ConnectionRef connectionRef(ConnectedEntityRef... connections) {

    ConnectionRef connection = new ConnectionRefImpl(headEntity, connections);

    return connection;

  }

  @Override
  @Metered(group = "core", name = "RelationManager_deleteConnection")
  public void deleteConnection(ConnectionRef connectionRef) throws Exception {
    updateEntityConnection(true, new ConnectionRefImpl(connectionRef));
  }

  @Override
  @Metered(group = "core", name = "RelationManager_connectionExists")
  public boolean connectionExists(ConnectionRef connectionRef) throws Exception {
    List<ConnectionRefImpl> connections = getConnections(new ConnectionRefImpl(connectionRef), true);

    if (connections.size() > 0) {
      // TODO check here, are ghost tombstone rows a problem?
      return true;
    }

    return false;
  }

  @Override
  @Metered(group = "core", name = "RelationManager_getConnectionTypes_entity_id")
  public Set<String> getConnectionTypes(UUID connectedEntityId) throws Exception {
    Set<String> connection_types = new TreeSet<String>(CASE_INSENSITIVE_ORDER);

    List<ConnectionRefImpl> connections = getConnections(new ConnectionRefImpl(headEntity, new ConnectedEntityRefImpl(
        NULL_ID), new ConnectedEntityRefImpl(connectedEntityId)), false);

    for (ConnectionRefImpl connection : connections) {
      if ((connection.getConnectionType() != null) && (connection.getFirstPairedConnectedEntityId() == null)) {
        connection_types.add(connection.getConnectionType());
      }
    }

    return connection_types;
  }

  @Override
  public Set<String> getConnectionTypes() throws Exception {
    return getConnectionTypes(false);
  }

  @Override
  @Metered(group = "core", name = "RelationManager_getConnectionTypes")
  public Set<String> getConnectionTypes(boolean filterConnection) throws Exception {
    Set<String> connections = cast(em.getDictionaryAsSet(headEntity, Schema.DICTIONARY_CONNECTED_TYPES));
    if (connections == null) {
      return null;
    }
    if (filterConnection && (connections.size() > 0)) {
      connections.remove("connection");
    }
    return connections;
  }

  @Override
  @Metered(group = "core", name = "RelationManager_getConnectedEntities")
  public Results getConnectedEntities(String connectionType, String connectedEntityType, Results.Level resultsLevel)
      throws Exception {

    List<ConnectionRefImpl> connections = getConnections(new ConnectionRefImpl(headEntity, new ConnectedEntityRefImpl(
        NULL_ID), new ConnectedEntityRefImpl(connectionType, connectedEntityType, null)), false);

    Results results = Results.fromConnections(connections);
    results = em.loadEntities(results, resultsLevel, 0);
    return results;
  }

  @Override
  @Metered(group = "core", name = "RelationManager_getConnectingEntities")
  public Results getConnectingEntities(String connectionType, String connectedEntityType, Results.Level resultsLevel)
      throws Exception {

    List<ConnectionRefImpl> connections = getConnections(new ConnectionRefImpl(ref(), new ConnectedEntityRefImpl(
        NULL_ID), new ConnectedEntityRefImpl(connectionType, null, headEntity.getUuid())), false);

    Results results = Results.fromConnections(connections, false);
    results = em.loadEntities(results, resultsLevel, 0);
    return results;
  }

  @Override
  public List<ConnectedEntityRef> getConnections(Query query) throws Exception {

    return null;
  }

  // @Override
  // public Results searchConnectedEntitiesForProperty(String connectionType,
  // String connectedEntityType, String propertyName,
  // Object searchStartValue, Object searchFinishValue,
  // UUID startResult, int count, boolean reversed, Level resultsLevel)
  // throws Exception {
  //
  // Query query = new Query();
  // query.
  //
  // Results r = searchConnections(new ConnectionRefImpl(headEntity,
  // new ConnectedEntityRefImpl(connectionType, connectedEntityType,
  // null)), propertyName, searchStartValue,
  // searchFinishValue, startResult, null, count + 1, reversed,
  // resultsLevel);
  //
  // return em.loadEntities(r, resultsLevel, count);
  // }

  @Override
  @Metered(group = "core", name = "RelationManager_searchConnectedEntities")
  public Results searchConnectedEntities(Query query) throws Exception {

    if (query == null) {
      query = new Query();
    }

    String connectedEntityType = query.getEntityType();
    String connectionType = query.getConnectionType();

    headEntity = em.validate(headEntity);

    if (!query.hasQueryPredicates() && !query.hasSortPredicates()) {
      List<ConnectionRefImpl> connections = getConnections(new ConnectionRefImpl(headEntity,
          new ConnectedEntityRefImpl(NULL_ID), new ConnectedEntityRefImpl(connectionType, connectedEntityType, null)),
          false);

      Results results = Results.fromConnections(connections);
      results = em.loadEntities(results, query.getResultsLevel(), query.getLimit());

      return results;

    }

    ConnectionRefImpl connectionRef = new ConnectionRefImpl(headEntity, new ConnectedEntityRefImpl(connectionType,
        connectedEntityType, null));

    QueryProcessor qp = new QueryProcessor(query, null);
    SearchConnectionVisitor visitor = new SearchConnectionVisitor(query, qp, connectionRef);

    return qp.getResults(em, visitor, new EntityResultsLoader(em));
  }

  @Override
  @Metered(group = "core", name = "RelationManager_searchConnections")
  public List<ConnectionRef> searchConnections(Query query) throws Exception {

    if (query == null) {
      return null;
    }

    headEntity = em.validate(headEntity);

    if (!query.hasQueryPredicates()) {
      return null;
    }

    QueryProcessor qp = new QueryProcessor(query, null);

    ConnectionRefImpl connectionRef = new ConnectionRefImpl(headEntity, new ConnectedEntityRefImpl(
        ConnectionRefImpl.CONNECTION_ENTITY_CONNECTION_TYPE, ConnectionRefImpl.CONNECTION_ENTITY_TYPE, null));

    SearchConnectionVisitor visitor = new SearchConnectionVisitor(query, qp, connectionRef);

    return null;
    // TODO T.N. Query return qp.getResults(em, visitor);

  }

  @Override
  public Set<String> getConnectionIndexes(String connectionType) throws Exception {
    return getConnectionIndexes(new ConnectionRefImpl(headEntity, connectionType, null));
  }

  @Override
  public Object getAssociatedProperty(AssociatedEntityRef associatedEntityRef, String propertyName) throws Exception {

    return em.getProperty(associatedEntityRef, propertyName);
  }

  @Override
  public Map<String, Object> getAssociatedProperties(AssociatedEntityRef associatedEntityRef) throws Exception {

    return em.getProperties(associatedEntityRef);
  }

  @Override
  public void setAssociatedProperty(AssociatedEntityRef associatedEntityRef, String propertyName, Object propertyValue)
      throws Exception {

    em.setProperty(associatedEntityRef, propertyName, propertyValue);

  }

  private static final CollectionIndexSliceParser COLLECTION_PARSER = new CollectionIndexSliceParser();

  private static final UUIDIndexSliceParser UUID_PARSER = new UUIDIndexSliceParser();

  /**
   * Simple search visitor that performs all the joining
   * 
   * @author tnine
   * 
   */
  private class SearchCollectionVisitor extends SearchVisitor {

    private final CollectionInfo collection;

    /**
     * @param query
     * @param collectionName
     */
    public SearchCollectionVisitor(Query query, QueryProcessor queryProcessor, CollectionInfo collection) {
      super(query, queryProcessor);
      this.collection = collection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.usergrid.persistence.query.ir.NodeVisitor#visit(org.usergrid.
     * persistence.query.ir.SliceNode)
     */
    @Override
    public void visit(SliceNode node) throws Exception {

      // check if we have sub keys for equality clauses at this node
      // level. If so we can just use them as a row key for faster seek
      Object subKey = getCFKeyForSubkey(collection, node);

      IntersectionIterator intersection = new IntersectionIterator(PAGE_SIZE);

      for (QuerySlice slice : node.getAllSlices()) {

        // NOTE we explicitly do not append the slice value here. This
        // is done in the searchIndex method below
        Object indexKey = subKey == null ? key(headEntity.getUuid(), collection.getName()) : key(headEntity.getUuid(),
            collection.getName(), subKey);

        // update the cursor and order before we perform the slice
        // operation. Should be done after subkeying since this can
        // change the hash value of the slice
        queryProcessor.applyCursorAndSort(slice);

        IndexScanner columns = null;

        // nothing left to search for this range
        if (slice.isComplete()) {
          columns = new NoOpIndexScanner();
        }
        // perform the search
        else {
          columns = searchIndexBuckets(indexKey, slice, collection.getName());
        }

        intersection.addIterator(new SliceIterator<DynamicComposite>(columns, slice, COLLECTION_PARSER));

      }

      this.results.push(intersection);

    }

    public void visit(AllNode node) throws Exception {

      String collectionName = collection.getName();

      QuerySlice slice = node.getSlice();

      queryProcessor.applyCursorAndSort(slice);

      UUID startId = null;

      if (slice.hasCursor()) {
        startId = UUIDSerializer.get().fromByteBuffer(slice.getCursor());
      }

      IndexScanner results = cass.getIdList(cass.getApplicationKeyspace(applicationId),
          key(headEntity.getUuid(), DICTIONARY_COLLECTIONS, collectionName), startId, null, query.getLimit(),
          query.isReversed(), indexBucketLocator, applicationId, collectionName);

      this.results.push(new SliceIterator<UUID>(results, node.getSlice(), UUID_PARSER));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.usergrid.persistence.query.ir.NodeVisitor#visit(org.usergrid.
     * persistence.query.ir.WithinNode)
     */
    @Override
    public void visit(WithinNode node) throws Exception {

      List<EntityLocationRef> locations = em.getGeoIndexManager().proximitySearchCollection(headEntity,
          collection.getName(), node.getPropertyName(), new Point(node.getLattitude(), node.getLongitude()),
          node.getDistance(), null, PAGE_SIZE, false, query.getResultsLevel());

      // now wrap the results in a results iterator
      GeoIterator itr = new GeoIterator(locations, PAGE_SIZE);

      results.push(itr);
    }

  }

  /**
   * Simple search visitor that performs all the joining
   * 
   * @author tnine
   * 
   */
  private class SearchConnectionVisitor extends SearchVisitor {

    private final ConnectionRefImpl connection;

    /**
     * @param query
     * @param collectionName
     */
    public SearchConnectionVisitor(Query query, QueryProcessor queryProcessor, ConnectionRefImpl connection) {
      super(query, queryProcessor);
      this.connection = connection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.usergrid.persistence.query.ir.NodeVisitor#visit(org.usergrid.
     * persistence.query.ir.SliceNode)
     */
    @Override
    public void visit(SliceNode node) throws Exception {
      IntersectionIterator intersection = new IntersectionIterator(PAGE_SIZE);

      for (QuerySlice slice : node.getAllSlices()) {

        // update the cursor and order before we perform the slice
        // operation
        queryProcessor.applyCursorAndSort(slice);

        IndexScanner columns = searchIndex(key(connection.getIndexId(), INDEX_CONNECTIONS), slice);

        intersection.addIterator(new SliceIterator<DynamicComposite>(columns, slice, COLLECTION_PARSER));
        //
        // Results r = getIndexResults(columns, true,
        // connection.getConnectionType(), connection.getConnectedEntityType(),
        // resultsLevel, Integer.MAX_VALUE);
        //
        // if (r.size() > query.getLimit()) {
        // r.setCursorToLastResult();
        // }
        //
        // if (r.getCursor() != null) {
        // // TODO Todd finish this
        // // queryProcessor.updateCursor(slice, r.getCursor());
        // }
        //
        // r = r.excludeCursorMetadataAttribute();
        //
        // if (results != null) {
        // results.and(r);
        // } else {
        // results = r;
        // }
      }
      //
      // String prop = f.getPropertyName();
      // Object startValue = f.getStartValue();
      // Object finishValue = f.getFinishValue();
      //
      // Results r = searchConnections(
      // new ConnectionRefImpl(
      // headEntity,
      // new ConnectedEntityRefImpl(
      // ConnectionRefImpl.CONNECTION_ENTITY_CONNECTION_TYPE,
      // ConnectionRefImpl.CONNECTION_ENTITY_TYPE,
      // null)), prop, startValue, finishValue,
      // query.getStartResult(), f.getCursor(), search_count,
      // query.isReversed(), Level.IDS);
      //
      // if (results != null) {
      // results.and(r);
      // } else {
      // results = r;
      // }
      // }

      this.results.push(intersection);

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.usergrid.persistence.query.ir.NodeVisitor#visit(org.usergrid.
     * persistence.query.ir.WithinNode)
     */
    @Override
    public void visit(WithinNode node) throws Exception {

      List<EntityLocationRef> locations = em.getGeoIndexManager().proximitySearchConnections(connection.getIndexId(),
          node.getPropertyName(), new Point(node.getLattitude(), node.getLongitude()), node.getDistance(), null,
          PAGE_SIZE, false, query.getResultsLevel());

      // now wrap the results in a results iterator
      GeoIterator itr = new GeoIterator(locations, PAGE_SIZE);

      results.push(itr);

    }

    @Override
    public void visit(AllNode node) throws Exception {
      // TODO read connections and do paging
    }
  }

}
