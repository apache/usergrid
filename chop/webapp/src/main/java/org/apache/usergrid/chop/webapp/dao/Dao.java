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

import org.apache.usergrid.chop.webapp.elasticsearch.IElasticSearchClient;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class Dao {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    protected IElasticSearchClient elasticSearchClient;


    protected Dao(IElasticSearchClient elasticSearchClient) {
        this.elasticSearchClient = elasticSearchClient;
    }


    protected SearchRequestBuilder getRequest(String index, String type) {
        return elasticSearchClient.getClient()
                .prepareSearch(index)
                .setTypes(type);
    }

    /**
     * By default ElasticSearch searches with lower-case and ignores the dash. We need to fix this to get correct result.
     */
    protected static String fixTermValue(String value) {
        return value != null
                ? value.toLowerCase().replaceAll("-", "")
                : null;
    }
}


