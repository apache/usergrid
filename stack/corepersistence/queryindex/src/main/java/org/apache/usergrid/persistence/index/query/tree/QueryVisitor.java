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

package org.apache.usergrid.persistence.index.query.tree;


import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.usergrid.persistence.index.exceptions.NoFullTextIndexException;
import org.apache.usergrid.persistence.index.exceptions.NoIndexException;
import org.apache.usergrid.persistence.index.exceptions.IndexException;
import org.apache.usergrid.persistence.index.impl.GeoSortFields;
import org.apache.usergrid.persistence.index.query.SortPredicate;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;

import com.google.common.base.Optional;


/**
 * Interface for visiting nodes in our AST as we produce
 *
 * @author tnine
 */
public interface QueryVisitor {

    /**
     *
     * @param op
     * @throws IndexException
     */
    void visit( AndOperand op ) throws IndexException;

    /**
     * @param op
     * @throws IndexException
     */
    void visit( OrOperand op ) throws IndexException;

    /**
     * @param op
     * @throws IndexException
     */
    void visit( NotOperand op ) throws IndexException;

    /**
     * @param op
     * @throws NoIndexException
     */
    void visit( LessThan op ) throws NoIndexException;

    /**
     * @param op
     * @throws NoFullTextIndexException
     */
    void visit( ContainsOperand op ) throws NoFullTextIndexException;

    /**
     * @param op
     */
    void visit( WithinOperand op );

    /**
     * @param op
     * @throws NoIndexException
     */
    void visit( LessThanEqual op ) throws NoIndexException;

    /**
     * @param op
     * @throws NoIndexException
     */
    void visit( Equal op ) throws NoIndexException;

    /**
     * @param op
     * @throws NoIndexException
     */
    void visit( GreaterThan op ) throws NoIndexException;

    /**
     * @param op
     * @throws NoIndexException
     */
    void visit( GreaterThanEqual op ) throws NoIndexException;


    /**
     * Return any filters created during parsing
     * @return
     */
	Optional<FilterBuilder> getFilterBuilder();



    /**
     * Return any querybuilders
     * @return
     */
	Optional<QueryBuilder> getQueryBuilder();

    /**
     * Some searches, such as geo have a side effect of adding a geo sort.  Get any sorts that are side effects
     * of the query terms, in the order they should be applied.  Note that user specified sort orders will trump
     * these sorts
     *
     * @return The GeoSortFields  null safe
     */
    GeoSortFields getGeoSorts();
}
