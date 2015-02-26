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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.locking.LockManager;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.IndexBucketLocator.IndexType;
import org.apache.usergrid.persistence.cassandra.index.IndexBucketScanner;
import org.apache.usergrid.persistence.cassandra.index.IndexScanner;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.hector.CountingMutator;
import org.apache.usergrid.utils.MapUtils;

import com.google.inject.Injector;

import me.prettyprint.cassandra.connection.HConnectionManager;
import me.prettyprint.cassandra.model.ConfigurableConsistencyLevel;
import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.DynamicCompositeSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.ConsistencyLevelPolicy;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.MultigetSliceQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import me.prettyprint.hector.api.query.SliceQuery;

import static me.prettyprint.cassandra.service.FailoverPolicy.ON_FAIL_TRY_ALL_AVAILABLE;
import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static me.prettyprint.hector.api.factory.HFactory.createMultigetSliceQuery;

import static me.prettyprint.hector.api.factory.HFactory.createRangeSlicesQuery;
import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;
import static me.prettyprint.hector.api.factory.HFactory.createVirtualKeyspace;
import static org.apache.commons.collections.MapUtils.getIntValue;
import static org.apache.commons.collections.MapUtils.getString;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_ID_SETS;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.batchExecute;
import static org.apache.usergrid.utils.ConversionUtils.bytebuffer;
import static org.apache.usergrid.utils.ConversionUtils.bytebuffers;
import static org.apache.usergrid.utils.JsonUtils.mapToFormattedJsonString;
import static org.apache.usergrid.utils.MapUtils.asMap;
import static org.apache.usergrid.utils.MapUtils.filter;


public class CassandraService {

    //make the below two not static
   // public static String SYSTEM_KEYSPACE = "Usergrid";

    public static String applicationKeyspace;

    public static final String APPLICATIONS_CF = "Applications";
    public static final String PROPERTIES_CF = "Properties";
    public static final String TOKENS_CF = "Tokens";
    public static final String PRINCIPAL_TOKEN_CF = "PrincipalTokens";

    public static final int DEFAULT_COUNT = 1000;
    public static final int ALL_COUNT = 100000;
    public static final int INDEX_ENTRY_LIST_COUNT = 1000;
    public static final int DEFAULT_SEARCH_COUNT = 10000;

    public static final int RETRY_COUNT = 5;

    public static final String DEFAULT_APPLICATION = "default-app";
    public static final String DEFAULT_ORGANIZATION = "usergrid";
    public static final String MANAGEMENT_APPLICATION = "management";

    private static final Logger logger = LoggerFactory.getLogger( CassandraService.class );

    private static final Logger db_logger =
            LoggerFactory.getLogger( CassandraService.class.getPackage().getName() + ".DB" );

    Cluster cluster;
    CassandraHostConfigurator chc;
    Properties properties;
    LockManager lockManager;

    ConsistencyLevelPolicy consistencyLevelPolicy;

    private Keyspace systemKeyspace;

    private Map<String, String> accessMap;

    public static final StringSerializer se = new StringSerializer();
    public static final ByteBufferSerializer be = new ByteBufferSerializer();
    public static final UUIDSerializer ue = new UUIDSerializer();
    public static final BytesArraySerializer bae = new BytesArraySerializer();
    public static final DynamicCompositeSerializer dce = new DynamicCompositeSerializer();
    public static final LongSerializer le = new LongSerializer();

    public static final UUID NULL_ID = new UUID( 0, 0 );

//Wire guice injector via spring here, just pass the injector in the spring
    public CassandraService( Properties properties, Cluster cluster,
                             CassandraHostConfigurator cassandraHostConfigurator, LockManager lockManager,
                           final Injector injector) {
        this.properties = properties;
        this.cluster = cluster;
        chc = cassandraHostConfigurator;
        this.lockManager = lockManager;
        db_logger.info( "" + cluster.getKnownPoolHosts( false ) );
        //getInjector
        applicationKeyspace  = injector.getInstance( CassandraFig.class ).getApplicationKeyspace();
    }


