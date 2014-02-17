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


/** @author tnine */
public class NotNode extends QueryNode {

    protected QueryNode subtractNode, keepNode;


    /** @param keepNode may be null if there are parents to this */
    public NotNode( QueryNode subtractNode, QueryNode keepNode ) {
        this.subtractNode = subtractNode;
        this.keepNode = keepNode;
//        throw new RuntimeException( "I'm a not node" );
    }


    /** @return the child */
    public QueryNode getSubtractNode() {
        return subtractNode;
    }


    /** @return the all */
    public QueryNode getKeepNode() {
        return keepNode;
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


    @Override
    public String toString() {
        return "NotNode [child=" + subtractNode + "]";
    }
}
