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


import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.utils.UUIDUtils;

import me.prettyprint.cassandra.serializers.UUIDSerializer;

import static org.apache.usergrid.persistence.query.ir.result.IteratorHelper.uuidColumn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * @author tnine
 */
public class UnionIteratorTest {

    @Test
    public void testMutipleIterators() {

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
        third.add( id6 );
        third.add( id7 );
        third.add( id1 );
        third.add( id3 );
        third.add( id5 );
        third.add( id8 );

        InOrderIterator fourth = new InOrderIterator( 100 );
        fourth.add( id1 );
        fourth.add( id6 );
        fourth.add( id2 );
        fourth.add( id3 );
        fourth.add( id8 );
        fourth.add( id9 );


        UnionIterator iter = new UnionIterator( 100, 0, null );
        iter.addIterator( first );
        iter.addIterator( second );
        iter.addIterator( third );
        iter.addIterator( fourth );

        Set<ScanColumn> union = iter.next();

        // now make sure it's right, only 1, 3 and 8 intersect
        assertTrue( union.contains( uuidColumn( id1 ) ) );
        assertTrue( union.contains( uuidColumn( id2 ) ) );
        assertTrue( union.contains( uuidColumn( id3 ) ) );
        assertTrue( union.contains( uuidColumn( id4 ) ) );
        assertTrue( union.contains( uuidColumn( id5 ) ) );
        assertTrue( union.contains( uuidColumn( id6 ) ) );
        assertTrue( union.contains( uuidColumn( id7 ) ) );
        assertTrue( union.contains( uuidColumn( id8 ) ) );
        assertTrue( union.contains( uuidColumn( id9 ) ) );
        assertTrue( union.contains( uuidColumn( id10 ) ) );
    }


    @Test
    public void testOneIterator() {

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

        UnionIterator union = new UnionIterator( 100, 0, null );
        union.addIterator( first );

        Set<ScanColumn> ids = union.next();

        // now make sure it's right, only 1, 3 and 8 intersect
        assertTrue( ids.contains( uuidColumn( id1 ) ) );
        assertTrue( ids.contains( uuidColumn( id2 ) ) );
        assertTrue( ids.contains( uuidColumn( id3 ) ) );
        assertTrue( ids.contains( uuidColumn( id4 ) ) );

        assertFalse( union.hasNext() );
    }


    @Test
    public void testEmptyFirstIterator() {

        UUID id1 = UUIDUtils.minTimeUUID( 1 );
        UUID id2 = UUIDUtils.minTimeUUID( 2 );
        UUID id3 = UUIDUtils.minTimeUUID( 3 );
        UUID id4 = UUIDUtils.minTimeUUID( 4 );

        // we should get intersection on 1, 3, and 8
        InOrderIterator first = new InOrderIterator( 100 );

        InOrderIterator second = new InOrderIterator( 100 );
        second.add( id1 );
        second.add( id2 );
        second.add( id3 );
        second.add( id4 );

        UnionIterator union = new UnionIterator( 100, 0, null );
        union.addIterator( first );
        union.addIterator( second );

        Set<ScanColumn> ids = union.next();

        // now make sure it's right, only 1, 3 and 8 intersect
        assertTrue( ids.contains( uuidColumn( id1 ) ) );
        assertTrue( ids.contains( uuidColumn( id2 ) ) );
        assertTrue( ids.contains( uuidColumn( id3 ) ) );
        assertTrue( ids.contains( uuidColumn( id4 ) ) );

        assertFalse( union.hasNext() );
    }


    @Test
    public void testNoIterator() {

        UnionIterator union = new UnionIterator( 100, 0, null );

        // now make sure it's right, only 1, 3 and 8 intersect
        assertFalse( union.hasNext() );
    }


