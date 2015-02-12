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
package org.apache.usergrid.persistence.query.ir.result;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.utils.UUIDUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/** @author tnine */
public class IntersectionIteratorTest {

    @Test
    public void mutipleIterators() {

        UUID id1 = UUIDUtils.minTimeUUID( 1 );
        UUID id2 = UUIDUtils.minTimeUUID( 2 );
        UUID id3 = UUIDUtils.minTimeUUID( 3 );
        UUID id4 = UUIDUtils.minTimeUUID( 4 );
        UUID id5 = UUIDUtils.minTimeUUID( 5 );
        UUID id6 = UUIDUtils.minTimeUUID( 6 );
        UUID id7 = UUIDUtils.minTimeUUID( 7 );
        UUID id8 = UUIDUtils.minTimeUUID( 8 );
        UUID id9 = UUIDUtils.minTimeUUID( 9 );
        UUID id10 = UUIDUtils.minTimeUUID( 10 );

        // we should get intersection on 1, 3, and 8
        InOrderIterator first = new InOrderIterator( 100 );
        first.add( id9 );
        first.add( id8 );
        first.add( id1 );
        first.add( id2 );
        first.add( id3 );



        InOrderIterator second = new InOrderIterator( 100 );
        second.add( id1 );
        second.add( id2 );
        second.add( id3 );
        second.add( id4 );
        second.add( id8 );
        second.add( id10 );

        InOrderIterator third = new InOrderIterator( 100 );
        third.add( id1 );
        third.add( id3 );
        third.add( id5 );
        third.add( id6 );
        third.add( id7 );
        third.add( id8 );

        InOrderIterator fourth = new InOrderIterator( 100 );
        fourth.add( id1 );
        fourth.add( id2 );
        fourth.add( id3 );
        fourth.add( id6 );
        fourth.add( id8 );
        fourth.add( id10 );

        IntersectionIterator intersection = new IntersectionIterator( 100 );
        intersection.addIterator( first );
        intersection.addIterator( second );
        intersection.addIterator( third );
        intersection.addIterator( fourth );

        Iterator<ScanColumn> union = intersection.next().iterator();

        // now make sure it's right, only 1, 3 and 8 intersect
        assertTrue( union.hasNext() );
        assertEquals( id8, union.next().getUUID() );

        assertTrue( union.hasNext() );
        assertEquals( id1, union.next().getUUID() );

        assertTrue( union.hasNext() );
        assertEquals( id3, union.next().getUUID() );

        assertFalse( union.hasNext() );
    }


    @Test
    public void oneIterator() {

        UUID id1 = UUIDUtils.minTimeUUID( 1 );
        UUID id2 = UUIDUtils.minTimeUUID( 2 );
        UUID id3 = UUIDUtils.minTimeUUID( 3 );
        UUID id4 = UUIDUtils.minTimeUUID( 4 );

        // we should get intersection on 1, 3, and 8
        InOrderIterator first = new InOrderIterator( 100 );
        first.add( id1 );
        first.add( id2 );
        first.add( id3 );
        first.add( id4 );

        IntersectionIterator intersection = new IntersectionIterator( 100 );
        intersection.addIterator( first );

        // now make sure it's right, only 1, 3 and 8 intersect
        assertTrue( intersection.hasNext() );

        Set<ScanColumn> page = intersection.next();

        Iterator<ScanColumn> union = page.iterator();

        assertEquals( id1, union.next().getUUID() );

        assertTrue( union.hasNext() );
        assertEquals( id2, union.next().getUUID() );

        assertTrue( union.hasNext() );
        assertEquals( id3, union.next().getUUID() );

        assertTrue( union.hasNext() );
        assertEquals( id4, union.next().getUUID() );

        assertFalse( union.hasNext() );
    }


    @Test
    public void noIterator() {
        IntersectionIterator union = new IntersectionIterator( 100 );

        // now make sure it's right, only 1, 3 and 8 intersect
        assertFalse( union.hasNext() );
    }


