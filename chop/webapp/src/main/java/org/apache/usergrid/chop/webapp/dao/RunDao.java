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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.chop.api.Commit;
import org.apache.usergrid.chop.api.Run;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.webapp.dao.model.BasicRun;
import org.apache.usergrid.chop.webapp.elasticsearch.IElasticSearchClient;
import org.apache.usergrid.chop.webapp.elasticsearch.Util;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.inQuery;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;


/**
 * Run persistence operations
 */
public class RunDao extends Dao {

    public static final String DAO_INDEX_KEY = "modules";
    public static final String DAO_TYPE_KEY = "run";

    private static final int MAX_RESULT_SIZE = 10000;

    @Inject
    public RunDao( IElasticSearchClient elasticSearchClient ) {
        super( elasticSearchClient );
    }


    /**
     * @param run   Run to save in elastic search
     * @return      Whether the operation succeeded
     * @throws Exception
     */
    public boolean save( Run run ) throws IOException {

        IndexResponse response = elasticSearchClient.getClient()
                .prepareIndex( DAO_INDEX_KEY, DAO_TYPE_KEY, run.getId() )
                .setRefresh( true )
                .setSource(
                        jsonBuilder()
                                .startObject()
                                .field( "id", run.getId() )
                                .field( "commitId", run.getCommitId() )
                                .field( "runner", run.getRunner() )
                                .field( "runNumber", run.getRunNumber() )
                                .field( "testName", run.getTestName() )
                                .field( "chopType", run.getChopType() )
                                .field( "iterations", run.getIterations() )
                                .field( "totalTestsRun", run.getTotalTestsRun() )
                                .field( "threads", run.getThreads() )
                                .field( "delay", run.getDelay() )
                                .field( "time", run.getTime() )
                                .field( "actualTime", run.getActualTime() )
                                .field( "minTime", run.getMinTime() )
                                .field( "maxTime", run.getMaxTime() )
                                .field( "meanTime", run.getAvgTime() )
                                .field( "failures", run.getFailures() )
                                .field( "ignores", run.getIgnores() )
                                .field( "saturate", run.getSaturate() )
                                .field( "startTime", run.getStartTime() )
                                .field( "stopTime", run.getStopTime() )
                                .endObject()
                )
                .execute()
                .actionGet();

        return response.isCreated();
    }


    /**
     *
     * @param run
     * @return
     */
    public boolean delete( Run run ) {
        DeleteResponse response = elasticSearchClient.getClient()
                .prepareDelete( DAO_INDEX_KEY, DAO_TYPE_KEY, run.getId() )
                .setRefresh( true )
                .execute()
                .actionGet();

        return response.isFound();
    }


    /**
     * @param runId Id of queried Run
     * @return      Run object or null if it doesn't exist
     */
    public Run get( String runId ) {

        SearchResponse response = getRequest( DAO_INDEX_KEY, DAO_TYPE_KEY )
                .setQuery( termQuery( "_id", runId ) )
                .execute()
                .actionGet();

        SearchHit hits[] = response.getHits().hits();

        return hits.length > 0 ? toRun( hits[ 0 ] ) : null;
    }


    /**
     * Returns a map of all Runs with queried commitId, runNumber and testName.
     * <p>
     * <ul>
     *     <li>Key of the map is Run's id in elastic search</li>
     *     <li>Value of the map is Run itself</li>
     * </ul>
     *
     * @param commitId    commit id of the Run
     * @param runNumber   Run number to filter queried Runs
     * @param testName    Test class name that resulting Run is about
     * @return            Map satisfying given parameters. The map is empty if there are no Runs.
     */
    public Map<String, Run> getMap( String commitId, int runNumber, String testName ) {

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .must( termQuery( "commitId", commitId.toLowerCase() ) )
                .must( termQuery( "runNumber", runNumber ) )
                .must( termQuery( "testName", testName.toLowerCase() ) );

        SearchResponse response = getRequest( DAO_INDEX_KEY, DAO_TYPE_KEY )
                .setQuery( queryBuilder )
                .setSize( MAX_RESULT_SIZE )
                .execute()
                .actionGet();

        HashMap<String, Run> runs = new HashMap<String, Run>();

        for ( SearchHit hit : response.getHits().hits() ) {
            runs.put( hit.getId(), toRun( hit ) );
        }

        return runs;
    }


    /**
     * Returns a map of all runs with matching commitId, runNumber, testName, and one of given runners' hostname.
     * <p>
     * <ul>
     *     <li>Key field of the map is Run's id in elastic search</li>
     *     <li>Value field of the map is the Run itself</li>
     * </ul>
     *
     * @param commitId  commit id of the Run
     * @param runNumber Run number to filter queried Runs
     * @param testName  Test class name that resulting Run is about
     * @param runners   A Run whose runner field is one of runners' hostname fields is a match
     * @return          Map satisfying given parameters. The map is empty if there are no matching Runs.
     */
    public Map<String, Run> getMap( String commitId, int runNumber, String testName, Collection<Runner> runners ) {
        Map<String, Run> map = getMap( commitId, runNumber, testName );
        Map<String, Run> mapFilteredWithRunners = new HashMap<String, Run>();
        for( String key : map.keySet() ) {
            boolean matchesOne = false;
            for( Runner runner: runners ) {
                if( runner.getHostname().equals( map.get( key ).getRunner() ) ) {
                    matchesOne = true;
                    break;
                }
            }
            if( matchesOne ) {
                mapFilteredWithRunners.put( key, map.get( key ) );
            }
        }
        return mapFilteredWithRunners;
    }


