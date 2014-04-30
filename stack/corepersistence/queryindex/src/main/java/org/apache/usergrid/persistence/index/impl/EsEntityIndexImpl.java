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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.time.StopWatch;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccValidationUtils;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.exceptions.IndexException;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Results;
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

import com.google.common.util.concurrent.AtomicDouble;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.usergrid.persistence.model.field.value.EntityObject;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.indices.IndexAlreadyExistsException;


/**
 * Implements index using ElasticSearch Java API and Core Persistence Collections.
 */
public class EsEntityIndexImpl implements EntityIndex {

    private static final Logger log = LoggerFactory.getLogger(EsEntityIndexImpl.class);

    private final String indexName;

    private final OrganizationScope orgScope;
    private final CollectionScope appScope;

    private final Client client;

    protected EntityCollectionManagerFactory ecmFactory;

    private final boolean refresh;
    private final int cursorTimeout;

    private final AtomicLong indexedCount = new AtomicLong(0L);
    private final AtomicDouble averageIndexTime = new AtomicDouble(0);

    public static final String ANALYZED_SUFFIX = "_ug_analyzed";
    public static final String GEO_SUFFIX = "_ug_geo";
    public static final String COLLECTION_SCOPE_FIELDNAME = "zzz__collectionscope__zzz";

    public static final String DOC_ID_SEPARATOR = "|";
    public static final String DOC_ID_SEPARATOR_SPLITTER = "\\|";

    // These are not allowed in document type names: _ . , | #
    public static final String DOC_TYPE_SEPARATOR = "^";
    public static final String DOC_TYPE_SEPARATOR_SPLITTER = "\\^";

    public static final String INDEX_NAME_SEPARATOR = "^";

       
    @Inject
    public EsEntityIndexImpl(
            @Assisted final OrganizationScope orgScope, 
            @Assisted final CollectionScope appScope,
            IndexFig config,
            EsProvider provider,
            EntityCollectionManagerFactory factory) {
        
        ValidationUtils.validateOrganizationScope( orgScope );
        MvccValidationUtils.validateCollectionScope( appScope );

        this.orgScope = orgScope;
        this.appScope = appScope;

        this.client = provider.getClient();
        this.ecmFactory = factory;

        this.indexName = createIndexName( config.getIndexNamePrefix(), orgScope, appScope );

        this.refresh = config.isForcedRefresh();
        this.cursorTimeout = config.getQueryCursorTimeout();

        AdminClient admin = client.admin();
        try {
            admin.indices().prepareCreate(indexName).execute().actionGet();
            log.debug("Created new index: " + indexName);

        } catch (IndexAlreadyExistsException ignored) {
            //log.debug("Keyspace already exists", ignored);

        } catch (Exception e) {
            throw new RuntimeException("Error creating index", e);
        }
    }

    
    private void initType( String typeName ) {

        AdminClient admin = client.admin();

        // if new type then create mapping
        if (!admin.indices().typesExists(new TypesExistsRequest(
                new String[]{indexName}, typeName )).actionGet().isExists()) {

            try {
                XContentBuilder mxcb = EsEntityIndexImpl
                    .createDoubleStringIndexMapping(jsonBuilder(), typeName);

                PutMappingResponse pmr = admin.indices().preparePutMapping( indexName )
                    .setType( typeName ).setSource(mxcb).execute().actionGet();

                if (!admin.indices().typesExists(new TypesExistsRequest(
                    new String[] { indexName }, typeName )).actionGet().isExists()) {
                    throw new RuntimeException("Type does not exist in index: " + typeName);
                }

                GetMappingsResponse gmr = admin.indices().prepareGetMappings( indexName )
                    .addTypes( typeName ).execute().actionGet();
                if ( gmr.getMappings().isEmpty() ) {
                    throw new RuntimeException("Zero mappings exist for type: " + typeName);
                }

                log.debug("Created new type mapping");
                log.debug("   Scope organization: " + orgScope.getOrganization());
                log.debug("   Scope application: " + appScope.getOwner());
                log.debug("   Type name: " + typeName );

            } catch (IOException ex) {
                throw new RuntimeException(
                    "Error adding mapping for type " + typeName, ex);
            }
        }
    }


