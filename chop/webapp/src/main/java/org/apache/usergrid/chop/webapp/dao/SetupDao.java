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
import org.apache.usergrid.chop.webapp.elasticsearch.IElasticSearchClient;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.client.IndicesAdminClient;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;


public class SetupDao {

    private final static Logger LOG = LoggerFactory.getLogger(SetupDao.class);

    protected IElasticSearchClient elasticSearchClient;

    @Inject
    public SetupDao(IElasticSearchClient elasticSearchClient) {
        LOG.info("Acquired client: {}", elasticSearchClient);
        this.elasticSearchClient = elasticSearchClient;
    }

    public void setup() throws IOException, NoSuchFieldException, IllegalAccessException {
        String key;
        CreateIndexResponse ciResp;

        Reflections reflections = new Reflections("org.apache.usergrid.chop.webapp.dao");
        Set<Class<? extends Dao>> daoClasses = reflections.getSubTypesOf(Dao.class);

        IndicesAdminClient client = elasticSearchClient.getClient().admin().indices();

        for (Class<? extends Dao> daoClass : daoClasses) {

            key = daoClass.getDeclaredField("DAO_INDEX_KEY").get(null).toString();

            if (!client.exists(new IndicesExistsRequest(key)).actionGet().isExists()) {
                ciResp = client.create(new CreateIndexRequest(key)).actionGet();
                if (ciResp.isAcknowledged()) {
                    LOG.debug("Index for key {} didn't exist, now created", key);
                } else {
                    LOG.debug("Could not create index for key: {}", key);
                }
            } else {
                LOG.debug("Key {} already exists", key);
            }
        }
    }

}
