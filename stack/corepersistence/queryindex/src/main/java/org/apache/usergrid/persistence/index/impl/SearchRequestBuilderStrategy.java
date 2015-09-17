/*
 *
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
 *
 */
package org.apache.usergrid.persistence.index.impl;


import org.apache.usergrid.persistence.index.IndexAlias;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermFilterBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.index.SearchTypes;
import org.apache.usergrid.persistence.index.exceptions.IndexException;
import org.apache.usergrid.persistence.index.query.ParsedQuery;
import org.apache.usergrid.persistence.index.query.SortPredicate;
import org.apache.usergrid.persistence.index.query.tree.QueryVisitor;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import static org.apache.usergrid.persistence.index.impl.IndexingUtils.createContextName;
import static org.apache.usergrid.persistence.index.impl.SortBuilder.sortPropertyTermFilter;


/**
 * The strategy for creating a search request from a parsed query
 */

public class SearchRequestBuilderStrategy {

    private static final Logger logger = LoggerFactory.getLogger( SearchRequestBuilderStrategy.class );

    private final EsProvider esProvider;
    private final ApplicationScope applicationScope;
    private final IndexAlias alias;
    private final int cursorTimeout;


    public SearchRequestBuilderStrategy( final EsProvider esProvider, final ApplicationScope applicationScope,
                                         final IndexAlias alias, int cursorTimeout ) {

        this.esProvider = esProvider;
        this.applicationScope = applicationScope;
        this.alias = alias;
        this.cursorTimeout = cursorTimeout;
    }


    /**
     * Get the search request builder
     */
    public SearchRequestBuilder getBuilder( final SearchEdge searchEdge, final SearchTypes searchTypes,
                                            final ParsedQuery query, final int limit, final int from ) {

        Preconditions
            .checkArgument( limit <= EntityIndex.MAX_LIMIT, "limit is greater than max " + EntityIndex.MAX_LIMIT );

        SearchRequestBuilder srb =
            esProvider.getClient().prepareSearch( alias.getReadAlias() ).setTypes( IndexingUtils.ES_ENTITY_TYPE )
                      .setSearchType( SearchType.QUERY_THEN_FETCH );


        final QueryVisitor visitor = visitParsedQuery( query );

        final Optional<QueryBuilder> queryBuilder = visitor.getQueryBuilder();

        if ( queryBuilder.isPresent() ) {
            srb.setQuery( queryBuilder.get() );
        }

        srb.setPostFilter( createFilterBuilder( searchEdge, visitor, searchTypes ) );


        srb = srb.setFrom( from ).setSize( limit );


        //if we have a geo field, sort by closest to farthest by default
        final GeoSortFields geoFields = visitor.getGeoSorts();


        //no sort predicates, sort by edge time descending, entity id second
        if ( query.getSortPredicates().size() == 0 ) {
            applyDefaultSortPredicates( srb, geoFields );
        }
        else {
            applySortPredicates( srb, query, geoFields );
        }


        return srb;
    }


    /**
     * Apply our default sort predicate logic
     */
    private void applyDefaultSortPredicates( final SearchRequestBuilder srb, final GeoSortFields geoFields ) {
        //we have geo fields, sort through them in visit order
        for ( String geoField : geoFields.fields() ) {

            final GeoDistanceSortBuilder geoSort = geoFields.applyOrder( geoField, SortOrder.ASC );

            srb.addSort( geoSort );
        }

        //now sort by edge timestamp, then entity id
        //sort by the edge timestamp
        srb.addSort( SortBuilders.fieldSort( IndexingUtils.EDGE_TIMESTAMP_FIELDNAME ).order( SortOrder.DESC ) );

        //sort by the entity id if our times are equal
        srb.addSort( SortBuilders.fieldSort( IndexingUtils.ENTITY_ID_FIELDNAME ).order( SortOrder.ASC ) );

        return;
    }


