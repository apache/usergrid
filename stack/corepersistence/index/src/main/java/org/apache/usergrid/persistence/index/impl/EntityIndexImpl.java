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
import java.util.Map;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.model.entity.Entity;
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

        AdminClient admin = client.admin();
        if ( !admin.indices().exists( new IndicesExistsRequest( index )).actionGet().isExists() ) {
            admin.indices().prepareCreate( index ).execute().actionGet();
        }

        String[] indices = new String[] { index };
        if ( !admin.indices().typesExists( 
            new TypesExistsRequest( indices, scope.getName() )).actionGet().isExists() ) {

            try {
                // add dynamic string-double index mapping
                XContentBuilder mxcb = ElasticSearchUtils
                        .createDoubleStringIndexMapping( jsonBuilder(), scope.getName() );
                PutMappingResponse pmr = admin.indices().preparePutMapping(index)
                        .setType( scope.getName() ).setSource( mxcb ).execute().actionGet();

            } catch ( IOException ex ) {
                throw new RuntimeException("Error adding mapping for type " + scope.getName(), ex );
            }
        }

        Map<String, Object> entityAsMap = EntityUtils.entityToMap( entity );
        IndexResponse ir = client.prepareIndex(index, scope.getName(), entity.getId().toString() )
            .setSource( entityAsMap ).setRefresh( true ).execute().actionGet();
        
    }

    public void deindex( Entity entity, CollectionScope scope ) {
    }

    
}
