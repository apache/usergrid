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
import static me.prettyprint.hector.api.factory.HFactory.createCounterSliceQuery;
import static me.prettyprint.hector.api.factory.HFactory.createIndexedSlicesQuery;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.commons.lang.StringUtils.capitalize;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.usergrid.locking.LockHelper.getUniqueUpdateLock;
import static org.usergrid.persistence.Results.fromEntities;
import static org.usergrid.persistence.Results.Level.REFS;
import static org.usergrid.persistence.Schema.COLLECTION_ROLES;
import static org.usergrid.persistence.Schema.COLLECTION_USERS;
import static org.usergrid.persistence.Schema.DICTIONARY_COLLECTIONS;
import static org.usergrid.persistence.Schema.DICTIONARY_PERMISSIONS;
import static org.usergrid.persistence.Schema.DICTIONARY_PROPERTIES;
import static org.usergrid.persistence.Schema.DICTIONARY_ROLENAMES;
import static org.usergrid.persistence.Schema.DICTIONARY_ROLETIMES;
import static org.usergrid.persistence.Schema.DICTIONARY_SETS;
import static org.usergrid.persistence.Schema.PROPERTY_ASSOCIATED;
import static org.usergrid.persistence.Schema.PROPERTY_CREATED;
import static org.usergrid.persistence.Schema.PROPERTY_INACTIVITY;
import static org.usergrid.persistence.Schema.PROPERTY_MODIFIED;
import static org.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.usergrid.persistence.Schema.PROPERTY_TIMESTAMP;
import static org.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.usergrid.persistence.Schema.TYPE_APPLICATION;
import static org.usergrid.persistence.Schema.TYPE_CONNECTION;
import static org.usergrid.persistence.Schema.TYPE_ENTITY;
import static org.usergrid.persistence.Schema.TYPE_MEMBER;
import static org.usergrid.persistence.Schema.TYPE_ROLE;
import static org.usergrid.persistence.Schema.defaultCollectionName;
import static org.usergrid.persistence.Schema.deserializeEntityProperties;
import static org.usergrid.persistence.Schema.getDefaultSchema;
import static org.usergrid.persistence.SimpleEntityRef.getUuid;
import static org.usergrid.persistence.SimpleEntityRef.ref;
import static org.usergrid.persistence.SimpleRoleRef.getIdForGroupIdAndRoleName;
import static org.usergrid.persistence.SimpleRoleRef.getIdForRoleName;
import static org.usergrid.persistence.cassandra.ApplicationCF.APPLICATION_AGGREGATE_COUNTERS;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_ALIASES;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_COMPOSITE_DICTIONARIES;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_COUNTERS;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_DICTIONARIES;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_ID_SETS;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_PROPERTIES;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_UNIQUE;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.addDeleteToMutator;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.addInsertToMutator;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.addPropertyToMutator;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.batchExecute;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.toStorableBinaryValue;
import static org.usergrid.persistence.cassandra.CassandraService.ALL_COUNT;
import static org.usergrid.utils.ClassUtils.cast;
import static org.usergrid.utils.ConversionUtils.bytebuffer;
import static org.usergrid.utils.ConversionUtils.bytes;
import static org.usergrid.utils.ConversionUtils.getLong;
import static org.usergrid.utils.ConversionUtils.object;
import static org.usergrid.utils.ConversionUtils.string;
import static org.usergrid.utils.ConversionUtils.uuid;
import static org.usergrid.utils.InflectionUtils.singularize;
import static org.usergrid.utils.UUIDUtils.getTimestampInMicros;
import static org.usergrid.utils.UUIDUtils.getTimestampInMillis;
import static org.usergrid.utils.UUIDUtils.isTimeBased;
import static org.usergrid.utils.UUIDUtils.newTimeUUID;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.CounterRow;
import me.prettyprint.hector.api.beans.CounterRows;
import me.prettyprint.hector.api.beans.CounterSlice;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.MultigetSliceCounterQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceCounterQuery;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.usergrid.locking.Lock;
import org.usergrid.mq.Message;
import org.usergrid.mq.QueueManager;
import org.usergrid.mq.cassandra.QueueManagerFactoryImpl;
import org.usergrid.persistence.AggregateCounter;
import org.usergrid.persistence.AggregateCounterSet;
import org.usergrid.persistence.AssociatedEntityRef;
import org.usergrid.persistence.CollectionRef;
import org.usergrid.persistence.ConnectedEntityRef;
import org.usergrid.persistence.ConnectionRef;
import org.usergrid.persistence.CounterResolution;
import org.usergrid.persistence.DynamicEntity;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityFactory;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Identifier;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.IndexBucketLocator.IndexType;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Query.CounterFilterPredicate;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.Results.Level;
import org.usergrid.persistence.RoleRef;
import org.usergrid.persistence.Schema;
import org.usergrid.persistence.SimpleCollectionRef;
import org.usergrid.persistence.SimpleEntityRef;
import org.usergrid.persistence.SimpleRoleRef;
import org.usergrid.persistence.TypedEntity;
import org.usergrid.persistence.cassandra.CounterUtils.AggregateCounterSelection;
import org.usergrid.persistence.cassandra.util.TraceParticipant;
import org.usergrid.persistence.entities.Application;
import org.usergrid.persistence.entities.Event;
import org.usergrid.persistence.entities.Group;
import org.usergrid.persistence.entities.Role;
import org.usergrid.persistence.entities.User;
import org.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.usergrid.persistence.exceptions.EntityNotFoundException;
import org.usergrid.persistence.exceptions.RequiredPropertyNotFoundException;
import org.usergrid.persistence.exceptions.UnexpectedEntityTypeException;
import org.usergrid.persistence.schema.CollectionInfo;
import org.usergrid.utils.ClassUtils;
import org.usergrid.utils.CompositeUtils;
import org.usergrid.utils.UUIDUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.yammer.metrics.annotation.Metered;

import javax.annotation.Resource;

/**
 * Cassandra-specific implementation of Datastore
 *
 * @author edanuff
 *
 */
public class EntityManagerImpl implements EntityManager {

	/** The log4j logger. */
	private static final Logger logger = LoggerFactory
			.getLogger(EntityManagerImpl.class);
    public static final String APPLICATION_COLLECTION = "application.collection.";
    public static final String APPLICATION_ENTITIES = "application.entities";
    public static final long ONE_COUNT = 1L;
    @Resource
    private EntityManagerFactoryImpl emf;
    @Resource
	private QueueManagerFactoryImpl qmf;
    @Resource
	private IndexBucketLocator indexBucketLocator;

	private UUID applicationId;

    private Application application;
    @Resource
	private CassandraService cass;
    @Resource
	private CounterUtils counterUtils;

    private boolean skipAggregateCounters;

	public static final StringSerializer se = new StringSerializer();
	public static final ByteBufferSerializer be = new ByteBufferSerializer();
	public static final UUIDSerializer ue = new UUIDSerializer();
	public static final LongSerializer le = new LongSerializer();

	public EntityManagerImpl() {
	}

