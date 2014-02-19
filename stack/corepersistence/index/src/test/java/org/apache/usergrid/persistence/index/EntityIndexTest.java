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
package org.apache.usergrid.persistence.index;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.index.impl.EntityIndexImpl;
import org.apache.usergrid.persistence.index.impl.EntityUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.utils.ElasticSearchRule;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import static org.junit.Assert.assertEquals;
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

        EntityIndex entityIndex = new EntityIndexImpl( client, index );  

        indexSampleData( entityIndex, scope );

        testQueries( client, index, type );

        client.close();
    }

    
    @Test
    public void testRemoveIndex() {
    }
    

    private void indexSampleData( EntityIndex entityIndex, CollectionScope scope ) throws IOException {
        
        InputStream is = this.getClass().getResourceAsStream( "/sample.json" );
        ObjectMapper mapper = new ObjectMapper();
        List<Object> sampleJson = mapper.readValue( is, new TypeReference<List<Object>>() {} );

        for ( Object o : sampleJson ) {
            Map<String, Object> item = (Map<String, Object>)o;
            Entity entity = new Entity(new SimpleId(UUIDGenerator.newTimeUUID(), scope.getName()));
            entity = EntityUtils.mapToEntity( entity, item );
            entityIndex.index( entity, scope );
        }
    }

    private void testQuery( Client client, String index, String type, QueryBuilder qb, int num ) {
        SearchResponse sr = client.prepareSearch( index ).setTypes( type )
            .setQuery( qb ).setFrom( 0 ).setSize( 20 ).execute().actionGet();
        assertEquals( num, sr.getHits().getTotalHits() );
    }
   

    private void testQueries( Client client, String index, String type ) {

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
    }

}