    @Test
    public void largeUnionTest() {

        int size = 10000;
        int firstIntersection = 100;
        int secondIntersection = 200;

        int pageSize = 20;

        UUID[] firstSet = new UUID[size];
        UUID[] secondSet = new UUID[size];
        UUID[] thirdSet = new UUID[size];

        InOrderIterator first = new InOrderIterator( pageSize / 2 );
        InOrderIterator second = new InOrderIterator( pageSize / 2 );
        InOrderIterator third = new InOrderIterator( pageSize / 2 );

        Set<UUID> results = new HashSet<UUID>( size );

        for ( int i = 0; i < size; i++ ) {
            firstSet[i] = UUIDUtils.newTimeUUID();
            // every 100 elements, set the element equal to the first set. This way we
            // have intersection

            results.add( firstSet[i] );

            if ( i % firstIntersection == 0 ) {
                secondSet[i] = firstSet[i];
            }
            else {
                secondSet[i] = UUIDUtils.newTimeUUID();
                results.add( secondSet[i] );
            }

            if ( i % secondIntersection == 0 ) {
                thirdSet[i] = firstSet[i];
            }

            else {
                thirdSet[i] = UUIDUtils.newTimeUUID();
                results.add( thirdSet[i] );
            }
        }

        first.add( firstSet );

        reverse( secondSet );
        // reverse the second
        second.add( secondSet );
        third.add( thirdSet );

        // now intersect them and make sure we get all results in a small set
        UnionIterator union = new UnionIterator( pageSize, 0, null );
        union.addIterator( first );
        union.addIterator( second );
        union.addIterator( third );


        while ( union.hasNext() ) {

            // now get the 2nd page
            Set<ScanColumn> resultSet = union.next();

            for ( ScanColumn col : resultSet ) {
                boolean existed = results.remove( col.getUUID() );

                assertTrue( "Duplicate element was detected", existed );
            }
        }

        assertEquals( 0, results.size() );
        assertFalse( union.hasNext() );
    }


    @Test
    public void iterationCompleted() {

        UUID id1 = UUIDUtils.minTimeUUID( 1 );
        UUID id2 = UUIDUtils.minTimeUUID( 2 );
        UUID id3 = UUIDUtils.minTimeUUID( 3 );
        UUID id4 = UUIDUtils.minTimeUUID( 4 );
        UUID id5 = UUIDUtils.minTimeUUID( 5 );


        UnionIterator union = new UnionIterator( 5, 0, null );

        InOrderIterator first = new InOrderIterator( 100 );

        InOrderIterator second = new InOrderIterator( 100 );
        second.add( id1 );
        second.add( id2 );
        second.add( id3 );
        second.add( id4 );
        second.add( id5 );

        union.addIterator( first );
        union.addIterator( second );


        // now make sure it's right, only 1, 3 and 8 intersect
        assertTrue( union.hasNext() );

        Set<ScanColumn> ids = union.next();

        // now make sure it's right, only 1, 3 and 8 intersect
        assertTrue( ids.contains( uuidColumn( id1 ) ) );
        assertTrue( ids.contains( uuidColumn( id2 ) ) );
        assertTrue( ids.contains( uuidColumn( id3 ) ) );
        assertTrue( ids.contains( uuidColumn( id4 ) ) );
        assertTrue( ids.contains( uuidColumn( id5 ) ) );

        //now try to get the next page
        ids = union.next();
        assertNull( ids );
    }


    @Test
    public void nullCursorBytes() {

        UUID id1 = UUIDUtils.minTimeUUID( 1 );
        UUID id2 = UUIDUtils.minTimeUUID( 2 );
        UUID id3 = UUIDUtils.minTimeUUID( 3 );
        UUID id4 = UUIDUtils.minTimeUUID( 4 );
        UUID id5 = UUIDUtils.minTimeUUID( 5 );


        InOrderIterator second = new InOrderIterator( 100 );
        second.add( id1 );
        second.add( id2 );
        second.add( id3 );
        second.add( id4 );
        second.add( id5 );

        UnionIterator union = new UnionIterator( 100, 1, null );

        union.addIterator( second );

        Set<ScanColumn> ids = union.next();

        // now make sure it's right, only 1, 3 and 8 intersect
        assertTrue( ids.contains( uuidColumn( id1 ) ) );
        assertTrue( ids.contains( uuidColumn( id2 ) ) );
        assertTrue( ids.contains( uuidColumn( id3 ) ) );
        assertTrue( ids.contains( uuidColumn( id4 ) ) );
        assertTrue( ids.contains( uuidColumn( id5 ) ) );
    }