    public void init() throws Exception {
        if ( consistencyLevelPolicy == null ) {
            consistencyLevelPolicy = new ConfigurableConsistencyLevel();
            ( ( ConfigurableConsistencyLevel ) consistencyLevelPolicy )
                    .setDefaultReadConsistencyLevel( HConsistencyLevel.ONE );
        }
        accessMap = new HashMap<String, String>( 2 );
        accessMap.put( "username", properties.getProperty( "cassandra.username" ) );
        accessMap.put( "password", properties.getProperty( "cassandra.password" ) );
        systemKeyspace =
                HFactory.createKeyspace( getApplicationKeyspace() , cluster, consistencyLevelPolicy, ON_FAIL_TRY_ALL_AVAILABLE,
                        accessMap );


        final int flushSize = getIntValue( properties, "cassandra.mutation.flushsize", 2000 );
        CountingMutator.MAX_SIZE = flushSize;


    }

    public static String getApplicationKeyspace() {
        return applicationKeyspace;
    }

    public Cluster getCluster() {
        return cluster;
    }


    public void setCluster( Cluster cluster ) {
        this.cluster = cluster;
    }


    public CassandraHostConfigurator getCassandraHostConfigurator() {
        return chc;
    }


    public void setCassandraHostConfigurator( CassandraHostConfigurator chc ) {
        this.chc = chc;
    }


    public Properties getProperties() {
        return properties;
    }


    public void setProperties( Properties properties ) {
        this.properties = properties;
    }


    public Map<String, String> getPropertiesMap() {
        if ( properties != null ) {
            return asMap( properties );
        }
        return null;
    }


    public LockManager getLockManager() {
        return lockManager;
    }


    public void setLockManager( LockManager lockManager ) {
        this.lockManager = lockManager;
    }


    public ConsistencyLevelPolicy getConsistencyLevelPolicy() {
        return consistencyLevelPolicy;
    }


    public void setConsistencyLevelPolicy( ConsistencyLevelPolicy consistencyLevelPolicy ) {
        this.consistencyLevelPolicy = consistencyLevelPolicy;
    }


    /** @return keyspace for application UUID */
    public static String keyspaceForApplication( UUID applicationId ) {
            return getApplicationKeyspace();
    }


    public static UUID prefixForApplication( UUID applicationId ) {
            return applicationId;
    }


    public Keyspace getKeyspace( String keyspace, UUID prefix ) {
        Keyspace ko = null;
        if ( ( prefix != null ) ) {
            ko = createVirtualKeyspace( keyspace, prefix, ue, cluster, consistencyLevelPolicy,
                    ON_FAIL_TRY_ALL_AVAILABLE, accessMap );
        }
        else {
            ko = HFactory.createKeyspace( keyspace, cluster, consistencyLevelPolicy, ON_FAIL_TRY_ALL_AVAILABLE,
                    accessMap );
        }
        return ko;
    }


    public Keyspace getApplicationKeyspace( UUID applicationId ) {
        assert applicationId != null;
        Keyspace ko = getKeyspace( keyspaceForApplication( applicationId ), prefixForApplication( applicationId ) );
        return ko;
    }


    /** The Usergrid_Applications keyspace directly */
    public Keyspace getUsergridApplicationKeyspace() {
        return getKeyspace( getApplicationKeyspace(),  null );
    }


    public boolean checkKeyspacesExist() {
        boolean exists = false;
        try {
            exists = cluster.describeKeyspace( getApplicationKeyspace() ) != null;

        }
        catch ( Exception ex ) {
            logger.error( "could not describe keyspaces", ex );
        }
        return exists;
    }


    /**
     * Lazy creates a column family in the keyspace. If it doesn't exist, it will be created, then the call will sleep
     * until all nodes have acknowledged the schema change
     */
    public void createColumnFamily( String keyspace, ColumnFamilyDefinition cfDef ) {

        if ( !keySpaceExists( keyspace ) ) {
            createKeySpace( keyspace );
        }


        //add the cf

        if ( !cfExists( keyspace, cfDef.getName() ) ) {

            //default read repair chance to 0.1
            cfDef.setReadRepairChance( 0.1d );
            cfDef.setCompactionStrategy( "LeveledCompactionStrategy" );
            cfDef.setCompactionStrategyOptions( new MapUtils.HashMapBuilder().map("sstable_size_in_mb", "512"  ) );

            cluster.addColumnFamily( cfDef, true );
            logger.info( "Created column family {} in keyspace {}", cfDef.getName(), keyspace );
        }
    }


