/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.index.impl;


import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermFilterBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
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


    public SearchRequestBuilder getBuilder( final SearchEdge searchEdge, final SearchTypes searchTypes,
                                            final ParsedQuery query, final int limit ) {

        Preconditions
                .checkArgument( limit <= EntityIndex.MAX_LIMIT, "limit is greater than max " + EntityIndex.MAX_LIMIT );

        SearchRequestBuilder srb =
                esProvider.getClient().prepareSearch( alias.getReadAlias() ).setTypes( IndexingUtils.ES_ENTITY_TYPE )
                          .setScroll( cursorTimeout + "m" );

        //TODO, make this work
        //        searchTypes.getTypeNames( applicationScope )

        final QueryVisitor visitor = visitParsedQuery( query );

        srb.setQuery( createQueryBuilder( searchEdge, visitor, searchTypes ) );

        final Optional<FilterBuilder> fb = visitor.getFilterBuilder();

        if ( fb.isPresent() ) {
            srb.setPostFilter( fb.get() );
        }


        srb = srb.setFrom( 0 ).setSize( limit );

        //no sort predicates, sort by edge time descending, entity id second
        if ( query.getSortPredicates().size() == 0 ) {
            //sort by the edge timestamp
            srb.addSort( SortBuilders.fieldSort( IndexingUtils.EDGE_TIMESTAMP_FIELDNAME ).order( SortOrder.DESC ) );

            //sort by the entity id if our times are equal
            srb.addSort( SortBuilders.fieldSort( IndexingUtils.ENTITY_ID_FIELDNAME ).order( SortOrder.ASC ) );
        }

        //we have sort predicates, sort them
        for ( SortPredicate sp : query.getSortPredicates() ) {


            // we do not know the type of the "order by" property and so we do not know what
            // type prefix to use. So, here we add an order by clause for every possible type
            // that you can order by: string, number and boolean and we ask ElasticSearch
            // to ignore any fields that are not present.

            final SortOrder order = sp.getDirection().toEsSort();
            final String propertyName = sp.getPropertyName();


            srb.addSort( createSort( order, IndexingUtils.SORT_FIELD_STRING, propertyName ) );


            srb.addSort( createSort( order, IndexingUtils.SORT_FIELD_INT, propertyName ) );

            srb.addSort( createSort( order, IndexingUtils.SORT_FIELD_DOUBLE, propertyName ) );

            srb.addSort( createSort( order, IndexingUtils.SORT_FIELD_BOOLEAN, propertyName ) );


            srb.addSort( createSort( order, IndexingUtils.SORT_FIELD_LONG, propertyName ) );

            srb.addSort( createSort( order, IndexingUtils.SORT_FIELD_FLOAT, propertyName ) );
        }
        return srb;
    }


    public QueryBuilder createQueryBuilder( final SearchEdge searchEdge, final QueryVisitor visitor,
                                            final SearchTypes searchTypes ) {
        String context = createContextName( applicationScope, searchEdge );


        // Add our filter for context to our query for fast execution.
        // Fast because it utilizes bitsets internally. See this post for more detail.
        // http://www.elasticsearch.org/blog/all-about-elasticsearch-filter-bitsets/

        // TODO evaluate performance when it's an all query.
        // Do we need to put the context term first for performance?

        //make sure we have entity in the context
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        boolQueryBuilder.must( QueryBuilders.termQuery( IndexingUtils.EDGE_SEARCH_FIELDNAME, context ) );


        /**
         * Get the scopes and add them
         */


        final String[] sourceTypes = searchTypes.getTypeNames( applicationScope );

        //add all our types, 1 type must match per query
        boolQueryBuilder.must( QueryBuilders.termsQuery( IndexingUtils.ENTITY_TYPE_FIELDNAME, sourceTypes )
                                            .minimumMatch( 1 ) );

        Optional<QueryBuilder> queryBuilder = visitor.getQueryBuilder();

        if ( queryBuilder.isPresent() ) {
            boolQueryBuilder.must( queryBuilder.get() );
        }

        return boolQueryBuilder;
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

        final TermFilterBuilder propertyFilter = FilterBuilders.termFilter( IndexingUtils.FIELD_NAME, propertyName );


        return SortBuilders.fieldSort( fieldName ).order( sortOrder ).ignoreUnmapped( true )
                           .setNestedFilter( propertyFilter );
    }
}
