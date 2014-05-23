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


import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.apache.usergrid.persistence.index.exceptions.IndexException;


/**
 * Any logical operation should subclass.  Boolean logic, equality, not, contains, within and others are examples of
 * operands
 *
 * @author tnine
 */
public abstract class Operand extends CommonTree {


    /** Default constructor to take a token */
    public Operand( Token t ) {
        super( t );
    }


    /** Get the pointer to the parent node */
    public Operand getParent() {
        return ( Operand ) super.getParent();
    }


    /** Visitor method */
    public abstract void visit( QueryVisitor visitor ) throws IndexException;
}