    /** Create the column families in the list */
    public void createColumnFamilies( String keyspace, List<ColumnFamilyDefinition> cfDefs ) {
        for ( ColumnFamilyDefinition cfDef : cfDefs ) {
            createColumnFamily( keyspace, cfDef );
        }
    }


    /** Check if the keyspace exsts */
    public boolean keySpaceExists( String keyspace ) {
        KeyspaceDefinition ksDef = cluster.describeKeyspace( keyspace );

        return ksDef != null;
    }


    /** Create the keyspace */
    private void createKeySpace( String keyspace ) {
        logger.info( "Creating keyspace: {}", keyspace );

        String strategy_class =
                getString( properties, "cassandra.keyspace.strategy", "org.apache.cassandra.locator.SimpleStrategy" );
        logger.info( "Using strategy: {}", strategy_class );

        int replication_factor = getIntValue( properties, "cassandra.keyspace.replication", 1 );
        logger.info( "Using replication (may be overriden by strategy options): {}", replication_factor );

        // try {
        ThriftKsDef ks_def = ( ThriftKsDef ) HFactory
                .createKeyspaceDefinition( keyspace, strategy_class, replication_factor,
                        new ArrayList<ColumnFamilyDefinition>() );

        @SuppressWarnings({ "unchecked", "rawtypes" }) Map<String, String> strategy_options =
                filter( ( Map ) properties, "cassandra.keyspace.strategy.options.", true );
        if ( strategy_options.size() > 0 ) {
            logger.info( "Strategy options: {}", mapToFormattedJsonString( strategy_options ) );
            ks_def.setStrategyOptions( strategy_options );
        }

        cluster.addKeyspace( ks_def );

        waitForCreation( keyspace );

        logger.info( "Created keyspace {}", keyspace );
    }


    /** Wait until all nodes agree on the same schema version */
    private void waitForCreation( String keyspace ) {

        while ( true ) {
            Map<String, List<String>> versions = cluster.describeSchemaVersions();
            // only 1 version, return
            if ( versions != null && versions.size() == 1 ) {
                return;
            }
            // sleep and try again
            try {
                Thread.sleep( 100 );
            }
            catch ( InterruptedException e ) {
            }
        }
    }


    /** Return true if the column family exists */
    public boolean cfExists( String keyspace, String cfName ) {
        KeyspaceDefinition ksDef = cluster.describeKeyspace( keyspace );

        if ( ksDef == null ) {
            return false;
        }

        for ( ColumnFamilyDefinition cf : ksDef.getCfDefs() ) {
            if ( cfName.equals( cf.getName() ) ) {
                return true;
            }
        }

        return false;
    }


    /**
     * Gets the columns.
     *
     * @param ko the keyspace
     * @param columnFamily the column family
     * @param key the key
     *
     * @return columns
     *
     * @throws Exception the exception
     */
    public <N, V> List<HColumn<N, V>> getAllColumns( Keyspace ko, Object columnFamily, Object key,
                                                     Serializer<N> nameSerializer, Serializer<V> valueSerializer )
            throws Exception {

        if ( db_logger.isInfoEnabled() ) {
            db_logger.info( "getColumns cf={} key={}", columnFamily, key );
        }

        SliceQuery<ByteBuffer, N, V> q = createSliceQuery( ko, be, nameSerializer, valueSerializer );
        q.setColumnFamily( columnFamily.toString() );
        q.setKey( bytebuffer( key ) );
        q.setRange( null, null, false, ALL_COUNT );
        QueryResult<ColumnSlice<N, V>> r = q.execute();
        ColumnSlice<N, V> slice = r.get();
        List<HColumn<N, V>> results = slice.getColumns();

        if ( db_logger.isInfoEnabled() ) {
            if ( results == null ) {
                db_logger.info( "getColumns returned null" );
            }
            else {
                db_logger.info( "getColumns returned {} columns", results.size() );
            }
        }

        return results;
    }


    public List<HColumn<String, ByteBuffer>> getAllColumns( Keyspace ko, Object columnFamily, Object key )
            throws Exception {
        return getAllColumns( ko, columnFamily, key, se, be );
    }


