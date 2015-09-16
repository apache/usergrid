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


import java.util.Stack;
import java.util.UUID;

import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.NestedFilterBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.index.query.TermFilterBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.index.exceptions.IndexException;
import org.apache.usergrid.persistence.index.exceptions.NoFullTextIndexException;
import org.apache.usergrid.persistence.index.exceptions.NoIndexException;
import org.apache.usergrid.persistence.index.query.tree.AndOperand;
import org.apache.usergrid.persistence.index.query.tree.ContainsOperand;
import org.apache.usergrid.persistence.index.query.tree.Equal;
import org.apache.usergrid.persistence.index.query.tree.GreaterThan;
import org.apache.usergrid.persistence.index.query.tree.GreaterThanEqual;
import org.apache.usergrid.persistence.index.query.tree.LessThan;
import org.apache.usergrid.persistence.index.query.tree.LessThanEqual;
import org.apache.usergrid.persistence.index.query.tree.NotOperand;
import org.apache.usergrid.persistence.index.query.tree.OrOperand;
import org.apache.usergrid.persistence.index.query.tree.QueryVisitor;
import org.apache.usergrid.persistence.index.query.tree.WithinOperand;

import com.google.common.base.Optional;

import static org.apache.usergrid.persistence.index.impl.SortBuilder.sortPropertyTermFilter;


/**
 * Visits tree of  parsed Query operands and populates ElasticSearch QueryBuilder that represents the query.
 */
public class EsQueryVistor implements QueryVisitor {
    private static final Logger logger = LoggerFactory.getLogger( EsQueryVistor.class );

    /**
     * Our queryBuilders for query operations
     */
    private final Stack<QueryBuilder> queryBuilders = new Stack<>();

    /**
     * Our queryBuilders for filter operations
     */
    private final Stack<FilterBuilder> filterBuilders = new Stack<>();

    private final GeoSortFields geoSortFields = new GeoSortFields();


    @Override
    public void visit( AndOperand op ) throws IndexException {


        op.getLeft().visit( this );
        op.getRight().visit( this );

        //get all the right
        final QueryBuilder rightQuery = queryBuilders.pop();
        final FilterBuilder rightFilter = filterBuilders.pop();


        //get all the left
        final QueryBuilder leftQuery = queryBuilders.pop();
        final FilterBuilder leftFilter = filterBuilders.pop();


        //push our boolean filters


        final boolean useLeftQuery = use( leftQuery );
        final boolean useRightQuery = use( rightQuery );

        /**
         * We use a left and a right, add our boolean query
         */
        if ( useLeftQuery && useRightQuery ) {
            final BoolQueryBuilder qb = QueryBuilders.boolQuery().must(leftQuery).must(rightQuery);
            queryBuilders.push( qb );
        }
        //only use the left
        else if ( useLeftQuery ) {
            queryBuilders.push( leftQuery );
        }
        //only use the right
        else if ( useRightQuery ) {
            queryBuilders.push( rightQuery );
        }
        //put in an empty in case we're not the root.  I.E X and Y and Z
        else {
            queryBuilders.push( NoOpQueryBuilder.INSTANCE );
        }

        //possibly use neither if the is a no-op


        final boolean useLeftFilter = use( leftFilter );
        final boolean useRightFilter = use( rightFilter );

        //use left and right
        if ( useLeftFilter && useRightFilter ) {
            final BoolFilterBuilder fb = FilterBuilders.boolFilter().must(leftFilter).must(rightFilter);
            filterBuilders.push( fb );
        }

        //only use left
        else if ( useLeftFilter ) {
            filterBuilders.push( leftFilter );
        }
        //only use right
        else if ( useRightFilter ) {
            filterBuilders.push( rightFilter );
        }
        //push in a no-op in case we're not the root   I.E X and Y and Z
        else {
            filterBuilders.push( NoOpFilterBuilder.INSTANCE );
        }
    }


