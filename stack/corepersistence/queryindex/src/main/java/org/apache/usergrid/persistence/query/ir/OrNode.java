/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.usergrid.persistence.query.ir;


/**
 * @author tnine
 */
public class OrNode extends BooleanNode {

    private final int id;


    /**
     * @param left
     * @param right
     */
    public OrNode( QueryNode left, QueryNode right, int id ) {
        super( left, right );
        this.id = id;
    }


    /**
     * Get the context id
     */
    public int getId() {
        return this.id;
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.persistence.query.ir.QueryNode#visit(org.apache.usergrid.persistence.query.ir.NodeVisitor)
     */
    @Override
    public void visit( NodeVisitor visitor ) throws Exception {
        visitor.visit( this );
    }
}
