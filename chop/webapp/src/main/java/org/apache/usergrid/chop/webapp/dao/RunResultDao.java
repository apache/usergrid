/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.webapp.dao;

import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.chop.api.Run;
import org.apache.usergrid.chop.api.RunResult;
import org.apache.usergrid.chop.webapp.dao.model.BasicRunResult;
import org.apache.usergrid.chop.webapp.elasticsearch.IElasticSearchClient;
import org.apache.usergrid.chop.webapp.elasticsearch.Util;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.search.sort.SortBuilders.fieldSort;


/**
 * RunResult persistence operations
 */
public class RunResultDao extends Dao {

    public static final String DAO_INDEX_KEY = "modules";
    public static final String DAO_TYPE_KEY = "runResult";

    private static final int MAX_RESULT_SIZE = 1000000;


    @Inject
    public RunResultDao( IElasticSearchClient elasticSearchClient ) {
        super( elasticSearchClient );
    }


    /**
     * Saves given RunResult in elastic search, uses the id field as index id
     *
     * @param runResult RunResult object to save
     * @return          Whether the operation succeeded
     * @throws Exception
     */
    public boolean save( RunResult runResult ) throws IOException {

        IndexResponse response = elasticSearchClient.getClient()
                .prepareIndex( DAO_INDEX_KEY, DAO_TYPE_KEY, runResult.getId() )
                .setRefresh( true )
                .setSource(
                        jsonBuilder()
                                .startObject()
                                .field( "runId", runResult.getRunId() )
                                .field( "runCount", runResult.getRunCount() )
                                .field( "runTime", runResult.getRunTime() )
                                .field( "ignoreCount", runResult.getIgnoreCount() )
                                .field( "failureCount", runResult.getFailureCount() )
                                .field( "createTime", System.nanoTime() )
                                .field( "failures", runResult.getFailures() )
                                .endObject()
                )
                .execute()
                .actionGet();

        return response.isCreated();
    }


    /**
     * Deletes the RunResult with given id field from elastic search
     *
     * @param id    Corresponds to the result of RunResult.getId() which used as index id in ES
     * @return      Whether the operation succeeded
     */
    public boolean delete( String id ) {

        DeleteResponse response = elasticSearchClient.getClient()
                .prepareDelete( DAO_INDEX_KEY, DAO_TYPE_KEY, id )
                .setRefresh( true )
                .execute()
                .actionGet();

        return response.isFound();
    }


    /**
     * @return  All RunResult objects, stored in elastic search
     */
    public List<RunResult> getAll() {

        SearchResponse response = elasticSearchClient.getClient()
                .prepareSearch( DAO_INDEX_KEY )
                .setTypes( DAO_TYPE_KEY )
                .addSort( fieldSort( "createTime" ) )
                .setSize( MAX_RESULT_SIZE )
                .execute()
                .actionGet();

        return toList( response );
    }


    private static List<RunResult> toList( SearchResponse response ) {
        ArrayList<RunResult> list = new ArrayList<RunResult>();

        for ( SearchHit hit : response.getHits().hits() ) {
            list.add( toRunResult( hit ) );
        }

        return list;
    }


    private static RunResult toRunResult( SearchHit hit ) {

        Map<String, Object> json = hit.getSource();

        return new BasicRunResult(
                hit.getId(),
                Util.getString( json, "runId" ),
                Util.getInt( json, "runCount" ),
                Util.getInt( json, "runTime" ),
                Util.getInt( json, "ignoreCount" ),
                Util.getInt( json, "failureCount" ),
                Util.getString( json, "failures" )
        );
    }


    /**
     * Creates and returns a map of all RunResults belonging to given runs map.
     * <p>
     * Resulting map;
     * <ul\>
     *  <li>Keyset of map consists of all Run objects in runs argument</li>
     *  <li>Value field is the list of RunResults, belonging to the Run in key field</li>
     * </ul\>
     *
     * @param runs  map of Run objects and their IDs as key of the map
     * @return      map of all RunResults belonging to given runs map
     */
    public Map<Run, List<RunResult>> getMap( Map<String, Run> runs ) {

        String runIds = StringUtils.join( runs.keySet(), ' ' );

        SearchResponse response = elasticSearchClient.getClient()
                .prepareSearch( DAO_INDEX_KEY )
                .setTypes( DAO_TYPE_KEY )
                .setQuery( multiMatchQuery( runIds, "runId" ) )
                .addSort( fieldSort( "createTime" ) )
                .setSize( MAX_RESULT_SIZE )
                .execute()
                .actionGet();

        HashMap<Run, List<RunResult>> runResults = new HashMap<Run, List<RunResult>>();

        for ( SearchHit hit : response.getHits().hits() ) {

            RunResult runResult = toRunResult( hit );
            Run run = runs.get( runResult.getRunId() );
            List<RunResult> list = runResults.get( run );

            if ( list == null ) {
                list = new ArrayList<RunResult>();
                runResults.put( run, list );
            }

            list.add( runResult );
        }

        return runResults;
    }


    public String getFailures( String runResultId ) {

        SearchResponse response = elasticSearchClient.getClient()
                .prepareSearch( DAO_INDEX_KEY )
                .setTypes( DAO_TYPE_KEY )
                .setQuery( termQuery( "_id", runResultId ) )
                .execute()
                .actionGet();

        SearchHit hits[] = response.getHits().hits();

        return hits.length > 0 ? Util.getString( hits[ 0 ].getSource(), "failures" ) : "";
    }
}