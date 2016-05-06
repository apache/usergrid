/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.collection.serialization.impl;


import java.nio.ByteBuffer;
import java.util.*;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.Session;
import org.apache.cassandra.db.marshal.BytesType;

import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.CassandraConfig;
import org.apache.usergrid.persistence.core.CassandraFig;
import org.apache.usergrid.persistence.core.astyanax.ColumnTypes;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKeySerializer;
import org.apache.usergrid.persistence.core.datastax.CQLUtils;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;


/**
 * V1 impl with unique value serialization strategy with the collection scope
 */
@Singleton
public class UniqueValueSerializationStrategyV2Impl  extends UniqueValueSerializationStrategyImpl<TypeField, Id> {

    private static final String UNIQUE_VALUES_TABLE = CQLUtils.quote("Unique_Values_V2");
    private static final Collection<String> UNIQUE_VALUES_PARTITION_KEYS = Collections.singletonList("key");
    private static final Collection<String> UNIQUE_VALUES_COLUMN_KEYS = Collections.singletonList("column1");
    private static final Map<String, DataType.Name> UNIQUE_VALUES_COLUMNS =
        new HashMap<String, DataType.Name>() {{
            put( "key", DataType.Name.BLOB );
            put( "column1", DataType.Name.BLOB );
            put( "value", DataType.Name.BLOB ); }};
    private static final Map<String, String> UNIQUE_VALUES_CLUSTERING_ORDER =
        new HashMap<String, String>(){{ put( "column1", "ASC" ); }};


    private static final String UNIQUE_VALUES_LOG_TABLE = CQLUtils.quote("Entity_Unique_Values_V2");
    private static final Collection<String> UNIQUE_VALUES_LOG_PARTITION_KEYS = Collections.singletonList("key");
    private static final Collection<String> UNIQUE_VALUES_LOG_COLUMN_KEYS = Collections.singletonList("column1");
    private static final Map<String, DataType.Name> UNIQUE_VALUES_LOG_COLUMNS =
        new HashMap<String, DataType.Name>() {{
            put( "key", DataType.Name.BLOB );
            put( "column1", DataType.Name.BLOB );
            put( "value", DataType.Name.BLOB ); }};
    private static final Map<String, String> UNIQUE_VALUES_LOG_CLUSTERING_ORDER =
        new HashMap<String, String>(){{ put( "column1", "ASC" ); }};

    private final static TableDefinition uniqueValues =
        new TableDefinition( UNIQUE_VALUES_TABLE, UNIQUE_VALUES_PARTITION_KEYS, UNIQUE_VALUES_COLUMN_KEYS,
            UNIQUE_VALUES_COLUMNS, TableDefinition.CacheOption.KEYS, UNIQUE_VALUES_CLUSTERING_ORDER);

    private final static TableDefinition uniqueValuesLog =
        new TableDefinition( UNIQUE_VALUES_LOG_TABLE, UNIQUE_VALUES_LOG_PARTITION_KEYS, UNIQUE_VALUES_LOG_COLUMN_KEYS,
            UNIQUE_VALUES_LOG_COLUMNS, TableDefinition.CacheOption.KEYS, UNIQUE_VALUES_LOG_CLUSTERING_ORDER);


    private static final ScopedRowKeySerializer<TypeField>  ROW_KEY_SER = new ScopedRowKeySerializer<>( UniqueTypeFieldRowKeySerializer.get() );


    private static final EntityVersionSerializer ENTITY_VERSION_SER = new EntityVersionSerializer();

    private static final MultiTenantColumnFamily<ScopedRowKey<TypeField>, EntityVersion>
        CF_UNIQUE_VALUES = new MultiTenantColumnFamily<>( "Unique_Values_V2", ROW_KEY_SER, ENTITY_VERSION_SER );


    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


    private static final ScopedRowKeySerializer<Id> ENTITY_ROW_KEY_SER =
        new ScopedRowKeySerializer<>( ID_SER );


    private static final MultiTenantColumnFamily<ScopedRowKey<Id>, UniqueFieldEntry>
        CF_ENTITY_UNIQUE_VALUE_LOG =
        new MultiTenantColumnFamily<>( "Entity_Unique_Values_V2", ENTITY_ROW_KEY_SER, UniqueFieldEntrySerializer.get() );