    @Override
    public void visit( OrOperand op ) throws IndexException {

        op.getLeft().visit( this );
        op.getRight().visit( this );

        final QueryBuilder rightQuery = queryBuilders.pop();
        final FilterBuilder rightFilter = filterBuilders.pop();


        //get all the left
        final QueryBuilder leftQuery = queryBuilders.pop();
        final FilterBuilder leftFilter = filterBuilders.pop();


        final boolean useLeftQuery = use( leftQuery );
        final boolean useRightQuery = use(rightQuery);

        //push our boolean filters
        if ( useLeftQuery && useRightQuery ) {
            //when we issue an OR query in usergrid, 1 or more of the terms should match.  When doing bool query in ES, there is no requirement for more than 1 to match, where as in a filter more than 1 must match
            final BoolQueryBuilder qb = QueryBuilders.boolQuery().should( leftQuery ).should(rightQuery).minimumNumberShouldMatch(
                1);
            queryBuilders.push( qb );
        }
        else if ( useLeftQuery ) {
            queryBuilders.push( leftQuery );
        }
        else if ( useRightQuery ) {
            queryBuilders.push( rightQuery );
        }

        //put in an empty in case we're not the root.  I.E X or Y or Z
        else {
            queryBuilders.push( NoOpQueryBuilder.INSTANCE );
        }


        final boolean useLeftFilter = use( leftFilter );
        final boolean useRightFilter = use(rightFilter);

        //use left and right
        if ( useLeftFilter && useRightFilter ) {
            final BoolFilterBuilder fb = FilterBuilders.boolFilter().should( leftFilter ).should( rightFilter );
            filterBuilders.push( fb );
        }

        //only use left
        else if ( useLeftFilter ) {
            filterBuilders.push( leftFilter );
        }
        //only use right
        else if ( useRightFilter ) {
            filterBuilders.push( rightFilter );
        }
        //put in an empty in case we're not the root.  I.E X or Y or Z
        else {
            filterBuilders.push( NoOpFilterBuilder.INSTANCE );
        }
    }


    @Override
    public void visit( NotOperand op ) throws IndexException {

        //we need to know if we're the root entry for building our queries correctly
        final boolean rootNode = queryBuilders.empty() && filterBuilders.isEmpty();

        op.getOperation().visit( this );

        //push our not operation into our query

        final QueryBuilder notQueryBuilder = queryBuilders.pop();

        if ( use( notQueryBuilder ) ) {
            final QueryBuilder notQuery = QueryBuilders.boolQuery().mustNot(notQueryBuilder);
            queryBuilders.push( notQuery  );
        }
        else {
            queryBuilders.push( NoOpQueryBuilder.INSTANCE );
        }

        final FilterBuilder notFilterBuilder = filterBuilders.pop();

        //push the filter in
        if ( use( notFilterBuilder ) ) {

            final FilterBuilder notFilter = FilterBuilders.boolFilter().mustNot( notFilterBuilder ) ;

            //just the root node
            if(!rootNode) {
                filterBuilders.push( notFilter );
            }
            //not the root node, we have to select all to subtract from with the NOT statement
            else{
                final FilterBuilder selectAllFilter = FilterBuilders.boolFilter().must( FilterBuilders.matchAllFilter()) .must( notFilter );
                filterBuilders.push( selectAllFilter );
            }

        }
        else {
            filterBuilders.push( NoOpFilterBuilder.INSTANCE );
        }
    }


    @Override
    public void visit( ContainsOperand op ) throws NoFullTextIndexException {
        final String name = op.getProperty().getValue().toLowerCase();
        final String value = op.getLiteral().getValue().toString().toLowerCase();


        // or field is just a string that does need a prefix
        if ( value.indexOf( "*" ) != -1 ) {
            final WildcardQueryBuilder wildcardQuery =
                    QueryBuilders.wildcardQuery( IndexingUtils.FIELD_STRING_NESTED, value );
            queryBuilders.push( fieldNameTerm( name, wildcardQuery ) );
        }
        else {
            final MatchQueryBuilder termQuery = QueryBuilders.matchQuery( IndexingUtils.FIELD_STRING_NESTED, value );

            queryBuilders.push( fieldNameTerm( name, termQuery ) );
        }


        //no op for filters, push an empty operation

        //TODO, validate this works
        filterBuilders.push( NoOpFilterBuilder.INSTANCE );
    }


