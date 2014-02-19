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
package org.apache.usergrid.persistence.index.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.usergrid.persistence.utils.ElasticSearchRule;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Elastic search experiments in the form of a test.
 */
public class ElasticSearchTest {
    private static final Logger logger = LoggerFactory.getLogger( ElasticSearchTest.class );

    @Rule
    public ElasticSearchRule elasticSearchRule = new ElasticSearchRule();
    
    @Test 
    public void testSimpleCrud() {

        String indexName = RandomStringUtils.randomAlphanumeric(20 ).toLowerCase();
        String collectionName = "testtype1";
        String id = RandomStringUtils.randomAlphanumeric(20 );

        Client client = elasticSearchRule.getClient();

        Map<String, Object> json = new HashMap<String, Object>();
        json.put( "user", "edward" );
        json.put( "postDate", new Date() );
        json.put( "message", "who knows if the moon’s a baloon" );

        // create
        IndexResponse indexResponse = client.prepareIndex(indexName, collectionName, id)
                .setSource( json ).execute().actionGet();
        assertTrue( indexResponse.isCreated() );

        // retrieve
        GetResponse getResponse = client.prepareGet( indexName, collectionName, id).get();
        assertEquals( "edward", getResponse.getSource().get( "user" ) );

        // update
        json.put( "message", "If freckles were lovely, and day was night");
        client.prepareUpdate( indexName, collectionName, id).setDoc( json ).execute().actionGet();
        getResponse = client.prepareGet( indexName, collectionName, id).get();
        assertEquals("If freckles were lovely, and day was night", 
            getResponse.getSource().get( "message" ) );

        // update via script
        client.prepareUpdate( indexName, collectionName, id)
            .setScript( "ctx._source.message = \"coming out of a keen city in the sky\"" )
            .execute().actionGet();
        getResponse = client.prepareGet( indexName, collectionName, id).get();
        assertEquals("coming out of a keen city in the sky", 
            getResponse.getSource().get( "message" ) );

        // delete
        client.prepareDelete(indexName, collectionName, id).execute().actionGet();
        getResponse = client.prepareGet( indexName, collectionName, id).get();
        assertFalse( getResponse.isExists() );

        client.close();
    } 
}