    public Set<String> getAllColumnNames( Keyspace ko, Object columnFamily, Object key ) throws Exception {
        List<HColumn<String, ByteBuffer>> columns = getAllColumns( ko, columnFamily, key );
        Set<String> set = new LinkedHashSet<String>();
        for ( HColumn<String, ByteBuffer> column : columns ) {
            set.add( column.getName() );
        }
        return set;
    }


    /**
     * Gets the columns.
     *
     * @param ko the keyspace
     * @param columnFamily the column family
     * @param key the key
     * @param start the start
     * @param finish the finish
     * @param count the count
     * @param reversed the reversed
     *
     * @return columns
     *
     * @throws Exception the exception
     */
    public List<HColumn<ByteBuffer, ByteBuffer>> getColumns( Keyspace ko, Object columnFamily, Object key, Object start,
                                                             Object finish, int count, boolean reversed )
            throws Exception {

        if ( db_logger.isDebugEnabled() ) {
            db_logger.debug( "getColumns cf=" + columnFamily + " key=" + key + " start=" + start + " finish=" + finish
                    + " count=" + count + " reversed=" + reversed );
        }

        SliceQuery<ByteBuffer, ByteBuffer, ByteBuffer> q = createSliceQuery( ko, be, be, be );
        q.setColumnFamily( columnFamily.toString() );
        q.setKey( bytebuffer( key ) );

        ByteBuffer start_bytes = null;
        if ( start instanceof DynamicComposite ) {
            start_bytes = ( ( DynamicComposite ) start ).serialize();
        }
        else if ( start instanceof List ) {
            start_bytes = DynamicComposite.toByteBuffer( ( List<?> ) start );
        }
        else {
            start_bytes = bytebuffer( start );
        }

        ByteBuffer finish_bytes = null;
        if ( finish instanceof DynamicComposite ) {
            finish_bytes = ( ( DynamicComposite ) finish ).serialize();
        }
        else if ( finish instanceof List ) {
            finish_bytes = DynamicComposite.toByteBuffer( ( List<?> ) finish );
        }
        else {
            finish_bytes = bytebuffer( finish );
        }

    /*
     * if (reversed) { q.setRange(finish_bytes, start_bytes, reversed, count); }
     * else { q.setRange(start_bytes, finish_bytes, reversed, count); }
     */
        q.setRange( start_bytes, finish_bytes, reversed, count );
        QueryResult<ColumnSlice<ByteBuffer, ByteBuffer>> r = q.execute();
        ColumnSlice<ByteBuffer, ByteBuffer> slice = r.get();
        List<HColumn<ByteBuffer, ByteBuffer>> results = slice.getColumns();

        if ( db_logger.isDebugEnabled() ) {
            if ( results == null ) {
                db_logger.debug( "getColumns returned null" );
            }
            else {
                db_logger.debug( "getColumns returned " + results.size() + " columns" );
            }
        }

        return results;
    }


    public Map<ByteBuffer, List<HColumn<ByteBuffer, ByteBuffer>>> multiGetColumns( Keyspace ko, Object columnFamily,
                                                                                   List<?> keys, Object start,
                                                                                   Object finish, int count,
                                                                                   boolean reversed ) throws Exception {

        if ( db_logger.isDebugEnabled() ) {
            db_logger.debug( "multiGetColumns cf=" + columnFamily + " keys=" + keys + " start=" + start + " finish="
                    + finish + " count=" + count + " reversed=" + reversed );
        }

        MultigetSliceQuery<ByteBuffer, ByteBuffer, ByteBuffer> q = createMultigetSliceQuery( ko, be, be, be );
        q.setColumnFamily( columnFamily.toString() );
        q.setKeys( bytebuffers( keys ) );

        ByteBuffer start_bytes = null;
        if ( start instanceof DynamicComposite ) {
            start_bytes = ( ( DynamicComposite ) start ).serialize();
        }
        else if ( start instanceof List ) {
            start_bytes = DynamicComposite.toByteBuffer( ( List<?> ) start );
        }
        else {
            start_bytes = bytebuffer( start );
        }

        ByteBuffer finish_bytes = null;
        if ( finish instanceof DynamicComposite ) {
            finish_bytes = ( ( DynamicComposite ) finish ).serialize();
        }
        else if ( finish instanceof List ) {
            finish_bytes = DynamicComposite.toByteBuffer( ( List<?> ) finish );
        }
        else {
            finish_bytes = bytebuffer( finish );
        }

        q.setRange( start_bytes, finish_bytes, reversed, count );
        QueryResult<Rows<ByteBuffer, ByteBuffer, ByteBuffer>> r = q.execute();
        Rows<ByteBuffer, ByteBuffer, ByteBuffer> rows = r.get();

        Map<ByteBuffer, List<HColumn<ByteBuffer, ByteBuffer>>> results =
                new LinkedHashMap<ByteBuffer, List<HColumn<ByteBuffer, ByteBuffer>>>();
        for ( Row<ByteBuffer, ByteBuffer, ByteBuffer> row : rows ) {
            results.put( row.getKey(), row.getColumnSlice().getColumns() );
        }

        return results;
    }


