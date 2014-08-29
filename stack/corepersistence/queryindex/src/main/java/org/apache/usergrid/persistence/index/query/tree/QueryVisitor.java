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


import org.apache.usergrid.persistence.index.exceptions.NoFullTextIndexException;
import org.apache.usergrid.persistence.index.exceptions.NoIndexException;
import org.apache.usergrid.persistence.index.exceptions.IndexException;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;


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
    public void visit( AndOperand op ) throws IndexException;

    /**
     * @param op
     * @throws IndexException
     */
    public void visit( OrOperand op ) throws IndexException;

    /**
     * @param op
     * @throws IndexException
     */
    public void visit( NotOperand op ) throws IndexException;

    /**
     * @param op
     * @throws NoIndexException
     */
    public void visit( LessThan op ) throws NoIndexException;

    /**
     * @param op
     * @throws NoFullTextIndexException
     */
    public void visit( ContainsOperand op ) throws NoFullTextIndexException;

    /**
     * @param op
     */
    public void visit( WithinOperand op );

    /**
     * @param op
     * @throws NoIndexException
     */
    public void visit( LessThanEqual op ) throws NoIndexException;

    /**
     * @param op
     * @throws NoIndexException
     */
    public void visit( Equal op ) throws NoIndexException;

    /**
     * @param op
     * @throws NoIndexException
     */
    public void visit( GreaterThan op ) throws NoIndexException;

    /**
     * @param op
     * @throws NoIndexException
     */
    public void visit( GreaterThanEqual op ) throws NoIndexException;

    /** 
     * Returns resulting query builder.
     */
    public QueryBuilder getQueryBuilder();

	public FilterBuilder getFilterBuilder();
}
