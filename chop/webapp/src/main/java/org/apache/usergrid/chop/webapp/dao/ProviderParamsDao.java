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
import com.google.inject.Singleton;
import org.apache.usergrid.chop.api.ProviderParams;
import org.apache.usergrid.chop.webapp.dao.model.BasicProviderParams;
import org.apache.usergrid.chop.webapp.elasticsearch.IElasticSearchClient;
import org.apache.usergrid.chop.webapp.elasticsearch.Util;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;


@Singleton
public class ProviderParamsDao extends Dao {

    public static final String DAO_INDEX_KEY = "providerparams";
    public static final String DAO_TYPE_KEY = "providerparam";

    private static final int MAX_RESULT_SIZE = 10000;


    @Inject
    public ProviderParamsDao(IElasticSearchClient e) {
        super(e);
    }


    public boolean save(final ProviderParams pp) throws Exception {

        IndexResponse response = elasticSearchClient.getClient()
                .prepareIndex(DAO_INDEX_KEY, DAO_TYPE_KEY, pp.getUsername())
                .setRefresh(true)
                .setSource(
                        jsonBuilder()
                                .startObject()
                                .field("username", pp.getUsername())
                                .field("instanceType", pp.getInstanceType())
                                .field("accessKey", pp.getAccessKey())
                                .field("secretKey", pp.getSecretKey())
                                .field("imageId", pp.getImageId())
                                .field("keyName", pp.getKeyName())
                                .field("keys", pp.getKeys().toString())
                )
                .execute()
                .actionGet();

        return response.isCreated();
    }


    /**
     * Gets the ProviderParams that belongs to the given username
     */
    public ProviderParams getByUser(String username) {

        SearchResponse response = getRequest(DAO_INDEX_KEY, DAO_TYPE_KEY)
                .setQuery(termQuery("_id", username))
                .execute()
                .actionGet();

        LOG.debug("response: {}", response);

        SearchHit hits[] = response.getHits().hits();

        return hits.length > 0 ? toProviderParams(hits[0]) : null;
    }


    private static ProviderParams toProviderParams(SearchHit hit) {

        Map<String, Object> json = hit.getSource();

        BasicProviderParams params = new BasicProviderParams(
                Util.getString(json, "username"),
                Util.getString(json, "instanceType"),
                Util.getString(json, "accessKey"),
                Util.getString(json, "secretKey"),
                Util.getString(json, "imageId"),
                Util.getString(json, "keyName")
        );

        params.setKeys(Util.getMap(json, "keys"));

        return params;
    }

    /**
     * Gets all ProviderParams
     */
    public List<ProviderParams> getAll() {

        SearchResponse response = getRequest(DAO_INDEX_KEY, DAO_TYPE_KEY)
                .setSize(MAX_RESULT_SIZE)
                .execute()
                .actionGet();

        LOG.debug("response: {}", response);

        SearchHit hits[] = response.getHits().hits();
        ArrayList<ProviderParams> list = new ArrayList<ProviderParams>();

        for (SearchHit hit : hits) {
            list.add(toProviderParams(hit));
        }

        return list;
    }
}