    /**
     * Gets the columns.
     *
     * @param ko the keyspace
     * @param columnFamily the column family
     * @param keys the keys
     *
     * @return map of keys to columns
     *
     * @throws Exception the exception
     */
    public <K, N, V> Rows<K, N, V> getRows( Keyspace ko, Object columnFamily, Collection<K> keys,
                                            Serializer<K> keySerializer, Serializer<N> nameSerializer,
                                            Serializer<V> valueSerializer ) throws Exception {

        if ( db_logger.isDebugEnabled() ) {
            db_logger.debug( "getColumns cf=" + columnFamily + " keys=" + keys );
        }

        MultigetSliceQuery<K, N, V> q = createMultigetSliceQuery( ko, keySerializer, nameSerializer, valueSerializer );
        q.setColumnFamily( columnFamily.toString() );
        q.setKeys( keys );
        q.setRange( null, null, false, ALL_COUNT );
        QueryResult<Rows<K, N, V>> r = q.execute();
        Rows<K, N, V> results = r.get();

        if ( db_logger.isInfoEnabled() ) {
            if ( results == null ) {
                db_logger.info( "getColumns returned null" );
            }
            else {
                db_logger.info( "getColumns returned " + results.getCount() + " columns" );
            }
        }

        return results;
    }


    /**
     * Gets the columns.
     *
     * @param ko the keyspace
     * @param columnFamily the column family
     * @param key the key
     * @param columnNames the column names
     *
     * @return columns
     *
     * @throws Exception the exception
     */
    @SuppressWarnings("unchecked")
    public <N, V> List<HColumn<N, V>> getColumns( Keyspace ko, Object columnFamily, Object key, Set<String> columnNames,
                                                  Serializer<N> nameSerializer, Serializer<V> valueSerializer )
            throws Exception {

        if ( db_logger.isDebugEnabled() ) {
            db_logger.debug( "getColumns cf=" + columnFamily + " key=" + key + " names=" + columnNames );
        }

        SliceQuery<ByteBuffer, N, V> q = createSliceQuery( ko, be, nameSerializer, valueSerializer );
        q.setColumnFamily( columnFamily.toString() );
        q.setKey( bytebuffer( key ) );
        // q.setColumnNames(columnNames.toArray(new String[0]));
        q.setColumnNames( ( N[] ) nameSerializer.fromBytesSet( se.toBytesSet( new ArrayList<String>( columnNames ) ) )
                                                .toArray() );

        QueryResult<ColumnSlice<N, V>> r = q.execute();
        ColumnSlice<N, V> slice = r.get();
        List<HColumn<N, V>> results = slice.getColumns();

        if ( db_logger.isInfoEnabled() ) {
            if ( results == null ) {
                db_logger.info( "getColumns returned null" );
            }
            else {
                db_logger.info( "getColumns returned " + results.size() + " columns" );
            }
        }

        return results;
    }


