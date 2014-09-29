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


import com.google.common.base.Joiner;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.utils.IndexValidationUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.ArrayField;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.EntityObjectField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.FloatField;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.ListField;
import org.apache.usergrid.persistence.model.field.LocationField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.SetField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;
import org.apache.usergrid.persistence.model.field.value.EntityObject;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implements index using ElasticSearch Java API.
 */
public class EsEntityIndexImpl implements EntityIndex {

    private static final Logger log = LoggerFactory.getLogger(EsEntityIndexImpl.class);

    private final String indexName;

    private final String indexType;

    private final IndexScope indexScope;

    private final Client client;

    // Keep track of what types we have already initialized to avoid cost 
    // of attempting to init them again. Used in the initType() method.
    private Set<String> knownTypes = new TreeSet<String>();

    private final boolean refresh;
    private final int cursorTimeout;

    private final AtomicLong indexedCount = new AtomicLong(0L);
    private final AtomicDouble averageIndexTime = new AtomicDouble(0);

    public static final String STRING_PREFIX = "su_";
    public static final String ANALYZED_STRING_PREFIX = "sa_";
    public static final String GEO_PREFIX = "go_";
    public static final String NUMBER_PREFIX = "nu_";
    public static final String BOOLEAN_PREFIX = "bu_";

    public static final String ENTITYID_FIELDNAME = "zzz_entityid_zzz";

    public static final String DOC_ID_SEPARATOR = "|";
    public static final String DOC_ID_SEPARATOR_SPLITTER = "\\|";

    // These are not allowed in document type names: _ . , | #
    public static final String DOC_TYPE_SEPARATOR = "^";
    public static final String DOC_TYPE_SEPARATOR_SPLITTER = "\\^";

    public static final String INDEX_NAME_SEPARATOR = "^";

       
    @Inject
    public EsEntityIndexImpl(
            @Assisted final IndexScope indexScope,
            IndexFig config,
            EsProvider provider
    ) {

        IndexValidationUtils.validateIndexScope( indexScope );

        try {
            this.indexScope = indexScope;

            this.client = provider.getClient();

            this.indexName = createIndexName( config.getIndexPrefix(), indexScope);
            this.indexType = createCollectionScopeTypeName( indexScope );

            this.refresh = config.isForcedRefresh();
            this.cursorTimeout = config.getQueryCursorTimeout();

        } catch ( Exception e ) {
            log.error("Error setting up index", e);
            throw e;
        }

        AdminClient admin = client.admin();
        try {
            CreateIndexResponse r = admin.indices().prepareCreate(indexName).execute().actionGet();
            log.debug("Created new Index Name [{}] ACK=[{}]", indexName, r.isAcknowledged());

            client.admin().indices().prepareRefresh( indexName ).execute().actionGet();

            try {
                // TODO: figure out what refresh above is not enough to ensure index is ready
                Thread.sleep(500);
            } catch (InterruptedException ex) {}

        } catch (IndexAlreadyExistsException ignored) {
            // expected
        }
    }

  
    /**
     * Create ElasticSearch mapping for each type of Entity.
     */
    private void initType( String typeName ) {

        // no need for synchronization here, it's OK if we init attempt to init type multiple times
        if ( knownTypes.contains( typeName )) {
            return;
        }

        AdminClient admin = client.admin();
        try {
            XContentBuilder mxcb = EsEntityIndexImpl
                .createDoubleStringIndexMapping(jsonBuilder(), typeName);

            admin.indices().preparePutMapping(indexName)
                .setType(typeName).setSource(mxcb).execute().actionGet();

            admin.indices().prepareGetMappings(indexName)
                .addTypes(typeName).execute().actionGet();

//            log.debug("Created new type mapping");
//            log.debug("   Scope application: " + indexScope.getApplication());
//            log.debug("   Scope owner: " + indexScope.getOwner());
//            log.debug("   Type name: " + typeName);

            knownTypes.add( typeName );

        } catch (IndexAlreadyExistsException ignored) {
            // expected
        } 
        catch (IOException ex) {
            throw new RuntimeException("Exception initing type " + typeName 
                + " in app " + indexScope.getApplication().toString());
        }
    }


