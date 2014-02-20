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
import org.apache.usergrid.persistence.index.EntityCollectionIndex;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.ArrayField;
import org.apache.usergrid.persistence.model.field.EntityObjectField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.ListField;
import org.apache.usergrid.persistence.model.field.SetField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


public class EntityCollectionIndexImpl implements EntityCollectionIndex {
    private final Client client;
    private final String index;
    private final boolean refresh;
    private final CollectionScope scope;


    public EntityCollectionIndexImpl( Client client, String index, CollectionScope scope, boolean refresh ) {
        this.client = client;
        this.index = index;
        this.scope = scope;
        this.refresh = refresh;
        
        // if new index then create it 
        AdminClient admin = client.admin();
        if ( !admin.indices().exists( new IndicesExistsRequest( index )).actionGet().isExists() ) {
            admin.indices().prepareCreate( index ).execute().actionGet();
        }

        // if new type then create mapping
        if ( !admin.indices().typesExists( new TypesExistsRequest( 
            new String[] {index}, scope.getName() )).actionGet().isExists()) {

            try {
                XContentBuilder mxcb = EntityCollectionIndexImpl
                    .createDoubleStringIndexMapping( jsonBuilder(), scope.getName() );

                PutMappingResponse pmr = admin.indices().preparePutMapping(index)
                    .setType( scope.getName() ).setSource( mxcb ).execute().actionGet();

            } catch ( IOException ex ) {
                throw new RuntimeException("Error adding mapping for type " + scope.getName(), ex );
            }
        }
    }

    
    public String getScopeName() {
        return scope.getName();
    }
  

    public void index( Entity entity ) {

        Map<String, Object> entityAsMap = EntityCollectionIndexImpl.entityToMap( entity );

        IndexRequestBuilder irb = client.prepareIndex(index, scope.getName(), entity.getId().toString() )
            .setSource( entityAsMap )
            .setRefresh( refresh );

        // Cannot set version. As far as I can tell ES insists that initial version number is -1 
        // and that number is incremented by 1 on each update.
        // irb = irb.setVersion( entity.getVersion().timestamp() );
        
        IndexResponse ir = irb.execute().actionGet();
    }


    public void deindex( Entity entity ) {
    }


    /**
     * Convert Entity to Map, adding version_ug_field and a {name}_ug_analyzed field for each StringField.
     */
    public static Map entityToMap( Entity entity ) {

        Map<String, Object> entityMap = new HashMap<String, Object>();

        if ( entity.getVersion() != null ) {
            entityMap.put( "version_ug_field", entity.getVersion().toString() );
        }

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
