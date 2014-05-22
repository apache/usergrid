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


import org.apache.usergrid.persistence.index.query.tree.LongLiteral;
import org.antlr.runtime.CommonToken;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/** @author tnine */
public class LongLiteralTest {


    /**
     * Test method for {@link org.apache.usergrid.persistence.query.tree.LongLiteral#IntegerLiteral(org.antlr.runtime
     * .Token)}.
     */
    @Test
    public void longMin() {

        long value = Long.MIN_VALUE;

        String stringVal = String.valueOf( value );

        LongLiteral literal = new LongLiteral( new CommonToken( 0, stringVal ) );

        assertEquals( value, literal.getValue().longValue() );
    }


    /**
     * Test method for {@link org.apache.usergrid.persistence.query.tree.LongLiteral#IntegerLiteral(org.antlr.runtime
     * .Token)}.
     */
    @Test
    public void longMax() {

        long value = Long.MAX_VALUE;

        String stringVal = String.valueOf( value );

        LongLiteral literal = new LongLiteral( new CommonToken( 0, stringVal ) );

        assertEquals( value, literal.getValue().longValue() );
    }
}
