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
package org.apache.usergrid.persistence.geo;


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.geo.model.Point;
import org.apache.usergrid.utils.UUIDUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/** @author tnine */
public class EntityLocationRefDistanceComparatorTest {


    @Test
    public void locationDistanceComparator() {
        EntityLocationRefDistanceComparator comp = new EntityLocationRefDistanceComparator();

        UUID firstId = UUIDUtils.newTimeUUID();
        UUID matchId = UUID.fromString( firstId.toString() );


        Point zero = new Point( 0, 0 );

        EntityLocationRef first = new EntityLocationRef( ( String ) null, firstId, 0, 0 );
        first.calcDistance( zero );

        EntityLocationRef second = new EntityLocationRef( ( String ) null, matchId, 0, 0 );
        second.calcDistance( zero );

        assertEquals( 0, comp.compare( first, second ) );

        //now increase the distance on the second one

        second = new EntityLocationRef( ( String ) null, matchId, 1, 1 );
        second.calcDistance( zero );

        assertTrue( comp.compare( first, second ) < 0 );

        //set the first one to be farther
        first = new EntityLocationRef( ( String ) null, firstId, 1, 1 );
        first.calcDistance( zero );

        second = new EntityLocationRef( ( String ) null, matchId, 0, 0 );
        second.calcDistance( zero );

        assertTrue( comp.compare( first, second ) > 0 );

        //now compare by UUID.

        UUID secondId = UUIDUtils.newTimeUUID();

        first = new EntityLocationRef( ( String ) null, firstId, 0, 0 );
        first.calcDistance( zero );

        second = new EntityLocationRef( ( String ) null, secondId, 0, 0 );
        second.calcDistance( zero );

        assertTrue( comp.compare( first, second ) < 0 );

        first = new EntityLocationRef( ( String ) null, secondId, 0, 0 );
        first.calcDistance( zero );

        second = new EntityLocationRef( ( String ) null, firstId, 0, 0 );
        second.calcDistance( zero );

        assertTrue( comp.compare( first, second ) > 0 );

        //compare nulls

        assertTrue( comp.compare( null, first ) > 0 );
        assertTrue( comp.compare( first, null ) < 0 );

        assertEquals( 0, comp.compare( null, null ) );


        double less = 0;
        double more = 1000;

        int compare = Double.compare( less, more );

        assertTrue( compare < 1 );
    }
}
