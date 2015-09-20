/*
 *
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *
 */

package org.apache.usergrid.persistence.index.query;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.usergrid.persistence.index.SelectFieldMapping;
import org.apache.usergrid.persistence.index.exceptions.QueryParseException;
import org.apache.usergrid.persistence.index.query.tree.Operand;


/**
 * Our object that represents our parsed query
 */
public class ParsedQuery {

    //fast lookup for our sort predicates
    private Set<String> sortPropertyNames = new HashSet<>();

    //The sort predicates ordered by their input order in the grammar
    private List<SortPredicate> sortPredicateList = new ArrayList<>();

    /**
     * Our map that contains field mappings
     */
    private Map<String, SelectFieldMapping> fieldMappings = new HashMap<>();

    /**
     * The root operand of our query
     */
    private Operand rootOperand;

    private String originalQuery;


    /**
     * Get the original query
     * @return
     */
    public String getOriginalQuery() {
        return originalQuery;
    }


    /**
     * Set the original query
     * @param originalQuery
     */
    public void setOriginalQuery( final String originalQuery ) {
        this.originalQuery = originalQuery;
    }


    /**
     * Set the root operand of our query tree
     * @param rootOperand
     * @return
     */
    public ParsedQuery setRootOperand(final Operand rootOperand){
        this.rootOperand = rootOperand;

        return this;
    }


    /**
     * Get our field mappings from the parsed query
     * @return
     */
    public Collection<SelectFieldMapping> getSelectFieldMappings(){
        return fieldMappings.values();
    }

    /**
     * Add a sort predicate to our list
     */
    public ParsedQuery addSort( final SortPredicate sort ) {
        if ( sort == null ) {
            return this;
        }

        final String sortPropertyName = sort.getPropertyName();

        if ( sortPropertyNames.contains( sortPropertyName ) ) {
            throw new QueryParseException(
                    String.format( "Attempted to set sort order for %s more than once", sort.getPropertyName() ) );
        }


        sortPredicateList.add( sort );
        sortPropertyNames.add( sortPropertyName );

        return this;
    }


    /**
     * Get the list of sort predicates
     * @return
     */
    public List<SortPredicate> getSortPredicates() {
        return sortPredicateList;
    }


    /**
     * Possiblly add a single select to our results
     */
    public ParsedQuery addSelect( final String select ) {

        final String normalizedSelect = getSelect( select );

        if ( normalizedSelect == null ) {
            return this;
        }

        fieldMappings.put( select, new SelectFieldMapping( normalizedSelect, normalizedSelect ) );

        return this;
    }


    /**
     * Use this when the user does a "select id:mynewidname " in the select clause of the grammar
     * @param select
     * @param output
     * @return
     */
    public ParsedQuery addSelect( final String select, final String output ) {
        final String normalizedSelect = getSelect( select );

        if ( normalizedSelect == null ) {
            return this;
        }

        final String normalizedOutput = getSelect( output );

        if ( normalizedOutput == null ) {
            return this;
        }


        fieldMappings.put( select, new SelectFieldMapping( normalizedSelect, normalizedOutput ) );

        return this;
    }


    /**
     * Get the field for select.  This trims the field and validates the input.  If the field is not valid, null willl
     * be returned.  If null is returned, the caller should short circuit.
     */
    private String getSelect( final String select ) {
        // be paranoid with the null checks because
        // the query parser sometimes flakes out
        if ( select == null ) {
            return null;
        }

        final String trimmedSelect = select.trim();

        if ( trimmedSelect.equals( "*" ) ) {
            return null;
        }

        return trimmedSelect;
    }


    /**
     * Get the root operand
     * @return
     */
    public Operand getRootOperand() {
        return rootOperand;
    }
}