    /**
     * Create the index name based on our prefix+appUUID+AppType
     * @param prefix
     * @param indexScope
     * @return
     */
    private String createIndexName( 
            String prefix, IndexScope indexScope) {
        StringBuilder sb = new StringBuilder();
        String sep = INDEX_NAME_SEPARATOR;
        sb.append( prefix ).append(sep);
        sb.append( indexScope.getApplication().getUuid() ).append(sep);
        sb.append( indexScope.getApplication().getType() );
        return sb.toString();
    }


    /**
     * Create the index doc from the given entity
     * @param entity
     * @return
     */
    private String createIndexDocId(Entity entity) {
        return createIndexDocId(entity.getId(), entity.getVersion());
    }


    /**
     * Create the doc Id. This is the entitie's type + uuid + version
     * @param entityId
     * @param version
     * @return
     */
    private String createIndexDocId(Id entityId, UUID version) {
        StringBuilder sb = new StringBuilder();
        String sep = DOC_ID_SEPARATOR;
        sb.append( entityId.getUuid() ).append(sep);
        sb.append( entityId.getType() ).append(sep);
        sb.append( version.toString() );
        return sb.toString();
    }


    /**
     * Create our sub scope.  This is the ownerUUID + type
     * @param scope
     * @return
     */
    private static String createCollectionScopeTypeName( IndexScope scope ) {
        StringBuilder sb = new StringBuilder();
        String sep = DOC_TYPE_SEPARATOR;
        sb.append( scope.getApplication().getUuid() ).append(sep);
        sb.append( scope.getApplication().getType() ).append(sep);
        sb.append( scope.getOwner().getUuid() ).append(sep);
        sb.append( scope.getOwner().getType() ).append(sep);
        sb.append( scope.getName() );
        return sb.toString();
    }


    
    @Override
    public void index( Entity entity ) {

        if ( log.isDebugEnabled() ) {
            log.debug("Indexing entity {}:{} in scope\n   app {}\n   owner {}\n   name {}\n   type {}", 
                new Object[] { 
                    entity.getId().getType(), 
                    entity.getId().getUuid(), 
                    indexScope.getApplication(), 
                    indexScope.getOwner(), 
                    indexScope.getName(),
                    indexType
            });
        }

        ValidationUtils.verifyEntityWrite(entity);

        initType( indexType );

        StopWatch timer = null;
        if ( log.isDebugEnabled() ) {
            timer = new StopWatch();
            timer.start();
        }

        Map<String, Object> entityAsMap = EsEntityIndexImpl.entityToMap(entity);

        // need prefix here becuase we index UUIDs as strings
        entityAsMap.put( STRING_PREFIX + ENTITYID_FIELDNAME, 
            entity.getId().getUuid().toString().toLowerCase());

        // let caller add these fields if needed
        // entityAsMap.put("created", entity.getId().getUuid().timestamp();
        // entityAsMap.put("updated", entity.getVersion().timestamp());

        String indexId = EsEntityIndexImpl.this.createIndexDocId(entity);

        log.debug("Indexing entity id {} data {} ", indexId, entityAsMap);

        IndexRequestBuilder irb = client
            .prepareIndex( indexName, this.indexType, indexId)
            .setSource(entityAsMap)
            .setRefresh(refresh);

        irb.execute().actionGet();

        if ( refresh) {
            refresh();
        }

        //log.debug("Indexed Entity with index id " + indexId);

        if ( log.isDebugEnabled() ) {
            timer.stop();
            double average = averageIndexTime.get();
            if ( !averageIndexTime.compareAndSet( 0, timer.getTime() ) ) {
                averageIndexTime.compareAndSet( average, (average + timer.getTime()) / 2.0 );
            }
            long count = indexedCount.addAndGet(1);
            if ( count % 1000 == 0 ) {
                log.debug("Indexed {} entities, average time {}ms", 
                        count, averageIndexTime.get() );
            }
        }
    }

