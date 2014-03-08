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
package org.apache.usergrid.persistence;


import org.junit.Test;

import static org.junit.Assert.assertEquals;


/** Test for the entity comparator */
public class EntityPropertyComparatorTest {


    @Test
    public void testNulls() throws Exception {

        DynamicEntity first = new DynamicEntity();
        first.setProperty( "test", true );

        EntityPropertyComparator forward = new EntityPropertyComparator( "test", false );


        assertEquals( 0, forward.compare( null, null ) );

        assertEquals( -1, forward.compare( first, null ) );

        assertEquals( 1, forward.compare( null, first ) );


        //now test in reverse

        EntityPropertyComparator reverse = new EntityPropertyComparator( "test", true );


        assertEquals( 0, reverse.compare( null, null ) );

        assertEquals( -1, reverse.compare( first, null ) );

        assertEquals( 1, reverse.compare( null, first ) );
    }


    @Test
    public void testBooleans() throws Exception {

        DynamicEntity second = new DynamicEntity();
        second.setProperty( "test", true );


        DynamicEntity first = new DynamicEntity();
        first.setProperty( "test", false );

        EntityPropertyComparator forward = new EntityPropertyComparator( "test", false );


        assertEquals( 0, forward.compare( second, second ) );

        assertEquals( 1, forward.compare( second, first ) );

        assertEquals( -1, forward.compare( first, second ) );


        //now test in reverse

        EntityPropertyComparator reverse = new EntityPropertyComparator( "test", true );


        assertEquals( 0, reverse.compare( second, second ) );

        assertEquals( 1, reverse.compare( first, second ) );

        assertEquals( -1, reverse.compare( second, first ) );
    }


    @Test
    public void testFloat() throws Exception {

        DynamicEntity second = new DynamicEntity();
        second.setProperty( "test", 1.0f );


        DynamicEntity first = new DynamicEntity();
        first.setProperty( "test", 0.0f );

        EntityPropertyComparator forward = new EntityPropertyComparator( "test", false );


        assertEquals( 0, forward.compare( second, second ) );

        assertEquals( 1, forward.compare( second, first ) );

        assertEquals( -1, forward.compare( first, second ) );


        //now test in reverse

        EntityPropertyComparator reverse = new EntityPropertyComparator( "test", true );


        assertEquals( 0, reverse.compare( second, second ) );

        assertEquals( 1, reverse.compare( first, second ) );

        assertEquals( -1, reverse.compare( second, first ) );
    }


    @Test
    public void testLong() throws Exception {

        DynamicEntity second = new DynamicEntity();
        second.setProperty( "test", 1l );


        DynamicEntity first = new DynamicEntity();
        first.setProperty( "test", 0l );

        EntityPropertyComparator forward = new EntityPropertyComparator( "test", false );


        assertEquals( 0, forward.compare( second, second ) );

        assertEquals( 1, forward.compare( second, first ) );

        assertEquals( -1, forward.compare( first, second ) );


        //now test in reverse

        EntityPropertyComparator reverse = new EntityPropertyComparator( "test", true );


        assertEquals( 0, reverse.compare( second, second ) );

        assertEquals( 1, reverse.compare( first, second ) );

        assertEquals( -1, reverse.compare( second, first ) );
    }


    @Test
    public void testDouble() throws Exception {

        DynamicEntity second = new DynamicEntity();
        second.setProperty( "test", 1d );


        DynamicEntity first = new DynamicEntity();
        first.setProperty( "test", 0d );


        EntityPropertyComparator forward = new EntityPropertyComparator( "test", false );


        assertEquals( 0, forward.compare( second, second ) );

        assertEquals( 1, forward.compare( second, first ) );

        assertEquals( -1, forward.compare( first, second ) );


        //now test in reverse

        EntityPropertyComparator reverse = new EntityPropertyComparator( "test", true );


        assertEquals( 0, reverse.compare( second, second ) );

        assertEquals( 1, reverse.compare( first, second ) );

        assertEquals( -1, reverse.compare( second, first ) );
    }


    @Test
    public void testString() throws Exception {

        DynamicEntity second = new DynamicEntity();
        second.setProperty( "test", "b" );


        DynamicEntity first = new DynamicEntity();
        first.setProperty( "test", "a" );

        EntityPropertyComparator forward = new EntityPropertyComparator( "test", false );


        assertEquals( 0, forward.compare( second, second ) );

        assertEquals( 1, forward.compare( second, first ) );

        assertEquals( -1, forward.compare( first, second ) );


        //now test in reverse

        EntityPropertyComparator reverse = new EntityPropertyComparator( "test", true );


        assertEquals( 0, reverse.compare( second, second ) );

        assertEquals( 1, reverse.compare( first, second ) );

        assertEquals( -1, reverse.compare( second, first ) );
    }
}
