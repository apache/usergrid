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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.usergrid.persistence.utils.ElasticSearchRule;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** 
 * Test indexing and searching using the methods from ElasticSearchUtils.
 */
public class ElasticSearchUtilsTest {
    private static final Logger logger = LoggerFactory.getLogger( ElasticSearchUtilsTest.class );

    @Rule
    public ElasticSearchRule elasticSearchRule = new ElasticSearchRule();
    
    @Test
    public void testStringDoubleIndexDynamicMapping() throws IOException {

        Client client = elasticSearchRule.getClient();

        AdminClient admin = client.admin();

        String index = RandomStringUtils.randomAlphanumeric(20).toLowerCase();
        String type = "testtype";
        admin.indices().prepareCreate( index ).execute().actionGet();

        // add dynamic string-double index mapping
        XContentBuilder mxcb = ElasticSearchUtils
            .createDoubleStringIndexMapping( jsonBuilder(), type );
        PutMappingResponse pmr = admin.indices().preparePutMapping(index)
            .setType( type ).setSource( mxcb ).execute().actionGet();

        indexSampleData( type, client, index );

        testQuery( client, index, type, 
            QueryBuilders.termQuery( "name", "Orr Byers" ), 1 );

        // term query is exact match
        testQuery( client, index, type, 
            QueryBuilders.termQuery("name", "Byers" ), 0 );

        // match query allows partial match 
        testQuery( client, index, type, 
            QueryBuilders.matchQuery( "name_ug_analyzed", "Byers" ), 1 );

        testQuery( client, index, type, 
            QueryBuilders.termQuery( "company", "Geologix" ), 1 );

        testQuery( client, index, type, 
            QueryBuilders.termQuery( "gender", "female" ), 3 );

        // query of nested object fields supported
        testQuery( client, index, type, 
            QueryBuilders.termQuery( "contact.email", "orrbyers@bittor.com" ), 1 );

        client.close();
    }

    private void indexSampleData( String type, Client client, String index ) 
            throws ElasticsearchException, IOException {
        
        InputStream is = this.getClass().getResourceAsStream( "/sample.json" );
        ObjectMapper mapper = new ObjectMapper();
        List<Object> sampleJson = mapper.readValue( is, new TypeReference<List<Object>>() {} );

        for ( Object o : sampleJson ) {
            Map<String, Object> item = (Map<String, Object>)o;
            String id = item.get( "id" ).toString();

            XContentBuilder dxcb = ElasticSearchUtils
                .prepareContentForIndexing( jsonBuilder(), type, item );

            IndexResponse ir = client.prepareIndex(index, type, id )
                .setSource( dxcb ).setRefresh( true ).execute().actionGet();
        }
    }

    private void testQuery( Client client, String index, String type, QueryBuilder qb, int num ) {
        SearchResponse sr = client.prepareSearch( index ).setTypes( type )
            .setQuery( qb ).setFrom( 0 ).setSize( 20 ).execute().actionGet();
        assertEquals( num, sr.getHits().getTotalHits() );
    }

    void log( GetResponse getResponse ) {
        logger.info( "-------------------------------------------------------------------------" );
        logger.info( "id:      " + getResponse.getId() );
        logger.info( "type:    " + getResponse.getType() );
        logger.info( "version: " + getResponse.getVersion() );
        logger.info( "index:   " + getResponse.getIndex() );
        logger.info( "source:  " + getResponse.getSourceAsString() );
    }
}