    @Override
    public void deindex( final Id id, final UUID version) {

        if ( log.isDebugEnabled() ) {
            log.debug("De-indexing entity {}:{} in scope\n   app {}\n   owner {}\n   name {} type {}", 
                new Object[] { 
                    id.getType(), 
                    id.getUuid(), 
                    indexScope.getApplication(), 
                    indexScope.getOwner(), 
                    indexScope.getName(),
                    indexType
            });
        }

        String indexId = createIndexDocId( id, version ); 
        client.prepareDelete( indexName, indexType, indexId )
            .setRefresh( refresh )
            .execute().actionGet();

        if ( refresh ) {
            refresh();
        }

        log.debug("Deindexed Entity with index id " + indexId);
    }
    @Override
    public void deindex( Entity entity ) {

        deindex( entity.getId(), entity.getVersion() );
    }

    @Override
    public void deindex( CandidateResult entity ) {

        deindex( entity.getId(), entity.getVersion() );

    }


    @Override
    public CandidateResults search(Query query) {

        QueryBuilder qb = query.createQueryBuilder();

        if ( log.isDebugEnabled() ) {
            log.debug("Searching index {}\n   type {}\n   query {} limit {}", 
                new Object[] { 
                    this.indexName,
                    this.indexType,
                    qb.toString().replace("\n", " "),
                    query.getLimit()
            });
        }
            
        SearchResponse searchResponse;
        if (query.getCursor() == null) {


            SearchRequestBuilder srb = client.prepareSearch(indexName)
                .setTypes( indexType )
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

                final String stringFieldName = STRING_PREFIX + sp.getPropertyName(); 
                final FieldSortBuilder stringSort = SortBuilders
                    .fieldSort( stringFieldName )
                    .order(order)
                    .ignoreUnmapped(true);
                srb.addSort( stringSort );
                log.debug("   Sort: {} order by {}", stringFieldName, order.toString());

                final String numberFieldName = NUMBER_PREFIX + sp.getPropertyName(); 
                final FieldSortBuilder numberSort = SortBuilders
                    .fieldSort( numberFieldName )
                    .order(order)
                    .ignoreUnmapped(true);
                srb.addSort( numberSort );
                log.debug("   Sort: {} order by {}", numberFieldName, order.toString());
            }

            searchResponse = srb.execute().actionGet();

        } else {
            String scrollId = query.getCursor();
            if ( scrollId.startsWith("\"")) {
                scrollId = scrollId.substring(1);
            }
            if ( scrollId.endsWith("\"")) {
                scrollId = scrollId.substring(0, scrollId.length() - 1 );
            }
            log.debug("Executing query with cursor: {} ", scrollId);

            SearchScrollRequestBuilder ssrb = client
                .prepareSearchScroll(scrollId)
                .setScroll( cursorTimeout + "m" );
            searchResponse = ssrb.execute().actionGet();
        }

        SearchHits hits = searchResponse.getHits();
        log.debug("   Hit count: {} Total hits: {}", hits.getHits().length, hits.getTotalHits() );

        List<CandidateResult> candidates = new ArrayList<CandidateResult>();

        for (SearchHit hit : hits.getHits()) {

            String[] idparts = hit.getId().split( DOC_ID_SEPARATOR_SPLITTER );
            String id      = idparts[0];
            String type    = idparts[1];
            String version = idparts[2];

            Id entityId = new SimpleId(UUID.fromString(id), type);

            candidates.add( new CandidateResult( entityId, UUID.fromString(version) ));
        }

        CandidateResults candidateResults = new CandidateResults( query, candidates );

