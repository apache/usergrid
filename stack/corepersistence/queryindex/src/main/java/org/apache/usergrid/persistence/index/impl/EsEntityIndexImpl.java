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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
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

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.usergrid.persistence.index.impl.IndexingUtils.BOOLEAN_PREFIX;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.DOC_ID_SEPARATOR_SPLITTER;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.ENTITYID_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.NUMBER_PREFIX;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.STRING_PREFIX;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.common.xcontent.XContentFactory;


/**
 * Implements index using ElasticSearch Java API.
 */
public class EsEntityIndexImpl implements EntityIndex {

    private static final Logger log = LoggerFactory.getLogger(EsEntityIndexImpl.class);

    private static final AtomicBoolean mappingsCreated = new AtomicBoolean(false);

    private final String indexName;

    private final ApplicationScope applicationScope;

    private final Client client;

    private final int cursorTimeout;

    private final IndexFig config;

    @Inject
    public EsEntityIndexImpl(
            @Assisted final ApplicationScope appScope, 
            final IndexFig config, 
            final EsProvider provider) {

        ValidationUtils.validateApplicationScope(appScope);

        try {
            this.applicationScope = appScope;
            this.client = provider.getClient();
            this.config = config;
            this.cursorTimeout = config.getQueryCursorTimeout();
            this.indexName = IndexingUtils.createIndexName(config.getIndexPrefix(), appScope);

            initIndex();

        } catch ( IOException ex ) {
            throw new RuntimeException("Error initializing ElasticSearch mappings or index", ex);
        }
    }


    private void initIndex() throws IOException {

        try {
            if ( !mappingsCreated.getAndSet(true) ) {
                createMappings();
            }

            AdminClient admin = client.admin();
            CreateIndexResponse cir = admin.indices().prepareCreate(indexName).execute().actionGet();
            log.debug("Created new Index Name [{}] ACK=[{}]", indexName, cir.isAcknowledged());

            admin.indices().prepareRefresh(indexName).execute().actionGet();

            try {
                // TODO: figure out what refresh above is not enough to ensure index is ready
                Thread.sleep(500);
            } catch (InterruptedException ex) {}

        } catch ( IndexAlreadyExistsException expected ) {
            // this is expected to happen if index already exists
        }
    }


    /**
     * Setup ElasticSearch type mappings as a template that applies to all new indexes.
     * Applies to all indexes that start with our prefix.
     */
    private void createMappings() throws IOException {

        XContentBuilder xcb = IndexingUtils.createDoubleStringIndexMapping(
            XContentFactory.jsonBuilder(), "_default_");

        PutIndexTemplateResponse pitr = client.admin().indices()
            .preparePutTemplate("usergrid_template")
            .setTemplate(config.getIndexPrefix() + "*") 
            .addMapping("_default_", xcb) // set mapping as the default for all types
            .execute()
            .actionGet();
    }


    @Override
    public EntityIndexBatch createBatch() {
        return new EsEntityIndexBatchImpl(applicationScope, client, config, 1000);
    }

    @Override
    public CandidateResults search(final IndexScope indexScope, final Query query) {

        final String indexType = IndexingUtils.createCollectionScopeTypeName(indexScope);

        QueryBuilder qb = query.createQueryBuilder();

        if (log.isDebugEnabled()) {
            log.debug("Searching index {}\n   type {}\n   query {} limit {}", new Object[]{
                this.indexName, indexType, qb.toString().replace("\n", " "), query.getLimit()
            });
        }

        SearchResponse searchResponse;
        if (query.getCursor() == null) {

            SearchRequestBuilder srb
                    = client.prepareSearch(indexName).setTypes(indexType).setScroll(cursorTimeout + "m")
                    .setQuery(qb);

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

                // we do not know the type of the "order by" property and so we do not know what
                // type prefix to use. So, here we add an order by clause for every possible type 
                // that you can order by: string, number and boolean and we ask ElasticSearch 
                // to ignore any fields that are not present.
                final String stringFieldName = STRING_PREFIX + sp.getPropertyName();
                final FieldSortBuilder stringSort
                        = SortBuilders.fieldSort(stringFieldName).order(order).ignoreUnmapped(true);
                srb.addSort(stringSort);
                log.debug("   Sort: {} order by {}", stringFieldName, order.toString());

                final String numberFieldName = NUMBER_PREFIX + sp.getPropertyName();
                final FieldSortBuilder numberSort
                        = SortBuilders.fieldSort(numberFieldName).order(order).ignoreUnmapped(true);
                srb.addSort(numberSort);
                log.debug("   Sort: {} order by {}", numberFieldName, order.toString());

                final String booleanFieldName = BOOLEAN_PREFIX + sp.getPropertyName();
                final FieldSortBuilder booleanSort
                        = SortBuilders.fieldSort(booleanFieldName).order(order).ignoreUnmapped(true);
                srb.addSort(booleanSort);
                log.debug("   Sort: {} order by {}", booleanFieldName, order.toString());
            }

            searchResponse = srb.execute().actionGet();
        } else {
            String scrollId = query.getCursor();
            if (scrollId.startsWith("\"")) {
                scrollId = scrollId.substring(1);
            }
            if (scrollId.endsWith("\"")) {
                scrollId = scrollId.substring(0, scrollId.length() - 1);
            }
            log.debug("Executing query with cursor: {} ", scrollId);

            SearchScrollRequestBuilder ssrb = 
                    client.prepareSearchScroll(scrollId).setScroll(cursorTimeout + "m");
            searchResponse = ssrb.execute().actionGet();
        }

        SearchHits hits = searchResponse.getHits();
        log.debug("   Hit count: {} Total hits: {}", hits.getHits().length, hits.getTotalHits());

        List<CandidateResult> candidates = new ArrayList<CandidateResult>();

        for (SearchHit hit : hits.getHits()) {

            String[] idparts = hit.getId().split(DOC_ID_SEPARATOR_SPLITTER);
            String id = idparts[0];
            String type = idparts[1];
            String version = idparts[2];

            Id entityId = new SimpleId(UUID.fromString(id), type);

            candidates.add(new CandidateResult(entityId, UUID.fromString(version)));
        }

        CandidateResults candidateResults = new CandidateResults(query, candidates);

        if (candidates.size() >= query.getLimit()) {
            candidateResults.setCursor(searchResponse.getScrollId());
            log.debug("   Cursor = " + searchResponse.getScrollId());
        }

        return candidateResults;
    }

    public void refresh() {
        client.admin().indices().prepareRefresh(indexName).execute().actionGet();
        log.debug("Refreshed index: " + indexName);
    }

    @Override
    public CandidateResults getEntityVersions(final IndexScope scope, final Id id) {
        Query query = new Query();
        query.addEqualityFilter(ENTITYID_FIELDNAME, id.getUuid().toString());
        CandidateResults results = search(scope, query);
        return results;
    }

    /**
     * For testing only.
     */
    public void deleteIndex() {
        AdminClient adminClient = client.admin();
        DeleteIndexResponse response = adminClient.indices().prepareDelete(indexName).get();
        if (response.isAcknowledged()) {
            log.info("Deleted index: " + indexName);
        } else {
            log.info("Failed to delete index " + indexName);
        }
    }

}
