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


import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.utils.UUIDUtils;

import static org.apache.usergrid.persistence.query.ir.result.IteratorHelper.uuidColumn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/** @author tnine */
public class SubtractionIteratorTest {

    @Test
    public void smallerSubtract() {
        UUID id1 = UUIDUtils.minTimeUUID( 1 );
        UUID id2 = UUIDUtils.minTimeUUID( 2 );
        UUID id3 = UUIDUtils.minTimeUUID( 3 );
        UUID id4 = UUIDUtils.minTimeUUID( 4 );
        UUID id5 = UUIDUtils.minTimeUUID( 5 );

        // we should get intersection on 1, 3, and 8
        InOrderIterator keep = new InOrderIterator( 2 );
        keep.add( id1 );
        keep.add( id2 );
        keep.add( id3 );
        keep.add( id4 );
        keep.add( id5 );

        InOrderIterator subtract = new InOrderIterator( 2 );
        subtract.add( id1 );
        subtract.add( id3 );
        subtract.add( id5 );

        SubtractionIterator sub = new SubtractionIterator( 100 );
        sub.setKeepIterator( keep );
        sub.setSubtractIterator( subtract );

        // now make sure it's right, only 2 and 8 aren't intersected
        Set<ScanColumn> page = sub.next();

        assertTrue( page.contains( uuidColumn( id2 ) ) );
        assertTrue( page.contains( uuidColumn( id4 ) ) );

        assertEquals( 2, page.size() );
    }


    @Test
    public void smallerKeep() {

        UUID id1 = UUIDUtils.minTimeUUID( 1 );
        UUID id2 = UUIDUtils.minTimeUUID( 2 );
        UUID id3 = UUIDUtils.minTimeUUID( 3 );
        UUID id4 = UUIDUtils.minTimeUUID( 4 );
        UUID id5 = UUIDUtils.minTimeUUID( 5 );
        UUID id6 = UUIDUtils.minTimeUUID( 6 );

        // we should get intersection on 1, 3, and 8
        InOrderIterator keep = new InOrderIterator( 100 );
        keep.add( id1 );
        keep.add( id2 );
        keep.add( id5 );
        keep.add( id6 );

        InOrderIterator subtract = new InOrderIterator( 100 );
        subtract.add( id1 );
        subtract.add( id3 );
        subtract.add( id4 );
        subtract.add( id5 );
        subtract.add( id6 );

        SubtractionIterator sub = new SubtractionIterator( 100 );
        sub.setKeepIterator( keep );
        sub.setSubtractIterator( subtract );

        // now make sure it's right, only 2 and 8 aren't intersected

        Set<ScanColumn> page = sub.next();

        assertTrue( page.contains( uuidColumn( id2 ) ) );

        assertEquals( 1, page.size() );
    }


    @Test
    public void smallerKeepRemoveAll() {

        UUID id1 = UUIDUtils.minTimeUUID( 1 );
        UUID id2 = UUIDUtils.minTimeUUID( 2 );
        UUID id3 = UUIDUtils.minTimeUUID( 3 );
        UUID id4 = UUIDUtils.minTimeUUID( 4 );
        UUID id5 = UUIDUtils.minTimeUUID( 5 );
        UUID id6 = UUIDUtils.minTimeUUID( 6 );

        // we should get intersection on 1, 3, and 8
        InOrderIterator keep = new InOrderIterator( 100 );
        keep.add( id1 );
        keep.add( id3 );
        keep.add( id4 );

        InOrderIterator subtract = new InOrderIterator( 100 );
        subtract.add( id1 );
        subtract.add( id2 );
        subtract.add( id3 );
        subtract.add( id4 );
        subtract.add( id5 );
        subtract.add( id6 );

        SubtractionIterator sub = new SubtractionIterator( 100 );
        sub.setKeepIterator( keep );
        sub.setSubtractIterator( subtract );

        // now make sure it's right, only 2 and 8 aren't intersected

        assertFalse( sub.hasNext() );
    }


    @Test
    public void noKeep() {
        UUID id1 = UUIDUtils.minTimeUUID( 1 );

        // we should get intersection on 1, 3, and 8
        InOrderIterator keep = new InOrderIterator( 100 );

        InOrderIterator subtract = new InOrderIterator( 100 );
        subtract.add( id1 );

        SubtractionIterator sub = new SubtractionIterator( 100 );
        sub.setKeepIterator( keep );
        sub.setSubtractIterator( subtract );

        assertFalse( sub.hasNext() );
    }


    @Test
    public void noSubtract() {
        UUID id1 = UUIDUtils.minTimeUUID( 1 );

        //keep only id 1
        InOrderIterator keep = new InOrderIterator( 100 );
        keep.add( id1 );

        InOrderIterator subtract = new InOrderIterator( 100 );


        SubtractionIterator sub = new SubtractionIterator( 100 );
        sub.setKeepIterator( keep );
        sub.setSubtractIterator( subtract );

        assertTrue( sub.hasNext() );
        Set<ScanColumn> page = sub.next();

        assertTrue( page.contains( uuidColumn( id1 ) ) );
        assertEquals( 1, page.size() );
    }
}
