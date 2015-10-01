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


import org.antlr.runtime.Token;
import org.apache.usergrid.persistence.index.exceptions.IndexException;


/** @author tnine */
public class NotOperand extends Operand {


    public NotOperand( Token t ) {
        super( t );
    }


    /** get the only child operation */
    public Operand getOperation() {
        return ( Operand ) this.children.get( 0 );
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.persistence.query.tree.Operand#visit(org.apache.usergrid.persistence.query.tree.QueryVisitor)
     */
    @Override
    public void visit( QueryVisitor visitor ) throws IndexException {
        visitor.visit( this );
    }
}