    private static Run toRun( SearchHit hit ) {
        Map<String, Object> json = hit.getSource();

        BasicRun run = new BasicRun(
                Util.getString( json, "id" ),
                Util.getString( json, "commitId" ),
                Util.getString( json, "runner" ),
                Util.getInt( json, "runNumber" ),
                Util.getString(json, "testName" )
        );
        run.copyJson( hit.getSource() );

        return run;
    }


    private static String concatIds( List<Commit> commits ) {
        StringBuilder ids = new StringBuilder();

        for ( Commit commit : commits ) {
            ids.append( commit.getId() )
               .append( " " );
        }
        return ids.toString();
    }


    private static List<Run> toList( SearchResponse response ) {
        ArrayList<Run> list = new ArrayList<Run>();
        if( response.getHits().getTotalHits() == 0 ) {
            return list;
        }

        for ( SearchHit hit : response.getHits().hits() ) {
            list.add( toRun( hit ) );
        }
        return list;
    }


    /**
     * @return  All registered Runs in elastic search
     */
    public List<Run> getAll() {

        SearchResponse response = getRequest( DAO_INDEX_KEY, DAO_TYPE_KEY )
                .setSize( MAX_RESULT_SIZE )
                .execute().actionGet();

        return toList( response );
    }


    /**
     * Narrows the registered Runs list to ones that match given commitId and testName
     *
     * @param commitId
     * @param testName
     * @return
     */
    public List<Run> getList( String commitId, String testName ) {

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .must( termQuery( "testName", testName.toLowerCase() ) )
                .must( termQuery( "commitId", commitId.toLowerCase() ) );

        SearchResponse response = getRequest( DAO_INDEX_KEY, DAO_TYPE_KEY )
                .setQuery( queryBuilder )
                .setSize( MAX_RESULT_SIZE )
                .execute().actionGet();

        return toList( response );
    }


    /**
     * Narrows the registered Runs list to ones that match given commitId and runNumber
     *
     * @param commitId
     * @param runNumber
     * @return
     */
    public List<Run> getList( String commitId, int runNumber ) {

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .must( termQuery( "commitId", commitId.toLowerCase() ) )
                .must( termQuery( "runNumber", runNumber ) );

        SearchResponse response = getRequest( DAO_INDEX_KEY, DAO_TYPE_KEY )
                .setQuery( queryBuilder )
                .setSize( MAX_RESULT_SIZE )
                .execute()
                .actionGet();

        return toList( response );
    }


    /**
     * @param commits
     * @param testName
     * @return
     */
    public List<Run> getList( List<Commit> commits, String testName ) {
        String commitIds = concatIds( commits );
        LOG.debug( "commitIds: {}; testName: {}", commitIds, testName );

        SearchResponse response = getRequest( DAO_INDEX_KEY, DAO_TYPE_KEY )
                .setQuery( multiMatchQuery( commitIds, "commitId" ) )
                .setQuery( termQuery( "testName", testName.toLowerCase() ) )
                .setSize( MAX_RESULT_SIZE )
                .execute()
                .actionGet();

        List<Run> runs = toList( response );

        LOG.debug( "runs found: {}", runs.size() );

        return runs;
    }


    /**
     * Gets the runs given runner has run for given commit.
     *
     * @param runner    Runner hostname
     * @param commitId  Runs return will be the runs for this commitId
     * @return          List of runs of this runner
     */
    public List<Run> getRuns( String runner, String commitId ) {
        List<Run> runnerRuns = new ArrayList<Run>();
        List<Run> runs = getAll();
        for( Run run: runs ) {
            if( runner.equals( run.getRunner() ) && commitId.equals( run.getCommitId() ) ) {
                runnerRuns.add( run );
            }
        }
        return runnerRuns;
    }


    /**
     * @param commitId  commit id of the Run
     * @return          collection of runs for the given commitId
     */
    public Collection<Run> getRuns( String commitId ) {
        SearchResponse response = getRequest( DAO_INDEX_KEY, DAO_TYPE_KEY )
                .setQuery( termQuery( "commitId", commitId.toLowerCase() ) )
                .setSize( MAX_RESULT_SIZE )
                .execute()
                .actionGet();

        Collection<Run> runs = new LinkedList<Run>();

        for ( SearchHit hit : response.getHits().hits() ) {
            runs.add( toRun( hit ) );
        }
        return runs;
    }


    /**
     *
     * @param commits
     * @return
     */
    public Set<String> getTestNames( List<Commit> commits ) {

        String commitIds = StringUtils.join( commits, ' ' );

        SearchResponse response = getRequest( DAO_INDEX_KEY, DAO_TYPE_KEY )
                .setQuery( multiMatchQuery( commitIds, "commitId" ) )
                .setSize( MAX_RESULT_SIZE )
                .execute()
                .actionGet();

        HashSet<String> names = new HashSet<String>();

        for ( SearchHit hit : response.getHits().hits() ) {
            names.add( Util.getString( hit.getSource(), "testName" ) );
        }

        return names;
    }


    /**
     * Gets the stored runs for the given commit Id
     * and returns +1 of the maximum runNumber in all found runs.
     * <p>
     * If no runs for this commitId could be found, this automatically is 1.
     *
     *
     * @param commitId  commit id of the Run
     * @return          next run number for the tests for this commitId
     */
    public int getNextRunNumber( String commitId ) {
        Collection<Run> runs = getRuns( commitId );
        int maxRunNumber = 0;
        for( Run run: runs ) {
            if( run.getRunNumber() > maxRunNumber ) {
                maxRunNumber = run.getRunNumber();
            }
        }

        return maxRunNumber + 1;
    }
}