    /**
     * Construct serialization strategy for keyspace.
     *
     * @param keyspace Keyspace in which to store Unique Values.
     * @param cassandraFig The cassandra configuration
     * @param serializationFig The serialization configuration
     */
    @Inject
    public UniqueValueSerializationStrategyV2Impl( final Keyspace keyspace, final CassandraFig cassandraFig,
                                                   final SerializationFig serializationFig,
                                                   final Session session,
                                                   final CassandraConfig cassandraConfig) {
        super( keyspace, cassandraFig, serializationFig, session, cassandraConfig );
    }


    @Override
    public Collection<MultiTenantColumnFamilyDefinition> getColumnFamilies() {

        final MultiTenantColumnFamilyDefinition uniqueLookupCF =
            new MultiTenantColumnFamilyDefinition( CF_UNIQUE_VALUES, BytesType.class.getSimpleName(),
                ColumnTypes.DYNAMIC_COMPOSITE_TYPE, BytesType.class.getSimpleName(),
                MultiTenantColumnFamilyDefinition.CacheOption.KEYS );

        final MultiTenantColumnFamilyDefinition uniqueLogCF =
            new MultiTenantColumnFamilyDefinition( CF_ENTITY_UNIQUE_VALUE_LOG, BytesType.class.getSimpleName(),
                ColumnTypes.DYNAMIC_COMPOSITE_TYPE, BytesType.class.getSimpleName(),
                MultiTenantColumnFamilyDefinition.CacheOption.KEYS );

        return Arrays.asList( uniqueLookupCF, uniqueLogCF );
    }

    @Override
    public Collection<TableDefinition> getTables() {


        return Arrays.asList( uniqueValues, uniqueValuesLog );

    }

    @Override
    protected MultiTenantColumnFamily<ScopedRowKey<TypeField>, EntityVersion> getUniqueValuesCF() {
        return CF_UNIQUE_VALUES;
    }


    @Override
    protected TableDefinition getUniqueValuesTable(){
        return uniqueValues;
    }


    @Override
    protected MultiTenantColumnFamily<ScopedRowKey<Id>, UniqueFieldEntry>
    getEntityUniqueLogCF() {
        return CF_ENTITY_UNIQUE_VALUE_LOG;
    }


    @Override
    protected TableDefinition getEntityUniqueLogTable(){
        return uniqueValuesLog;
    }



    @Override
    protected TypeField createUniqueValueKey( final Id applicationId,  final String type, final Field field) {
        return new TypeField(type,field);
    }


    @Override
    protected Field parseRowKey( final ScopedRowKey<TypeField> rowKey ) {
        return rowKey.getKey().getField();
    }

    @Override
    protected List<Object> deserializePartitionKey(ByteBuffer bb){


        /**
         *  List<Object> keys = new ArrayList<>(6);
         keys.add(0, appUUID); // UUID
         keys.add(1, applicationType); // String
         keys.add(2, entityType); // String
         keys.add(3, fieldType); // String
         keys.add(4, fieldName); // String
         keys.add(5, fieldValueString); // String

         */

        List<Object> stuff = new ArrayList<>();
        while(bb.hasRemaining()){
            ByteBuffer data = CQLUtils.getWithShortLength(bb);
            if(stuff.size() == 0){
                stuff.add(DataType.uuid().deserialize(data.slice(), ProtocolVersion.NEWEST_SUPPORTED));
            }else{
                stuff.add(DataType.text().deserialize(data.slice(), ProtocolVersion.NEWEST_SUPPORTED));
            }
            byte equality = bb.get(); // we don't use this but take the equality byte off the buffer

        }

        return stuff;

    }

