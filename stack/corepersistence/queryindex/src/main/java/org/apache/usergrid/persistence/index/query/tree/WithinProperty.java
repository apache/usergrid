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


import org.antlr.runtime.ClassicToken;
import org.antlr.runtime.Token;


/**
 * A property
 *
 * @author tnine
 */
public class WithinProperty extends Property {

    private String indexedName = null;


    public WithinProperty( Token t ) {
        super( t );
        this.indexedName = String.format( "%s.coordinates", super.getValue() );
    }


    public WithinProperty( String property ) {
        this( new ClassicToken( 0, property ) );
    }


    /** Get the */
    public String getIndexedName() {
        return this.indexedName;
    }


    /** @return the property */
    public WithinProperty getProperty() {
        return ( WithinProperty ) this.children.get( 0 );
    }
}
