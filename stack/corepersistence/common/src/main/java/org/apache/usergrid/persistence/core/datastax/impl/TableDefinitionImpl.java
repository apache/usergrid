/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.core.datastax.impl;


import com.datastax.driver.core.DataType;
import com.google.common.base.Preconditions;
import org.apache.usergrid.persistence.core.CassandraFig;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.apache.usergrid.persistence.core.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import static org.apache.usergrid.persistence.core.datastax.CQLUtils.*;


public class TableDefinitionImpl implements TableDefinition {

    public enum CacheOption {
        ALL, KEYS, ROWS, NONE
    }

    private final String keyspace;
    private final String tableName;
    private final Collection<String> partitionKeys;
    private final Collection<String> columnKeys;
    private final Map<String, DataType.Name> columns;
    private final CacheOption cacheOption;
    private final Map<String, Object> compaction;
    private final String bloomFilterChance;
    private final String readRepairChance;
    private final Map<String, Object> compression;
    private final String gcGraceSeconds;
    private final Map<String, String> clusteringOrder;

    static String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS";
    static String ALTER_TABLE = "ALTER TABLE";
    static String WITH ="WITH";
    static String AND = "AND";
    static String EQUAL = "=";
    static String COMPRESSION = "compression";
    static String COMPACTION = "compaction";
    static String CACHING = "caching";
    static String GC_GRACE_SECONDS = "gc_grace_seconds";
    static String PRIMARY_KEY = "PRIMARY KEY";
    static String COMPACT_STORAGE = "COMPACT STORAGE";
    static String CLUSTERING_ORDER_BY = "CLUSTERING ORDER BY";
    static String COMMA = ",";
    static String PAREN_LEFT = "(";
    static String PAREN_RIGHT = ")";

    static String COMPOSITE_TYPE = "'org.apache.cassandra.db.marshal.DynamicCompositeType(a=>org.apache.cassandra.db.marshal.AsciiType,A=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.AsciiType),b=>org.apache.cassandra.db.marshal.BytesType,B=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.BytesType),i=>org.apache.cassandra.db.marshal.IntegerType,I=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.IntegerType),l=>org.apache.cassandra.db.marshal.LongType,L=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.LongType),s=>org.apache.cassandra.db.marshal.UTF8Type,S=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.UTF8Type),t=>org.apache.cassandra.db.marshal.TimeUUIDType,T=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.TimeUUIDType),u=>org.apache.cassandra.db.marshal.UUIDType,U=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.UUIDType),x=>org.apache.cassandra.db.marshal.LexicalUUIDType,X=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.LexicalUUIDType))'";


    public TableDefinitionImpl(
        final String keyspace,
        final String tableName,
        final Collection<String> partitionKeys,
        final Collection<String> columnKeys,
        final Map<String, DataType.Name> columns,
        final CacheOption cacheOption,
        final Map<String, String> clusteringOrder) {

        Preconditions.checkNotNull(tableName, "Table name cannot be null");
        Preconditions.checkNotNull(partitionKeys, "Primary Key(s) cannot be null");
        Preconditions.checkNotNull(columns, "Columns cannot be null");
        Preconditions.checkNotNull(cacheOption, "CacheOption cannot be null");

        this.keyspace = keyspace;
        this.tableName = tableName;
        this.partitionKeys = partitionKeys;
        this.columnKeys = columnKeys;
        this.columns = columns;
        this.cacheOption = cacheOption;
        this.clusteringOrder = clusteringOrder;


        // this are default settings always used
        this.compaction = new HashMap<>(1);
        compaction.put( "class", "LeveledCompactionStrategy" );
        this.bloomFilterChance = "0.1d";
        this.readRepairChance = "0.1d";
        this.compression = new HashMap<>(1);
        compression.put("sstable_compression", "LZ4Compressor");
        this.gcGraceSeconds = "864000";
    }

    @Override
    public String getKeyspace() {
        return keyspace;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public String getTableCQL(CassandraFig cassandraFig, ACTION tableAction) throws Exception {

        StringJoiner cql = new StringJoiner(" ");

        if ( tableAction.equals( ACTION.CREATE) ){
            cql.add(CREATE_TABLE);
        } else if ( tableAction.equals( ACTION.UPDATE) ){
            cql.add(ALTER_TABLE);
        }else{
            throw new Exception("Invalid Action specified.  Must of of type CQLUtils.Action");
        }

        cql.add( getTableName() );



        if ( tableAction.equals( ACTION.CREATE) ){

            cql.add(PAREN_LEFT).add( spaceSeparatedKeyValue( getColumns()) ).add(COMMA)
                .add(PRIMARY_KEY)
                .add(PAREN_LEFT).add(PAREN_LEFT)
                .add( StringUtils.join(getPartitionKeys(), COMMA) ).add(PAREN_RIGHT);

            if ( getColumnKeys() != null && !getColumnKeys().isEmpty() ){

                cql.add(COMMA).add( StringUtils.join(getColumnKeys(), COMMA) );
            }

            cql.add(PAREN_RIGHT).add(PAREN_RIGHT)
                .add(WITH)
                .add(CLUSTERING_ORDER_BY)
                .add(PAREN_LEFT)
                .add( spaceSeparatedKeyValue(getClusteringOrder()) )
                .add(PAREN_RIGHT)
                .add(AND)
                .add(COMPACT_STORAGE)
                .add(AND);

        } else if ( tableAction.equals( ACTION.UPDATE) ){
            cql.add(WITH);

        }


        cql.add(COMPACTION).add(EQUAL).add( getMapAsCQLString( getCompaction() ) )
            .add(AND)
            .add(COMPRESSION).add(EQUAL).add( getMapAsCQLString( getCompression() ) )
            .add(AND)
            .add(GC_GRACE_SECONDS).add(EQUAL).add( getGcGraceSeconds() )
            .add(AND)
            .add(CACHING).add(EQUAL).add( getCachingOptions( cassandraFig, getCacheOption() ) );

        return cql.toString();
    }



    public Collection<String> getPartitionKeys() {
        return partitionKeys;
    }

    public Collection<String> getColumnKeys() {
        return columnKeys;
    }

    public Map<String, DataType.Name> getColumns() {
        return columns;
    }

    public CacheOption getCacheOption() {
        return cacheOption;
    }

    public Map<String, Object> getCompaction() {
        return compaction;
    }

    public String getBloomFilterChance() {
        return bloomFilterChance;
    }

    public String getReadRepairChance() {
        return readRepairChance;
    }

    public Map<String, Object> getCompression() {
        return compression;
    }

    public String getGcGraceSeconds() {
        return gcGraceSeconds;
    }

    public Map<String, String> getClusteringOrder() {
        return clusteringOrder;
    }


}
