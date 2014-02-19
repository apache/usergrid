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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Methods for preparing data for indexing in ElasticSearch, and for creating mapping.
 */
public class ElasticSearchUtils {
    private static final Logger logger = LoggerFactory.getLogger( ElasticSearchUtils.class );

    /**
     * Duplicate each string field with a new field named {fieldname}_ug_analyzed
     * 
     * @param name    Name of JSON object to be emitted or null for none
     * @param builder Builder to emit to
     * @param type    Type a.k.a. collection name
     * @param data    Data to be augmented with _ug_analyzed fields.
     * 
     * @return builder With augmented content
     * @throws IOException ON JSON generation error. 
     */
    public static XContentBuilder prepareContentForIndexing( String name, XContentBuilder builder, 
        String type, Map<String, Object> data ) throws IOException {

        if ( name != null ) {
            builder = builder.startObject( name );
        } else {
            builder = builder.startObject();
        }
        for ( String key : data.keySet() ) {
            Object value = data.get( key );
            logger.info( "value class = " + value.getClass().getName() );
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
                logger.error( "Error processing {} : {}", key, value, e );
                throw new RuntimeException(e);
            }
        }
        builder = builder.endObject();
        return builder;
    }
    
   
    /**
     * Duplicate each string field with a new field named {fieldname}_ug_analyzed
     * 
     * @param builder Builder to emit to
     * @param type    Type a.k.a. collection name
     * @param dataMap Data to be augmented with _ug_analyzed fields.
     * 
     * @return builder With augmented content
     * @throws IOException ON JSON generation error. 
     */
    public static XContentBuilder prepareContentForIndexing( XContentBuilder builder, 
        String type, Map<String, Object> dataMap ) throws IOException {
        return prepareContentForIndexing( null, builder, type, dataMap );
    }

    
    /**
     * Duplicate each string field with a new field named {fieldname}_ug_analyzed
     * 
     * @param name     Name of JSON object to be emitted or null for none
     * @param builder  Builder to emit to
     * @param type     Type a.k.a. collection name
     * @param dataList Data to be augmented with _ug_analyzed fields.
     * 
     * @return builder With augmented content
     * @throws IOException ON JSON generation error. 
     */
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


    /**
     * Duplicate each string field with a new field named {fieldname}_ug_analyzed
     * 
     * @param builder  Builder to emit to
     * @param type     Type a.k.a. collection name
     * @param dataList Data to be augmented with _ug_analyzed fields.
     * 
     * @return builder With augmented content
     * @throws IOException ON JSON generation error. 
     */ 
    public static XContentBuilder prepareContentForIndexing( XContentBuilder builder, 
        String type, List dataList ) throws IOException {
        return prepareContentForIndexing( null, builder, type, dataList );
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