    @Test
    public void largeIntersection() {

        int size = 10000;
        int firstIntersection = 100;
        int secondIntersection = 200;

        UUID[] firstSet = new UUID[size];
        UUID[] secondSet = new UUID[size];
        UUID[] thirdSet = new UUID[size];

        InOrderIterator first = new InOrderIterator( 100 );
        InOrderIterator second = new InOrderIterator( 100 );
        InOrderIterator third = new InOrderIterator( 100 );

        List<UUID> results = new ArrayList<UUID>( size / secondIntersection );

        for ( int i = 0; i < size; i++ ) {
            firstSet[i] = UUIDUtils.newTimeUUID();
            // every 100 elements, set the element equal to the first set. This way we
            // have intersection

            if ( i % firstIntersection == 0 ) {
                secondSet[i] = firstSet[i];
            }
            else {
                secondSet[i] = UUIDUtils.newTimeUUID();
            }

            if ( i % secondIntersection == 0 ) {
                thirdSet[i] = firstSet[i];
                results.add( firstSet[i] );
            }

            else {
                thirdSet[i] = UUIDUtils.newTimeUUID();
            }
        }

        first.add( firstSet );

        reverse( secondSet );
        //reverse the second
        second.add( secondSet );
        third.add( thirdSet );

        //now itersect them and make sure we get all results in a small set

        int numPages = 2;
        int pageSize = results.size() / numPages;

        IntersectionIterator intersection = new IntersectionIterator( pageSize );
        intersection.addIterator( first );
        intersection.addIterator( second );
        intersection.addIterator( third );

        assertTrue( intersection.hasNext() );


        Iterator<UUID> expected = results.iterator();
        Set<ScanColumn> resultSet = intersection.next();
        Iterator<ScanColumn> union = resultSet.iterator();


        while ( union.hasNext() ) {
            assertTrue( expected.hasNext() );
            assertEquals( expected.next(), union.next().getUUID() );
        }


        //now get the 2nd page
        resultSet = intersection.next();
        union = resultSet.iterator();


        while ( union.hasNext() ) {
            assertTrue( expected.hasNext() );
            assertEquals( expected.next(), union.next().getUUID() );
        }

        //no more elements
        assertFalse( intersection.hasNext() );
        assertFalse( expected.hasNext() );
    }


    /**
     * Tests that when there are multiple iterators, and one in the "middle" of the list returns no results, it will
     * short circuit since no results will be possible
     */
    @Test
    public void mutipleIteratorsNoIntersection() {

        UUID id1 = UUIDUtils.minTimeUUID( 1 );
        UUID id2 = UUIDUtils.minTimeUUID( 2 );
        UUID id3 = UUIDUtils.minTimeUUID( 3 );
        UUID id4 = UUIDUtils.minTimeUUID( 4 );
        UUID id6 = UUIDUtils.minTimeUUID( 6 );
        UUID id8 = UUIDUtils.minTimeUUID( 8 );
        UUID id9 = UUIDUtils.minTimeUUID( 9 );
        UUID id10 = UUIDUtils.minTimeUUID( 10 );

        // we should get intersection on 1, 3, and 8
        InOrderIterator first = new InOrderIterator( 100 );
        first.add( id1 );
        first.add( id2 );
        first.add( id3 );
        first.add( id8 );
        first.add( id9 );

        InOrderIterator second = new InOrderIterator( 100 );
        second.add( id1 );
        second.add( id2 );
        second.add( id3 );
        second.add( id4 );
        second.add( id8 );
        second.add( id10 );

        InOrderIterator third = new InOrderIterator( 100 );

        InOrderIterator fourth = new InOrderIterator( 100 );
        fourth.add( id1 );
        fourth.add( id2 );
        fourth.add( id3 );
        fourth.add( id6 );
        fourth.add( id8 );
        fourth.add( id10 );

        IntersectionIterator intersection = new IntersectionIterator( 100 );
        intersection.addIterator( first );
        intersection.addIterator( second );
        intersection.addIterator( third );
        intersection.addIterator( fourth );

        Iterator<ScanColumn> union = intersection.next().iterator();

        // now make sure it's right, only 1, 3 and 8 intersect
        assertFalse( union.hasNext() );
    }


    private void reverse( UUID[] array ) {

        UUID temp = null;

        for ( int i = 0; i < array.length / 2; i++ ) {
            temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
    }
}
