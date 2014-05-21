/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.index.query.tree;


import org.apache.usergrid.persistence.exceptions.NoFullTextIndexException;
import org.apache.usergrid.persistence.exceptions.NoIndexException;
import org.apache.usergrid.persistence.exceptions.PersistenceException;


/**
 * Interface for visiting nodes in our AST as we produce
 *
 * @author tnine
 */
public interface QueryVisitor {

    /**
     *
     * @param op
     * @throws PersistenceException
     */
    public void visit( AndOperand op ) throws PersistenceException;

    /**
     * @param op
     * @throws PersistenceException
     */
    public void visit( OrOperand op ) throws PersistenceException;

    /**
     * @param op
     * @throws PersistenceException
     */
    public void visit( NotOperand op ) throws PersistenceException;

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
}
