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
import java.util.Stack;
import org.apache.usergrid.persistence.index.exceptions.NoFullTextIndexException;
import org.apache.usergrid.persistence.index.exceptions.NoIndexException;
import org.apache.usergrid.persistence.index.exceptions.IndexException;
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
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;


/**
 * Visits tree of  parsed Query operands and populates 
 * ElasticSearch QueryBuilder that represents the query.
 */
public class EsQueryVistor implements QueryVisitor {
    Stack<QueryBuilder> stack = new Stack<QueryBuilder>();
    List<FilterBuilder> filterBuilders = new ArrayList<FilterBuilder>();

    @Override
    public void visit( AndOperand op ) throws IndexException {
        op.getLeft().visit( this );
        op.getRight().visit( this );
        stack.push( QueryBuilders.boolQuery().must( stack.pop() ).must(  stack.pop() ));
    }

    @Override
    public void visit( OrOperand op ) throws IndexException {
        op.getLeft().visit( this );
        op.getRight().visit( this );
        stack.push( QueryBuilders.boolQuery().should( stack.pop() ).should(  stack.pop() ));
    }

    @Override
    public void visit( NotOperand op ) throws IndexException {
        op.getOperation().visit( this );
        stack.push( QueryBuilders.boolQuery().mustNot( stack.pop() ));
    }

    @Override
    public void visit( ContainsOperand op ) throws NoFullTextIndexException {
        String name = op.getProperty().getValue();
        Object value = op.getLiteral().getValue();
        if ( value instanceof String ) {
            name = addAnayzedSuffix( name );
        }        
        stack.push( QueryBuilders.matchQuery( name, value ));
    }

    @Override
    public void visit( WithinOperand op ) {

        String name = op.getProperty().getValue();

        float lat = op.getLatitude().getFloatValue();
        float lon = op.getLongitude().getFloatValue();
        float distance = op.getDistance().getFloatValue();

        if ( !name.endsWith( EsEntityIndexImpl.GEO_SUFFIX )) {
            name += EsEntityIndexImpl.GEO_SUFFIX;
        }
        FilterBuilder fb = FilterBuilders.geoDistanceFilter( name )
           .lat( lat ).lon( lon ).distance( distance, DistanceUnit.METERS );

        filterBuilders.add( fb );
    } 

    @Override
    public void visit( LessThan op ) throws NoIndexException {
        String name = op.getProperty().getValue();
        Object value = op.getLiteral().getValue();
        if ( value instanceof String ) {
            name = addAnayzedSuffix( name );
        }
        stack.push( QueryBuilders.rangeQuery( name ).lt( value ));
    }

    @Override
    public void visit( LessThanEqual op ) throws NoIndexException {
        String name = op.getProperty().getValue();
        Object value = op.getLiteral().getValue();
        if ( value instanceof String ) {
            name = addAnayzedSuffix( name );
        }
        stack.push( QueryBuilders.rangeQuery( name ).lte( value ));
    }

    @Override
    public void visit( Equal op ) throws NoIndexException {
        String name = op.getProperty().getValue();
        Object value = op.getLiteral().getValue();
        if ( value instanceof String ) {
            // for equal operation on string, need to use unanalyzed field, leave off the suffix
            value = ((String)value).toLowerCase();
        }
        stack.push( QueryBuilders.termQuery( name, value ));
    }

    @Override
    public void visit( GreaterThan op ) throws NoIndexException {
        String name = op.getProperty().getValue();
        Object value = op.getLiteral().getValue();
        if ( value instanceof String ) {
            name = addAnayzedSuffix( name );
        }
        stack.push( QueryBuilders.rangeQuery( name ).gt( value ) );
    }

    @Override
    public void visit( GreaterThanEqual op ) throws NoIndexException {
        String name = op.getProperty().getValue();
        Object value = op.getLiteral().getValue();
        if ( value instanceof String ) {
            name = addAnayzedSuffix( name );
        }
        stack.push( QueryBuilders.rangeQuery( name ).gte( value ) );
    }

    private String addAnayzedSuffix( String name ) {
        if ( name.endsWith(EsEntityIndexImpl.ANALYZED_SUFFIX) ) {
            return name;
        } 
        return name + EsEntityIndexImpl.ANALYZED_SUFFIX;
    } 

    @Override
    public QueryBuilder getQueryBuilder() {
        if ( stack.isEmpty() ) {
            return null;
        }
        return stack.pop();
    }

    @Override
	public FilterBuilder getFilterBuilder() {

		if ( filterBuilders.size() >  1 ) {

			FilterBuilder andFilter = null;
			for ( FilterBuilder fb : filterBuilders ) {
				if ( andFilter == null ) {
					andFilter = FilterBuilders.andFilter( fb );
				} else {	
					andFilter = FilterBuilders.andFilter( andFilter, fb );
				}
			}	

		} else if ( !filterBuilders.isEmpty() ) {
			return filterBuilders.get(0);
		}
		return null;
	}
}
