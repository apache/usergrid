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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.ArrayField;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.EntityObjectField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.ListField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.SetField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


public class EntityIndexImpl implements EntityIndex {
    private final Client client;
    private final String index;


    public EntityIndexImpl( Client client, String index ) {
        this.client = client;
        this.index = index;
    }

   
    public void index( Entity entity, CollectionScope scope ) {

        // if new index then create it 
        AdminClient admin = client.admin();
        if ( !admin.indices().exists( new IndicesExistsRequest( index )).actionGet().isExists() ) {
            admin.indices().prepareCreate( index ).execute().actionGet();
        }

        // if new type then create mapping
        if ( !admin.indices().typesExists( new TypesExistsRequest( 
            new String[] {index}, scope.getName() )).actionGet().isExists()) {

            try {
                XContentBuilder mxcb = EntityIndexImpl
                    .createDoubleStringIndexMapping( jsonBuilder(), scope.getName() );

                PutMappingResponse pmr = admin.indices().preparePutMapping(index)
                    .setType( scope.getName() ).setSource( mxcb ).execute().actionGet();

            } catch ( IOException ex ) {
                throw new RuntimeException("Error adding mapping for type " + scope.getName(), ex );
            }
        }

        Map<String, Object> entityAsMap = EntityIndexImpl.entityToMap( entity );

        IndexResponse ir = client.prepareIndex(index, scope.getName(), entity.getId().toString() )
            .setSource( entityAsMap ).setRefresh( true ).execute().actionGet();
    }


    public void deindex( Entity entity, CollectionScope scope ) {
    }


    public static Map entityToMap( Entity entity ) {

        Map<String, Object> entityMap = new HashMap<String, Object>();

        for ( Object f : entity.getFields().toArray() ) {

            if ( f instanceof ListField || f instanceof ArrayField ) {
                Field field = (Field)f;
                List list = (List)field.getValue();
                entityMap.put( field.getName(), 
                    new ArrayList( processCollectionForMap( list ) ) );

            } else if ( f instanceof SetField ) {
                Field field = (Field)f;
                Set set = (Set)field.getValue();
                entityMap.put( field.getName(), 
                    new ArrayList( processCollectionForMap( set ) ) );

            } else if ( f instanceof EntityObjectField ) {
                Field field = (Field)f;
                Entity ev = (Entity)field.getValue();
                entityMap.put( field.getName(), entityToMap( ev ) ); // recursion

            } else if ( f instanceof StringField ) {
                Field field = (Field)f;
                entityMap.put( field.getName(), field.getValue() );
                entityMap.put( field.getName() + "_ug_analyzed", field.getValue() );

            } else {
                Field field = (Field)f;
                entityMap.put( field.getName(), field.getValue() );
            }
        }

        return entityMap;
    }

    
    private static Collection processCollectionForMap( Collection c ) {
        if ( c.isEmpty() ) {
            return c;
        }
        List processed = new ArrayList();
        Object sample = c.iterator().next();

        if ( sample instanceof Entity ) {
            for ( Object o : c.toArray() ) {
                Entity e = (Entity)o;
                processed.add( entityToMap( e ) );
            }

        } else if ( sample instanceof List ) {
            for ( Object o : c.toArray() ) {
                List list = (List)o;
                processed.add( processCollectionForMap( list ) ); // recursion;
            }

        } else if ( sample instanceof Set ) {
            for ( Object o : c.toArray() ) {
                Set set = (Set)o;
                processed.add( processCollectionForMap( set ) ); // recursion;
            }

        } else {
            for ( Object o : c.toArray() ) {
                processed.add( o );
            }
        }
        return processed;
    }


    public static Entity mapToEntity( Map<String, Object> item ) {
        return mapToEntity( null, item );
    }


    public static Entity mapToEntity( Entity entity, Map<String, Object> map ) {

        if ( entity == null ) {
            entity = new Entity();
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


    /** 
     * Build mappings for data to be indexed. Setup String fields as not_analyzed and analyzed, 
     * where the analyzed field is named {name}_ug_analyzed
     * 
     * @param builder Add JSON object to this builder.
     * @param type    ElasticSearch type of entity.
     * @return         Content builder with JSON for mapping.
     * 
     * @throws java.io.IOException On JSON generation error.
     */
    public static XContentBuilder createDoubleStringIndexMapping( 
            XContentBuilder builder, String type ) throws IOException {

        builder = builder
            .startObject()
                .startObject( type )
                    .startArray( "dynamic_templates" )

                        // any string with field name that ends with _ug_analyzed gets analyzed
                        .startObject()
                            .startObject( "template_1" )
                                .field( "match", "*_ug_analyzed")
                                .field( "match_mapping_type", "string")
                                .startObject( "mapping" )
                                    .field( "type", "string" )
                                    .field( "index", "analyzed" )
                                .endObject()
                            .endObject()
                        .endObject()

                        // all other strings are not analyzed
                        .startObject()
                            .startObject( "template_2" )
                                .field( "match", "*")
                                .field( "match_mapping_type", "string")
                                .startObject( "mapping" )
                                    .field( "type", "string" )
                                    .field( "index", "not_analyzed" )
                                .endObject()
                            .endObject()
                        .endObject()

                    .endArray()
                .endObject()
            .endObject();
        
        return builder;
    }
}
