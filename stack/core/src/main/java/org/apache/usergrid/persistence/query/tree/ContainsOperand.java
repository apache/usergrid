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
package org.apache.usergrid.persistence.query.tree;


import org.antlr.runtime.Token;
import org.apache.usergrid.persistence.exceptions.PersistenceException;


/** @author tnine */
public class ContainsOperand extends EqualityOperand {

    public ContainsOperand( Token t ) {
        super( t );
    }

    @Override
    public void visit( QueryVisitor visitor ) throws PersistenceException {
        visitor.visit( this );
    }


    public StringLiteral getString() {
        return ( StringLiteral ) getLiteral();
    }

    @Override
    protected Property newProperty( String name ) {
        return new ContainsProperty( name );
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.persistence.query.tree.EqualityOperand#getProperty()
     */
    @Override
    public ContainsProperty getProperty() {
        return ( ContainsProperty ) this.children.get( 0 );
    }
}
