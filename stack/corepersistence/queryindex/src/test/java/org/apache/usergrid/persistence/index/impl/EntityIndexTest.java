/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.index.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.index.EntityCollectionIndex;
import org.apache.usergrid.persistence.index.EntitySearchResults;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.utils.ElasticSearchRule;
import org.apache.usergrid.test.EntityMapUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Rule;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EntityIndexTest {
    private static final Logger logger = LoggerFactory.getLogger( EntityIndexTest.class );

    @Rule
    public ElasticSearchRule elasticSearchRule = new ElasticSearchRule(); 


    @Test
    public void testIndex() throws IOException {

        Client client = elasticSearchRule.getClient();
        final CollectionScope scope = mock( CollectionScope.class );
        when( scope.getName() ).thenReturn( "contacts" );
        String index = RandomStringUtils.randomAlphanumeric(20 ).toLowerCase();
        String type = scope.getName();

        EntityCollectionIndex entityIndex = 
            new EntityCollectionIndexImpl( client, index, scope, true );  

        InputStream is = this.getClass().getResourceAsStream( "/sample-large.json" );
        ObjectMapper mapper = new ObjectMapper();
        List<Object> sampleJson = mapper.readValue( is, new TypeReference<List<Object>>() {} );

        int count = 0;
        StopWatch timer = new StopWatch();
        timer.start();
        for ( Object o : sampleJson ) {

            Map<String, Object> item = (Map<String, Object>)o;

            Entity entity = new Entity(new SimpleId(UUIDGenerator.newTimeUUID(), scope.getName()));
            entity = EntityMapUtils.mapToEntity( scope.getName(), entity, item );
            EntityUtils.setVersion( entity, UUIDGenerator.newTimeUUID() );

            entityIndex.index( entity );

            count++;
        }
        timer.stop();
        logger.info( "Total time to index {} entries {}ms, average {}ms/entry", 
            count, timer.getTime(), timer.getTime() / count );

        // test queries via Java API QueryBuilder
        testQueries( client, index, type );

        // test queries via Lucene syntax
        testQueries( entityIndex );

        client.close();
    }


    private void testQuery( Client client, String index, String type, QueryBuilder qb, int num ) {

        StopWatch timer = new StopWatch();
        timer.start();
        SearchResponse sr = client.prepareSearch( index ).setTypes( type )
            .setQuery( qb ).setFrom( 0 ).setSize( 999 ).execute().actionGet();
        timer.stop();

        assertEquals( num, sr.getHits().getTotalHits() );
        logger.debug( "Query1 time {}ms", timer.getTime() );
    }
   

    private void testQueries( Client client, String index, String type ) {

        testQuery( client, index, type, 
            QueryBuilders.termQuery( "name", "Morgan Pierce" ), 1 );

        // term query is exact match
        testQuery( client, index, type, 
            QueryBuilders.termQuery("name", "Pierce" ), 0 );

        // match query allows partial match 
        testQuery( client, index, type, 
            QueryBuilders.matchQuery( "name_ug_analyzed", "Pierce" ), 2 );

        testQuery( client, index, type, 
            QueryBuilders.termQuery( "company", "Blurrybus" ), 1 );

        testQuery( client, index, type, 
            QueryBuilders.termQuery( "gender", "female" ), 433 );

        // query of nested object fields supported
        testQuery( client, index, type, 
            QueryBuilders.termQuery( "contact.email", "nadiabrown@concility.com" ), 1 ); 
    }

   
    private void testQuery( EntityCollectionIndex entityIndex, String query, int num ) {

        StopWatch timer = new StopWatch();
        timer.start();
        EntitySearchResults results = entityIndex.simpleQuery( query, 0, 999);
        timer.stop();

        assertEquals( num, results.count() );
        logger.debug( "Query2 time {}ms", timer.getTime() );
    }


    private void testQueries( EntityCollectionIndex entityIndex ) {

        testQuery( entityIndex, "name:\"Morgan Pierce\"", 1);

        testQuery( entityIndex, "name:\"Morgan\"", 0);

        testQuery( entityIndex, "name_ug_analyzed:\"Morgan\"", 1);

        testQuery( entityIndex, "gender:female", 433);
    }

    
    @Test
    public void testRemoveIndex() {
        fail("Not implemented");
    }
}
