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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.usergrid.persistence.core.cassandra.CassandraRule;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Elastic search experiments in the form of a test.
 */
public class ElasticSearchTest extends BaseIT {
    private static final Logger log = LoggerFactory.getLogger( ElasticSearchTest.class );

    @ClassRule
    public static ElasticSearchRule es = new ElasticSearchRule();

    @ClassRule
    public static CassandraRule cass = new CassandraRule();
    
    @Test 
    public void testSimpleCrud() {

        String indexName = RandomStringUtils.randomAlphanumeric(20 ).toLowerCase();
        String collectionName = "testtype1";
        String id = RandomStringUtils.randomAlphanumeric(20 );

        Client client = es.getClient();

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
    } 


    @Test
    public void testStringDoubleIndexDynamicMapping() throws IOException {

        Client client = es.getClient();

        AdminClient admin = client.admin();

        String index = RandomStringUtils.randomAlphanumeric(20).toLowerCase();
        String type = "testtype";
        admin.indices().prepareCreate( index ).execute().actionGet();

        // add dynamic string-double index mapping
        XContentBuilder mxcb = EsEntityIndexImpl
            .createDoubleStringIndexMapping( jsonBuilder(), type );
        PutMappingResponse pmr = admin.indices().preparePutMapping(index)
            .setType( type ).setSource( mxcb ).execute().actionGet();

        indexSampleData( type, client, index );

        testQuery( client, index, type, 
            QueryBuilders.termQuery( "name", "Orr Byers"), 1 );

        testQuery( client, index, type, 
            QueryBuilders.termQuery( "name", "orr byers" ), 0 );

        // term query is exact match
        testQuery( client, index, type, 
            QueryBuilders.termQuery("name", "Byers" ), 0 );

        // match query allows partial match 
        testQuery( client, index, type, 
            QueryBuilders.matchQuery( "name_ug_analyzed", "Byers" ), 1 );

        testQuery( client, index, type, 
            QueryBuilders.termQuery( "company", "Geologix" ), 1 );

        testQuery( client, index, type, 
            QueryBuilders.rangeQuery("company").gt( "Geologix" ), 2 );

        testQuery( client, index, type, 
            QueryBuilders.termQuery( "gender", "female" ), 3 );

        // query of nested object fields supported
        testQuery( client, index, type, 
            QueryBuilders.termQuery( "contact.email", "orrbyers@bittor.com" ), 1 );

    }

    
    private void indexSampleData( String type, Client client, String index ) 
            throws ElasticsearchException, IOException {
        
        InputStream is = this.getClass().getResourceAsStream( "/sample-small.json" );
        ObjectMapper mapper = new ObjectMapper();
        List<Object> contacts = mapper.readValue( is, new TypeReference<List<Object>>() {} );

        for ( Object o : contacts ) {
            Map<String, Object> item = (Map<String, Object>)o;
            String id = item.get( "id" ).toString();

            XContentBuilder dxcb = ElasticSearchTest
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

    
   public static XContentBuilder prepareContentForIndexing( String name, XContentBuilder builder, 
        String type, Map<String, Object> data ) throws IOException {

        if ( name != null ) {
            builder = builder.startObject( name );
        } else {
            builder = builder.startObject();
        }
        for ( String key : data.keySet() ) {
            Object value = data.get( key );
            try {

                if ( value instanceof Map ) {
                    builder = prepareContentForIndexing(
                        key, builder, type, (Map<String, Object>)data.get(key));

                } else if ( value instanceof List ) {
                    builder = prepareContentForIndexing(key, builder, type, (List)value);

                } else if ( value instanceof String ) {
                    builder = builder
                        .field( key + "_ug_analyzed", value )
                        .field( key, value );

                } else {
                    builder = builder
                        .field( key, value );

                }
            } catch ( Exception e ) {
                log.error( "Error processing {} : {}", key, value );
                throw new RuntimeException(e);
            }
        }
        builder = builder.endObject();
        return builder;
    }
    
   
    public static XContentBuilder prepareContentForIndexing( XContentBuilder builder, 
        String type, Map<String, Object> dataMap ) throws IOException {
        return prepareContentForIndexing( null, builder, type, dataMap );
    }

    
    public static XContentBuilder prepareContentForIndexing( String name, XContentBuilder builder, 
        String type, List dataList ) throws IOException {

        if ( name != null ) {
            builder = builder.startArray( name );
        } else {
            builder = builder.startArray();
        }
        for ( Object o : dataList ) {

            if ( o instanceof Map ) {
                builder = prepareContentForIndexing( builder, type, (Map<String, Object>)o );

            } else if ( o instanceof List ) {
                builder = prepareContentForIndexing( builder, type, (List)o);

            } else {
                builder = builder.value( o );

            }
        }
        builder = builder.endArray();
        return builder;
    }


    public static XContentBuilder prepareContentForIndexing( XContentBuilder builder, 
        String type, List dataList ) throws IOException {
        return prepareContentForIndexing( null, builder, type, dataList );
    }

    void log( GetResponse getResponse ) {
        log.info( "-------------------------------------------------------------------------" );
        log.info( "id:      " + getResponse.getId() );
        log.info( "type:    " + getResponse.getType() );
        log.info( "version: " + getResponse.getVersion() );
        log.info( "index:   " + getResponse.getIndex() );
        log.info( "source:  " + getResponse.getSourceAsString() );
    }
}