    /**
     * Gets the columns.
     *
     * @param ko the keyspace
     * @param columnFamily the column family
     * @param keys the keys
     * @param columnNames the column names
     *
     * @return map of keys to columns
     *
     * @throws Exception the exception
     */
    @SuppressWarnings("unchecked")
    public <K, N, V> Rows<K, N, V> getRows( Keyspace ko, Object columnFamily, Collection<K> keys,
                                            Collection<String> columnNames, Serializer<K> keySerializer,
                                            Serializer<N> nameSerializer, Serializer<V> valueSerializer )
            throws Exception {

        if ( db_logger.isDebugEnabled() ) {
            db_logger.debug( "getColumns cf=" + columnFamily + " keys=" + keys + " names=" + columnNames );
        }

        MultigetSliceQuery<K, N, V> q = createMultigetSliceQuery( ko, keySerializer, nameSerializer, valueSerializer );
        q.setColumnFamily( columnFamily.toString() );
        q.setKeys( keys );
        q.setColumnNames( ( N[] ) nameSerializer.fromBytesSet( se.toBytesSet( new ArrayList<String>( columnNames ) ) )
                                                .toArray() );
        QueryResult<Rows<K, N, V>> r = q.execute();
        Rows<K, N, V> results = r.get();

        if ( db_logger.isInfoEnabled() ) {
            if ( results == null ) {
                db_logger.info( "getColumns returned null" );
            }
            else {
                db_logger.info( "getColumns returned " + results.getCount() + " columns" );
            }
        }

        return results;
    }


    /**
     * Gets the column.
     *
     * @param ko the keyspace
     * @param columnFamily the column family
     * @param key the key
     * @param column the column
     *
     * @return column
     *
     * @throws Exception the exception
     */
    public <N, V> HColumn<N, V> getColumn( Keyspace ko, Object columnFamily, Object key, N column,
                                           Serializer<N> nameSerializer, Serializer<V> valueSerializer )
            throws Exception {

        if ( db_logger.isDebugEnabled() ) {
            db_logger.debug( "getColumn cf=" + columnFamily + " key=" + key + " column=" + column );
        }

    /*
     * ByteBuffer column_bytes = null; if (column instanceof List) {
     * column_bytes = Composite.serializeToByteBuffer((List<?>) column); } else
     * { column_bytes = bytebuffer(column); }
     */

        ColumnQuery<ByteBuffer, N, V> q = HFactory.createColumnQuery( ko, be, nameSerializer, valueSerializer );
        QueryResult<HColumn<N, V>> r =
                q.setKey( bytebuffer( key ) ).setName( column ).setColumnFamily( columnFamily.toString() ).execute();
        HColumn<N, V> result = r.get();

        if ( db_logger.isInfoEnabled() ) {
            if ( result == null ) {
                db_logger.info( "getColumn returned null" );
            }
        }

        return result;
    }


    public <N, V> ColumnSlice<N, V> getColumns( Keyspace ko, Object columnFamily, Object key, N[] columns,
                                                Serializer<N> nameSerializer, Serializer<V> valueSerializer )
            throws Exception {

        if ( db_logger.isDebugEnabled() ) {
            db_logger.debug( "getColumn cf=" + columnFamily + " key=" + key + " column=" + columns );
        }

    /*
     * ByteBuffer column_bytes = null; if (column instanceof List) {
     * column_bytes = Composite.serializeToByteBuffer((List<?>) column); } else
     * { column_bytes = bytebuffer(column); }
     */

        SliceQuery<ByteBuffer, N, V> q = HFactory.createSliceQuery( ko, be, nameSerializer, valueSerializer );
        QueryResult<ColumnSlice<N, V>> r =
                q.setKey( bytebuffer( key ) ).setColumnNames( columns ).setColumnFamily( columnFamily.toString() )
                 .execute();
        ColumnSlice<N, V> result = r.get();

        if ( db_logger.isDebugEnabled() ) {
            if ( result == null ) {
                db_logger.debug( "getColumn returned null" );
            }
        }

        return result;
    }


    public HColumn<String, ByteBuffer> getColumn( Keyspace ko, Object columnFamily, Object key, String column )
            throws Exception {
        return getColumn( ko, columnFamily, key, column, se, be );
    }


    public void setColumn( Keyspace ko, Object columnFamily, Object key, Object columnName, Object columnValue )
            throws Exception {
        this.setColumn( ko, columnFamily, key, columnName, columnValue, 0 );
    }


