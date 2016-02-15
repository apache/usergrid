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
package org.apache.usergrid.persistence.core.datastax;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.util.StringUtils;

import java.nio.ByteBuffer;
import java.util.*;

public class CQLUtils {

    private final CassandraFig cassandraFig;
    private final static ObjectMapper mapper = new ObjectMapper();

    public enum ACTION {
        CREATE, UPDATE
    }

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

    @Inject
    public CQLUtils ( final CassandraFig cassandraFig ){

        this.cassandraFig = cassandraFig;

    }


    public static String getFormattedReplication(String strategy, String strategyOptions) throws JsonProcessingException {

        Map<String, String> replicationSettings = new HashMap<>();
        replicationSettings.put("class", strategy);
        String[] strategyOptionsSplit = strategyOptions.split(",");
        for ( String option : strategyOptionsSplit){
            String[] splitOptions = option.split(":");
            replicationSettings.put(splitOptions[0], splitOptions[1]);
        }
        return mapper.writeValueAsString(replicationSettings).replace("\"", "'");
    }


    public static String getMapAsCQLString(Map<String, Object> map) throws JsonProcessingException {

        return mapper.writeValueAsString(map).replace("\"", "'");
    }


    public static String getTableCQL( CassandraFig cassandraFig, TableDefinition tableDefinition,
                               ACTION tableAction) throws Exception {

        StringJoiner cql = new StringJoiner(" ");

        if ( tableAction.equals(ACTION.CREATE) ){
            cql.add(CREATE_TABLE);
        } else if ( tableAction.equals(ACTION.UPDATE) ){
            cql.add(ALTER_TABLE);
        }else{
            throw new Exception("Invalid Action specified.  Must of of type CQLUtils.Action");
        }

        cql.add( tableDefinition.getTableName() );



        if ( tableAction.equals(ACTION.CREATE) ){

            cql.add(PAREN_LEFT).add( spaceSeparatedKeyValue(tableDefinition.getColumns()) ).add(COMMA)
                .add(PRIMARY_KEY)
                .add(PAREN_LEFT).add(PAREN_LEFT)
                .add( StringUtils.join(tableDefinition.getPartitionKeys(), COMMA) ).add(PAREN_RIGHT);

            if ( tableDefinition.getColumnKeys() != null && !tableDefinition.getColumnKeys().isEmpty() ){

                cql.add(COMMA).add( StringUtils.join(tableDefinition.getColumnKeys(), COMMA) );
            }

            cql.add(PAREN_RIGHT).add(PAREN_RIGHT)
                .add(WITH)
                .add(CLUSTERING_ORDER_BY)
                .add(PAREN_LEFT)
                .add( spaceSeparatedKeyValue(tableDefinition.getClusteringOrder()) )
                .add(PAREN_RIGHT)
                .add(AND)
                .add(COMPACT_STORAGE)
                .add(AND);

        } else if ( tableAction.equals(ACTION.UPDATE) ){
            cql.add(WITH);

        }


        cql.add(COMPACTION).add(EQUAL).add( getMapAsCQLString( tableDefinition.getCompaction() ) )
            .add(AND)
            .add(COMPRESSION).add(EQUAL).add( getMapAsCQLString( tableDefinition.getCompression() ) )
            .add(AND)
            .add(GC_GRACE_SECONDS).add(EQUAL).add( tableDefinition.getGcGraceSeconds() )
            .add(AND)
            .add(CACHING).add(EQUAL).add( getCachingOptions( cassandraFig, tableDefinition.getCacheOption() ) );

        return cql.toString();

    }

    public static String quote( String value){

        return "\"" + value + "\"";

    }

    public static String spaceSeparatedKeyValue(Map<String, ?> columns){

        StringJoiner columnsSchema = new StringJoiner(",");
        columns.forEach( (key, value) -> columnsSchema.add(key+" "+String.valueOf(value)));

        return columnsSchema.toString();

    }


    public static String getCachingOptions(CassandraFig cassandraFig, TableDefinition.CacheOption cacheOption) throws JsonProcessingException {

        // Cassandra 2.0 and below has a different CQL syntax for caching
        if( Double.parseDouble( cassandraFig.getVersion() ) <= 2.0 ){

            return quote( getLegacyCacheValue( cacheOption ) );

        } else {

            return getCacheValue( cacheOption );
        }

    }


    public static String getCacheValue( TableDefinition.CacheOption cacheOption ) throws JsonProcessingException {


        Map<String, Object>  cacheValue = new HashMap<>(2);
        switch (cacheOption) {

            case ALL:
                cacheValue.put("keys", "ALL");
                cacheValue.put("rows_per_partition", "ALL");
                break;

            case KEYS:
                cacheValue.put("keys", "ALL");
                cacheValue.put("rows_per_partition", "NONE");
                break;

            case ROWS:
                cacheValue.put("keys", "NONE");
                cacheValue.put("rows_per_partition", "ALL");
                break;

            case NONE:
                cacheValue.put("keys", "NONE");
                cacheValue.put("rows_per_partition", "NONE");
                break;

        }

        return getMapAsCQLString( cacheValue );

    }

    public static String getLegacyCacheValue( TableDefinition.CacheOption cacheOption ){

        String cacheValue = "none"; // default to no caching
        switch (cacheOption) {

            case ALL:
                cacheValue = "all";
                break;

            case KEYS:
                cacheValue = "keys_only";
                break;

            case ROWS:
                cacheValue = "rows_only";
                break;

            case NONE:
                cacheValue = "none";
                break;

        }

        return cacheValue;

    }


    /**
     * Below functions borrowed from Astyanax until the schema is re-written to be more CQL friendly
     */

    public static int getShortLength(ByteBuffer bb) {
        int length = (bb.get() & 255) << 8;
        return length | bb.get() & 255;
    }

    public static ByteBuffer getBytes(ByteBuffer bb, int length) {
        ByteBuffer copy = bb.duplicate();
        copy.limit(copy.position() + length);
        bb.position(bb.position() + length);
        return copy;
    }

    public static ByteBuffer getWithShortLength(ByteBuffer bb) {
        int length = getShortLength(bb);
        return getBytes(bb, length);
    }


}
