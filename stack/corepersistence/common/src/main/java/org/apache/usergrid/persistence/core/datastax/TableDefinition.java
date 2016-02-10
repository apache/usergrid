/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.usergrid.persistence.core.datastax;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TableDefinition {


    public enum CacheOption {

        ALL( "ALL" ),
        KEYS( "KEYS_ONLY" ),
        ROWS( "ROWS_ONLY" ),
        NONE( "NONE" );

        private String value;


        CacheOption( String value ) {
            this.value = value;
        }


        public String getValue() {
            return value;
        }
    }


    private final String tableName;
    private final Collection<String> primaryKeys;
    private final Map<String, String> columns;
    private final CacheOption cacheOption;
    private final Map<String, Object> compaction;
    private final String bloomFilterChance;
    private final String readRepairChance;
    private final Map<String, Object> compression;
    private final String gcGraceSeconds;
    private final Map<String, String> clusteringOrder;

    public TableDefinition( final String tableName, final Collection<String> primaryKeys,
                            final Map<String, String> columns, final CacheOption cacheOption,
                            final Map<String, String> clusteringOrder){

        this.tableName = tableName;
        this.primaryKeys = primaryKeys;
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

    public String getTableName() {
        return tableName;
    }

    public Collection<String> getPrimaryKeys() {
        return primaryKeys;
    }

    public Map<String, String> getColumns() {
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
