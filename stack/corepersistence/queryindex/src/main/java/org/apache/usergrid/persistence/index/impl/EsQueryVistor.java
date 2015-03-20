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
import java.util.UUID;

import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
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

import com.google.common.base.Joiner;

import static org.apache.usergrid.persistence.index.impl.IndexingUtils.*;


/**
 * Visits tree of  parsed Query operands and populates
 * ElasticSearch QueryBuilder that represents the query.
 */
public class EsQueryVistor implements QueryVisitor {
    private static final Logger logger = LoggerFactory.getLogger( EsQueryVistor.class );

    Stack<QueryBuilder> stack = new Stack<QueryBuilder>();
    List<FilterBuilder> filterBuilders = new ArrayList<FilterBuilder>();


    @Override
    public void visit( AndOperand op ) throws IndexException {


        op.getLeft().visit( this );
        QueryBuilder left = null;

        // special handling for WithinOperand because ElasticSearch wants us to use
        // a filter and not have WithinOperand as part of the actual query itself
        if ( !(op.getLeft() instanceof WithinOperand) ) {
            left = stack.peek();
        }

        op.getRight().visit( this );
        QueryBuilder right = null;

        // special handling for WithinOperand on the right too
        if ( !(op.getRight()instanceof WithinOperand) ) {
            right = stack.peek();
        }

        if ( left == right ) {
            return;
        }

        if ( !(op.getLeft() instanceof WithinOperand) ) {
            left = stack.pop();
        }

        if ( !(op.getRight()instanceof WithinOperand) ) {
            right = stack.pop();
        }

        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        if ( left != null ) {
            qb = qb.must( left );
        }
        if ( right != null ) {
            qb = qb.must( right );
        }

        stack.push( qb );
    }


    @Override
    public void visit( OrOperand op ) throws IndexException {

        op.getLeft().visit( this );
        op.getRight().visit( this );

        QueryBuilder left = null;
        if ( !(op.getLeft()instanceof WithinOperand) ) {
            left = stack.pop();
        }
        QueryBuilder right = null;
        if ( !(op.getRight()instanceof WithinOperand) ) {
            right = stack.pop();
        }

        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        if ( left != null ) {
            qb = qb.should( left );
        }
        if ( right != null ) {
            qb = qb.should( right );
        }

        stack.push( qb );
    }


    @Override
    public void visit( NotOperand op ) throws IndexException {
        op.getOperation().visit( this );

        if ( !(op.getOperation() instanceof WithinOperand) ) {
            stack.push( QueryBuilders.boolQuery().mustNot( stack.pop() ));
        }
    }


    @Override
    public void visit( ContainsOperand op ) throws NoFullTextIndexException {
        String name = op.getProperty().getValue();
        name = name.toLowerCase();
        Object value = op.getLiteral().getValue();

        BoolQueryBuilder qb = QueryBuilders.boolQuery(); // let's do a boolean OR
        qb.minimumNumberShouldMatch(1);

        // field is an entity/array that needs no name prefix
        qb = qb.should( QueryBuilders.matchQuery( name, value ) );

        // OR field is a string and needs the prefix on the name
        qb = qb.should( QueryBuilders.matchQuery( addPrefix( value.toString(), name, true), value));

        stack.push( qb );
    }


    @Override
    public void visit( WithinOperand op ) {

        String name = op.getProperty().getValue();
        name = name.toLowerCase();

        float lat = op.getLatitude().getFloatValue();
        float lon = op.getLongitude().getFloatValue();
        float distance = op.getDistance().getFloatValue();

        if ( !name.startsWith( GEO_PREFIX )) {
            name = GEO_PREFIX + name;
        }

        FilterBuilder fb = FilterBuilders.geoDistanceFilter( name )
           .lat( lat ).lon( lon ).distance( distance, DistanceUnit.METERS );
        filterBuilders.add( fb );
    }


    @Override
    public void visit( LessThan op ) throws NoIndexException {
        String name = op.getProperty().getValue();
        name = name.toLowerCase();
        Object value = op.getLiteral().getValue();
        name = addPrefix( value, name );
        stack.push( QueryBuilders.rangeQuery( name ).lt( value ));
    }


