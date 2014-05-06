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
import org.apache.usergrid.chop.api.Commit;
import org.apache.usergrid.chop.api.Run;
import org.apache.usergrid.chop.webapp.dao.model.BasicRun;
import org.apache.usergrid.chop.webapp.elasticsearch.IElasticSearchClient;
import org.apache.usergrid.chop.webapp.elasticsearch.Util;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.util.*;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;


public class RunDao extends Dao {

    public static final String DAO_INDEX_KEY = "modules";
    public static final String DAO_TYPE_KEY = "run";

    private static final int MAX_RESULT_SIZE = 10000;


    @Inject
    public RunDao(IElasticSearchClient elasticSearchClient) {
        super(elasticSearchClient);
    }


    public boolean save(Run run) throws Exception {

        IndexResponse response = elasticSearchClient.getClient()
                .prepareIndex(DAO_INDEX_KEY, DAO_TYPE_KEY, run.getId())
                .setRefresh(true)
                .setSource(
                        jsonBuilder()
                                .startObject()
                                .field("commitId", run.getCommitId())
                                .field("runner", run.getRunner())
                                .field("runNumber", run.getRunNumber())
                                .field("testName", run.getTestName())
                                .field("chopType", run.getChopType())
                                .field("iterations", run.getIterations())
                                .field("totalTestsRun", run.getTotalTestsRun())
                                .field("threads", run.getThreads())
                                .field("delay", run.getDelay())
                                .field("time", run.getTime())
                                .field("actualTime", run.getActualTime())
                                .field("minTime", run.getMinTime())
                                .field("maxTime", run.getMaxTime())
                                .field("meanTime", run.getAvgTime())
                                .field("failures", run.getFailures())
                                .field("ignores", run.getIgnores())
                                .field("saturate", run.getSaturate())
                                        // Error in ElasticSearch while saving Long - tries to store as Integer
                                        //.field("startTime", run.getStartTime())
                                        //.field("stopTime", run.getStopTime())
                                .endObject()
                )
                .execute()
                .actionGet();

        return response.isCreated();
    }


    public Run get(String runId) {

        SearchResponse response = getRequest(DAO_INDEX_KEY, DAO_TYPE_KEY)
                .setQuery(termQuery("_id", runId))
                .execute()
                .actionGet();

        SearchHit hits[] = response.getHits().hits();

        return hits.length > 0 ? toRun(hits[0]) : null;
    }


    // <runId, Run>
    public Map<String, Run> getMap(String commitId, int runNumber, String testName) {

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .must(termQuery("commitId", commitId.toLowerCase()))
                .must(termQuery("runNumber", runNumber))
                .must(termQuery("testName", testName.toLowerCase()));

        SearchResponse response = getRequest(DAO_INDEX_KEY, DAO_TYPE_KEY)
                .setQuery(queryBuilder)
                .setSize(MAX_RESULT_SIZE)
                .execute()
                .actionGet();

        HashMap<String, Run> runs = new HashMap<String, Run>();

        for (SearchHit hit : response.getHits().hits()) {
            runs.put(hit.getId(), toRun(hit));
        }

        return runs;
    }


    public static Run toRun(SearchHit hit) {

        Map<String, Object> json = hit.getSource();

        BasicRun run = new BasicRun(
                Util.getString(json, "commitId"),
                Util.getString(json, "runner"),
                Util.getInt(json, "runNumber"),
                Util.getString(json, "testName")
        );

        run.copyJson(hit.getSource());

        return run;
    }


    public List<Run> getAll() {

        SearchResponse response = getRequest(DAO_INDEX_KEY, DAO_TYPE_KEY)
                .setSize(MAX_RESULT_SIZE)
                .execute().actionGet();

        return toList(response);
    }


    public List<Run> getList(String commitId, String testName) {

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .must(termQuery("testName", testName.toLowerCase()))
                .must(termQuery("commitId", commitId.toLowerCase()));

        SearchResponse response = getRequest(DAO_INDEX_KEY, DAO_TYPE_KEY)
                .setQuery(queryBuilder)
                .setSize(MAX_RESULT_SIZE)
                .execute().actionGet();

        return toList(response);
    }


    public List<Run> getList(String commitId, int runNumber) {

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .must(termQuery("commitId", commitId.toLowerCase()))
                .must(termQuery("runNumber", runNumber));

        SearchResponse response = getRequest(DAO_INDEX_KEY, DAO_TYPE_KEY)
                .setQuery(queryBuilder)
                .setSize(MAX_RESULT_SIZE)
                .execute()
                .actionGet();

        return toList(response);
    }


    private String concatIds(List<Commit> commits) {

        String ids = "";

        for (Commit commit : commits) {
            ids += commit.getId() + " ";
        }

        return ids;
    }


    public List<Run> getList(List<Commit> commits, String testName) {

        String commitIds = concatIds(commits);
        LOG.info("commitIds: {}; testName: {}", commitIds, testName);

        SearchResponse response = getRequest(DAO_INDEX_KEY, DAO_TYPE_KEY)
                .setQuery(multiMatchQuery(commitIds, "commitId"))
                .setQuery(termQuery("testName", testName.toLowerCase()))
                .setSize(MAX_RESULT_SIZE)
                .execute()
                .actionGet();

        List<Run> runs = toList(response);

        LOG.info("runs found: {}", runs.size());

        return runs;
    }


    public Set<String> getTestNames(List<Commit> commits) {

        String commitIds = StringUtils.join(commits, ' ');

        SearchResponse response = getRequest(DAO_INDEX_KEY, DAO_TYPE_KEY)
                .setQuery(multiMatchQuery(commitIds, "commitId"))
                .setSize(MAX_RESULT_SIZE)
                .execute()
                .actionGet();

        HashSet<String> names = new HashSet<String>();

        for (SearchHit hit : response.getHits().hits()) {
            names.add(Util.getString(hit.getSource(), "testName"));
        }

        return names;
    }


    private static List<Run> toList(SearchResponse response) {

        ArrayList<Run> list = new ArrayList<Run>();

        for (SearchHit hit : response.getHits().hits()) {
            list.add(toRun(hit));
        }

        return list;
    }


    public int getNextRunNumber(String commitId) {

        SearchResponse response = getRequest(DAO_INDEX_KEY, DAO_TYPE_KEY)
                .setQuery(termQuery("commitId", commitId))
                .setSize(0)
                .execute()
                .actionGet();

        return (int) response.getHits().totalHits() + 1;
    }
}