    @Override
    protected Object serializeUniqueValueLogColumn(UniqueFieldEntry fieldEntry){

        /**
         *         final UUID version = value.getVersion();
         final Field<?> field = value.getField();

         final FieldTypeName fieldType = field.getTypeName();
         final String fieldValue = field.getValue().toString().toLowerCase();


         DynamicComposite composite = new DynamicComposite(  );

         //we want to sort ascending to descending by version
         composite.addComponent( version,  UUID_SERIALIZER, ColumnTypes.UUID_TYPE_REVERSED);
         composite.addComponent( field.getName(), STRING_SERIALIZER );
         composite.addComponent( fieldValue, STRING_SERIALIZER );
         composite.addComponent( fieldType.name() , STRING_SERIALIZER);
         */

        // values are serialized as strings, not sure why, and always lower cased
        String fieldValueString = fieldEntry.getField().getValue().toString().toLowerCase();


        List<Object> keys = new ArrayList<>(4);
        keys.add(fieldEntry.getVersion());
        keys.add(fieldEntry.getField().getName());
        keys.add(fieldValueString);
        keys.add(fieldEntry.getField().getTypeName().name());

        String comparator = UUID_TYPE_REVERSED;

        int size = 16+fieldEntry.getField().getName().length()+fieldEntry.getField().getValue().toString().length()+
            fieldEntry.getField().getTypeName().name().length();

        // we always need to add length for the 2 byte comparator short,  2 byte length short and 1 byte equality
        size += keys.size()*65;

        // uuid type comparator is longest, ensure we allocate buffer using the max size to avoid overflow
        size += keys.size()*comparator.length();

        ByteBuffer stuff = ByteBuffer.allocate(size);


        for (Object key : keys) {

            if(key.equals(fieldEntry.getVersion())) {
                int p = comparator.indexOf("(reversed=true)");
                boolean desc = false;
                if (p >= 0) {
                    comparator = comparator.substring(0, p);
                    desc = true;
                }

                byte a = (byte) 85; // this is the byte value for UUIDType in astyanax used in legacy data
                if (desc) {
                    a = (byte) Character.toUpperCase((char) a);
                }

                stuff.putShort((short) ('耀' | a));
            }else{
                comparator = "UTF8Type"; // only strings are being serialized other than UUIDs here
                stuff.putShort((short)comparator.length());
                stuff.put(DataType.serializeValue(comparator, ProtocolVersion.NEWEST_SUPPORTED));
            }

            ByteBuffer kb = DataType.serializeValue(key, ProtocolVersion.NEWEST_SUPPORTED);
            if (kb == null) {
                kb = ByteBuffer.allocate(0);
            }

            // put a short that indicates how big the buffer is for this item
            stuff.putShort((short) kb.remaining());

            // put the actual item
            stuff.put(kb.slice());

            // put an equality byte ( again not used by part of legacy thrift Astyanax schema)
            stuff.put((byte) 0);


        }

        stuff.flip();
        return stuff.duplicate();

    }

    @Override
    protected ByteBuffer getPartitionKey(Id applicationId, String entityType, String fieldType, String fieldName, Object fieldValue ){

        return serializeKey(applicationId.getUuid(), applicationId.getType(),
            entityType, fieldType, fieldName, fieldValue);

    }

    @Override
    protected ByteBuffer getLogPartitionKey(final Id applicationId, final Id uniqueValueId){

        return serializeLogKey(applicationId.getUuid(), applicationId.getType(),
            uniqueValueId.getUuid(), uniqueValueId.getType());

    }

    @Override
    protected ByteBuffer serializeUniqueValueColumn(EntityVersion entityVersion){

        /**
         *         final Id entityId = ev.getEntityId();
         final UUID entityUuid = entityId.getUuid();
         final String entityType = entityId.getType();

         CompositeBuilder builder = Composites.newDynamicCompositeBuilder();

         builder.addUUID( entityVersion );
         builder.addUUID( entityUuid );
         builder.addString(entityType );
         */

        String comparator = "UTF8Type";

        List<Object> keys = new ArrayList<>(3);
        keys.add(entityVersion.getEntityVersion());
        keys.add(entityVersion.getEntityId().getUuid());
        keys.add(entityVersion.getEntityId().getType());

        // UUIDs are 16 bytes
        int size = 16+16+entityVersion.getEntityId().getType().length();

        // we always need to add length for the 2 byte comparator short,  2 byte length short and 1 byte equality
        size += keys.size()*5;

        // we always add comparator to the buffer as well
        size += keys.size()*comparator.length();

        ByteBuffer stuff = ByteBuffer.allocate(size);

        for (Object key : keys) {

            if(key instanceof UUID){
                comparator = "UUIDType";
            }else{
                comparator = "UTF8Type"; // if it's not a UUID, the only other thing we're serializing is text
            }

            stuff.putShort((short)comparator.length());
            stuff.put(DataType.serializeValue(comparator, ProtocolVersion.NEWEST_SUPPORTED));

            ByteBuffer kb = DataType.serializeValue(key, ProtocolVersion.NEWEST_SUPPORTED);
            if (kb == null) {
                kb = ByteBuffer.allocate(0);
            }

            // put a short that indicates how big the buffer is for this item
            stuff.putShort((short) kb.remaining());

            // put the actual item
            stuff.put(kb.slice());

            // put an equality byte ( again not used by part of legacy thrift Astyanax schema)
            stuff.put((byte) 0);


        }

        stuff.flip();
        return stuff.duplicate();

    }

