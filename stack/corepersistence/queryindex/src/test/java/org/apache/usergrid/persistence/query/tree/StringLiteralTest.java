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


import org.antlr.runtime.CommonToken;
import org.apache.usergrid.persistence.index.query.tree.StringLiteral;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/** @author tnine */
public class StringLiteralTest {

    @Test
    public void exactToken() {

        StringLiteral literal = new StringLiteral( new CommonToken( 0, "'value'" ) );

        assertEquals( "value", literal.getValue() );
        assertEquals( "value", literal.getEndValue() );
    }


    @Test
    public void exactString() {

        StringLiteral literal = new StringLiteral( "value" );

        assertEquals( "value", literal.getValue() );
        assertEquals( "value", literal.getEndValue() );
    }


    @Test
    public void wildcardToken() {

        StringLiteral literal = new StringLiteral( new CommonToken( 0, "'*'" ) );

        assertNull( literal.getValue() );
        assertNull( literal.getEndValue() );
    }


    @Test
    public void wildcardString() {

        StringLiteral literal = new StringLiteral( "*" );

        assertNull( literal.getValue() );
        assertNull( literal.getEndValue() );
    }

//      removing this because it breaks queries like "select * where name = "fred*"
//
//    @Test
//    public void wildcardEndToken() {
//
//        StringLiteral literal = new StringLiteral( new CommonToken( 0, "'value*'" ) );
//
//        assertEquals( "value", literal.getValue() );
//        assertEquals( "value\uffff", literal.getEndValue() );
//    }
//
//
//    @Test
//    public void wildcardEndString() {
//
//        StringLiteral literal = new StringLiteral( "value*" );
//
//        assertEquals( "value", literal.getValue() );
//        assertEquals( "value\uffff", literal.getEndValue() );
//    }
}