	public EntityManagerImpl init(EntityManagerFactoryImpl emf,
			CassandraService cass, CounterUtils counterUtils, UUID applicationId,
            boolean skipAggregateCounters) {
	    this.emf = emf;
		this.cass = cass;
		this.counterUtils = counterUtils;
		this.applicationId = applicationId;
		this.skipAggregateCounters = skipAggregateCounters;
		qmf = (QueueManagerFactoryImpl) getApplicationContext().getBean("queueManagerFactory");
		indexBucketLocator = (IndexBucketLocator) getApplicationContext().getBean("indexBucketLocator");
        // prime the application entity for the EM
        try {
            getApplication();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
		return this;
	}

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public ApplicationContext getApplicationContext() {
        return emf.applicationContext;
    }

	@Override
	public EntityRef getApplicationRef() {
		return ref(TYPE_APPLICATION, applicationId);
	}

	@Override
	public Application getApplication() throws Exception {
        if ( application == null) {
            application = get(applicationId, Application.class);
        }
		return application;
	}

	@Override
	public void updateApplication(Application app) throws Exception {
		update(app);
        this.application = app;
	}

	@Override
	public void updateApplication(Map<String, Object> properties)
			throws Exception {
		this.updateProperties(applicationId, properties);
        this.application = get(applicationId, Application.class);
	}

	@Override
	public RelationManagerImpl getRelationManager(EntityRef entityRef) {
        //RelationManagerImpl rmi = applicationContext.getBean(RelationManagerImpl.class);
		RelationManagerImpl rmi = new RelationManagerImpl();
        rmi.init(this,cass,applicationId,entityRef,indexBucketLocator);
        return rmi;
	}

	/**
	 * Batch dictionary property.
	 *
	 * @param batch
	 *            the batch
	 * @param applicationId
	 *            the application id
	 * @param entityType
	 *            the entity type
	 * @param entityId
	 *            the entity id
	 * @param properties
	 *            the properties
	 * @param propertyName
	 *            the property name
	 * @param propertyValue
	 *            the property value
	 * @param timestamp
	 *            the timestamp
	 * @return batch
	 * @throws Exception
	 *             the exception
	 */
	public Mutator<ByteBuffer> batchSetProperty(Mutator<ByteBuffer> batch,
			EntityRef entity, String propertyName, Object propertyValue,
			UUID timestampUuid) throws Exception {
		return this.batchSetProperty(batch, entity, propertyName,
				propertyValue, false, false, timestampUuid);
	}

	public Mutator<ByteBuffer> batchSetProperty(Mutator<ByteBuffer> batch,
			EntityRef entity, String propertyName, Object propertyValue,
			boolean force, boolean noRead, UUID timestampUuid) throws Exception {

		long timestamp = getTimestampInMicros(timestampUuid);

		// propertyName = propertyName.toLowerCase();

		boolean entitySchemaHasProperty = getDefaultSchema().hasProperty(
				entity.getType(), propertyName);

		propertyValue = getDefaultSchema().validateEntityPropertyValue(
				entity.getType(), propertyName, propertyValue);

		Schema defaultSchema = Schema.getDefaultSchema();

		if (PROPERTY_TYPE.equalsIgnoreCase(propertyName)
				&& (propertyValue != null)) {
			if ("entity".equalsIgnoreCase(propertyValue.toString())
					|| "dynamicentity".equalsIgnoreCase(propertyValue
							.toString())) {
				String errorMsg = "Unable to dictionary entity type to "
						+ propertyValue + " because that is not a valid type.";
				logger.error(errorMsg);
				throw new IllegalArgumentException(errorMsg);
			}
		}

		if (entitySchemaHasProperty) {

			if (!force) {
				if (!defaultSchema.isPropertyMutable(entity.getType(),
						propertyName)) {
					return batch;
				}

				// Passing null for propertyValue indicates delete the property
				// so if required property, exit
				if ((propertyValue == null)
						&& defaultSchema.isRequiredProperty(
								entity.getType(), propertyName)) {
					return batch;
				}
			}

			if (!isPropertyValueUniqueForEntity(entity.getUuid(),
					entity.getType(), propertyName, propertyValue)) {
				throw new DuplicateUniquePropertyExistsException(
						entity.getType(), propertyName, propertyValue);
			}

			if (propertyName.equals(defaultSchema.aliasProperty(
					entity.getType()))) {
			  Lock lock = getUniqueUpdateLock(cass.getLockManager(), applicationId, propertyValue, entity.getType(), propertyName);
			  
			  try{
  				lock.lock();
  				deleteAliasesForEntity(batch, entity.getUuid(), timestamp-1);
  				createAlias(batch, applicationId, entity, entity.getType(),
  						string(propertyValue), timestamp);
			  }finally{
			    lock.unlock();
			  }
			}

			/**
			 * Unique property, load the old value and remove it, check if it's not a duplicate
			 */
			if(defaultSchema.getEntityInfo(entity.getType()).isPropertyUnique(propertyName)){
			  
			    Lock lock = getUniqueUpdateLock(cass.getLockManager(),applicationId, propertyValue,
                        entity.getType(), propertyName);

			    try {
            lock.lock();
            
            String collectionName = Schema.defaultCollectionName(entity.getType());

            uniquePropertyDelete(batch, collectionName, entity.getType(), propertyName, propertyValue, entity.getUuid(), timestamp-1);
            uniquePropertyWrite(batch, collectionName, propertyName, propertyValue, entity.getUuid(), timestamp);
          } finally {
            lock.unlock();
          }

			    
			}
		}

		if (getDefaultSchema()
				.isPropertyIndexed(entity.getType(), propertyName)) {
			getRelationManager(entity).batchUpdatePropertyIndexes(batch,
					propertyName, propertyValue, entitySchemaHasProperty,
					noRead, timestampUuid);
		}



		if (propertyValue != null) {
			// Set the new value
			addPropertyToMutator(batch, key(entity.getUuid()),
					entity.getType(), propertyName, propertyValue, timestamp);

			if (!entitySchemaHasProperty) {
				// Make a list of all the properties ever dictionary on this
				// entity
				addInsertToMutator(batch, ENTITY_DICTIONARIES,
						key(entity.getUuid(), DICTIONARY_PROPERTIES),
						propertyName, null, timestamp);
			}
		} else {
			addDeleteToMutator(batch, ENTITY_PROPERTIES, key(entity.getUuid()),
					propertyName, timestamp);
		}

		return batch;
	}

	/**
	 * Batch update properties.
	 *
	 * @param batch
	 *            the batch
	 * @param applicationId
	 *            the application id
	 * @param entityType
	 *            the entity type
	 * @param entityId
	 *            the entity id
	 * @param entityProperties
	 *            the entity properties
	 * @param properties
	 *            the properties
	 * @param timestamp
	 *            the timestamp
	 * @return batch
	 * @throws Exception
	 *             the exception
	 */
	public Mutator<ByteBuffer> batchUpdateProperties(Mutator<ByteBuffer> batch,
			EntityRef entity, Map<String, Object> properties, UUID timestampUuid)
			throws Exception {

		for (String propertyName : properties.keySet()) {
			Object propertyValue = properties.get(propertyName);

			batch = batchSetProperty(batch, entity, propertyName,
					propertyValue, timestampUuid);
		}

		return batch;
	}

	/**
	 * Batch update properties.
	 *
	 * @param batch
	 *            the batch
	 * @param applicationId
	 *            the application id
	 * @param entityId
	 *            the entity id
	 * @param properties
	 *            the properties
	 * @param timestamp
	 *            the timestamp
	 * @return batch
	 * @throws Exception
	 *             the exception
	 */
	public Mutator<ByteBuffer> batchUpdateProperties(Mutator<ByteBuffer> batch,
			UUID entityId, Map<String, Object> properties, UUID timestampUuid)
			throws Exception {

    EntityRef entity = getRef(entityId);
		if (entity == null) {
			return batch;
		}

		for (Map.Entry<String,Object> entry : properties.entrySet()) {
      String propertyName = entry.getKey();
			Object propertyValue = entry.getValue();

			batch = batchSetProperty(batch, entity, propertyName,
					propertyValue, timestampUuid);
		}

		return batch;
	}

	/**
	 * Batch update set.
	 *
	 * @param batch
	 *            the batch
	 * @param applicationId
	 *            the application id
	 * @param entityType
	 *            the entity type
	 * @param entityId
	 *            the entity id
	 * @param properties
	 *            the properties
	 * @param dictionaryName
	 *            the dictionary name
	 * @param property
	 *            the property
	 * @param elementValue
	 *            the dictionary value
	 * @param removeFromSet
	 *            the remove from set
	 * @param timestamp
	 *            the timestamp
	 * @return batch
	 * @throws Exception
	 *             the exception
	 */
	public Mutator<ByteBuffer> batchUpdateDictionary(Mutator<ByteBuffer> batch,
			EntityRef entity, String dictionaryName, Object elementValue,
			boolean removeFromDictionary, UUID timestampUuid) throws Exception {
		return batchUpdateDictionary(batch, entity, dictionaryName,
				elementValue, null, removeFromDictionary, timestampUuid);
	}

	public Mutator<ByteBuffer> batchUpdateDictionary(Mutator<ByteBuffer> batch,
			EntityRef entity, String dictionaryName, Object elementValue,
			Object elementCoValue, boolean removeFromDictionary,
			UUID timestampUuid) throws Exception {

		long timestamp = getTimestampInMicros(timestampUuid);

		// dictionaryName = dictionaryName.toLowerCase();
		if (elementCoValue == null) {
			elementCoValue = ByteBuffer.allocate(0);
		}

		boolean entityHasDictionary = getDefaultSchema().hasDictionary(
				entity.getType(), dictionaryName);

		// Don't index dynamic dictionaries not defined by the schema
		if (entityHasDictionary) {
			getRelationManager(entity).batchUpdateSetIndexes(batch,
					dictionaryName, elementValue, removeFromDictionary,
					timestampUuid);
		}

		ApplicationCF dictionary_cf = entityHasDictionary ? ENTITY_DICTIONARIES
				: ENTITY_COMPOSITE_DICTIONARIES;

		if (elementValue != null) {
			if (!removeFromDictionary) {
				// Set the new value

				elementCoValue = toStorableBinaryValue(elementCoValue,
						!entityHasDictionary);

				addInsertToMutator(batch, dictionary_cf,
						key(entity.getUuid(), dictionaryName),
						entityHasDictionary ? elementValue
								: asList(elementValue), elementCoValue,
						timestamp);

				if (!entityHasDictionary) {
					addInsertToMutator(batch, ENTITY_DICTIONARIES,
							key(entity.getUuid(), DICTIONARY_SETS),
							dictionaryName, null, timestamp);
				}
			} else {
				addDeleteToMutator(batch, dictionary_cf,
						key(entity.getUuid(), dictionaryName),
						entityHasDictionary ? elementValue
								: asList(elementValue), timestamp);
			}
		}

		return batch;
	}

	/**
	 * Returns true if the property is unique, and the entity can be saved.  If it's not unique, false is returned
	 * @param thisEntity
	 * @param entityType
	 * @param propertyName
	 * @param propertyValue
	 * @return
	 * @throws Exception
	 */
	@Metered(group="core", name="EntityManager_isPropertyValueUniqueForEntity")
	public boolean isPropertyValueUniqueForEntity(UUID thisEntity,
			String entityType, String propertyName, Object propertyValue)
			throws Exception {

		if (!getDefaultSchema().isPropertyUnique(entityType, propertyName)) {
			return true;
		}

		if (propertyValue == null) {
			return true;
		}

		String collectionName = defaultCollectionName(entityType);


    Lock lock = getUniqueUpdateLock(cass.getLockManager(), applicationId, propertyValue, entityType, propertyName);
    List<HColumn<ByteBuffer, ByteBuffer>> cols = null;
    
    try {
      lock.lock();

      Object key = createUniqueIndexKey(collectionName, propertyName, propertyValue);

      cols = cass.getColumns(cass.getApplicationKeyspace(applicationId),
          ENTITY_UNIQUE, key, null, null, 2, false);

    } finally {

      lock.unlock();
    }

		//No columns at all, it's unique
		if(cols.size() == 0){
		    return true;
		}

		//shouldn't happen, but it's an error case
		if(cols.size() > 1){
		    logger.error("INDEX CORRUPTION: More than 1 unique value exists for entities in application {} of type {} on property {} with value {}", new Object[]{applicationId, entityType, propertyName, propertyValue});
		}

		/**
		 * Doing this in a loop sucks, but we need to account for possibly having more than 1 entry in the index due to corruption.  We need to allow them to update, otherwise
		 * both entities will be unable to update and must be deleted
		 */
		for(HColumn<ByteBuffer, ByteBuffer> col: cols){
		    UUID id = ue.fromByteBuffer(col.getName());

		    if(thisEntity.equals(id)){
		        return true;
		    }


		}

		return false;
	}

	/**
	 * Add this unique index to the delete
	 * @param m
	 * @param collectionName
	 * @param propertyName
	 * @param propertyValue
	 * @param entityId
	 * @param timestamp
	 * @throws Exception
	 */
	private void uniquePropertyDelete(Mutator<ByteBuffer> m, String collectionName, String entityType, String propertyName, Object propertyValue, UUID entityId, long timestamp) throws Exception{
	    //read the old value and delete it

	    Object oldValue = getProperty(new SimpleEntityRef(entityType, entityId), propertyName);

	    //we have an old value.  If the new value is empty, we want to delete the old value.  If the new value is different we want to delete, otherwise we don't issue the delete
	    if(oldValue != null && (propertyValue == null || !oldValue.equals(propertyValue))){
	        Object key = createUniqueIndexKey(collectionName, propertyName, oldValue);

	        addDeleteToMutator(m, ENTITY_UNIQUE, key, timestamp, entityId);
	    }
	}

	/**
     * Add this unique index to the delete
     * @param m
     * @param collectionName
     * @param propertyName
     * @param propertyValue
     * @param entityId
     * @param timestamp
     * @throws Exception
     */
    private void uniquePropertyWrite(Mutator<ByteBuffer> m, String collectionName, String propertyName, Object propertyValue, UUID entityId, long timestamp) throws Exception{
        Object key = createUniqueIndexKey(collectionName, propertyName, propertyValue);

        addInsertToMutator(m, ENTITY_UNIQUE, key, entityId, null, timestamp);
    }


	/**
	 * Create a row key for the entity of the given type with the name and value in the property.  Used for fast unique index lookups
	 * @param collectionName
	 * @param propertyName
	 * @param value
	 * @return
	 */
	private Object createUniqueIndexKey(String collectionName, String propertyName, Object value){
	    return key(applicationId, collectionName, propertyName, value);
	}


  @Metered(group="core",name="EntityManager_createAlias")
	public UUID createAlias(Mutator<ByteBuffer> mutator, UUID ownerId, EntityRef ref, String aliasType,
			String alias, long timestamp) throws Exception {

		UUID entityId = ref.getUuid();
		String entityType = ref.getType();
		if (entityType == null) {
			entityType = getEntityType(entityId);
		}
		if (entityType == null) {
			return null;
		}
		if (aliasType == null) {
			aliasType = entityType;
		}
		if (ownerId == null) {
			ownerId = applicationId;
		}
		if (alias == null) {
			return null;
		}
		alias = alias.toLowerCase().trim();
		UUID keyId = CassandraPersistenceUtils.aliasID(ownerId, aliasType,
				alias);


		addInsertToMutator(mutator, ENTITY_ALIASES, keyId, "entityId", entityId,
				timestamp);
		addInsertToMutator(mutator, ENTITY_ALIASES, keyId, "entityType", entityType,
				timestamp);
		addInsertToMutator(mutator, ENTITY_ALIASES, keyId, "aliasType", aliasType,
				timestamp);
		addInsertToMutator(mutator, ENTITY_ALIASES, keyId, "alias", alias, timestamp);


		return keyId;
	}

	@Override
  @Metered(group="core", name="EntityManager_deleteAlias")
	public void deleteAlias(UUID ownerId, String aliasType, String alias)
			throws Exception {
		if (ownerId == null) {
			ownerId = applicationId;
		}
		if (aliasType == null) {
			return;
		}
		if (alias == null) {
			return;
		}
		alias = alias.toLowerCase().trim();
		UUID keyId = CassandraPersistenceUtils.aliasID(ownerId, aliasType,
				alias);
		cass.deleteRow(cass.getApplicationKeyspace(applicationId),
				ENTITY_ALIASES, keyId);

	}

	@Override
  @Metered(group="core", name="EntityManager_getAlias_single")
	public EntityRef getAlias(UUID ownerId, String aliasType, String alias)
			throws Exception {
		if (ownerId == null) {
			ownerId = applicationId;
		}
		if (aliasType == null) {
			return null;
		}
		if (alias == null) {
			return null;
		}
		alias = alias.toLowerCase().trim();
		UUID keyId = CassandraPersistenceUtils.aliasID(ownerId, aliasType,
				alias);
		Set<String> columnNames = new LinkedHashSet<String>();
		columnNames.add("entityId");
		columnNames.add("entityType");
		List<HColumn<String, ByteBuffer>> columns = cass.getColumns(
				cass.getApplicationKeyspace(applicationId), ENTITY_ALIASES,
				keyId, columnNames, se, be);

		if (columns != null && columns.size() > 0) {
			Map<String, ByteBuffer> cols = CassandraPersistenceUtils
					.getColumnMap(columns);
			String entityType = string(cols.get("entityType"));
			UUID entityId = uuid(cols.get("entityId"), null);
			if ((entityId != null) && (entityType != null)) {
				return ref(entityType, entityId);
			}
		}

		return null;
	}

	@Override
	public Map<String, EntityRef> getAlias(String aliasType,
			List<String> aliases) throws Exception {
		return getAlias(applicationId, aliasType, aliases);
	}

	@Override
  @Metered(group="core", name="EntityManager_getAlias_multi")
	public Map<String, EntityRef> getAlias(UUID ownerId, String aliasType,
			List<String> aliases) throws Exception {
		if (ownerId == null) {
			ownerId = applicationId;
		}
		if (aliasType == null) {
			return null;
		}
		if (aliases == null) {
			return null;
		}
		List<UUID> keyIds = new ArrayList<UUID>();
		for (String alias : aliases) {
			if (alias != null) {
				alias = alias.toLowerCase().trim();
				UUID keyId = CassandraPersistenceUtils.aliasID(ownerId,
						aliasType, alias);
				keyIds.add(keyId);
			}
		}
		if (keyIds.size() == 0) {
			return null;
		}

		Set<String> columnNames = new LinkedHashSet<String>();
		columnNames.add("entityId");
		columnNames.add("entityType");
		columnNames.add("alias");
		Rows<UUID, String, ByteBuffer> rows = cass.getRows(
				cass.getApplicationKeyspace(applicationId), ENTITY_ALIASES,
				keyIds, columnNames, ue, se, be);

		Map<String, EntityRef> aliasedEntities = new HashMap<String, EntityRef>();
		for (Row<UUID, String, ByteBuffer> row : rows) {
			ColumnSlice<String, ByteBuffer> slice = row.getColumnSlice();
			if (slice == null) {
				continue;
			}
			List<HColumn<String, ByteBuffer>> columns = slice.getColumns();
			if (columns != null) {
				Map<String, ByteBuffer> cols = CassandraPersistenceUtils
						.getColumnMap(columns);
				String entityType = string(cols.get("entityType"));
				UUID entityId = uuid(cols.get("entityId"), null);
				String alias = string(cols.get("alias"));
				if ((entityId != null) && (entityType != null)
						&& (alias != null)) {
					aliasedEntities.put(alias, ref(entityType, entityId));
				}
			}
		}

		return aliasedEntities;
	}

  @Metered(group="core", name="EntityManager_getAliases")
	public List<UUID> getAliases(UUID entityId) {
		Keyspace ko = cass.getApplicationKeyspace(applicationId);
		List<UUID> aliases = new ArrayList<UUID>();
		IndexedSlicesQuery<UUID, String, UUID> q = createIndexedSlicesQuery(ko,
				ue, se, ue);
		q.setColumnFamily(ENTITY_ALIASES.toString());
		q.setColumnNames("entityId");
		q.addEqualsExpression("entityId", entityId);
		QueryResult<OrderedRows<UUID, String, UUID>> r = q.execute();
		OrderedRows<UUID, String, UUID> rows = r.get();
		for (Row<UUID, String, UUID> row : rows) {
			UUID aliasId = row.getKey();
			aliases.add(aliasId);
		}

		return aliases;
	}


    @Metered(group="core",name="EntityManager_deleteAliasesForEntity")
	public void deleteAliasesForEntity(Mutator<ByteBuffer> mutator, UUID entityId, long timestamp)
			throws Exception {
		List<UUID> aliases = getAliases(entityId);

		for (UUID alias : aliases) {

		    addDeleteToMutator(mutator, ENTITY_ALIASES, alias, timestamp);

		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public <A extends Entity> A create(String entityType, Class<A> entityClass,
			Map<String, Object> properties) throws Exception {
		if ((entityType != null)
				&& (entityType.startsWith(TYPE_ENTITY) || entityType
						.startsWith("entities"))) {
			throw new IllegalArgumentException("Invalid entity type");
		}
		A e = null;
		try {
			e = (A) create(entityType, (Class<Entity>) entityClass, properties,
					null);
		} catch (ClassCastException e1) {
			logger.error("Unable to create typed entity", e1);
		}
		return e;
	}

	@Override
	public Entity create(UUID importId, String entityType,
			Map<String, Object> properties) throws Exception {
		return create(entityType, null, properties, importId);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A extends TypedEntity> A create(A entity) throws Exception {
		return (A) create(entity.getType(), entity.getClass(),
				entity.getProperties());
	}

	@Override
    @TraceParticipant
	public Entity create(String entityType, Map<String, Object> properties)
			throws Exception {
		return create(entityType, null, properties);
	}

	/**
	 * Creates a new entity.
	 *
	 * @param <A>
	 *            the generic type
	 * @param applicationId
	 *            the application id
	 * @param entityType
	 *            the entity type
	 * @param entityClass
	 *            the entity class
	 * @param properties
	 *            the properties
	 * @param importId
	 *            an existing external uuid to use as the id for the new entity
	 * @return new entity
	 * @throws Exception
	 *             the exception
	 */
  @Metered(group="core",name="EntityManager_create")
  @TraceParticipant
	public <A extends Entity> A create(String entityType, Class<A> entityClass,
			Map<String, Object> properties, UUID importId) throws Exception {

		UUID timestampUuid = newTimeUUID();

		Keyspace ko = cass.getApplicationKeyspace(applicationId);
		Mutator<ByteBuffer> m = createMutator(ko, be);
		A entity = batchCreate(m, entityType, entityClass, properties,
				importId, timestampUuid);

		batchExecute(m, CassandraService.RETRY_COUNT);

		return entity;
	}

	@SuppressWarnings("unchecked")
  @Metered(group = "core", name = "EntityManager_batchCreate")
	public <A extends Entity> A batchCreate(Mutator<ByteBuffer> m,
			String entityType, Class<A> entityClass,
			Map<String, Object> properties, UUID importId, UUID timestampUuid)
			throws Exception {

		String eType = Schema.normalizeEntityType(entityType);

		Schema schema = getDefaultSchema();

		boolean is_application = TYPE_APPLICATION.equals(eType);

		if (((applicationId == null) || applicationId
				.equals(UUIDUtils.zeroUUID)) && !is_application) {
			return null;
		}

		long timestamp = getTimestampInMicros(timestampUuid);

		UUID itemId = UUIDUtils.newTimeUUID();

		if (is_application) {
			itemId = applicationId;
		}
		if (importId != null) {
			itemId = importId;
		}
		boolean emptyPropertyMap = false;
		if (properties == null) {
			properties = new TreeMap<String, Object>(CASE_INSENSITIVE_ORDER);
		}
		if(properties.isEmpty()) {
			emptyPropertyMap = true;
		}

		if (importId != null) {
			if (isTimeBased(importId)) {
				timestamp = UUIDUtils.getTimestampInMicros(importId);
			} else if (properties.get(PROPERTY_CREATED) != null) {
				timestamp = getLong(properties.get(PROPERTY_CREATED)) * 1000;
			}
		}

		if (entityClass == null) {
			entityClass = (Class<A>) Schema.getDefaultSchema().getEntityClass(entityType);
		}

		Set<String> required = schema.getRequiredProperties(
				entityType);

		if (required != null) {
			for (String p : required) {
				if (!PROPERTY_UUID.equals(p) && !PROPERTY_TYPE.equals(p)
						&& !PROPERTY_CREATED.equals(p)
						&& !PROPERTY_MODIFIED.equals(p)) {
					Object v = properties.get(p);
					if (schema.isPropertyTimestamp(entityType, p)) {
						if (v == null) {
							properties.put(p, timestamp / 1000);
						} else {
							long ts = getLong(v);
							if (ts <= 0) {
								properties.put(p, timestamp / 1000);
							}
						}
						continue;
					}
					if (v == null) {
						throw new RequiredPropertyNotFoundException(entityType,
								p);
					} else if ((v instanceof String) && isBlank((String) v)) {
						throw new RequiredPropertyNotFoundException(entityType,
								p);
					}
				}
			}
		}

		// Create collection name based on entity: i.e. "users"
		String collection_name = Schema.defaultCollectionName(eType);
		// Create collection key based collection name
		String bucketId = indexBucketLocator.getBucket(applicationId,
				IndexType.COLLECTION, itemId, collection_name);

		Object collection_key = key(applicationId,
				Schema.DICTIONARY_COLLECTIONS, collection_name, bucketId);

		CollectionInfo collection = null;

		if (!is_application) {
			// Add entity to collection

			collection = schema.getCollection(TYPE_APPLICATION,
					collection_name);
			if(!emptyPropertyMap) {
				addInsertToMutator(m, ENTITY_ID_SETS, collection_key, itemId, null,
					timestamp);
			}

			// Add name of collection to dictionary property
			// Application.collections
			addInsertToMutator(m, ENTITY_DICTIONARIES,
					key(applicationId, Schema.DICTIONARY_COLLECTIONS),
					collection_name, null, timestamp);

			addInsertToMutator(m, ENTITY_COMPOSITE_DICTIONARIES,
					key(itemId, Schema.DICTIONARY_CONTAINER_ENTITIES),
					asList(TYPE_APPLICATION, collection_name, applicationId),
					null, timestamp);

			// If the collection has subkeys, find each subkey variant
			// and insert into the subkeyed collection as well

			if (collection != null) {
				if (collection.hasSubkeys()) {
					List<String[]> combos = collection.getSubkeyCombinations();
					for (String[] combo : combos) {
						List<Object> subkey_props = new ArrayList<Object>();
						for (String subkey_name : combo) {
							Object subkey_value = null;
							if (subkey_name != null) {
								subkey_value = properties.get(subkey_name);
							}
							subkey_props.add(subkey_value);
						}
						Object subkey_key = key(subkey_props.toArray());
						if(!emptyPropertyMap) {
							addInsertToMutator(m, ENTITY_ID_SETS,
									key(collection_key, subkey_key), itemId, null,
									timestamp);
						}
					}
				}
			}
		}

		if(emptyPropertyMap){
			return null;
		}
		properties.put(PROPERTY_UUID, itemId);
		properties.put(PROPERTY_TYPE,
				Schema.normalizeEntityType(entityType, false));

		if (importId != null) {
			if (properties.get(PROPERTY_CREATED) == null) {
				properties.put(PROPERTY_CREATED, timestamp / 1000);
			}

			if (properties.get(PROPERTY_MODIFIED) == null) {
				properties.put(PROPERTY_MODIFIED, timestamp / 1000);
			}
		} else {
			properties.put(PROPERTY_CREATED, timestamp / 1000);
			properties.put(PROPERTY_MODIFIED, timestamp / 1000);

		}

		// special case timestamp and published properties
		// and dictionary their timestamp values if not set
		// this is sure to break something for someone someday

		if (properties.containsKey(PROPERTY_TIMESTAMP)) {
			long ts = getLong(properties.get(PROPERTY_TIMESTAMP));
			if (ts <= 0) {
				properties.put(PROPERTY_TIMESTAMP, timestamp / 1000);
			}
		}

		A entity = EntityFactory.newEntity(itemId, eType, entityClass);
		logger.info("Entity created of type {}", entity.getClass().getName());

		if (Event.ENTITY_TYPE.equals(eType)) {
			Event event = (Event) entity.toTypedEntity();
			for (String prop_name : properties.keySet()) {
				Object propertyValue = properties.get(prop_name);
				if (propertyValue != null) {
					event.setProperty(prop_name, propertyValue);
				}
			}
			Message message = storeEventAsMessage(m, event, timestamp);
			incrementEntityCollection("events", timestamp);

			entity.setUuid(message.getUuid());
			return entity;
		}

		String aliasName = schema.aliasProperty(entityType);
		logger.info("Alias property is {}", aliasName);
		for (String prop_name : properties.keySet()) {

			Object propertyValue = properties.get(prop_name);

			if (propertyValue == null) {
				continue;
			}

			if (!is_application
					&& !isPropertyValueUniqueForEntity(applicationId,
							entityType, prop_name, propertyValue)) {
				throw new DuplicateUniquePropertyExistsException(entityType,
						prop_name, propertyValue);
			}

			if (User.ENTITY_TYPE.equals(entityType) && "me".equals(prop_name)) {
				throw new DuplicateUniquePropertyExistsException(entityType,
						prop_name, propertyValue);
			}

			if (!Schema.isAssociatedEntityType(entityType)
					&& prop_name.equals(aliasName)) {
				String aliasValue = propertyValue.toString().toLowerCase()
						.trim();
				logger.info("Alias property value for {} is {}", aliasName,
						aliasValue);
				createAlias(m, applicationId, ref(entityType, itemId), entityType,
						aliasValue, timestamp);
			}

			 /**
       * Unique property, load the old value and remove it, check if it's not a duplicate
       */
      if (schema.getEntityInfo(entity.getType()).isPropertyUnique(prop_name)) {
        /**
         * Only lock on the target values. We don't want lock contention if
         * another node is trying to set the property do a different value
         */
        Lock lock = getUniqueUpdateLock(cass.getLockManager(), applicationId, propertyValue, entityType, prop_name);

        try {
          lock.lock();

          String collectionName = Schema.defaultCollectionName(entityType);

          uniquePropertyWrite(m, collectionName, prop_name, propertyValue, itemId, timestamp);

        } finally {
          lock.unlock();
        }

      }

			entity.setProperty(prop_name, propertyValue);

			batchSetProperty(m, entity, prop_name, propertyValue, true, true,
					timestampUuid);

		}

		if (!is_application) {
			incrementEntityCollection(collection_name, timestamp);
		}

		return entity;
	}

	private void incrementEntityCollection(String collection_name,
			long cassandraTimestamp) {
		try {
			incrementAggregateCounters(null, null, null,
					new String(APPLICATION_COLLECTION + collection_name), ONE_COUNT,
					cassandraTimestamp);
		} catch (Exception e) {
			logger.error("Unable to increment counter application.collection."
					+ collection_name, e);
		}
		try {
			incrementAggregateCounters(null, null, null,
                    APPLICATION_ENTITIES, ONE_COUNT, cassandraTimestamp);
		} catch (Exception e) {
			logger.error("Unable to increment counter application.entities", e);
		}
	}

	public void decrementEntityCollection(String collection_name) {

		long cassandraTimestamp = cass.createTimestamp();
		decrementEntityCollection(collection_name, cassandraTimestamp);
	}

  	public void decrementEntityCollection(String collection_name,
			long cassandraTimestamp) {
		try {
			incrementAggregateCounters(null, null, null,
					APPLICATION_COLLECTION + collection_name, -ONE_COUNT,
					cassandraTimestamp);
		} catch (Exception e) {
			logger.error("Unable to decrement counter application.collection."
					+ collection_name, e);
		}
		try {
			incrementAggregateCounters(null, null, null,
                    APPLICATION_ENTITIES, -ONE_COUNT, cassandraTimestamp);
		} catch (Exception e) {
			logger.error("Unable to decrement counter application.entities", e);
		}
	}

  @Metered(group="core", name="EntityManager_insertEntity")
	public void insertEntity(String type, UUID entityId) throws Exception {

		Keyspace ko = cass.getApplicationKeyspace(applicationId);
		Mutator<ByteBuffer> m = createMutator(ko, be);

		Object itemKey = key(entityId);

		long timestamp = cass.createTimestamp();

		addPropertyToMutator(m, itemKey, type, PROPERTY_UUID, entityId,
				timestamp);
		addPropertyToMutator(m, itemKey, type, PROPERTY_TYPE, type, timestamp);

		batchExecute(m, CassandraService.RETRY_COUNT);

	}

	public Message storeEventAsMessage(Mutator<ByteBuffer> m, Event event,
			long timestamp) {

		counterUtils.addEventCounterMutations(m, applicationId, event,
				timestamp);

		QueueManager q = qmf.getQueueManager(applicationId);

		Message message = new Message();
		message.setType("event");
		message.setCategory(event.getCategory());
		message.setStringProperty("message", event.getMessage());
		message.setTimestamp(timestamp);
		q.postToQueue("events", message);

		return message;
	}

	/**
	 * Gets the type.
	 *
	 * @param applicationId
	 *            the application id
	 * @param entityId
	 *            the entity id
	 * @return entity type
	 * @throws Exception
	 *             the exception
	 */
  @Metered(group="core", name="EntityManager_getEntityType")
	public String getEntityType(UUID entityId) throws Exception {

		HColumn<String, String> column = cass.getColumn(
				cass.getApplicationKeyspace(applicationId), ENTITY_PROPERTIES,
				key(entityId), PROPERTY_TYPE, se, se);
		if (column != null) {
			return column.getValue();
		}
		return null;
	}

  /**
	 * Gets the entity info. If no propertyNames are passed it loads the ENTIRE entity!
	 *
	 * @param entityId
	 *            the entity id
	 * @param propertyNames
	 *            the property names
	 * @return DynamicEntity object holding properties
	 * @throws Exception
	 *             the exception
	 */
  @Metered(group="core",name="EntityManager_loadPartialEntity")
	public DynamicEntity loadPartialEntity(UUID entityId,
			String... propertyNames) throws Exception {

		List<HColumn<String, ByteBuffer>> results = null;
		if ((propertyNames != null) && (propertyNames.length > 0)) {
			Set<String> column_names = new TreeSet<String>(
					CASE_INSENSITIVE_ORDER);

			column_names.add(PROPERTY_TYPE);
			column_names.add(PROPERTY_UUID);

			for (String propertyName : propertyNames) {
				column_names.add(propertyName);
			}

			results = cass.getColumns(
					cass.getApplicationKeyspace(applicationId),
					ENTITY_PROPERTIES, key(entityId), column_names, se, be);
		} else {
			results = cass.getAllColumns(
					cass.getApplicationKeyspace(applicationId),
					ENTITY_PROPERTIES, key(entityId));
		}

		Map<String, Object> entityProperties = deserializeEntityProperties(results);
		if (entityProperties == null) {
			return null;
		}

		String entityType = (String) entityProperties.get(PROPERTY_TYPE);
		UUID id = (UUID) entityProperties.get(PROPERTY_UUID);

		return new DynamicEntity(entityType, id, entityProperties);
	}

	/**
	 * Gets the specified entity.
	 *
	 * @param <A>
	 *            the generic type
	 * @param applicationId
	 *            the application id
	 * @param entityId
	 *            the entity id
	 * @param entityType
	 *            the entity type
	 * @param entityClass
	 *            the entity class
	 * @return entity
	 * @throws Exception
	 *             the exception
	 */
	public <A extends Entity> A getEntity(UUID entityId, 
			Class<A> entityClass) throws Exception {

		Object entity_key = key(entityId);
		Map<String, Object> results = null;

		// if (entityType == null) {
		results = deserializeEntityProperties(cass.getAllColumns(
				cass.getApplicationKeyspace(applicationId), ENTITY_PROPERTIES,
				entity_key));
		// } else {
		// Set<String> columnNames = Schema.getPropertyNames(entityType);
		// results = getColumns(getApplicationKeyspace(applicationId),
		// EntityCF.PROPERTIES, entity_key, columnNames, se, be);
		// }

		if (results == null) {
			logger.warn("getEntity(): No properties found for entity "
					+ entityId + ", probably doesn't exist...");
			return null;
		}

		UUID id = uuid(results.get(PROPERTY_UUID));
		String type = string(results.get(PROPERTY_TYPE));

		if (!entityId.equals(id)) {

			logger.error("Expected entity id " + entityId + ", found " + id,
					new Throwable());
			return null;

		}

		A entity = EntityFactory.newEntity(id, type, entityClass);
		entity.setProperties(results);

		return entity;
	}

	/**
	 * Gets the specified list of entities.
	 *
	 * @param <A>
	 *            the generic type
	 * @param applicationId
	 *            the application id
	 * @param entityIds
	 *            the entity ids
	 * @param includeProperties
	 *            the include properties
	 * @param entityType
	 *            the entity type
	 * @param entityClass
	 *            the entity class
	 * @return entity
	 * @throws Exception
	 *             the exception
	 */
  @Metered(group="core", name="EntityManager_getEntities")
	public <A extends Entity> List<A> getEntities(List<UUID> entityIds,
			String entityType, Class<A> entityClass) throws Exception {

		List<A> entities = new ArrayList<A>();

		if ((entityIds == null) || (entityIds.size() == 0)) {
			return entities;
		}

		Map<UUID, A> resultSet = new LinkedHashMap<UUID, A>();

		Rows<UUID, String, ByteBuffer> results = null;

		// if (entityType == null) {
		results = cass.getRows(cass.getApplicationKeyspace(applicationId),
				ENTITY_PROPERTIES, entityIds, ue, se, be);
		// } else {
		// Set<String> columnNames = Schema.getPropertyNames(entityType);
		// results = getRows(getApplicationKeyspace(applicationId),
		// EntityCF.PROPERTIES,
		// entityIds, columnNames, ue, se, be);
		// }

		if (results != null) {
			for (UUID key : entityIds) {
				Map<String, Object> properties = deserializeEntityProperties(results
						.getByKey(key));

				if (properties == null) {
					logger.error("Error deserializing entity with key "
							+ key
							+ ", entity probaby doesn't exist, where did this key come from?");
					continue;
				}

				UUID id = uuid(properties.get(PROPERTY_UUID));
				String type = string(properties.get(PROPERTY_TYPE));

				if ((id == null) || (type == null)) {
					logger.error("Error retrieving entity with key "
							+ key
							+ ", no type or id deseriazable, where did this key come from?");
					continue;
				}
				A entity = EntityFactory.newEntity(id, type, entityClass);
				entity.setProperties(properties);

				resultSet.put(id, entity);
			}

			for (UUID entityId : entityIds) {
				A entity = resultSet.get(entityId);
				if (entity != null) {
					entities.add(entity);
				}
			}

		}

		return entities;
	}

  @Metered(group="core",name="EntityManager_getPropertyNames")
	public Set<String> getPropertyNames(EntityRef entity) throws Exception {

		Set<String> propertyNames = new TreeSet<String>(CASE_INSENSITIVE_ORDER);
		List<HColumn<String, ByteBuffer>> results = cass.getAllColumns(
				cass.getApplicationKeyspace(applicationId),
				ENTITY_DICTIONARIES,
				key(entity.getUuid(), DICTIONARY_PROPERTIES));
		for (HColumn<String, ByteBuffer> result : results) {
			String str = string(result.getName());
			if (str != null) {
				propertyNames.add(str);
			}
		}

		Set<String> schemaProperties = getDefaultSchema().getPropertyNames(
				entity.getType());
		if ((schemaProperties != null) && !schemaProperties.isEmpty()) {
			propertyNames.addAll(schemaProperties);
		}

		return propertyNames;
	}

  @Metered(group="core",name="EntityManager_getDictionaryNames")
	public Set<String> getDictionaryNames(EntityRef entity) throws Exception {

		Set<String> dictionaryNames = new TreeSet<String>(
				CASE_INSENSITIVE_ORDER);
		List<HColumn<String, ByteBuffer>> results = cass.getAllColumns(
				cass.getApplicationKeyspace(applicationId),
				ENTITY_DICTIONARIES, key(entity.getUuid(), DICTIONARY_SETS));
		for (HColumn<String, ByteBuffer> result : results) {
			String str = string(result.getName());
			if (str != null) {
				dictionaryNames.add(str);
			}
		}

		Set<String> schemaSets = getDefaultSchema().getDictionaryNames(
				entity.getType());
		if ((schemaSets != null) && !schemaSets.isEmpty()) {
			dictionaryNames.addAll(schemaSets);
		}

		return dictionaryNames;
	}

	@Override
  @Metered(group="core",name="EntityManager_getDictionaryElementValue")
	public Object getDictionaryElementValue(EntityRef entity,
			String dictionaryName, String elementName) throws Exception {

		Object value = null;

		ApplicationCF dictionaryCf = null;

		boolean entityHasDictionary = getDefaultSchema().hasDictionary(
				entity.getType(), dictionaryName);

		if (entityHasDictionary) {
			dictionaryCf = ENTITY_DICTIONARIES;
		} else {
			dictionaryCf = ENTITY_COMPOSITE_DICTIONARIES;
		}

		Class<?> dictionaryCoType = getDefaultSchema().getDictionaryValueType(
				entity.getType(), dictionaryName);
		boolean coTypeIsBasic = ClassUtils.isBasicType(dictionaryCoType);

		HColumn<ByteBuffer, ByteBuffer> result = cass.getColumn(cass
				.getApplicationKeyspace(applicationId), dictionaryCf,
				key(entity.getUuid(), dictionaryName),
				entityHasDictionary ? bytebuffer(elementName)
						: DynamicComposite.toByteBuffer(elementName), be, be);
		if (result != null) {
			if (entityHasDictionary && coTypeIsBasic) {
				value = object(dictionaryCoType, result.getValue());
			} else if (result.getValue().remaining() > 0) {
				value = Schema.deserializePropertyValueFromJsonBinary(result
						.getValue().slice(), dictionaryCoType);
			}
		} else {
			logger.info("Results of EntityManagerImpl.getDictionaryElementValue is null");
		}

		return value;
	}

  @Metered(group="core",name="EntityManager_getDictionaryElementValues")
	public Map<String, Object> getDictionaryElementValues(EntityRef entity,
			String dictionaryName, String... elementNames) throws Exception {

		Map<String, Object> values = null;

		ApplicationCF dictionaryCf = null;

		boolean entityHasDictionary = getDefaultSchema().hasDictionary(
				entity.getType(), dictionaryName);

		if (entityHasDictionary) {
			dictionaryCf = ENTITY_DICTIONARIES;
		} else {
			dictionaryCf = ENTITY_COMPOSITE_DICTIONARIES;
		}

		Class<?> dictionaryCoType = getDefaultSchema().getDictionaryValueType(
				entity.getType(), dictionaryName);
		boolean coTypeIsBasic = ClassUtils.isBasicType(dictionaryCoType);

		ByteBuffer[] columnNames = new ByteBuffer[elementNames.length];
		for (int i = 0; i < elementNames.length; i++) {
			columnNames[i] = entityHasDictionary ? bytebuffer(elementNames[i])
					: DynamicComposite.toByteBuffer(elementNames[i]);
		}

		ColumnSlice<ByteBuffer, ByteBuffer> results = cass.getColumns(
				cass.getApplicationKeyspace(applicationId), dictionaryCf,
				key(entity.getUuid(), dictionaryName), columnNames, be, be);
		if (results != null) {
			values = new HashMap<String, Object>();
			for (HColumn<ByteBuffer, ByteBuffer> result : results.getColumns()) {
				String name = entityHasDictionary ? string(result.getName())
						: DynamicComposite.fromByteBuffer(result.getName())
								.get(0, se);
				if (entityHasDictionary && coTypeIsBasic) {
					values.put(name,
							object(dictionaryCoType, result.getValue()));
				} else if (result.getValue().remaining() > 0) {
					values.put(name, Schema
							.deserializePropertyValueFromJsonBinary(result
									.getValue().slice(), dictionaryCoType));
				}
			}
		} else {
			logger.error("Results of EntityManagerImpl.getDictionaryElementValues is null");
		}

		return values;
	}

	/**
	 * Gets the set.
	 *
	 * @param applicationId
	 *            the application id
	 * @param entityType
	 *            the entity type
	 * @param entityId
	 *            the entity id
	 * @param dictionaryName
	 *            the dictionary name
	 * @param joint
	 *            the joint
	 * @param property
	 *            the property
	 * @return contents of dictionary property
	 * @throws Exception
	 *             the exception
	 */
	@Override
  @Metered(group="core",name="EntityManager_getDictionaryAsMap")
	public Map<Object, Object> getDictionaryAsMap(EntityRef entity,
			String dictionaryName) throws Exception {

		entity = validate(entity);

		Map<Object, Object> dictionary = new LinkedHashMap<Object, Object>();

		ApplicationCF dictionaryCf = null;

		boolean entityHasDictionary = getDefaultSchema().hasDictionary(
				entity.getType(), dictionaryName);

		if (entityHasDictionary) {
			dictionaryCf = ENTITY_DICTIONARIES;
		} else {
			dictionaryCf = ENTITY_COMPOSITE_DICTIONARIES;
		}

		Class<?> setType = getDefaultSchema().getDictionaryKeyType(
				entity.getType(), dictionaryName);
		Class<?> setCoType = getDefaultSchema().getDictionaryValueType(
				entity.getType(), dictionaryName);
		boolean coTypeIsBasic = ClassUtils.isBasicType(setCoType);

		List<HColumn<ByteBuffer, ByteBuffer>> results = cass.getAllColumns(
				cass.getApplicationKeyspace(applicationId), dictionaryCf,
				key(entity.getUuid(), dictionaryName), be, be);
		for (HColumn<ByteBuffer, ByteBuffer> result : results) {
			Object name = null;
			if (entityHasDictionary) {
				name = object(setType, result.getName());
			} else {
				name = CompositeUtils.deserialize(result.getName());
			}
			Object value = null;
			if (entityHasDictionary && coTypeIsBasic) {
				value = object(setCoType, result.getValue());
			} else if (result.getValue().remaining() > 0) {
				value = Schema.deserializePropertyValueFromJsonBinary(result
						.getValue().slice(), setCoType);
			}
			if (name != null) {
				dictionary.put(name, value);
			}
		}

		return dictionary;
	}

	@Override
	public Set<Object> getDictionaryAsSet(EntityRef entity,
			String dictionaryName) throws Exception {
		return new LinkedHashSet<Object>(getDictionaryAsMap(entity,
				dictionaryName).keySet());
	}

	/**
	 * Update properties.
	 *
	 * @param applicationId
	 *            the application id
	 * @param entityId
	 *            the entity id
	 * @param properties
	 *            the properties
	 * @throws Exception
	 *             the exception
	 */
  @Metered(group="core",name="EntityManager_updateProperties")
	public void updateProperties(UUID entityId, Map<String, Object> properties)
			throws Exception {

    EntityRef entity = getRef(entityId);
    if (entity == null) {
			return;
		}

		Keyspace ko = cass.getApplicationKeyspace(applicationId);
		Mutator<ByteBuffer> m = createMutator(ko, be);

		UUID timestampUuid = newTimeUUID();
		properties.put(PROPERTY_MODIFIED, getTimestampInMillis(timestampUuid));

		batchUpdateProperties(m, entity, properties, timestampUuid);

		batchExecute(m, CassandraService.RETRY_COUNT);
	}

  @Metered(group="core",name="EntityManager_deleteEntity")
	public void deleteEntity(UUID entityId) throws Exception {

		logger.info("deleteEntity {} of application {}", entityId,
				applicationId);

		EntityRef entity = getRef(entityId);
		if (entity == null) {
			return;
		}

		logger.info("deleteEntity: {} is of type {}", entityId,
				entity.getType());

		Keyspace ko = cass.getApplicationKeyspace(applicationId);
		Mutator<ByteBuffer> m = createMutator(ko, be);

		UUID timestampUuid = newTimeUUID();
		long timestamp = getTimestampInMicros(timestampUuid);

		// get all connections and disconnect them
		getRelationManager(ref(entityId)).batchDisconnect(m, timestampUuid);

		// delete all core properties and any dynamic property that's ever been
		// dictionary for this entity
		Set<String> properties = getPropertyNames(entity);
		if (properties != null) {
			for (String propertyName : properties) {
				m = batchSetProperty(m, entity, propertyName, null, true,
						false, timestampUuid);
			}
		}

		// delete any core dictionaries and dynamic dictionaries associated with
		// this entity
		Set<String> dictionaries = getDictionaryNames(entity);
		if (dictionaries != null) {
			for (String dictionary : dictionaries) {
				Set<Object> values = getDictionaryAsSet(entity, dictionary);
				if (values != null) {
					for (Object value : values) {
						batchUpdateDictionary(m, entity, dictionary, value,
								true, timestampUuid);
					}
				}
			}
		}

		// find all the containing collections
		getRelationManager(entity).batchRemoveFromContainers(m, timestampUuid);

		//decrease entity count
		if(!TYPE_APPLICATION.equals(entity.getType())) {
			String collection_name = Schema.defaultCollectionName(entity.getType());
			decrementEntityCollection(collection_name);
		}


		timestamp += 1;

		if (dictionaries != null) {
			for (String dictionary : dictionaries) {

			    ApplicationCF cf = getDefaultSchema().hasDictionary(entity.getType(),
                        dictionary) ? ENTITY_DICTIONARIES
                        : ENTITY_COMPOSITE_DICTIONARIES;

			    addDeleteToMutator(m, cf, key(entity.getUuid(), dictionary), timestamp);
			}
		}

		addDeleteToMutator(m, ENTITY_PROPERTIES, key(entityId), timestamp);

		deleteAliasesForEntity(m, entityId, timestamp);

		batchExecute(m, CassandraService.RETRY_COUNT);

	}

	@Override
	public void delete(EntityRef entityRef) throws Exception {
		deleteEntity(entityRef.getUuid());
	}

	public void batchCreateRole(Mutator<ByteBuffer> batch, UUID groupId,
			String roleName, String roleTitle, long inactivity,
			RoleRef roleRef, UUID timestampUuid) throws Exception {

		long timestamp = getTimestampInMicros(timestampUuid);

		if (roleRef == null) {
			roleRef = new SimpleRoleRef(groupId, roleName);
		}
		if (roleTitle == null) {
			roleTitle = roleRef.getRoleName();
		}

		EntityRef ownerRef = null;
		if (roleRef.getGroupId() != null) {
			ownerRef = new SimpleEntityRef(Group.ENTITY_TYPE,
					roleRef.getGroupId());
		} else {
			ownerRef = new SimpleEntityRef(Application.ENTITY_TYPE,
					applicationId);
		}

		Map<String, Object> properties = new TreeMap<String, Object>(
				CASE_INSENSITIVE_ORDER);
		properties.put(PROPERTY_TYPE, Role.ENTITY_TYPE);
		properties.put("group", roleRef.getGroupId());
		properties.put(PROPERTY_NAME, roleRef.getApplicationRoleName());
		properties.put("roleName", roleRef.getRoleName());
		properties.put("title", roleTitle);
		properties.put(PROPERTY_INACTIVITY, inactivity);

		Entity role = batchCreate(batch, Role.ENTITY_TYPE, null, properties,
				roleRef.getUuid(), timestampUuid);

		addInsertToMutator(batch, ENTITY_DICTIONARIES,
				key(ownerRef.getUuid(), Schema.DICTIONARY_ROLENAMES),
				roleRef.getRoleName(), roleTitle, timestamp);

		addInsertToMutator(batch, ENTITY_DICTIONARIES,
				key(ownerRef.getUuid(), Schema.DICTIONARY_ROLETIMES),
				roleRef.getRoleName(), inactivity, timestamp);

		addInsertToMutator(batch, ENTITY_DICTIONARIES,
				key(ownerRef.getUuid(), DICTIONARY_SETS),
				Schema.DICTIONARY_ROLENAMES, null, timestamp);

		if (roleRef.getGroupId() != null) {
			getRelationManager(ownerRef).batchAddToCollection(batch,
					COLLECTION_ROLES, role, timestampUuid);
		}

	}

	@Override
	public EntityRef getUserByIdentifier(Identifier identifier)
			throws Exception {
		if (identifier == null) {
			return null;
		}
		if (identifier.isUUID()) {
			return new SimpleEntityRef("user", identifier.getUUID());
		}
		if (identifier.isName()) {
			return this.getAlias(null, "user", identifier.getName());
		}
		if (identifier.isEmail()) {

			Query query = new Query();
			query.setEntityType("user");
			query.addEqualityFilter("email", identifier.getEmail());
			query.setLimit(1);
			query.setResultsLevel(REFS);

			Results r = getRelationManager(ref(applicationId))
					.searchCollection("users", query);
			if (r != null && r.getRef()!=null) {
				return r.getRef();
			} else {
                // look-aside as it might be an email in the name field
                return this.getAlias(null, "user",identifier.getEmail());
            }
		}
		return null;
	}

	@Override
	public EntityRef getGroupByIdentifier(Identifier identifier)
			throws Exception {
		if (identifier == null) {
			return null;
		}
		if (identifier.isUUID()) {
			return new SimpleEntityRef("group", identifier.getUUID());
		}
		if (identifier.isName()) {
			return this.getAlias(null, "group", identifier.getName());
		}
		return null;
	}

	@Override
	public Results getAggregateCounters(UUID userId, UUID groupId,
			String category, String counterName, CounterResolution resolution,
			long start, long finish, boolean pad) {
		return this.getAggregateCounters(userId, groupId, null, category,
				counterName, resolution, start, finish, pad);
	}

	@Override
  @Metered(group="core",name="EntityManager_getAggregateCounters")
	public Results getAggregateCounters(UUID userId, UUID groupId,
			UUID queueId, String category, String counterName,
			CounterResolution resolution, long start, long finish, boolean pad) {
		start = resolution.round(start);
		finish = resolution.round(finish);
		long expected_time = start;
		Keyspace ko = cass.getApplicationKeyspace(applicationId);
		SliceCounterQuery<String, Long> q = createCounterSliceQuery(ko, se, le);
		q.setColumnFamily(APPLICATION_AGGREGATE_COUNTERS.toString());
		q.setRange(start, finish, false, ALL_COUNT);
		QueryResult<CounterSlice<Long>> r = q.setKey(
				counterUtils.getAggregateCounterRow(counterName, userId,
						groupId, queueId, category, resolution)).execute();
		List<AggregateCounter> counters = new ArrayList<AggregateCounter>();
		for (HCounterColumn<Long> column : r.get().getColumns()) {
			AggregateCounter count = new AggregateCounter(column.getName(),
					column.getValue());
			if (pad && !(resolution == CounterResolution.ALL)) {
				while (count.getTimestamp() != expected_time) {
					counters.add(new AggregateCounter(expected_time, 0));
					expected_time = resolution.next(expected_time);
				}
				expected_time = resolution.next(expected_time);
			}
			counters.add(count);
		}
		if (pad && !(resolution == CounterResolution.ALL)) {
			while (expected_time <= finish) {
				counters.add(new AggregateCounter(expected_time, 0));
				expected_time = resolution.next(expected_time);
			}
		}
		return Results.fromCounters(new AggregateCounterSet(counterName,
				userId, groupId, category, counters));
	}

	@Override
  @Metered(group="core",name="EntityManager_getAggregateCounters_fromQueryObj")
	public Results getAggregateCounters(Query query) throws Exception {
		CounterResolution resolution = query.getResolution();
		if (resolution == null) {
			resolution = CounterResolution.ALL;
		}
		long start = query.getStartTime() != null ? query.getStartTime() : 0;
		long finish = query.getFinishTime() != null ? query.getFinishTime() : 0;
		boolean pad = query.isPad();
		if (start <= 0) {
			start = 0;
		}
		if ((finish <= 0) || (finish < start)) {
			finish = System.currentTimeMillis();
		}
		start = resolution.round(start);
		finish = resolution.round(finish);
		long expected_time = start;

		if (pad && (resolution != CounterResolution.ALL)) {
			long max_counters = (finish - start) / resolution.interval();
			if (max_counters > 1000) {
				finish = resolution.round(start
						+ (resolution.interval() * 1000));
			}
		}

		List<CounterFilterPredicate> filters = query.getCounterFilters();
		if (filters == null) {
			return null;
		}
		Map<String, AggregateCounterSelection> selections = new HashMap<String, AggregateCounterSelection>();
		Keyspace ko = cass.getApplicationKeyspace(applicationId);

		for (CounterFilterPredicate filter : filters) {
			AggregateCounterSelection selection = new AggregateCounterSelection(
					filter.getName(),
					getUuid(getUserByIdentifier(filter.getUser())),
					getUuid(getGroupByIdentifier(filter.getGroup())),
					org.usergrid.mq.Queue.getQueueId(filter.getQueue()),
					filter.getCategory());
			selections.put(selection.getRow(resolution), selection);
		}

		MultigetSliceCounterQuery<String, Long> q = HFactory
				.createMultigetSliceCounterQuery(ko, se, le);
		q.setColumnFamily(APPLICATION_AGGREGATE_COUNTERS.toString());
		q.setRange(start, finish, false, ALL_COUNT);
		QueryResult<CounterRows<String, Long>> rows = q.setKeys(
				selections.keySet()).execute();

		List<AggregateCounterSet> countSets = new ArrayList<AggregateCounterSet>();
		for (CounterRow<String, Long> r : rows.get()) {
			expected_time = start;
			List<AggregateCounter> counters = new ArrayList<AggregateCounter>();
			for (HCounterColumn<Long> column : r.getColumnSlice().getColumns()) {
				AggregateCounter count = new AggregateCounter(column.getName(),
						column.getValue());
				if (pad && (resolution != CounterResolution.ALL)) {
					while (count.getTimestamp() != expected_time) {
						counters.add(new AggregateCounter(expected_time, 0));
						expected_time = resolution.next(expected_time);
					}
					expected_time = resolution.next(expected_time);
				}
				counters.add(count);
			}
			if (pad && (resolution != CounterResolution.ALL)) {
				while (expected_time <= finish) {
					counters.add(new AggregateCounter(expected_time, 0));
					expected_time = resolution.next(expected_time);
				}
			}
			AggregateCounterSelection selection = selections.get(r.getKey());
			countSets.add(new AggregateCounterSet(selection.getName(),
					selection.getUserId(), selection.getGroupId(), selection
							.getCategory(), counters));
		}

		Collections.sort(countSets, new Comparator<AggregateCounterSet>() {
			@Override
			public int compare(AggregateCounterSet o1, AggregateCounterSet o2) {
				String s1 = o1.getName();
				String s2 = o2.getName();
				return s1.compareTo(s2);
			}
		});
		return Results.fromCounters(countSets);
	}

	@Override
  @Metered(group="core",name="EntityManager_getEntityCounters")
	public Map<String, Long> getEntityCounters(UUID entityId) throws Exception {

		Map<String, Long> counters = new HashMap<String, Long>();
		Keyspace ko = cass.getApplicationKeyspace(applicationId);
		SliceCounterQuery<UUID, String> q = createCounterSliceQuery(ko, ue, se);
		q.setColumnFamily(ENTITY_COUNTERS.toString());
		q.setRange(null, null, false, ALL_COUNT);
		QueryResult<CounterSlice<String>> r = q.setKey(entityId).execute();
		for (HCounterColumn<String> column : r.get().getColumns()) {
			counters.put(column.getName(), column.getValue());
		}
		return counters;
	}

	@Override
	public Map<String, Long> getApplicationCounters() throws Exception {
		return getEntityCounters(applicationId);
	}

	@Override
  @Metered(group="core",name="EntityManager_createApplicationCollection")
	public void createApplicationCollection(String entityType) throws Exception {

		Keyspace ko = cass.getApplicationKeyspace(applicationId);
		Mutator<ByteBuffer> m = createMutator(ko, be);

		long timestamp = cass.createTimestamp();

		String collection_name = Schema.defaultCollectionName(entityType);
		// Add name of collection to dictionary property Application.collections
		addInsertToMutator(m, ENTITY_DICTIONARIES,
				key(applicationId, Schema.DICTIONARY_COLLECTIONS),
				collection_name, null, timestamp);

		batchExecute(m, CassandraService.RETRY_COUNT);

	}



	@Override
	public void deleteAlias(String aliasType, String alias) throws Exception {
		deleteAlias(null, aliasType, alias);
	}

	@Override
	public EntityRef getAlias(String aliasType, String alias) throws Exception {
		return getAlias(null, aliasType, alias);
	}

	@Override
	public EntityRef validate(EntityRef entityRef) throws Exception {
		return validate(entityRef, true);

	}

	public EntityRef validate(EntityRef entityRef, boolean verify)
			throws Exception {
		if ((entityRef == null) || (entityRef.getUuid() == null)) {
			return null;
		}
		if ((entityRef.getType() == null) || verify) {
			UUID entityId = entityRef.getUuid();
			String entityType = entityRef.getType();
			try {
				entityRef = getRef(entityRef.getUuid());
			} catch (Exception e) {
				logger.error("Unable to load entity"
						+ entityRef.getUuid().toString(), e);
			}
			if (entityRef == null) {
				throw new EntityNotFoundException("Entity "
						+ entityId.toString() + " cannot be verified");
			}
			if ((entityType != null)
					&& !entityType.equalsIgnoreCase(entityRef.getType())) {
				throw new UnexpectedEntityTypeException("Entity " + entityId
						+ " is not the expected type, expected " + entityType
						+ ", found " + entityRef.getType());
			}
		}
		return entityRef;
	}

	@Override
	public String getType(UUID entityid) throws Exception {
		return getEntityType(entityid);
	}

	public String getType(EntityRef entity) throws Exception {
		if (entity.getType() != null) {
			return entity.getType();
		}
		return getEntityType(entity.getUuid());
	}

	@Override
	public Entity get(UUID entityid) throws Exception {
		return getEntity(entityid, null);
	}

	@Override
	public EntityRef getRef(UUID entityId) throws Exception {
		String entityType = getEntityType(entityId);
		if (entityType == null) {
			logger.warn("Unable to get type for entity " + entityId);
			return null;
		}
		return ref(entityType, entityId);
	}

	@Override
	public Entity get(EntityRef entityRef) throws Exception {
		if (entityRef == null) {
			return null;
		}
		return getEntity(entityRef.getUuid(), null);
	}

	@Override
	public <A extends Entity> A get(EntityRef entityRef, Class<A> entityClass)
			throws Exception {
		if (entityRef == null) {
			return null;
		}
		return get(entityRef.getUuid(), entityClass);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A extends Entity> A get(UUID entityId, Class<A> entityClass)
			throws Exception {
		A e = null;
		try {
			e = (A) getEntity(entityId, (Class<Entity>) entityClass);
		} catch (ClassCastException e1) {
			logger.error("Unable to get typed entity", e1);
		}
		return e;
	}

	@Override
	public Results get(List<UUID> entityIds, Results.Level resultsLevel)
			throws Exception {
		List<? extends Entity> results = getEntities(entityIds, null, null);
		return Results.fromEntities(results);
	}

	@Override
	public Results get(List<UUID> entityIds,
			Class<? extends Entity> entityClass, Results.Level resultsLevel)
			throws Exception {
		return fromEntities(getEntities(entityIds, null, entityClass));
	}

	@Override
	public Results get(List<UUID> entityIds, String entityType,
			Class<? extends Entity> entityClass, Results.Level resultsLevel)
			throws Exception {
		return fromEntities(getEntities(entityIds, entityType, entityClass));
	}

	public Results loadEntities(Results results, Results.Level resultsLevel,
			int count) throws Exception {
		return loadEntities(results, resultsLevel, null, count);
	}

	public Results loadEntities(Results results, Results.Level resultsLevel,
			Map<UUID, UUID> associatedMap, int count) throws Exception {

		results = results.trim(count);
		if (resultsLevel.ordinal() <= results.getLevel().ordinal()) {
			return results;
		}

		results.setEntities(getEntities(results.getIds(), null,null));

		if (resultsLevel == Results.Level.LINKED_PROPERTIES) {
			List<Entity> entities = results.getEntities();
			BiMap<UUID, UUID> associatedIds = null;

			if (associatedMap != null) {
				associatedIds = HashBiMap.create(associatedMap);
			} else {
				associatedIds = HashBiMap.create(entities.size());
				for (Entity entity : entities) {
					Object id = entity.getMetadata(PROPERTY_ASSOCIATED);
					if (id instanceof UUID) {
						associatedIds.put(entity.getUuid(), (UUID) id);
					}
				}
			}
			List<DynamicEntity> linked = getEntities(new ArrayList<UUID>(
					associatedIds.values()), null, null);
			for (DynamicEntity l : linked) {
				Map<String, Object> p = l.getDynamicProperties();
				if ((p != null) && (p.size() > 0)) {
					Entity e = results.getEntitiesMap().get(
							associatedIds.inverse().get(l.getUuid()));
					if (l.getType().endsWith(TYPE_MEMBER)) {
						e.setProperty(TYPE_MEMBER, p);
					} else if (l.getType().endsWith(TYPE_CONNECTION)) {
						e.setProperty(TYPE_CONNECTION, p);
					}
				}

			}
		}

		return results;
	}

	@Override
	public void update(Entity entity) throws Exception {
		updateProperties(entity.getUuid(), entity.getProperties());
	}

	@Override
	public Object getProperty(EntityRef entityRef, String propertyName)
			throws Exception {
		Entity entity = loadPartialEntity(entityRef.getUuid(), propertyName);

		if(entity == null){
		    return null;
		}

		return entity.getProperty(propertyName);
	}

	@Override
	public Map<String, Object> getProperties(EntityRef entityRef)
			throws Exception {
		Entity entity = loadPartialEntity(entityRef.getUuid());
		Map<String, Object> props = entity.getProperties();
		return props;
	}

	@Override
	public void setProperty(EntityRef entityRef, String propertyName,
			Object propertyValue) throws Exception {

		setProperty(entityRef, propertyName, propertyValue, false);

	}

	@Override
	public void setProperty(EntityRef entityRef, String propertyName,
			Object propertyValue, boolean override) throws Exception {

		if ((propertyValue instanceof String)
				&& ((String) propertyValue).equals("")) {
			propertyValue = null;
		}

    // todo: would this ever need to load more?
		DynamicEntity entity = loadPartialEntity(entityRef.getUuid(), propertyName);

		UUID timestampUuid = newTimeUUID();
		Mutator<ByteBuffer> batch = createMutator(
				cass.getApplicationKeyspace(applicationId), be);

		propertyValue = getDefaultSchema().validateEntityPropertyValue(
				entity.getType(), propertyName, propertyValue);

		entity.setProperty(propertyName, propertyValue);
		batch = batchSetProperty(batch, entity, propertyName, propertyValue,
				override, false, timestampUuid);
		batchExecute(batch, CassandraService.RETRY_COUNT);

	}

	@Override
	public void updateProperties(EntityRef entityRef,
			Map<String, Object> properties) throws Exception {
		entityRef = validate(entityRef);
		properties = getDefaultSchema().cleanUpdatedProperties(
				entityRef.getType(), properties, false);
		updateProperties(entityRef.getUuid(), properties);
	}

	@Override
	public void addToDictionary(EntityRef entityRef, String dictionaryName,
			Object elementValue) throws Exception {
		addToDictionary(entityRef, dictionaryName, elementValue, null);
	}

	@Override
	public void addToDictionary(EntityRef entityRef, String dictionaryName,
			Object elementValue, Object elementCoValue) throws Exception {

		if (elementValue == null) {
			return;
		}

    EntityRef entity = getRef(entityRef.getUuid());

		UUID timestampUuid = newTimeUUID();
		Mutator<ByteBuffer> batch = createMutator(
				cass.getApplicationKeyspace(applicationId), be);

		batch = batchUpdateDictionary(batch, entity, dictionaryName,
				elementValue, elementCoValue, false, timestampUuid);

		batchExecute(batch, CassandraService.RETRY_COUNT);
	}

	@Override
	public void addSetToDictionary(EntityRef entityRef, String dictionaryName,
			Set<?> elementValues) throws Exception {

		if ((elementValues == null) || elementValues.isEmpty()) {
			return;
		}

    EntityRef entity = getRef(entityRef.getUuid());

		UUID timestampUuid = newTimeUUID();
		Mutator<ByteBuffer> batch = createMutator(
				cass.getApplicationKeyspace(applicationId), be);

		for (Object elementValue : elementValues) {
			batch = batchUpdateDictionary(batch, entity, dictionaryName,
					elementValue, null, false, timestampUuid);
		}

		batchExecute(batch, CassandraService.RETRY_COUNT);
	}

	@Override
	public void addMapToDictionary(EntityRef entityRef, String dictionaryName,
			Map<?, ?> elementValues) throws Exception {

		if ((elementValues == null) || elementValues.isEmpty()
				|| entityRef == null) {
			return;
		}

    EntityRef entity = getRef(entityRef.getUuid());

		UUID timestampUuid = newTimeUUID();
		Mutator<ByteBuffer> batch = createMutator(
				cass.getApplicationKeyspace(applicationId), be);

		for (Map.Entry<?, ?> elementValue : elementValues.entrySet()) {
			batch = batchUpdateDictionary(batch, entity, dictionaryName,
					elementValue.getKey(), elementValue.getValue(), false,
					timestampUuid);
		}

		batchExecute(batch, CassandraService.RETRY_COUNT);
	}

	@Override
	public void removeFromDictionary(EntityRef entityRef,
			String dictionaryName, Object elementValue) throws Exception {

		if (elementValue == null) {
			return;
		}

    EntityRef entity = getRef(entityRef.getUuid());

		UUID timestampUuid = newTimeUUID();
		Mutator<ByteBuffer> batch = createMutator(
				cass.getApplicationKeyspace(applicationId), be);

		batch = batchUpdateDictionary(batch, entity, dictionaryName,
				elementValue, true, timestampUuid);

		batchExecute(batch, CassandraService.RETRY_COUNT);
	}

	@Override
	public Set<String> getDictionaries(EntityRef entity) throws Exception {
		return getDictionaryNames(entity);
	}

	@Override
	public void deleteProperty(EntityRef entityRef, String propertyName)
			throws Exception {
		setProperty(entityRef, propertyName, null);
	}

	@Override
	public Set<String> getApplicationCollections() throws Exception {
		Set<String> collections = new TreeSet<String>(CASE_INSENSITIVE_ORDER);
		Set<String> dynamic_collections = cast(getDictionaryAsSet(
				getApplicationRef(), DICTIONARY_COLLECTIONS));
		if (dynamic_collections != null) {
			for (String collection : dynamic_collections) {
				if (!Schema.isAssociatedEntityType(collection)) {
					collections.add(collection);
				}
			}
		}
		Set<String> system_collections = getDefaultSchema().getCollectionNames(
				Application.ENTITY_TYPE);
		if (system_collections != null) {
			for (String collection : system_collections) {
				if (!Schema.isAssociatedEntityType(collection)) {
					collections.add(collection);
				}
			}
		}
		return collections;
	}

	@Override
	public long getApplicationCollectionSize(String collectionName)
			throws Exception {
		Long count = null;
		if (!Schema.isAssociatedEntityType(collectionName)) {
			Map<String, Long> counts = getApplicationCounters();
			count = counts.get(new String(APPLICATION_COLLECTION + collectionName));
		}
		return count != null ? count : 0;
	}

	@Override
	public Map<String, Object> getApplicationCollectionMetadata()
			throws Exception {
		Set<String> collections = getApplicationCollections();
		Map<String, Long> counts = getApplicationCounters();
		Map<String, Object> metadata = new HashMap<String, Object>();
		if (collections != null) {
			for (String collectionName : collections) {
				if (!Schema.isAssociatedEntityType(collectionName)) {
					Long count = counts.get(APPLICATION_COLLECTION
							+ collectionName);
					/*
					 * int count = emf .countColumns(
					 * getApplicationKeyspace(applicationId),
					 * ApplicationCF.ENTITY_ID_SETS, key(applicationId,
					 * DICTIONARY_COLLECTIONS, collectionName));
					 */
					Map<String, Object> entry = new HashMap<String, Object>();
					entry.put("count", count != null ? count : 0);
					entry.put("type", singularize(collectionName));
					entry.put("name", collectionName);
					entry.put("title", capitalize(collectionName));
					metadata.put(collectionName, entry);
				}
			}
		}
		/*
		 * if ((counts != null) && !counts.isEmpty()) { metadata.put("counters",
		 * counts); }
		 */
		return metadata;
	}

	public Object getRolePermissionsKey(String roleName) {
		return key(getIdForRoleName(roleName), DICTIONARY_PERMISSIONS);
	}

	public Object getRolePermissionsKey(UUID groupId, String roleName) {
		return key(getIdForGroupIdAndRoleName(groupId, roleName),
				DICTIONARY_PERMISSIONS);
	}

	public EntityRef userRef(UUID userId) {
		return ref(User.ENTITY_TYPE, userId);
	}

	public EntityRef groupRef(UUID groupId) {
		return ref(Group.ENTITY_TYPE, groupId);
	}

	public EntityRef roleRef(String roleName) {
		return ref(TYPE_ROLE, getIdForRoleName(roleName));
	}

	public EntityRef roleRef(UUID groupId, String roleName) {
		return ref(TYPE_ROLE, getIdForGroupIdAndRoleName(groupId, roleName));
	}

	@Override
	public Map<String, String> getRoles() throws Exception {
		return cast(getDictionaryAsMap(getApplicationRef(),
				DICTIONARY_ROLENAMES));
	}

	@Override
	public String getRoleTitle(String roleName) throws Exception {
		String title = string(getDictionaryElementValue(getApplicationRef(),
				DICTIONARY_ROLENAMES, roleName));
		if (title == null) {
			title = roleName;
		}
		return title;
	}

	@Override
	public Map<String, Role> getRolesWithTitles(Set<String> roleNames)
			throws Exception {

		Map<String, Role> rolesWithTitles = new HashMap<String, Role>();

		Map<String, Object> nameResults = null;

		if (roleNames != null) {
			nameResults = getDictionaryElementValues(getApplicationRef(),
					DICTIONARY_ROLENAMES, roleNames.toArray(new String[0]));
		} else {
			nameResults = cast(getDictionaryAsMap(getApplicationRef(),
					DICTIONARY_ROLENAMES));
			roleNames = nameResults.keySet();
		}
		Map<String, Object> timeResults = getDictionaryElementValues(
				getApplicationRef(), DICTIONARY_ROLETIMES,
				roleNames.toArray(new String[0]));

		for (String roleName : roleNames) {

			String savedTitle = string(nameResults.get(roleName));

			// no title, skip the role
			if (savedTitle == null) {
				continue;
			}

			Role newRole = new Role();
			newRole.setName(roleName);
			newRole.setTitle(savedTitle);
			newRole.setInactivity(getLong(timeResults.get(roleName)));

			rolesWithTitles.put(roleName, newRole);

		}

		return rolesWithTitles;
	}

	@Override
	public Entity createRole(String roleName, String roleTitle, long inactivity)
			throws Exception {
		UUID timestampUuid = newTimeUUID();
		Mutator<ByteBuffer> batch = createMutator(
				cass.getApplicationKeyspace(applicationId), be);
		batchCreateRole(batch, null, roleName, roleTitle, inactivity, null,
				timestampUuid);
		batchExecute(batch, CassandraService.RETRY_COUNT);
		return get(roleRef(roleName));
	}

	@Override
	public void grantRolePermission(String roleName, String permission)
			throws Exception {
		roleName = roleName.toLowerCase();
		permission = permission.toLowerCase();
		long timestamp = cass.createTimestamp();
		Mutator<ByteBuffer> batch = createMutator(
				cass.getApplicationKeyspace(applicationId), be);
		addInsertToMutator(batch, ApplicationCF.ENTITY_DICTIONARIES,
				getRolePermissionsKey(roleName), permission,
				ByteBuffer.allocate(0), timestamp);
		batchExecute(batch, CassandraService.RETRY_COUNT);
	}

	@Override
	public void grantRolePermissions(String roleName,
			Collection<String> permissions) throws Exception {
		roleName = roleName.toLowerCase();
		long timestamp = cass.createTimestamp();
		Mutator<ByteBuffer> batch = createMutator(
				cass.getApplicationKeyspace(applicationId), be);
		for (String permission : permissions) {
			permission = permission.toLowerCase();
			addInsertToMutator(batch, ApplicationCF.ENTITY_DICTIONARIES,
					getRolePermissionsKey(roleName), permission,
					ByteBuffer.allocate(0), timestamp);
		}
		batchExecute(batch, CassandraService.RETRY_COUNT);
	}

	@Override
	public void revokeRolePermission(String roleName, String permission)
			throws Exception {
		roleName = roleName.toLowerCase();
		permission = permission.toLowerCase();
		long timestamp = cass.createTimestamp();
		Mutator<ByteBuffer> batch = createMutator(
				cass.getApplicationKeyspace(applicationId), be);
		CassandraPersistenceUtils.addDeleteToMutator(batch,
				ApplicationCF.ENTITY_DICTIONARIES,
				getRolePermissionsKey(roleName), permission, timestamp);
		batchExecute(batch, CassandraService.RETRY_COUNT);
	}

	@Override
	public Set<String> getRolePermissions(String roleName) throws Exception {
		roleName = roleName.toLowerCase();
		return cass.getAllColumnNames(
				cass.getApplicationKeyspace(applicationId),
				ApplicationCF.ENTITY_DICTIONARIES,
				getRolePermissionsKey(roleName));
	}

	@Override
	public void deleteRole(String roleName) throws Exception {
		roleName = roleName.toLowerCase();
		removeFromDictionary(getApplicationRef(), DICTIONARY_ROLENAMES,
				roleName);
		removeFromDictionary(getApplicationRef(), DICTIONARY_ROLETIMES,
				roleName);
		delete(roleRef(roleName));
	}

	public CollectionRef memberRef(UUID groupId, UUID userId) {
		return new SimpleCollectionRef(groupRef(groupId), COLLECTION_USERS,
				userRef(userId));
	}

	@Override
	public Map<String, String> getGroupRoles(UUID groupId) throws Exception {
		return cast(getDictionaryAsMap(groupRef(groupId), DICTIONARY_ROLENAMES));
	}

	@Override
	public Entity createGroupRole(UUID groupId, String roleName, long inactivity)
			throws Exception {
		UUID timestampUuid = newTimeUUID();
		Mutator<ByteBuffer> batch = createMutator(
				cass.getApplicationKeyspace(applicationId), be);
		batchCreateRole(batch, groupId, roleName, null, inactivity, null,
				timestampUuid);
		batchExecute(batch, CassandraService.RETRY_COUNT);
		return get(roleRef(groupId, roleName));
	}

	@Override
	public void grantGroupRolePermission(UUID groupId, String roleName,
			String permission) throws Exception {
		roleName = roleName.toLowerCase();
		permission = permission.toLowerCase();
		long timestamp = cass.createTimestamp();
		Mutator<ByteBuffer> batch = createMutator(
				cass.getApplicationKeyspace(applicationId), be);
		addInsertToMutator(batch, ApplicationCF.ENTITY_DICTIONARIES,
				getRolePermissionsKey(groupId, roleName), permission,
				ByteBuffer.allocate(0), timestamp);
		batchExecute(batch, CassandraService.RETRY_COUNT);
	}

	@Override
	public void revokeGroupRolePermission(UUID groupId, String roleName,
			String permission) throws Exception {
		roleName = roleName.toLowerCase();
		permission = permission.toLowerCase();
		long timestamp = cass.createTimestamp();
		Mutator<ByteBuffer> batch = createMutator(
				cass.getApplicationKeyspace(applicationId), be);
		CassandraPersistenceUtils
				.addDeleteToMutator(batch, ApplicationCF.ENTITY_DICTIONARIES,
						getRolePermissionsKey(groupId, roleName), permission,
						timestamp);
		batchExecute(batch, CassandraService.RETRY_COUNT);
	}

	@Override
	public Set<String> getGroupRolePermissions(UUID groupId, String roleName)
			throws Exception {
		roleName = roleName.toLowerCase();
		return cass.getAllColumnNames(
				cass.getApplicationKeyspace(applicationId),
				ApplicationCF.ENTITY_DICTIONARIES,
				getRolePermissionsKey(groupId, roleName));
	}

	@Override
	public void deleteGroupRole(UUID groupId, String roleName) throws Exception {
		roleName = roleName.toLowerCase();
		removeFromDictionary(groupRef(groupId), DICTIONARY_ROLENAMES, roleName);
		cass.deleteRow(cass.getApplicationKeyspace(applicationId),
				ApplicationCF.ENTITY_DICTIONARIES,
				getIdForGroupIdAndRoleName(groupId, roleName));
	}

	@Override
	public Set<String> getUserRoles(UUID userId) throws Exception {
		return cast(getDictionaryAsSet(userRef(userId), DICTIONARY_ROLENAMES));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Role> getUserRolesWithTitles(UUID userId)
			throws Exception {
		return getRolesWithTitles((Set<String>) cast(getDictionaryAsSet(
				userRef(userId), DICTIONARY_ROLENAMES)));
	}

	@Override
	public void addUserToRole(UUID userId, String roleName) throws Exception {
		roleName = roleName.toLowerCase();
		addToDictionary(userRef(userId), DICTIONARY_ROLENAMES, roleName,
				roleName);
		addToCollection(userRef(userId), COLLECTION_ROLES, roleRef(roleName));
		// addToCollection(roleRef(roleName), COLLECTION_USERS,
		// userRef(userId));
	}

	@Override
	public void removeUserFromRole(UUID userId, String roleName)
			throws Exception {
		roleName = roleName.toLowerCase();
		removeFromDictionary(userRef(userId), DICTIONARY_ROLENAMES, roleName);
		removeFromCollection(userRef(userId), COLLECTION_ROLES,
				roleRef(roleName));
		// removeFromCollection(roleRef(roleName), COLLECTION_USERS,
		// userRef(userId));
	}

	@Override
	public Set<String> getUserPermissions(UUID userId) throws Exception {
		return cast(getDictionaryAsSet(userRef(userId),
				Schema.DICTIONARY_PERMISSIONS));
	}

	@Override
	public void grantUserPermission(UUID userId, String permission)
			throws Exception {
		permission = permission.toLowerCase();
		addToDictionary(userRef(userId), DICTIONARY_PERMISSIONS, permission);
	}

	@Override
	public void revokeUserPermission(UUID userId, String permission)
			throws Exception {
		permission = permission.toLowerCase();
		removeFromDictionary(userRef(userId), DICTIONARY_PERMISSIONS,
				permission);
	}

	@Override
	public Map<String, String> getUserGroupRoles(UUID userId, UUID groupId)
			throws Exception {
        // TODO this never returns anything - write path not invoked
		return cast(getDictionaryAsMap(memberRef(groupId, userId),
				DICTIONARY_ROLENAMES));
	}

	@Override
	public void addUserToGroupRole(UUID userId, UUID groupId, String roleName)
			throws Exception {
		roleName = roleName.toLowerCase();
		EntityRef memberRef = memberRef(groupId, userId);
		EntityRef roleRef = roleRef(groupId, roleName);
		addToDictionary(memberRef, DICTIONARY_ROLENAMES, roleName, roleName);
		addToCollection(memberRef, COLLECTION_ROLES, roleRef);
		addToCollection(roleRef, COLLECTION_USERS, userRef(userId));
	}

	@Override
	public void removeUserFromGroupRole(UUID userId, UUID groupId,
			String roleName) throws Exception {
		roleName = roleName.toLowerCase();
		EntityRef memberRef = memberRef(groupId, userId);
		EntityRef roleRef = roleRef(groupId, roleName);
		removeFromDictionary(memberRef, DICTIONARY_ROLENAMES, roleName);
		removeFromCollection(memberRef, COLLECTION_ROLES, roleRef);
		removeFromCollection(roleRef, COLLECTION_USERS, userRef(userId));
	}

	@Override
	public Results getUsersInGroupRole(UUID groupId, String roleName,
			Results.Level level) throws Exception {
		EntityRef roleRef = roleRef(groupId, roleName);
		return this.getCollection(roleRef, COLLECTION_USERS, null, 10000,
				level, false);
	}

	@Override
	public void incrementAggregateCounters(UUID userId, UUID groupId,
			String category, String counterName, long value) {
		long cassandraTimestamp = cass.createTimestamp();
		incrementAggregateCounters(userId, groupId, category, counterName,
				value, cassandraTimestamp);
	}

	private void incrementAggregateCounters(UUID userId, UUID groupId,
			String category, String counterName, long value,
			long cassandraTimestamp) {
        // TODO short circuit
        if ( !skipAggregateCounters ) {
            Mutator<ByteBuffer> m = createMutator(
                    cass.getApplicationKeyspace(applicationId), be);

            counterUtils.batchIncrementAggregateCounters(m, applicationId, userId,
                    groupId, null, category, counterName, value,
                    cassandraTimestamp / 1000, cassandraTimestamp);

            batchExecute(m, CassandraService.RETRY_COUNT);
        }
	}

	@Override
	public void incrementAggregateCounters(UUID userId, UUID groupId,
			String category, Map<String, Long> counters) {
        // TODO shortcircuit
        if ( !skipAggregateCounters ) {
            long timestamp = cass.createTimestamp();
            Mutator<ByteBuffer> m = createMutator(
                    cass.getApplicationKeyspace(applicationId), be);
            counterUtils.batchIncrementAggregateCounters(m, applicationId, userId,
                    groupId, null, category, counters, timestamp);

            batchExecute(m, CassandraService.RETRY_COUNT);
        }
	}

	@Override
	public Set<String> getCounterNames() throws Exception {
		Set<String> names = new TreeSet<String>(CASE_INSENSITIVE_ORDER);
		Set<String> nameSet = cast(getDictionaryAsSet(getApplicationRef(),
				Schema.DICTIONARY_COUNTERS));
		names.addAll(nameSet);
		return names;
	}

	@Override
	public boolean isPropertyValueUniqueForEntity(String entityType,
			String propertyName, Object propertyValue) throws Exception {
		return isPropertyValueUniqueForEntity(applicationId, entityType,
				propertyName, propertyValue);
	}

	@Override
	public Map<String, Map<UUID, Set<String>>> getOwners(EntityRef entityRef)
			throws Exception {
		return getRelationManager(entityRef).getOwners();
	}

	/* (non-Javadoc)
   * @see org.usergrid.persistence.EntityManager#isOwner(org.usergrid.persistence.EntityRef, org.usergrid.persistence.EntityRef)
   */
  @Override
  public boolean isCollectionMember(EntityRef owner, String collectionName,EntityRef entity) throws Exception {
    return getRelationManager(owner).isCollectionMember(collectionName, entity);
  }
  
  

  /* (non-Javadoc)
   * @see org.usergrid.persistence.EntityManager#isConnectionMember(org.usergrid.persistence.EntityRef, java.lang.String, org.usergrid.persistence.EntityRef)
   */
  @Override
  public boolean isConnectionMember(EntityRef owner, String connectionName, EntityRef entity) throws Exception {
    return getRelationManager(owner).isConnectionMember(connectionName, entity);
  }

  @Override
	public Set<String> getCollections(EntityRef entityRef) throws Exception {
		return getRelationManager(entityRef).getCollections();
	}

	@Override
	public Results getCollection(EntityRef entityRef, String collectionName,
			UUID startResult, int count, Level resultsLevel, boolean reversed)
			throws Exception {
		return getRelationManager(entityRef).getCollection(collectionName,
				startResult, count, resultsLevel, reversed);
	}

	// @Override
	// public Results getCollection(EntityRef entityRef, String collectionName,
	// Map<String, Object> subkeyProperties, UUID startResult, int count,
	// Level resultsLevel, boolean reversed) throws Exception {
	// return getRelationManager(entityRef).getCollection(collectionName,
	// subkeyProperties, startResult, count, resultsLevel, reversed);
	// }

	@Override
	public Results getCollection(UUID entityId, String collectionName,
			Query query, Level resultsLevel) throws Exception {
		return getRelationManager(ref(entityId)).getCollection(collectionName,
				query, resultsLevel);
	}

	@Override
	public Entity addToCollection(EntityRef entityRef, String collectionName,
			EntityRef itemRef) throws Exception {
		return getRelationManager(entityRef).addToCollection(collectionName,
				itemRef);
	}

	@Override
	public Entity addToCollections(List<EntityRef> ownerEntities,
			String collectionName, EntityRef itemRef) throws Exception {
		return getRelationManager(itemRef).addToCollections(ownerEntities,
				collectionName);
	}

	@Override
	public Entity createItemInCollection(EntityRef entityRef,
			String collectionName, String itemType,
			Map<String, Object> properties) throws Exception {
		return getRelationManager(entityRef).createItemInCollection(
				collectionName, itemType, properties);
	}

	@Override
	public void removeFromCollection(EntityRef entityRef,
			String collectionName, EntityRef itemRef) throws Exception {
		getRelationManager(entityRef).removeFromCollection(collectionName,
				itemRef);
	}

	@Override
	public void copyRelationships(EntityRef srcEntityRef,
			String srcRelationName, EntityRef dstEntityRef,
			String dstRelationName) throws Exception {
		getRelationManager(srcEntityRef).copyRelationships(srcRelationName,
				dstEntityRef, dstRelationName);
	}

	@Override
	public Results searchCollection(EntityRef entityRef, String collectionName,
			Query query) throws Exception {
		return getRelationManager(entityRef).searchCollection(collectionName,
				query);
	}

	@Override
	public Set<String> getCollectionIndexes(EntityRef entity,
			String collectionName) throws Exception {
		return getRelationManager(entity).getCollectionIndexes(collectionName);
	}

	@Override
	public ConnectionRef createConnection(ConnectionRef connection)
			throws Exception {
		return getRelationManager(connection).createConnection(connection);
	}

	@Override
	public ConnectionRef createConnection(EntityRef connectingEntity,
			String connectionType, EntityRef connectedEntityRef)
			throws Exception {
		return getRelationManager(connectingEntity).createConnection(
				connectionType, connectedEntityRef);
	}

	@Override
	public ConnectionRef createConnection(EntityRef connectingEntity,
			String pairedConnectionType, EntityRef pairedEntity,
			String connectionType, EntityRef connectedEntityRef)
			throws Exception {
		return getRelationManager(connectingEntity).createConnection(
				pairedConnectionType, pairedEntity, connectionType,
				connectedEntityRef);
	}

	@Override
	public ConnectionRef createConnection(EntityRef connectingEntity,
			ConnectedEntityRef... connections) throws Exception {
		return getRelationManager(connectingEntity).createConnection(
				connections);
	}

	@Override
	public ConnectionRef connectionRef(EntityRef connectingEntity,
			String connectionType, EntityRef connectedEntityRef)
			throws Exception {
		return getRelationManager(connectingEntity).connectionRef(
				connectionType, connectedEntityRef);
	}

	@Override
	public ConnectionRef connectionRef(EntityRef connectingEntity,
			String pairedConnectionType, EntityRef pairedEntity,
			String connectionType, EntityRef connectedEntityRef)
			throws Exception {
		return getRelationManager(connectingEntity).connectionRef(
				pairedConnectionType, pairedEntity, connectionType,
				connectedEntityRef);
	}

	@Override
	public ConnectionRef connectionRef(EntityRef connectingEntity,
			ConnectedEntityRef... connections) {
		return getRelationManager(connectingEntity).connectionRef(connections);
	}

	@Override
	public void deleteConnection(ConnectionRef connectionRef) throws Exception {
		getRelationManager(connectionRef).deleteConnection(connectionRef);
	}

	@Override
	public boolean connectionExists(ConnectionRef connectionRef)
			throws Exception {
		return getRelationManager(connectionRef)
				.connectionExists(connectionRef);
	}

	@Override
	public Set<String> getConnectionTypes(UUID entityId, UUID connectedEntityId)
			throws Exception {
		return getRelationManager(ref(entityId)).getConnectionTypes(
				connectedEntityId);
	}

	@Override
	public Set<String> getConnectionTypes(EntityRef ref) throws Exception {
		return getRelationManager(ref).getConnectionTypes();
	}

	@Override
	public Set<String> getConnectionTypes(EntityRef ref,
			boolean filterConnection) throws Exception {
		return getRelationManager(ref).getConnectionTypes(filterConnection);
	}

	@Override
	public Results getConnectedEntities(UUID entityId, String connectionType,
			String connectedEntityType, Level resultsLevel) throws Exception {	  
		return getRelationManager(ref(entityId)).getConnectedEntities(
				connectionType, connectedEntityType, resultsLevel);
	}

	@Override
	public Results getConnectingEntities(UUID entityId, String connectionType,
			String connectedEntityType, Level resultsLevel) throws Exception {
		return getRelationManager(ref(entityId)).getConnectingEntities(
				connectionType, connectedEntityType, resultsLevel);
	}

	@Override
	public List<ConnectedEntityRef> getConnections(UUID entityId, Query query)
			throws Exception {
		return getRelationManager(ref(entityId)).getConnections(query);
	}

	// T.N. This isn't used anywhere. Removing for this release
	// @Override
	// public Results searchConnectedEntitiesForProperty(
	// EntityRef connectingEntity, String connectionType,
	// String connectedEntityType, String propertyName,
	// Object searchStartValue, Object searchFinishValue,
	// UUID startResult, int count, boolean reversed, Level resultsLevel)
	// throws Exception {
	// return getRelationManager(connectingEntity)
	// .searchConnectedEntitiesForProperty(connectionType,
	// connectedEntityType, propertyName, searchStartValue,
	// searchFinishValue, startResult, count, reversed,
	// resultsLevel);
	// }

	@Override
	public Results searchConnectedEntities(EntityRef connectingEntity,
			Query query) throws Exception {

		// TODO Todd Ed the query type and connection type needs set here.
		return getRelationManager(connectingEntity).searchConnectedEntities(
				query);
	}

	@Override
	public Object getAssociatedProperty(
			AssociatedEntityRef associatedEntityRef, String propertyName)
			throws Exception {
		return getRelationManager(associatedEntityRef).getAssociatedProperty(
				associatedEntityRef, propertyName);
	}

	@Override
	public Map<String, Object> getAssociatedProperties(
			AssociatedEntityRef associatedEntityRef) throws Exception {
		return getRelationManager(associatedEntityRef).getAssociatedProperties(
				associatedEntityRef);
	}

	@Override
	public void setAssociatedProperty(AssociatedEntityRef associatedEntityRef,
			String propertyName, Object propertyValue) throws Exception {
		getRelationManager(associatedEntityRef).setAssociatedProperty(
				associatedEntityRef, propertyName, propertyValue);
	}

	@Override
	public List<ConnectionRef> searchConnections(EntityRef connectingEntity,
			Query query) throws Exception {
		return getRelationManager(connectingEntity).searchConnections(query);
	}

	@Override
	public Set<String> getConnectionIndexes(EntityRef entity,
			String connectionType) throws Exception {
		return getRelationManager(entity).getConnectionIndexes(connectionType);
	}

	@Override
	public void resetRoles() throws Exception {

		try {
			createRole("admin", "Administrator", 0);
		} catch (Exception e) {
			logger.error("Could not create admin role, may already exist", e);
		}

		try {
			createRole("default", "Default", 0);
		} catch (Exception e) {
			logger.error("Could not create default role, may already exist", e);
		}

		try {
			createRole("guest", "Guest", 0);
		} catch (Exception e) {
			logger.error("Could not create guest role, may already exist", e);
		}

		try {
			grantRolePermissions("default",
					Arrays.asList("get,put,post,delete:/**"));
		} catch (Exception e) {
			logger.error("Could not populate default role", e);
		}

		try {
			grantRolePermissions("guest", Arrays.asList("post:/users",
					"post:/devices", "put:/devices/*"));
		} catch (Exception e) {
			logger.error("Could not populate guest role", e);
		}

	}

	/**
	 * @return the applicationId
	 */
	public UUID getApplicationId() {
		return applicationId;
	}

	/**
	 * @return the cass
	 */
	public CassandraService getCass() {
		return cass;
	}

	public GeoIndexManager getGeoIndexManager() {
	    GeoIndexManager gim = new GeoIndexManager();
	    gim.init(this);
	    return gim;
		//return applicationContext.getAutowireCapableBeanFactory()
		//		.createBean(GeoIndexManager.class).init(this);
	}

	/**
	 * @return the indexBucketLocator
	 */
	public IndexBucketLocator getIndexBucketLocator() {
		return indexBucketLocator;
	}


  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Role> getGroupRolesWithTitles(UUID groupId) throws Exception {
    return getRolesWithTitles(
            (Set<String>) cast(getDictionaryAsSet(groupRef(groupId), DICTIONARY_ROLENAMES)));
  }

  @Override
  public void addGroupToRole(UUID groupId, String roleName) throws Exception {
    roleName = roleName.toLowerCase();
    addToDictionary(groupRef(groupId), DICTIONARY_ROLENAMES, roleName, roleName);
    addToCollection(groupRef(groupId), COLLECTION_ROLES, roleRef(roleName));
  }

  @Override
  public void removeGroupFromRole(UUID groupId, String roleName) throws Exception {
    roleName = roleName.toLowerCase();
    removeFromDictionary(groupRef(groupId), DICTIONARY_ROLENAMES, roleName);
    removeFromCollection(groupRef(groupId), COLLECTION_ROLES, roleRef(roleName));
  }

  @Override
  public Set<String> getGroupPermissions(UUID groupId) throws Exception {
    return cast(getDictionaryAsSet(groupRef(groupId), Schema.DICTIONARY_PERMISSIONS));
  }

  @Override
  public void grantGroupPermission(UUID groupId, String permission) throws Exception {
    permission = permission.toLowerCase();
    addToDictionary(groupRef(groupId), DICTIONARY_PERMISSIONS, permission);
  }

  @Override
  public void revokeGroupPermission(UUID groupId, String permission) throws Exception {
    permission = permission.toLowerCase();
    removeFromDictionary(groupRef(groupId), DICTIONARY_PERMISSIONS, permission);
  }

}
