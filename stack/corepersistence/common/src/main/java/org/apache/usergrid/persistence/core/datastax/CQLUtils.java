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

import com.datastax.driver.core.DataType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.core.CassandraFig;
import org.apache.usergrid.persistence.core.datastax.impl.TableDefinitionImpl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class CQLUtils {

    private final static ObjectMapper mapper = new ObjectMapper();

    static String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS";
    static String ALTER_TABLE = "ALTER TABLE";

    static String COMPOSITE_TYPE = "'org.apache.cassandra.db.marshal.DynamicCompositeType(a=>org.apache.cassandra.db.marshal.AsciiType,A=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.AsciiType),b=>org.apache.cassandra.db.marshal.BytesType,B=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.BytesType),i=>org.apache.cassandra.db.marshal.IntegerType,I=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.IntegerType),l=>org.apache.cassandra.db.marshal.LongType,L=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.LongType),s=>org.apache.cassandra.db.marshal.UTF8Type,S=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.UTF8Type),t=>org.apache.cassandra.db.marshal.TimeUUIDType,T=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.TimeUUIDType),u=>org.apache.cassandra.db.marshal.UUIDType,U=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.UUIDType),x=>org.apache.cassandra.db.marshal.LexicalUUIDType,X=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.LexicalUUIDType))'";


    @Inject
    public CQLUtils ( ) {
    }


    public static String getFormattedReplication(
        String strategy, String strategyOptions) throws JsonProcessingException {

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

    public static String quote( String value ){
        return "\"" + value + "\"";
    }

    public static String unquote( String value ) {
        return value.replace("\"", "");
    }
    public static String spaceSeparatedKeyValue(Map<String, ?> columns){

        StringJoiner columnsSchema = new StringJoiner(",");
        columns.forEach( (key, value) -> {

            if( value == DataType.Name.CUSTOM ){
                columnsSchema.add(key+" "+COMPOSITE_TYPE);
            }else {
                columnsSchema.add(key + " " + String.valueOf(value));
            }
        });

        return columnsSchema.toString();

    }


    public static String getCachingOptions(
        CassandraFig cassandraFig, TableDefinitionImpl.CacheOption cacheOption) throws JsonProcessingException {

        // Cassandra 2.0 and below has a different CQL syntax for caching
        if( Double.parseDouble( cassandraFig.getVersion() ) <= 2.0 ){

            return quote( getLegacyCacheValue( cacheOption ) );

        } else {

            return getCacheValue( cacheOption );
        }

    }


    public static String getCacheValue( TableDefinitionImpl.CacheOption cacheOption ) throws JsonProcessingException {


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
            default:
                cacheValue.put("keys", "NONE");
                cacheValue.put("rows_per_partition", "NONE");
                break;

        }

        return getMapAsCQLString( cacheValue );

    }

    public static String getLegacyCacheValue( TableDefinitionImpl.CacheOption cacheOption ){

        String cacheValue;
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
            default:
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