    public void setColumn( Keyspace ko, Object columnFamily, Object key, Object columnName, Object columnValue,
                           int ttl ) throws Exception {

        if ( db_logger.isDebugEnabled() ) {
            db_logger.debug( "setColumn cf=" + columnFamily + " key=" + key + " name=" + columnName + " value="
                    + columnValue );
        }

        ByteBuffer name_bytes = null;
        if ( columnName instanceof List ) {
            name_bytes = DynamicComposite.toByteBuffer( ( List<?> ) columnName );
        }
        else {
            name_bytes = bytebuffer( columnName );
        }

        ByteBuffer value_bytes = null;
        if ( columnValue instanceof List ) {
            value_bytes = DynamicComposite.toByteBuffer( ( List<?> ) columnValue );
        }
        else {
            value_bytes = bytebuffer( columnValue );
        }

        HColumn<ByteBuffer, ByteBuffer> col = createColumn( name_bytes, value_bytes, be, be );
        if ( ttl != 0 ) {
            col.setTtl( ttl );
        }
        Mutator<ByteBuffer> m = CountingMutator.createFlushingMutator( ko, be );
        m.insert( bytebuffer( key ), columnFamily.toString(), col );
    }


    /**
     * Sets the columns.
     *
     * @param ko the keyspace
     * @param columnFamily the column family
     * @param key the key
     * @param map the map
     *
     * @throws Exception the exception
     */
    public void setColumns( Keyspace ko, Object columnFamily, byte[] key, Map<?, ?> map ) throws Exception {
        this.setColumns( ko, columnFamily, key, map, 0 );
    }


    public void setColumns( Keyspace ko, Object columnFamily, byte[] key, Map<?, ?> map, int ttl ) throws Exception {

        if ( db_logger.isDebugEnabled() ) {
            db_logger.debug( "setColumns cf=" + columnFamily + " key=" + key + " map=" + map + ( ttl != 0 ?
                                                                                                 " ttl=" + ttl : "" ) );
        }

        Mutator<ByteBuffer> m = CountingMutator.createFlushingMutator( ko, be );
        long timestamp = createTimestamp();

        for ( Object name : map.keySet() ) {
            Object value = map.get( name );
            if ( value != null ) {

                ByteBuffer name_bytes = null;
                if ( name instanceof List ) {
                    name_bytes = DynamicComposite.toByteBuffer( ( List<?> ) name );
                }
                else {
                    name_bytes = bytebuffer( name );
                }

                ByteBuffer value_bytes = null;
                if ( value instanceof List ) {
                    value_bytes = DynamicComposite.toByteBuffer( ( List<?> ) value );
                }
                else {
                    value_bytes = bytebuffer( value );
                }

                HColumn<ByteBuffer, ByteBuffer> col = createColumn( name_bytes, value_bytes, timestamp, be, be );
                if ( ttl != 0 ) {
                    col.setTtl( ttl );
                }
                m.addInsertion( bytebuffer( key ), columnFamily.toString(),
                        createColumn( name_bytes, value_bytes, timestamp, be, be ) );
            }
        }
        batchExecute( m, CassandraService.RETRY_COUNT );
    }


    /**
     * Create a timestamp based on the TimeResolution set to the cluster.
     *
     * @return a timestamp
     */
    public long createTimestamp() {
        return chc.getClockResolution().createClock();
    }


    /**
     * Delete column.
     *
     * @param ko the keyspace
     * @param columnFamily the column family
     * @param key the key
     * @param column the column
     *
     * @throws Exception the exception
     */
    public void deleteColumn( Keyspace ko, Object columnFamily, Object key, Object column ) throws Exception {

        if ( db_logger.isDebugEnabled() ) {
            db_logger.debug( "deleteColumn cf=" + columnFamily + " key=" + key + " name=" + column );
        }

        Mutator<ByteBuffer> m = CountingMutator.createFlushingMutator( ko, be );
        m.delete( bytebuffer( key ), columnFamily.toString(), bytebuffer( column ), be );
    }