    @Override
    protected List<Object> deserializeUniqueValueColumn(ByteBuffer bb){

        List<Object> stuff = new ArrayList<>();
        int count = 0;
        while(bb.hasRemaining()){

            // custom columns have a short at beginning for comparator (which we don't use here )
            ByteBuffer comparator = CQLUtils.getWithShortLength(bb);

            ByteBuffer data = CQLUtils.getWithShortLength(bb);


            // first two composites are UUIDs, rest are strings
            if(count == 0) {
                stuff.add(new UUID(data.getLong(), data.getLong()));
            }else if(count ==1){
                stuff.add(new UUID(data.getLong(), data.getLong()));
            }else{
                stuff.add(DataType.text().deserialize(data.duplicate(), ProtocolVersion.NEWEST_SUPPORTED));
            }

            byte equality = bb.get(); // we don't use this but take the equality byte off the buffer

            count++;
        }

        return stuff;

    }


    @Override
    protected Id createEntityUniqueLogKey( final Id applicationId, final Id uniqueValueId ) {
       return uniqueValueId;
    }


    @Override
    public int getImplementationVersion() {
        return CollectionDataVersions.LOG_REMOVAL.getVersion();
    }



    // row key = app UUID + app type + app UUID + app type + field type + field name + field value
    private ByteBuffer serializeKey(UUID appUUID,
                                    String applicationType,
                                    String entityType,
                                    String fieldType,
                                    String fieldName,
                                    Object fieldValue  ){

        // values are serialized as strings, not sure why, and always lower cased
        String fieldValueString = fieldValue.toString().toLowerCase();

        List<Object> keys = new ArrayList<>(6);
        keys.add(0, appUUID);
        keys.add(1, applicationType);
        keys.add(2, entityType);
        keys.add(3, fieldType);
        keys.add(4, fieldName);
        keys.add(5, fieldValueString);


        // UUIDs are 16 bytes, allocate the buffer accordingly
        int size = 16 + applicationType.length() + entityType.length() + fieldType.length() + fieldName.length()+fieldValueString.length();


        // we always need to add length for the 2 byte short and 1 byte equality
        size += keys.size()*3;

        ByteBuffer stuff = ByteBuffer.allocate(size);

        for (Object key : keys) {

            ByteBuffer kb = DataType.serializeValue(key, ProtocolVersion.NEWEST_SUPPORTED);
            if (kb == null) {
                kb = ByteBuffer.allocate(0);
            }

            stuff.putShort((short) kb.remaining());
            stuff.put(kb.slice());
            stuff.put((byte) 0);


        }
        stuff.flip();
        return stuff.duplicate();

    }

    private ByteBuffer serializeLogKey(UUID appUUID, String applicationType, UUID entityId, String entityType){

        List<Object> keys = new ArrayList<>(4);
        keys.add(appUUID);
        keys.add(applicationType);
        keys.add(entityId);
        keys.add(entityType);

        int size = 16+applicationType.length()+16+entityType.length();

        // we always need to add length for the 2 byte short and 1 byte equality
        size += keys.size()*3;

        ByteBuffer stuff = ByteBuffer.allocate(size);

        for (Object key : keys) {

            ByteBuffer kb = DataType.serializeValue(key, ProtocolVersion.NEWEST_SUPPORTED);
            if (kb == null) {
                kb = ByteBuffer.allocate(0);
            }

            stuff.putShort((short) kb.remaining());
            stuff.put(kb.slice());
            stuff.put((byte) 0);


        }
        stuff.flip();
        return stuff.duplicate();

    }

}
