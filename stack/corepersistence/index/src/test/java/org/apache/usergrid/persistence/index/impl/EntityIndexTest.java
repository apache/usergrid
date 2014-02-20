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
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.index.EntityCollectionIndex;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.EntityObjectField;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.ListField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
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
            entity = EntityIndexTest.mapToEntity( entity, item );

            entityIndex.index( entity );

            count++;
        }
        timer.stop();
        logger.info( "Total time to index {} entries {}ms, average {}ms/entry", 
            count, timer.getTime(), timer.getTime() / count );

        testQueries( client, index, type );

        client.close();
    }


    private void testQuery( Client client, String index, String type, QueryBuilder qb, int num ) {
        StopWatch timer = new StopWatch();
        timer.start();
        SearchResponse sr = client.prepareSearch( index ).setTypes( type )
            .setQuery( qb ).setFrom( 0 ).setSize( 20 ).execute().actionGet();
        assertEquals( num, sr.getHits().getTotalHits() );
        timer.stop();
        logger.debug( "Query time {}ms", timer.getTime() );
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

    
    @Test
    public void testRemoveIndex() {
    }


    /**
     * Test of mapToEntity method, of class EntityUtils.
     */
    @Test
    public void testMapToEntityRoundTrip() throws IOException {

        InputStream is = this.getClass().getResourceAsStream( "/sample-large.json" );
        ObjectMapper mapper = new ObjectMapper();
        List<Object> contacts = mapper.readValue( is, new TypeReference<List<Object>>() {} );

        for ( Object o : contacts ) {

            Map<String, Object> map1 = (Map<String, Object>)o;

            // convert map to entity
            Entity entity1 = EntityIndexTest.mapToEntity( map1 );

            // convert entity back to map
            Map map2 = EntityCollectionIndexImpl.entityToMap( entity1 );

            // the two maps should be the same except for the two new _ug_analyzed properties
            Map diff = Maps.difference( map1, map2 ).entriesDiffering();
            assertEquals( 2, diff.size() );
        }
    }

    
    public static Entity mapToEntity( Map<String, Object> item ) {
        return mapToEntity( null, item );
    }


    public static Entity mapToEntity( Entity entity, Map<String, Object> map ) {

        if ( entity == null ) {
            entity = new Entity();
        }

        if ( map.get( "version_ug_field") != null ) {
            EntityUtils.setVersion( entity, UUID.fromString( (String)map.get("version_ug_field")));
        }

        for ( String fieldName : map.keySet() ) {

            Object value = map.get( fieldName );

            if ( value instanceof String ) {
                entity.setField( new StringField(fieldName, (String)value ));

            } else if ( value instanceof Boolean ) {
                entity.setField( new BooleanField(fieldName, (Boolean)value ));
                        
            } else if ( value instanceof Integer ) {
                entity.setField( new IntegerField(fieldName, (Integer)value ));

            } else if ( value instanceof Double ) {
                entity.setField( new DoubleField(fieldName, (Double)value ));

            } else if ( value instanceof Long ) {
                entity.setField( new LongField(fieldName, (Long)value ));

            } else if ( value instanceof List) {
                entity.setField( listToListField( fieldName, (List)value ));

            } else if ( value instanceof Map ) {
                entity.setField( new EntityObjectField( fieldName, 
                    mapToEntity( (Map<String, Object>)value ))); // recursion

            } else {
                throw new RuntimeException("Unknown type " + value.getClass().getName());
            }
        }

        return entity;
    }

    
    private static ListField listToListField( String fieldName, List list ) {

        if (list.isEmpty()) {
            return new ListField( fieldName );
        }

        Object sample = list.get(0);

        if ( sample instanceof Map ) {
            return new ListField<Entity>( fieldName, processListForField( list ));

        } else if ( sample instanceof List ) {
            return new ListField<List>( fieldName, processListForField( list ));
            
        } else if ( sample instanceof String ) {
            return new ListField<String>( fieldName, (List<String>)list );
                    
        } else if ( sample instanceof Boolean ) {
            return new ListField<Boolean>( fieldName, (List<Boolean>)list );
                    
        } else if ( sample instanceof Integer ) {
            return new ListField<Integer>( fieldName, (List<Integer>)list );

        } else if ( sample instanceof Double ) {
            return new ListField<Double>( fieldName, (List<Double>)list );

        } else if ( sample instanceof Long ) {
            return new ListField<Long>( fieldName, (List<Long>)list );

        } else {
            throw new RuntimeException("Unknown type " + sample.getClass().getName());
        }
    }

    
    private static List processListForField( List list ) {
        if ( list.isEmpty() ) {
            return list;
        }
        Object sample = list.get(0);

        if ( sample instanceof Map ) {
            List<Entity> newList = new ArrayList<Entity>();
            for ( Map<String, Object> map : (List<Map<String, Object>>)list ) {
                newList.add( mapToEntity( map ) );
            }
            return newList;

        } else if ( sample instanceof List ) {
            return processListForField( list ); // recursion
            
        } else { 
            return list;
        } 
    }

}
