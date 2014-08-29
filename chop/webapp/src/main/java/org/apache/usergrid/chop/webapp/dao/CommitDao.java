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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.inject.Inject;
import org.apache.usergrid.chop.api.Commit;
import org.apache.usergrid.chop.webapp.dao.model.BasicCommit;
import org.apache.usergrid.chop.webapp.elasticsearch.IElasticSearchClient;
import org.apache.usergrid.chop.webapp.elasticsearch.Util;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;


import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;


/**
 * Commit persistence operations
 */
public class CommitDao extends Dao {

    public static final String DAO_INDEX_KEY = "modules";
    public static final String DAO_TYPE_KEY = "commit";

    private static final int MAX_RESULT_SIZE = 10000;


    @Inject
    public CommitDao( IElasticSearchClient elasticSearchClient ) {
        super( elasticSearchClient );
    }


    /**
     * Saves commit to elastic search and uses commit id as index id.
     *
     * @param commit    Commit to save
     * @return          Whether the operation was successful
     * @throws Exception
     */
    public boolean save( Commit commit ) throws IOException {

        IndexResponse response = elasticSearchClient.getClient()
                .prepareIndex( "modules", "commit", commit.getId() )
                .setRefresh( true )
                .setSource(
                        jsonBuilder()
                                .startObject()
                                .field( "moduleId", commit.getModuleId() )
                                .field( "md5", commit.getMd5() )
                                .field( "createTime", commit.getCreateTime() )
                                .endObject()
                )
                .execute()
                .actionGet();

        return response.isCreated();
    }


    /**
     * Gets all commits belonging to given module.
     * <p>
     * Commits, if any, in the returned list are ordered by createTime, older first.
     *
     * @param moduleId  Module id to filter returns
     * @return          List of date ordered commits, belonging to given module
     */
    public List<Commit> getByModule( String moduleId ) {
        LOG.debug( "moduleId: {}", moduleId );

        SearchResponse response = getRequest( DAO_INDEX_KEY, DAO_TYPE_KEY )
                .setQuery( termQuery( "moduleId", moduleId ) )
                .setSize( MAX_RESULT_SIZE )
                .execute().actionGet();

        TreeMap<Date, Commit> commitMap = new TreeMap<Date, Commit>();

        for ( SearchHit hit : response.getHits().hits() ) {
            Map<String, Object> json = hit.getSource();

            BasicCommit commit = new BasicCommit(
                    hit.getId(),
                    Util.getString( json, "moduleId" ),
                    Util.getString( json, "md5" ),
                    Util.toDate( Util.getString(json, "createTime" ) ),
                    Util.getString( json, "runnerPath" )
            );

            commitMap.put( commit.getCreateTime(), commit );
        }

        ArrayList<Commit> commitList = new ArrayList<Commit>( commitMap.values() );
        LOG.debug( "commits: {}", commitList.size() );

        return commitList;
    }
}