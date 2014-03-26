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


/**
 * A base class for any equality expression. Expressions must have a property and a value. Examples are >=, >, =, <,
 * <=,
 *
 * @author tnine
 */
public abstract class EqualityOperand extends Operand {

    /**
     * @param property
     * @param literal
     */
    public EqualityOperand( Token t ) {
        super( t );
    }


    public EqualityOperand( String propName, Literal<?> value ) {
        super( null );
    }


    /** Set the property on this operand */
    public void setProperty( String name ) {
        setAtIndex( 0, newProperty( name ) );
    }


    /** Get the property to set into the equality. Allows subclasses to override the type */
    protected Property newProperty( String name ) {
        return new Property( name );
    }


    /** Set the literal on this operand from the given value */
    public void setLiteral( Object value ) {
        setAtIndex( 1, LiteralFactory.getLiteral( value ) );
    }


    /** Set the child at the specified index. If it doesn't exist, it's added until it does */
    @SuppressWarnings("unchecked")
    private void setAtIndex( int index, Literal<?> value ) {

        if ( children == null ) {
            children = createChildrenList();
        }

        while ( children.size() - 1 < index ) {
            children.add( null );
        }

        setChild( index, value );
    }


    /** @return the property */
    public Property getProperty() {
        return ( Property ) this.children.get( 0 );
    }


    /** @return the literal */
    public Literal<?> getLiteral() {
        return ( Literal<?> ) this.children.get( 1 );
    }
}
