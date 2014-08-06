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
package org.apache.usergrid.persistence.query.ir;


import java.util.List;

import org.apache.usergrid.persistence.Query.SortPredicate;


/**
 * Intermediate representation of ordering operations
 *
 * @author tnine
 */
public class OrderByNode extends QueryNode {


    private final SliceNode firstPredicate;
    private final List<SortPredicate> secondarySorts;
    private final QueryNode queryOperations;


    /**
     * @param firstPredicate The first predicate that is in the order by statement
     * @param secondarySorts Any subsequent terms
     * @param queryOperations The subtree for boolean evaluation
     */
    public OrderByNode( SliceNode firstPredicate, List<SortPredicate> secondarySorts, QueryNode queryOperations ) {
        this.firstPredicate = firstPredicate;
        this.secondarySorts = secondarySorts;
        this.queryOperations = queryOperations;
    }


    /** @return the sorts */
    public List<SortPredicate> getSecondarySorts() {
        return secondarySorts;
    }


    /** @return the firstPredicate */
    public SliceNode getFirstPredicate() {
        return firstPredicate;
    }


    public QueryNode getQueryOperations() {
        return queryOperations;
    }


    /*
       * (non-Javadoc)
       *
       * @see
       * org.apache.usergrid.persistence.query.ir.QueryNode#visit(org.apache.usergrid.persistence
       * .query.ir.NodeVisitor)
       */
    @Override
    public void visit( NodeVisitor visitor ) throws Exception {
        visitor.visit( this );
    }


    /** Return true if this order has secondary sorts */
    public boolean hasSecondarySorts() {
        return secondarySorts != null && secondarySorts.size() > 0;
    }


    @Override
    public int getCount() {
        return firstPredicate.getCount() + secondarySorts.size();
    }


    @Override
    public boolean ignoreHintSize() {
        return false;
    }


    /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
    @Override
    public String toString() {
        return "OrderByNode [sorts=" + secondarySorts + "]";
    }
}