    private String createIndexName( 
            String prefix, OrganizationScope orgScope, CollectionScope appScope ) {
        StringBuilder sb = new StringBuilder();
        String sep = INDEX_NAME_SEPARATOR;
        sb.append( prefix ).append(sep);
        sb.append( orgScope.getOrganization().getUuid() ).append(sep);
        sb.append( orgScope.getOrganization().getType() ).append(sep);
        sb.append( appScope.getOwner().getUuid() ).append(sep);
        sb.append( appScope.getOwner().getType() );
        return sb.toString();
    }

    
    private String createIndexDocId(Entity entity) {
        return createIndexDocId(entity.getId(), entity.getVersion());
    }

    
    private String createIndexDocId(Id entityId, UUID version) {
        StringBuilder sb = new StringBuilder();
        String sep = DOC_ID_SEPARATOR;
        sb.append( entityId.getUuid() ).append(sep);
        sb.append( entityId.getType() ).append(sep);
        sb.append( version.toString() );
        return sb.toString();
    }


    public static String createCollectionScopeTypeName( CollectionScope scope ) {
        StringBuilder sb = new StringBuilder();
        String sep = DOC_TYPE_SEPARATOR;
        sb.append( scope.getName()                   ).append(sep);
        sb.append( scope.getOwner().getUuid()        ).append(sep);
        sb.append( scope.getOwner().getType()        ).append(sep);
        sb.append( scope.getOrganization().getUuid() ).append(sep);
        sb.append( scope.getOrganization().getType() );
        return sb.toString();
    }


    private String createEntityConnectionScopeTypeName( Id entityId, String type ) {
        StringBuilder sb = new StringBuilder();
        String sep = DOC_TYPE_SEPARATOR;
        sb.append( entityId.getUuid() ).append(sep);
        sb.append( entityId.getType() ).append(sep);
        sb.append( type );
        return sb.toString();
    }

    
    @Override
    public void index( CollectionScope collScope, Entity entity ) {

        String collScopeTypeName = createCollectionScopeTypeName( collScope ); 
        index( collScopeTypeName, collScopeTypeName, entity ); 
    }

    
    @Override
    public void indexConnection( 
        Entity source, String type, Entity target, CollectionScope targetScope ) {

        index( createEntityConnectionScopeTypeName( source.getId(), type), 
               createCollectionScopeTypeName( targetScope ), 
               target );
    }

    /**
     * Index entity into either a collection scope or an entity/connection-type scope.
     * @param estype      Elastic Search Type into which Entity will be indexed.
     * @param targetScope CollectionScope from which to fetch Entity.
     * @param entity      Entity to be indexed.
     */
    private void index( String estype, String targetScope, Entity entity ) {

        log.debug("Indexing entity:  " + entity.getId().toString());
        log.debug("    Index Name:   " + this.indexName);
        log.debug("    ES Type:      " + estype);
        log.debug("    Target Scope: " + targetScope);
        
        ValidationUtils.verifyEntityWrite(entity);

        initType( estype );

        StopWatch timer = null;
        if ( log.isDebugEnabled() ) {
            timer = new StopWatch();
            timer.start();
        }

        Map<String, Object> entityAsMap = EsEntityIndexImpl.entityToMap(entity);
        entityAsMap.put("created", entity.getId().getUuid().timestamp());
        entityAsMap.put("updated", entity.getVersion().timestamp());
        entityAsMap.put(COLLECTION_SCOPE_FIELDNAME, targetScope ); 

        log.debug("Indexing entity: " + entityAsMap);

        String indexId = EsEntityIndexImpl.this.createIndexDocId(entity);

        IndexRequestBuilder irb = client
            .prepareIndex( indexName, estype, indexId)
            .setSource(entityAsMap)
            .setRefresh(refresh);

        irb.execute().actionGet();

        //log.debug("Indexed Entity with index id " + indexId);

        if ( log.isDebugEnabled() ) {
            timer.stop();
            double average = averageIndexTime.get();
            if ( !averageIndexTime.compareAndSet( 0, timer.getTime() ) ) {
                averageIndexTime.compareAndSet( average, (average + timer.getTime()) / 2.0 );
            }
            long count = indexedCount.addAndGet(1);
            if ( count % 1000 == 0 ) {
               log.debug("Indexed {} entities, average time {}ms", count, averageIndexTime.get() ); 
            }
        }
    }


    @Override
    public void deindex( CollectionScope collScope, Entity entity ) {
        
        deindex( createCollectionScopeTypeName( collScope ), entity );     
    }


    @Override
    public void deindexConnection( Id sourceId, String type, Entity target ) {

        deindex( createEntityConnectionScopeTypeName( sourceId, type ), target );
    }


