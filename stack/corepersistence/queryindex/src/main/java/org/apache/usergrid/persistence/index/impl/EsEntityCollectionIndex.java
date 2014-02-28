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

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.index.EntityCollectionIndex;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.ArrayField;
import org.apache.usergrid.persistence.model.field.EntityObjectField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.ListField;
import org.apache.usergrid.persistence.model.field.LocationField;
import org.apache.usergrid.persistence.model.field.SetField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.query.EntityRef;
import org.apache.usergrid.persistence.query.Query;
import org.apache.usergrid.persistence.query.Results;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EsEntityCollectionIndex implements EntityCollectionIndex {
    private static final Logger logger = LoggerFactory.getLogger( EsEntityCollectionIndex.class );

    private final Client client;
    private final String index;
    private final boolean refresh;
    private final CollectionScope scope;
    private final EntityCollectionManager manager;

    public static final String ANALYZED_SUFFIX = "_ug_analyzed";

    @Inject
    public EsEntityCollectionIndex( @Assisted final CollectionScope scope, 
            IndexFig config, 
            EsProvider provider, 
            EntityCollectionManagerFactory factory ) {

        this.manager = factory.createCollectionManager(scope );
        this.client = provider.getClient();
        this.scope = scope;
        this.index = config.getIndexName();
        this.refresh = config.isForcedRefresh();
        
        // if new index then create it 
        AdminClient admin = client.admin();
        if ( !admin.indices().exists( new IndicesExistsRequest( index )).actionGet().isExists() ) {
            admin.indices().prepareCreate( index ).execute().actionGet();
            logger.debug( "Created new index: " + index );
        }

        // if new type then create mapping
        if ( !admin.indices().typesExists( new TypesExistsRequest( 
            new String[] {index}, scope.getName() )).actionGet().isExists()) {

            try {
                XContentBuilder mxcb = EsEntityCollectionIndex
                    .createDoubleStringIndexMapping( jsonBuilder(), scope.getName() );

                PutMappingResponse pmr = admin.indices().preparePutMapping(index)
                    .setType( scope.getName() ).setSource( mxcb ).execute().actionGet();

                logger.debug( "Created new type mapping for scope: " + scope.getName() );
                logger.debug( "   Scope organization: " + scope.getOrganization());
                logger.debug( "   Scope owner: " + scope.getOwner() );

            } catch ( IOException ex ) {
                throw new RuntimeException("Error adding mapping for type " + scope.getName(), ex );
            }
        }

    }
  

    public void index( Entity entity ) {

        Map<String, Object> entityAsMap = EsEntityCollectionIndex.entityToMap( entity );
        entityAsMap.put("created", entity.getVersion().timestamp() );

        String indexId = createIndexId( entity ); 

        IndexRequestBuilder irb = client.prepareIndex(index, scope.getName(), indexId )
            .setSource( entityAsMap )
            .setRefresh( refresh );

        irb.execute().actionGet();

        logger.debug( "Indexed Entity with index id " + indexId );
    }


    private String createIndexId( Entity entity ) {

        return entity.getId().getUuid().toString() + "|" 
             + entity.getId().getType() + "|" 
             + entity.getVersion().toString();
    }


    public void deindex( Entity entity ) {

        String indexId = createIndexId( entity ); 
        client.prepareDelete( index, scope.getName(), indexId ).execute().actionGet();

        logger.debug( "Deindexed Entity with index id " + indexId );
    }


    public Results execute( Query query ) {

        // TODO add support for cursor

        QueryBuilder qb = query.createQueryBuilder();
        logger.debug( "Executing query: " + qb.toString() );

        SearchRequestBuilder srb = client.prepareSearch( index ).setTypes( scope.getName() )
            .setQuery( qb ).setFrom( 0 ).setSize( query.getLimit() );

        for ( Query.SortPredicate sp : query.getSortPredicates() ) {
            final SortOrder order;
            if ( sp.getDirection().equals( Query.SortDirection.ASCENDING ) ) { 
                order = SortOrder.ASC;
            } else {
                order = SortOrder.DESC;
            }
            srb.addSort( sp.getPropertyName(), order );
        }

        SearchResponse sr = srb.execute().actionGet();
        SearchHits hits = sr.getHits();
        logger.debug( "   Hit count: " + hits.getTotalHits());

        Results results = new Results();

        // TODO: do we always want to fetch entities? When do we fetch refs or ids?

        // list of entities that will be returned
        List<Entity> entities = new ArrayList<Entity>();

        for ( SearchHit hit : hits.getHits() ) {

            String[] idparts = hit.getId().split( "\\|" );
            String id = idparts[0];
            String type = idparts[1];
            String version = idparts[2];

            Id entityId = new SimpleId( UUID.fromString(id), type);
            UUID entityVersion = UUID.fromString(version);

            Entity entity = manager.load( entityId ).toBlockingObservable().last();
            if ( entity == null ) {
                // TODO exception types instead of RuntimeException
                throw new RuntimeException("Entity id [" + entityId + "] not found"); 
            }

            if ( entityVersion.compareTo( entity.getVersion()) == -1 ) {
                logger.debug("   Stale hit " + hit.getId() ); 

            } else {
                entities.add( entity );
            }
        }

        if ( entities.size() == 1 ) {
            results.setEntity( entities.get( 0 ) );

        } else {
            logger.debug( "   Returning " + entities.size() + " entities");
            results.setEntities( entities );
        }
        return results;
    }

    static class EntityRefVersionComparator implements Comparator<EntityRef> {
        public int compare( EntityRef o1, EntityRef o2 ) {
            return o1.getVersion().compareTo( o2.getVersion() );
        }
    } 


    /**
     * Convert Entity to Map, adding version_ug_field and a {name}_ug_analyzed field for each StringField.
     */
    public static Map entityToMap( Entity entity ) {

        Map<String, Object> entityMap = new HashMap<String, Object>();

        for ( Object f : entity.getFields().toArray() ) {
            Field field = (Field)f;

            if ( f instanceof ListField || f instanceof ArrayField ) {
                List list = (List)field.getValue();
                entityMap.put( field.getName(), 
                    new ArrayList( processCollectionForMap( list ) ) );

            } else if ( f instanceof SetField ) {
                Set set = (Set)field.getValue();
                entityMap.put( field.getName(), 
                    new ArrayList( processCollectionForMap( set ) ) );

            } else if ( f instanceof EntityObjectField ) {
                Entity ev = (Entity)field.getValue();
                entityMap.put( field.getName(), entityToMap( ev ) ); // recursion

            } else if ( f instanceof StringField ) {
                // index in lower case because Usergrid queries are case insensitive
                entityMap.put( field.getName(), ((String)field.getValue()).toLowerCase() );
                entityMap.put( field.getName() + ANALYZED_SUFFIX, field.getValue() );

            } else if ( f instanceof LocationField ) {
                LocationField locField = (LocationField)f;
                Map<String, Object> locMap = new HashMap<String, Object>();

                // field names lat and lon triggerl ElasticSearch geo location 
                locMap.put("lat", locField.getValue().getLatitude() );
                locMap.put("lon", locField.getValue().getLongtitude());
                entityMap.put( field.getName(), locMap );

            } else {
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
                                .field( "match", "*" + ANALYZED_SUFFIX)
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