    @Test
    public void validCursorBytes() {


        ByteBuffer cursor = UUIDSerializer.get().toByteBuffer( UUIDUtils.minTimeUUID( 4 ) );

        UUID id1 = UUIDUtils.minTimeUUID( 1 );
        UUID id2 = UUIDUtils.minTimeUUID( 2 );
        UUID id3 = UUIDUtils.minTimeUUID( 3 );
        UUID id4 = UUIDUtils.minTimeUUID( 4 );
        UUID id5 = UUIDUtils.minTimeUUID( 5 );


        InOrderIterator second = new InOrderIterator( 100 );
        second.add( id1 );
        second.add( id2 );
        second.add( id3 );
        second.add( id4 );
        second.add( id5 );

        UnionIterator union = new UnionIterator( 100, 1, cursor );

        union.addIterator( second );

        Set<ScanColumn> ids = union.next();

        // now make sure it's right, only 1, 3 and 8 intersect
        assertFalse( ids.contains( uuidColumn( id1 ) ) );
        assertFalse( ids.contains( uuidColumn( id2 ) ) );
        assertFalse( ids.contains( uuidColumn( id3 ) ) );
        assertFalse( ids.contains( uuidColumn( id4 ) ) );
        assertTrue( ids.contains( uuidColumn( id5 ) ) );
    }


    @Test
    public void resetCorrect() {

        UUID id1 = UUIDUtils.minTimeUUID( 1 );
        UUID id2 = UUIDUtils.minTimeUUID( 2 );
        UUID id3 = UUIDUtils.minTimeUUID( 3 );
        UUID id4 = UUIDUtils.minTimeUUID( 4 );
        UUID id5 = UUIDUtils.minTimeUUID( 5 );
        UUID id6 = UUIDUtils.minTimeUUID( 6 );
        UUID id7 = UUIDUtils.minTimeUUID( 75 );


        UnionIterator union = new UnionIterator( 5, 0, null );

        InOrderIterator first = new InOrderIterator( 100 );
        first.add( id3 );
        first.add( id6 );
        first.add( id4 );


        InOrderIterator second = new InOrderIterator( 100 );
        second.add( id7 );
        second.add( id1 );
        second.add( id2 );
        second.add( id5 );


        union.addIterator( first );
        union.addIterator( second );


        // now make sure it's right, only 1, 3 and 8 intersect
        assertTrue( union.hasNext() );

        Set<ScanColumn> ids = union.next();


        assertEquals(5, ids.size());

        // now make sure it's right, only 1, 3 and 8 intersect
        assertTrue( ids.contains( uuidColumn( id1 ) ) );
        assertTrue( ids.contains( uuidColumn( id2 ) ) );
        assertTrue( ids.contains( uuidColumn( id3 ) ) );
        assertTrue( ids.contains( uuidColumn( id4 ) ) );
        assertTrue( ids.contains( uuidColumn( id5 ) ) );

        ids = union.next();


        assertEquals(2, ids.size());

        assertTrue( ids.contains( uuidColumn( id6 ) ) );
        assertTrue( ids.contains( uuidColumn( id7 ) ) );

        //now try to get the next page
        ids = union.next();
        assertNull( ids );

        //now reset and re-test
        union.reset();

        ids = union.next();

        assertEquals(5, ids.size());


        // now make sure it's right, only 1, 3 and 8 intersect
        assertTrue( ids.contains( uuidColumn( id1 ) ) );
        assertTrue( ids.contains( uuidColumn( id2 ) ) );
        assertTrue( ids.contains( uuidColumn( id3 ) ) );
        assertTrue( ids.contains( uuidColumn( id4 ) ) );
        assertTrue( ids.contains( uuidColumn( id5 ) ) );


        ids = union.next();

        assertEquals(2, ids.size());

        assertTrue( ids.contains( uuidColumn( id6 ) ) );
        assertTrue( ids.contains( uuidColumn( id7 ) ) );


        //now try to get the next page
        ids = union.next();
        assertNull( ids );
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