    @Override
    public void visit( LessThanEqual op ) throws NoIndexException {
        String name = op.getProperty().getValue();
        name = name.toLowerCase();
        Object value = op.getLiteral().getValue();
        name = addPrefix( value, name );
        stack.push( QueryBuilders.rangeQuery( name ).lte( value ));
    }


    @Override
    public void visit( Equal op ) throws NoIndexException {
        String name = op.getProperty().getValue();
        name = name.toLowerCase();
        Object value = op.getLiteral().getValue();

        if ( value instanceof String ) {
            String svalue = (String)value;

            BoolQueryBuilder qb = QueryBuilders.boolQuery();  // let's do a boolean OR
            qb.minimumNumberShouldMatch(1);

            // field is an entity/array that does not need a prefix on its name
            // TODO is this right now that we've updated our doc structure?
            // Should this be "must" instead of should?
            qb = qb.should( QueryBuilders.wildcardQuery( name, svalue ) );

            // or field is just a string that does need a prefix
            if ( svalue.indexOf("*") != -1 ) {
                qb = qb.should( QueryBuilders.wildcardQuery( addPrefix( value, name ), svalue ) );
            } else {
                qb = qb.should( QueryBuilders.termQuery(     addPrefix( value, name ), value ));
            }
            stack.push( qb );
            return;
        }

        // assume all other types need prefix
        stack.push( QueryBuilders.termQuery( addPrefix( value, name ), value ));
    }


    @Override
    public void visit( GreaterThan op ) throws NoIndexException {
        String name = op.getProperty().getValue();
        name = name.toLowerCase();
        Object value = op.getLiteral().getValue();
        name = addPrefix( value, name );
        stack.push( QueryBuilders.rangeQuery( name ).gt( value ) );
    }


    @Override
    public void visit( GreaterThanEqual op ) throws NoIndexException {
        String name = op.getProperty().getValue();
        name = name.toLowerCase();
        Object value = op.getLiteral().getValue();
        name = addPrefix( value, name );
        stack.push( QueryBuilders.rangeQuery( name ).gte( value ) );
    }


    private String addPrefix( Object value, String origname ) {
        return addPrefix(value, origname, false);
    }


    private String addPrefix( Object value, String origname, boolean analyzed ) {

        String name = origname;

        // logic to deal with nested property names
        // only add prefix to last name in property
        String[] parts = origname.split("\\.");
        if ( parts.length > 1 ) {
            name = parts[ parts.length - 1 ];
        }

        if ( value instanceof String && analyzed ) {
            name = addAnalyzedStringPrefix( name );

        } else if ( value instanceof String ) {
            name = addStringPrefix( name );

        }else if ( value instanceof Integer ) {
            name = addLongPrefix(name);

        }else if ( value instanceof Long ) {
            name = addLongPrefix(name);

        } else if ( value instanceof Float ) {
            name = addDoublePrefix(name);

        }else if ( value instanceof Float ) {
            name = addDoublePrefix(name);

        } else if ( value instanceof Boolean ) {
            name = addBooleanPrefix(name);

        } else if ( value instanceof UUID ) {
            name = addStringPrefix(name);
        }

        // re-create nested property name
        if ( parts.length > 1 ) {
            parts[parts.length - 1] = name;
            Joiner joiner = Joiner.on(".").skipNulls();
            return joiner.join(parts);
        }

        return name;
    }


    private String addAnalyzedStringPrefix( String name ) {
        if ( name.startsWith( ANALYZED_STRING_PREFIX ) ) {
            return name;
        }
        return ANALYZED_STRING_PREFIX + name;
    }


    private String addStringPrefix( String name ) {
        if ( name.startsWith( STRING_PREFIX ) ) {
            return name;
        }
        return STRING_PREFIX + name;
    }


    private String addDoublePrefix( String name ) {
        if ( name.startsWith( DOUBLE_PREFIX ) ) {
            return name;
        }
        return DOUBLE_PREFIX + name;
    }

    private String addLongPrefix( String name ) {
        if ( name.startsWith( LONG_PREFIX ) ) {
            return name;
        }
        return LONG_PREFIX + name;
    }


    private String addBooleanPrefix( String name ) {
        if ( name.startsWith( BOOLEAN_PREFIX ) ) {
            return name;
        }
        return BOOLEAN_PREFIX + name;
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