    public void deindex( String typeName, Entity entity ) {

        String indexId = createIndexDocId( entity.getId(), entity.getVersion() );

        client
            .prepareDelete( indexName, typeName, indexId )
            .setRefresh( refresh )
            .execute().actionGet();

        log.debug("Deindexed Entity with index id " + indexId);
    }


    @Override
    public Results searchConnections( Entity source, String type, Query query ) {

        return search( createEntityConnectionScopeTypeName( source.getId(), type), query );
    }


    @Override
    public Results search( CollectionScope collScope, Query query) {

        return search( createCollectionScopeTypeName( collScope ), query);
    }


    public Results search( String estype, Query query) {

        QueryBuilder qb = query.createQueryBuilder();
        
        log.debug("Search");
        log.debug("    Index Name: " + this.indexName);
        log.debug("    ES Type:    " + estype);
        log.debug("    Query:      " + qb.toString().replace("\n", " ") );
        
            
        SearchResponse searchResponse;
        if (query.getCursor() == null) {


            SearchRequestBuilder srb = client.prepareSearch(indexName)
                .setTypes( estype )
                .setScroll( cursorTimeout + "m" )
                .setQuery( qb );

            FilterBuilder fb = query.createFilterBuilder();
            if (fb != null) {
                log.debug("   Filter: {} ", fb.toString());
                srb = srb.setPostFilter(fb);
            }

            srb = srb.setFrom(0).setSize(query.getLimit());

            for (Query.SortPredicate sp : query.getSortPredicates()) {
                final SortOrder order;
                if (sp.getDirection().equals(Query.SortDirection.ASCENDING)) {
                    order = SortOrder.ASC;
                } else {
                    order = SortOrder.DESC;
                }
                srb.addSort(sp.getPropertyName(), order);
                log.debug("   Sort: {} order by {}", sp.getPropertyName(), order.toString());
            }

            searchResponse = srb.execute().actionGet();

        } else {
            log.debug("Executing query with cursor: {} ", query.getCursor());

            SearchScrollRequestBuilder ssrb = client
                .prepareSearchScroll(query.getCursor())
                .setScroll( cursorTimeout + "m" );
            searchResponse = ssrb.execute().actionGet();
        }

        SearchHits hits = searchResponse.getHits();
        log.debug("   Hit count: {} Total hits: {}", hits.getHits().length, hits.getTotalHits() );

        // TODO: do we always want to fetch entities? When do we fetch refs or ids?
        // list of entities that will be returned
        List<Id> ids = new ArrayList<Id>();
        List<Entity> entities = new ArrayList<Entity>();

        for (SearchHit hit : hits.getHits()) {

            String[] idparts = hit.getId().split( DOC_ID_SEPARATOR_SPLITTER );
            String id      = idparts[0];
            String type    = idparts[1];
            String version = idparts[2];

            EntityCollectionManager ecm = getEntityCollectionManager(
                hit.getSource().get( COLLECTION_SCOPE_FIELDNAME ).toString() );

            Id entityId = new SimpleId(UUID.fromString(id), type);

            Entity entity = ecm.load(entityId).toBlockingObservable().last();
            if (entity == null) {
                throw new IndexException("Entity id [" + entityId + "] not found");
            }

            UUID entityVersion = UUID.fromString(version);
            if (entityVersion.compareTo(entity.getVersion()) == -1) {
                log.debug("   Stale hit " + hit.getId());

            } else {
                ids.add( entityId );
                entities.add( entity );
            }
        }

        Results results = new Results( query, ids, entities );

        if ( ids.size() == query.getLimit() ) {
            results.setCursor(searchResponse.getScrollId());
            log.debug("   Cursor = " + searchResponse.getScrollId() );
        }

        return results;
    }


    private EntityCollectionManager getEntityCollectionManager( String scope ) {

        String[] scopeParts = scope.split( DOC_TYPE_SEPARATOR_SPLITTER );

        String scopeName      =                  scopeParts[0];
        UUID   scopeOwnerUuid = UUID.fromString( scopeParts[1] );
        String scopeOwnerType =                  scopeParts[2];
        UUID   scopeOrgUuid   = UUID.fromString( scopeParts[3] );
        String scopeOrgType   =                  scopeParts[4];

        Id ownerId = new SimpleId( scopeOwnerUuid, scopeOwnerType );
        Id orgId = new SimpleId( scopeOrgUuid, scopeOrgType );

        CollectionScope collScope = new CollectionScopeImpl( orgId, ownerId, scopeName );

        EntityCollectionManager ecm = ecmFactory.createCollectionManager(collScope);

        return ecm;
    }