    @Override
    public void visit( WithinOperand op ) {

        final String name = op.getProperty().getValue().toLowerCase();


        float lat = op.getLatitude().getFloatValue();
        float lon = op.getLongitude().getFloatValue();
        float distance = op.getDistance().getFloatValue();


        final FilterBuilder fb =
                FilterBuilders.geoDistanceFilter( IndexingUtils.FIELD_LOCATION_NESTED ).lat( lat ).lon( lon )
                              .distance( distance, DistanceUnit.METERS );


        filterBuilders.push( fieldNameTerm( name, fb ) );


        //create our geo-sort based off of this point specified

        //this geoSort won't has a sort on it

        final GeoDistanceSortBuilder geoSort =
                SortBuilders.geoDistanceSort( IndexingUtils.FIELD_LOCATION_NESTED ).unit( DistanceUnit.METERS )
                            .geoDistance(GeoDistance.SLOPPY_ARC).point(lat, lon);

        final TermFilterBuilder sortPropertyName = sortPropertyTermFilter(name);

        geoSort.setNestedFilter( sortPropertyName );


        geoSortFields.addField(name, geoSort);
        //no op for query, push


        queryBuilders.push( NoOpQueryBuilder.INSTANCE );
    }


    @Override
    public void visit( LessThan op ) throws NoIndexException {
        final String name = op.getProperty().getValue().toLowerCase();
        final Object value = op.getLiteral().getValue();


        final RangeFilterBuilder termQuery =
                FilterBuilders.rangeFilter( getFieldNameForType( value ) ).lt(sanitize(value));


        queryBuilders.push( NoOpQueryBuilder.INSTANCE );

        //we do this by query, push empty

        filterBuilders.push( fieldNameTerm( name, termQuery ) );
    }


    @Override
    public void visit( LessThanEqual op ) throws NoIndexException {
        final String name = op.getProperty().getValue().toLowerCase();
        final Object value = op.getLiteral().getValue();


        final RangeFilterBuilder termQuery =
                FilterBuilders.rangeFilter( getFieldNameForType( value ) ).lte(sanitize(value));


        queryBuilders.push( NoOpQueryBuilder.INSTANCE );

        filterBuilders.push( fieldNameTerm( name, termQuery ) );
    }


    @Override
    public void visit( Equal op ) throws NoIndexException {
        final String name = op.getProperty().getValue().toLowerCase();
        final Object value = op.getLiteral().getValue();

        //special case so we support our '*' char with wildcard, also should work for uuids
        if ( value instanceof String || value instanceof UUID ) {
            final String stringValue = ((value instanceof String) ? (String)value : value.toString()).toLowerCase().trim();

            // or field is just a string that does need a prefix us a query
            if ( stringValue.contains( "*" ) ) {

                //Because of our legacy behavior, where we match CCCC*, we need to use the unanalyzed string to ensure that
                //we start
                final WildcardQueryBuilder wildcardQuery =
                        QueryBuilders.wildcardQuery( IndexingUtils.FIELD_STRING_NESTED_UNANALYZED, stringValue );
                queryBuilders.push( fieldNameTerm( name, wildcardQuery ) );
                filterBuilders.push( NoOpFilterBuilder.INSTANCE );
                return;
            }

            //it's an exact match, use a filter
            final TermFilterBuilder termFilter =
                    FilterBuilders.termFilter( IndexingUtils.FIELD_STRING_NESTED_UNANALYZED, stringValue );

            queryBuilders.push( NoOpQueryBuilder.INSTANCE );
            filterBuilders.push( fieldNameTerm( name, termFilter ) );

            return;
        }

        // assume all other types need prefix

        final TermFilterBuilder termQuery =
                FilterBuilders.termFilter(getFieldNameForType(value), sanitize(value));

        filterBuilders.push( fieldNameTerm( name, termQuery ) );

        queryBuilders.push( NoOpQueryBuilder.INSTANCE );
    }


