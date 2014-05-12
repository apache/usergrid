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
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.webapp.dao.model.BasicRunner;
import org.apache.usergrid.chop.webapp.dao.model.RunnerGroup;
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
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

public class RunnerDao extends Dao {

    public static final String DAO_INDEX_KEY = "runners";
    public static final String DAO_TYPE_KEY = "runner";


    @Inject
    public RunnerDao( IElasticSearchClient elasticSearchClient ) {
        super( elasticSearchClient );
    }


    /**
     *
     * @param runner
     * @param user
     * @param commitId
     * @param moduleId
     * @return
     * @throws IOException
     */
    public boolean save( Runner runner, String user, String commitId, String moduleId ) throws IOException {

        String groupId = new RunnerGroup( user, commitId, moduleId ).getId();

        IndexResponse response = elasticSearchClient.getClient()
                .prepareIndex( DAO_INDEX_KEY, DAO_TYPE_KEY, runner.getUrl() )
                .setRefresh( true )
                .setSource(
                        jsonBuilder()
                                .startObject()
                                .field( "groupId", groupId )
                                .field( "ipv4Address", runner.getIpv4Address() )
                                .field( "hostname", runner.getHostname() )
                                .field( "serverPort", runner.getServerPort() )
                                .field( "url", runner.getUrl() )
                                .field( "tempDir", runner.getTempDir() )
                                .field( "user", user )
                                .field( "commitId", commitId )
                                .field( "moduleId", moduleId )
                                .endObject()
                )
                .execute()
                .actionGet();

        return response.isCreated();
    }


    /**
     *
     * @param user
     * @param commitId
     * @param moduleId
     * @return
     */
    public List<Runner> getRunners( String user, String commitId, String moduleId ) {

        String groupId = new RunnerGroup( user, commitId, moduleId ).getId();

        SearchResponse response = getRequest( DAO_INDEX_KEY, DAO_TYPE_KEY )
                .setQuery( termQuery( "groupId", groupId ) )
                .execute()
                .actionGet();

        ArrayList<Runner> runners = new ArrayList<Runner>();

        for ( SearchHit hit : response.getHits().hits() ) {
            runners.add( toRunner( hit ) );
        }

        return runners;
    }


    /**
     * Returns collections of runners grouped by groupId
     */
    public Map<RunnerGroup, List<Runner>> getRunnersGrouped() {

        SearchResponse response = getRequest( DAO_INDEX_KEY, DAO_TYPE_KEY )
                .execute()
                .actionGet();

        LOG.debug( "response: {}", response );

        HashMap<RunnerGroup, List<Runner>> runnerGroups = new HashMap<RunnerGroup, List<Runner>>();

        for ( SearchHit hit : response.getHits().hits() ) {

            RunnerGroup group = getGroup( hit );
            if ( group.isNull() ) {
                continue;
            }

            List<Runner> runners = runnerGroups.get( group );

            if (runners == null) {
                runners = new ArrayList<Runner>();
                runnerGroups.put( group, runners );
            }

            runners.add( toRunner( hit ) );
        }

        return runnerGroups;
    }


    /**
     * Returns a runner group from the search
     */
    private static RunnerGroup getGroup( SearchHit hit ) {
        Map<String, Object> json = hit.getSource();

        return new RunnerGroup(
                Util.getString( json, "user" ),
                Util.getString( json, "commitId" ),
                Util.getString( json, "moduleId" )
        );
    }


    /**
     * @param runnerUrl    Runner with matching runnerUrl id will be deleted
     * @return             Whether such a Runner had existed
     */
    public boolean delete( String runnerUrl ) {

        DeleteResponse response = elasticSearchClient.getClient()
                .prepareDelete( DAO_INDEX_KEY, DAO_TYPE_KEY, runnerUrl )
                .setRefresh( true )
                .execute()
                .actionGet();

        return response.isFound();
    }


    private static Runner toRunner( SearchHit hit ) {

        Map<String, Object> json = hit.getSource();

        return new BasicRunner(
                Util.getString( json, "ipv4Address" ),
                Util.getString( json, "hostname" ),
                Util.getInt( json, "serverPort" ),
                Util.getString( json, "url" ),
                Util.getString( json, "tempDir" )
        );
    }

}