    /**
     * Convert Entity to Map, adding version_ug_field and a {name}_ug_analyzed field for each
     * StringField.
     */
    public static Map entityToMap(EntityObject entity) {

        Map<String, Object> entityMap = new HashMap<String, Object>();

        for (Object f : entity.getFields().toArray()) {
            Field field = (Field) f;

            if (f instanceof ListField || f instanceof ArrayField) {
                List list = (List) field.getValue();
                entityMap.put(field.getName(),
                        new ArrayList(processCollectionForMap(list)));

            } else if (f instanceof SetField) {
                Set set = (Set) field.getValue();
                entityMap.put(field.getName(),
                        new ArrayList(processCollectionForMap(set)));

            } else if (f instanceof EntityObjectField) {
                EntityObject eo = (EntityObject)field.getValue();
                entityMap.put(field.getName(), entityToMap(eo)); // recursion

            } else if (f instanceof StringField) {
                // index in lower case because Usergrid queries are case insensitive
                entityMap.put(field.getName(), ((String) field.getValue()).toLowerCase());
                entityMap.put(field.getName() + ANALYZED_SUFFIX, field.getValue());

            } else if (f instanceof LocationField) {
                LocationField locField = (LocationField) f;
                Map<String, Object> locMap = new HashMap<String, Object>();

                // field names lat and lon trigger ElasticSearch geo location 
                locMap.put("lat", locField.getValue().getLatitude());
                locMap.put("lon", locField.getValue().getLongtitude());
                entityMap.put(field.getName() + GEO_SUFFIX, locMap);

            } else {
                entityMap.put(field.getName(), field.getValue());
            }
        }

        return entityMap;
    }


    private static Collection processCollectionForMap(Collection c) {
        if (c.isEmpty()) {
            return c;
        }
        List processed = new ArrayList();
        Object sample = c.iterator().next();

        if (sample instanceof Entity) {
            for (Object o : c.toArray()) {
                Entity e = (Entity) o;
                processed.add(entityToMap(e));
            }

        } else if (sample instanceof List) {
            for (Object o : c.toArray()) {
                List list = (List) o;
                processed.add(processCollectionForMap(list)); // recursion;
            }

        } else if (sample instanceof Set) {
            for (Object o : c.toArray()) {
                Set set = (Set) o;
                processed.add(processCollectionForMap(set)); // recursion;
            }

        } else {
            for (Object o : c.toArray()) {
                processed.add(o);
            }
        }
        return processed;
    }


    /**
     * Build mappings for data to be indexed. Setup String fields as not_analyzed and analyzed,
     * where the analyzed field is named {name}_ug_analyzed
     *
     * @param builder Add JSON object to this builder.
     * @param type ElasticSearch type of entity.
     * @return Content builder with JSON for mapping.
     *
     * @throws java.io.IOException On JSON generation error.
     */
    public static XContentBuilder createDoubleStringIndexMapping(
            XContentBuilder builder, String type) throws IOException {

        builder = builder
            .startObject()
                .startObject(type)
                    .startArray("dynamic_templates")

                        // any string with field name that ends with _ug_analyzed gets analyzed
                        .startObject()
                            .startObject("template_1")
                                .field("match", "*" + ANALYZED_SUFFIX)
                                .field("match_mapping_type", "string")
                                .startObject("mapping")
                                    .field("type", "string")
                                    .field("index", "analyzed")
                                .endObject()
                            .endObject()
                        .endObject()

                        // all other strings are not analyzed
                        .startObject()
                            .startObject("template_2")
                                .field("match", "*")
                                .field("match_mapping_type", "string")
                                .startObject("mapping")
                                    .field("type", "string")
                                    .field("index", "not_analyzed")
                                .endObject()
                            .endObject()
                        .endObject()
                
                        // fields location_ug_geo get geo-indexed
                        .startObject()
                            .startObject("template_3")
                                .field("match", "location" + GEO_SUFFIX)
                                .startObject("mapping")
                                    .field("type", "geo_point")
                                .endObject()
                            .endObject()
                        .endObject()

                    .endArray()
                .endObject()
            .endObject();

        return builder;
    }


    public void refresh() {
        client.admin().indices().prepareRefresh( indexName ).execute().actionGet();
    }
}