    @Override
    public void visit( GreaterThan op ) throws NoIndexException {
        final String name = op.getProperty().getValue().toLowerCase();
        final Object value = op.getLiteral().getValue();


        final RangeFilterBuilder rangeQuery =
                FilterBuilders.rangeFilter( getFieldNameForType( value ) ).gt(sanitize(value));

        filterBuilders.push( fieldNameTerm( name, rangeQuery ) );

        queryBuilders.push( NoOpQueryBuilder.INSTANCE );
    }


    @Override
    public void visit( GreaterThanEqual op ) throws NoIndexException {
        String name = op.getProperty().getValue().toLowerCase();
        Object value = op.getLiteral().getValue();


        final RangeFilterBuilder rangeQuery =
                FilterBuilders.rangeFilter( getFieldNameForType( value ) ).gte(sanitize(value));

        filterBuilders.push(fieldNameTerm(name, rangeQuery));

        queryBuilders.push( NoOpQueryBuilder.INSTANCE );
    }


    @Override
    public Optional<FilterBuilder> getFilterBuilder() {
        if ( filterBuilders.empty() ) {
            return Optional.absent();
        }

        final FilterBuilder builder = filterBuilders.peek();

        if ( !use( builder ) ) {
            return Optional.absent();
        }

        return Optional.of( builder );
    }


    @Override
    public Optional<QueryBuilder> getQueryBuilder() {
        if ( queryBuilders.isEmpty() ) {
            return Optional.absent();
        }

        final QueryBuilder builder = queryBuilders.peek();

        if ( !use( builder ) ) {
            return Optional.absent();
        }


        return Optional.of( builder );
    }


    @Override
    public GeoSortFields getGeoSorts() {
        return geoSortFields;
    }


    /**
     * Generate the field name term for the field name  for queries
     */
    private NestedQueryBuilder fieldNameTerm( final String fieldName, final QueryBuilder fieldValueQuery ) {

        final BoolQueryBuilder booleanQuery = QueryBuilders.boolQuery();

        booleanQuery.must( QueryBuilders.termQuery(IndexingUtils.FIELD_NAME_NESTED, fieldName) );

        booleanQuery.must( fieldValueQuery );


        return QueryBuilders.nestedQuery(IndexingUtils.ENTITY_FIELDS, booleanQuery);
    }


    /**
     * Generate the field name term for the field name for filters
     */
    private NestedFilterBuilder fieldNameTerm( final String fieldName, final FilterBuilder fieldValueBuilder ) {

        final BoolFilterBuilder booleanQuery = FilterBuilders.boolFilter();

        booleanQuery.must( FilterBuilders.termFilter( IndexingUtils.FIELD_NAME_NESTED, fieldName ) );

        booleanQuery.must( fieldValueBuilder );


        return FilterBuilders.nestedFilter( IndexingUtils.ENTITY_FIELDS, booleanQuery );
    }


    /**
     * Get the field name for the primitive type
     */
    private String getFieldNameForType( final Object object ) {
        if ( object instanceof String || object instanceof UUID) {
            return IndexingUtils.FIELD_STRING_NESTED;
        }

        if ( object instanceof Boolean ) {
            return IndexingUtils.FIELD_BOOLEAN_NESTED;
        }


        if ( object instanceof Integer || object instanceof Long ) {
            return IndexingUtils.FIELD_LONG_NESTED;
        }

        if ( object instanceof Float || object instanceof Double ) {
            return IndexingUtils.FIELD_DOUBLE_NESTED;
        }



        throw new UnsupportedOperationException(
                "Unkown search type of " + object.getClass().getName() + " encountered" );
    }


    /**
     * Lowercase our input
     */
    private Object sanitize( final Object input ) {
        if ( input instanceof String ) {
            return ( ( String ) input ).toLowerCase();
        }

        if ( input instanceof UUID ) {
            return input.toString().toLowerCase() ;
        }

        return input;
    }


    /**
     * Return false if our element is a no-op, true otherwise
     */
    private boolean use( final QueryBuilder queryBuilder ) {
        return queryBuilder != NoOpQueryBuilder.INSTANCE;
    }


    /**
     * Return false if our element is a no-op, true otherwise
     */
    private boolean use( final FilterBuilder filterBuilder ) {
        return filterBuilder != NoOpFilterBuilder.INSTANCE;
    }
}
