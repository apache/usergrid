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
import org.apache.usergrid.persistence.core.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public class CQLUtils {


    enum ACTION {
        CREATE, UPDATE
    }

    static String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS";
    static String ALTER_TABLE = "ALTER TABLE";
    static String WITH ="WITH";
    static String AND = "AND";
    static String EQUAL = "=";
    static String COMPRESSION = "compression";
    static String COMPACTION = "compaction";
    static String GC_GRACE_SECONDS = "gc_grace_seconds";
    static String PRIMARY_KEY = "PRIMARY KEY";
    static String COMPACT_STORAGE = "COMPACT STORAGE";
    static String CLUSTERING_ORDER_BY = "CLUSTERING ORDER BY";

    private final static ObjectMapper mapper = new ObjectMapper();


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


    public static String getTableCQL(TableDefinition tableDefinition, ACTION tableAction) throws Exception {

        StringJoiner cql = new StringJoiner(" ");

        if ( tableAction.equals(ACTION.CREATE) ){
            cql.add(CREATE_TABLE);
        } else if ( tableAction.equals(ACTION.UPDATE) ){
            cql.add(ALTER_TABLE);
        }else{
            throw new Exception("Invalid Action specified.  Must of of type CQLUtils.Action");
        }

        cql.add( "\""+tableDefinition.getTableName()+"\"" );


        StringJoiner columnsString = new StringJoiner(",");
        Map<String, String> columns = tableDefinition.getColumns();
        columns.forEach( (key, value) -> columnsString.add(key+" "+value));
        columnsString.add(PRIMARY_KEY +" ( "+StringUtils.join(tableDefinition.getPrimaryKeys(), ",") + " )");

        StringJoiner orderingString = new StringJoiner(" ");
        Map<String, String> ordering = tableDefinition.getClusteringOrder();
        ordering.forEach( (key, value) -> orderingString.add(key+" "+value));

        if ( tableAction.equals(ACTION.CREATE) ){
            cql.add("(").add(columnsString.toString()).add(")")
                .add(WITH)
                .add(CLUSTERING_ORDER_BY).add("(").add(orderingString.toString()).add(")")
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
            .add(GC_GRACE_SECONDS).add(EQUAL).add( tableDefinition.getGcGraceSeconds() );



        return cql.toString();

    }


}