    /**
     * Invoked when there are sort predicates
     */
    private void applySortPredicates( final SearchRequestBuilder srb, final ParsedQuery query,
                                      final GeoSortFields geoFields ) {


        //we have sort predicates, sort them
        for ( SortPredicate sp : query.getSortPredicates() ) {


            // we do not know the type of the "order by" property and so we do not know what
            // type prefix to use. So, here we add an order by clause for every possible type
            // that you can order by: string, number and boolean and we ask ElasticSearch
            // to ignore any fields that are not present.
            final SortOrder order = sp.getDirection().toEsSort();
            final String propertyName = sp.getPropertyName();


            //if the user specified a geo field in their sort, then honor their sort order and use the point they
            // specified
            if ( geoFields.contains( propertyName ) ) {
                final GeoDistanceSortBuilder geoSort = geoFields.applyOrder( propertyName, SortOrder.ASC );
                srb.addSort( geoSort );
            }

            //apply regular sort logic, since this is not a geo point
            else {


                //sort order is arbitrary if the user changes data types.  Double, long, string, boolean are supported
                //default sort types

                srb.addSort( createSort( order, IndexingUtils.FIELD_DOUBLE_NESTED, propertyName ) );

                srb.addSort( createSort( order, IndexingUtils.FIELD_LONG_NESTED, propertyName ) );

                /**
                 * We always want to sort by the unanalyzed string field to ensure correct ordering
                 */
                srb.addSort( createSort( order, IndexingUtils.FIELD_STRING_NESTED_UNANALYZED, propertyName ) );

                srb.addSort( createSort( order, IndexingUtils.FIELD_BOOLEAN_NESTED, propertyName ) );
            }
        }
    }


    /**
     * Create our filter builder.  We need to restrict our results on edge search, as well as on types, and any filters
     * that came from the grammar.
     */
    private FilterBuilder createFilterBuilder( final SearchEdge searchEdge, final QueryVisitor visitor,
                                               final SearchTypes searchTypes ) {
        String context = createContextName( applicationScope, searchEdge );


        // Add our filter for context to our query for fast execution.
        // Fast because it utilizes bitsets internally. See this post for more detail.
        // http://www.elasticsearch.org/blog/all-about-elasticsearch-filter-bitsets/

        // TODO evaluate performance when it's an all query.
        // Do we need to put the context term first for performance?

        //make sure we have entity in the context
        BoolFilterBuilder boolQueryFilter = FilterBuilders.boolFilter();

        //add our edge search
        boolQueryFilter.must( FilterBuilders.termFilter( IndexingUtils.EDGE_SEARCH_FIELDNAME, context ) );


        /**
         * For the types the user specified, add them to an OR so 1 of them must match
         */
        final String[] sourceTypes = searchTypes.getTypeNames( applicationScope );


        if ( sourceTypes.length > 0 ) {
            final FilterBuilder[] typeTerms = new FilterBuilder[sourceTypes.length];

            for ( int i = 0; i < sourceTypes.length; i++ ) {
                typeTerms[i] = FilterBuilders.termFilter( IndexingUtils.ENTITY_TYPE_FIELDNAME, sourceTypes[i] );
            }

            //add all our types, 1 type must match per query
            boolQueryFilter.must( FilterBuilders.orFilter( typeTerms ) );
        }

        //if we have a filter from our visitor, add it

        Optional<FilterBuilder> queryBuilder = visitor.getFilterBuilder();

        if ( queryBuilder.isPresent() ) {
            boolQueryFilter.must( queryBuilder.get() );
        }

        return boolQueryFilter;
    }


    /**
     * Perform our visit of the query once for efficiency
     */
    private QueryVisitor visitParsedQuery( final ParsedQuery parsedQuery ) {
        QueryVisitor v = new EsQueryVistor();

        if ( parsedQuery.getRootOperand() != null ) {

            try {
                parsedQuery.getRootOperand().visit( v );
            }
            catch ( IndexException ex ) {
                throw new RuntimeException( "Error building ElasticSearch query", ex );
            }
        }

        return v;
    }


    /**
     * Create a sort for the property name and field name specified
     *
     * @param sortOrder The sort order
     * @param fieldName The name of the field for the type
     * @param propertyName The property name the user specified for the sort
     */
    private FieldSortBuilder createSort( final SortOrder sortOrder, final String fieldName,
                                         final String propertyName ) {

        final TermFilterBuilder propertyFilter = sortPropertyTermFilter( propertyName );


        return SortBuilders.fieldSort( fieldName ).order( sortOrder ).setNestedFilter( propertyFilter );
    }
}
