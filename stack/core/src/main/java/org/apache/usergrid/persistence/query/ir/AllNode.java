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


/**
 * Used to represent a "select all".  This will iterate over the entities by UUID
 *
 * @author tnine
 */
public class AllNode extends QueryNode {


    private final QuerySlice slice;
    private final boolean forceKeepFirst;


    /**
     * Note that the slice isn't used on select, but is used when creating cursors
     *
     * @param id. The unique numeric id for this node
     * @param forceKeepFirst True if we don't allow the iterator to skip the first result, regardless of cursor state.
     * Used for startUUID paging
     */
    public AllNode( int id, boolean forceKeepFirst ) {
        this.slice = new QuerySlice( "uuid", id );
        this.forceKeepFirst = forceKeepFirst;
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.persistence.query.ir.QueryNode#visit(org.apache.usergrid.persistence.query.ir.NodeVisitor)
     */
    @Override
    public void visit( NodeVisitor visitor ) throws Exception {
        visitor.visit( this );
    }


    @Override
    public int getCount() {
        return 1;
    }


    @Override
    public boolean ignoreHintSize() {
        return false;
    }


    @Override
    public String toString() {
        return "AllNode";
    }


    /** @return the slice */
    public QuerySlice getSlice() {
        return slice;
    }


    /** @return the skipFirstMatch */
    public boolean isForceKeepFirst() {
        return forceKeepFirst;
    }
}