        if ( candidates.size() >= query.getLimit() ) {
            candidateResults.setCursor(searchResponse.getScrollId());
            log.debug("   Cursor = " + searchResponse.getScrollId() );
        } 

        return candidateResults;
    }


    /**
     * Convert Entity to Map. Adding prefixes for types:
     * 
     *    su_ - String unanalyzed field
     *    sa_ - String analyzed field
     *    go_ - Location field
     *    nu_ - Number field
     *    bu_ - Boolean field
     */
    private static Map entityToMap(EntityObject entity) {

        Map<String, Object> entityMap = new HashMap<String, Object>();

        for (Object f : entity.getFields().toArray()) {

            Field field = (Field) f;

            if (f instanceof ListField)  {
                List list = (List) field.getValue();
                entityMap.put(field.getName().toLowerCase(),
                        new ArrayList(processCollectionForMap(list)));

                if ( !list.isEmpty() ) {
                    if ( list.get(0) instanceof String ) {
                        Joiner joiner = Joiner.on(" ").skipNulls();
                        String joined = joiner.join(list);
                        entityMap.put( ANALYZED_STRING_PREFIX + field.getName().toLowerCase(),
                            new ArrayList(processCollectionForMap(list)));
                        
                    }
                }

            } else if (f instanceof ArrayField) {
                List list = (List) field.getValue();
                entityMap.put(field.getName().toLowerCase(),
                        new ArrayList(processCollectionForMap(list)));

            } else if (f instanceof SetField) {
                Set set = (Set) field.getValue();
                entityMap.put(field.getName().toLowerCase(),
                        new ArrayList(processCollectionForMap(set)));

            } else if (f instanceof EntityObjectField) {
                EntityObject eo = (EntityObject)field.getValue();
                entityMap.put(field.getName().toLowerCase(), entityToMap(eo)); // recursion

            // Add type information as field-name prefixes

            } else if (f instanceof StringField) {

                // index in lower case because Usergrid queries are case insensitive
                entityMap.put( ANALYZED_STRING_PREFIX + field.getName().toLowerCase(), 
                        ((String) field.getValue()).toLowerCase());
                entityMap.put( STRING_PREFIX + field.getName().toLowerCase(),
                        ((String) field.getValue()).toLowerCase());

            } else if (f instanceof LocationField) {
                LocationField locField = (LocationField) f;
                Map<String, Object> locMap = new HashMap<String, Object>();

                // field names lat and lon trigger ElasticSearch geo location 
                locMap.put("lat", locField.getValue().getLatitude());
                locMap.put("lon", locField.getValue().getLongtitude());
                entityMap.put( GEO_PREFIX + field.getName().toLowerCase(), locMap);

            } else if ( f instanceof DoubleField
                     || f instanceof FloatField
                     || f instanceof IntegerField
                     || f instanceof LongField ) {

                entityMap.put( NUMBER_PREFIX + field.getName().toLowerCase(), field.getValue());

            } else if ( f instanceof BooleanField ) {

                entityMap.put( BOOLEAN_PREFIX + field.getName().toLowerCase(), field.getValue());

            } else if ( f instanceof UUIDField ) {

                entityMap.put( STRING_PREFIX + field.getName().toLowerCase(),
                    field.getValue().toString() );

            } else {
                entityMap.put(field.getName().toLowerCase(), field.getValue());
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

                        // any string with field name that starts with sa_ gets analyzed
                        .startObject()
                            .startObject("template_1")
                                .field("match", ANALYZED_STRING_PREFIX + "*")
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
                
                        // fields names starting with go_ get geo-indexed
                        .startObject()
                            .startObject("template_3")
                                .field("match", GEO_PREFIX + "location")
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
        log.debug("Refreshed index: " + indexName);
    }

    @Override
    public CandidateResults getEntityVersions(Id id) {
        Query query = new Query();
        query.addEqualityFilter(ENTITYID_FIELDNAME,id.getUuid().toString());
        CandidateResults results = search( query );
        return results;
    }

}