    /**
     * Gets the row keys.
     *
     * @param ko the keyspace
     * @param columnFamily the column family
     *
     * @return set of keys
     *
     * @throws Exception the exception
     */
    public <K> Set<K> getRowKeySet( Keyspace ko, Object columnFamily, Serializer<K> keySerializer ) throws Exception {

        if ( db_logger.isDebugEnabled() ) {
            db_logger.debug( "getRowKeys cf=" + columnFamily );
        }

        RangeSlicesQuery<K, ByteBuffer, ByteBuffer> q = createRangeSlicesQuery( ko, keySerializer, be, be );
        q.setColumnFamily( columnFamily.toString() );
        q.setKeys( null, null );
        q.setColumnNames( new ByteBuffer[0] );
        QueryResult<OrderedRows<K, ByteBuffer, ByteBuffer>> r = q.execute();
        OrderedRows<K, ByteBuffer, ByteBuffer> rows = r.get();

        Set<K> results = new LinkedHashSet<K>();
        for ( Row<K, ByteBuffer, ByteBuffer> row : rows ) {
            results.add( row.getKey() );
        }

        if ( db_logger.isDebugEnabled() ) {
            {
                db_logger.debug( "getRowKeys returned " + results.size() + " rows" );
            }
        }

        return results;
    }


    /**
     * Gets the row keys as uui ds.
     *
     * @param ko the keyspace
     * @param columnFamily the column family
     *
     * @return list of row key UUIDs
     *
     * @throws Exception the exception
     */
    public <K> List<K> getRowKeyList( Keyspace ko, Object columnFamily, Serializer<K> keySerializer ) throws Exception {

        RangeSlicesQuery<K, ByteBuffer, ByteBuffer> q = createRangeSlicesQuery( ko, keySerializer, be, be );
        q.setColumnFamily( columnFamily.toString() );
        q.setKeys( null, null );
        q.setColumnNames( new ByteBuffer[0] );
        QueryResult<OrderedRows<K, ByteBuffer, ByteBuffer>> r = q.execute();
        OrderedRows<K, ByteBuffer, ByteBuffer> rows = r.get();

        List<K> list = new ArrayList<K>();
        for ( Row<K, ByteBuffer, ByteBuffer> row : rows ) {
            list.add( row.getKey() );
            // K uuid = row.getKey();
            // if (uuid != UUIDUtils.ZERO_UUID) {
            // list.add(uuid);
            // }
        }

        return list;
    }


    /**
     * Delete row.
     *
     * @param ko the keyspace
     * @param columnFamily the column family
     * @param key the key
     *
     * @throws Exception the exception
     */
    public void deleteRow( Keyspace ko, final Object columnFamily, final Object key ) throws Exception {

        if ( db_logger.isDebugEnabled() ) {
            db_logger.debug( "deleteRow cf=" + columnFamily + " key=" + key );
        }

        CountingMutator.createFlushingMutator( ko, be ).addDeletion( bytebuffer( key ), columnFamily.toString() ).execute();
    }



    /**
     * Gets the id list.
     *
     * @param ko the keyspace
     * @param key the key
     * @param start the start
     * @param finish the finish
     * @param count the count
     * @param reversed True if the scan should be reversed
     * @param locator The index locator instance
     * @param applicationId The applicationId
     * @param collectionName The name of the collection to get the Ids for
     *
     * @return list of columns as UUIDs
     *
     * @throws Exception the exception
     */
    public IndexScanner getIdList( Keyspace ko, Object key, UUID start, UUID finish, int count, boolean reversed,
                                   IndexBucketLocator locator, UUID applicationId, String collectionName, boolean keepFirst )
            throws Exception {

        if ( count <= 0 ) {
            count = DEFAULT_COUNT;
        }

        if ( NULL_ID.equals( start ) ) {
            start = null;
        }


        final boolean skipFirst = start != null && !keepFirst;

        IndexScanner scanner =
                new IndexBucketScanner( this, locator, ENTITY_ID_SETS, applicationId, IndexType.COLLECTION, key, start,
                        finish, reversed, count, skipFirst, collectionName );

        return scanner;
    }




    public void destroy() throws Exception {
    	if (cluster != null) {
    		HConnectionManager connectionManager = cluster.getConnectionManager();
    		if (connectionManager != null) {
    			connectionManager.shutdown();
    		}
    	}
    	cluster = null;
    }
}
