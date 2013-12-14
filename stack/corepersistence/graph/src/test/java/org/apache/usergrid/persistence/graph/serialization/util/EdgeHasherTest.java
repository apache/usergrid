/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.graph.serialization.util;


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


/**
 *
 *
 */
public class EdgeHasherTest {


    @Test
    public void consistentOutput() {

        final String simpleValue = "simpleValue";

        UUID hashed = EdgeHasher.createEdgeHash( simpleValue );

        UUID otherHash = EdgeHasher.createEdgeHash( simpleValue );

        assertEquals( "hashMatches", hashed, otherHash );
    }


    @Test
    public void collisionTest() {

        //test 10 million entries
        final int totalCount = 10000000;
        final int delta = Character.MAX_CODE_POINT-Character.MIN_CODE_POINT;
        final int lengthToTest = totalCount/delta;

        final Set<UUID> hashed = new HashSet<UUID>(totalCount);


        char[] chars;
        UUID uuidHash;

        StringBuilder builder = new StringBuilder(  );

        for ( int index = 0; index < lengthToTest; index++ ) {

            builder.append( 'a' );

            for ( int charValue = Character.MIN_CODE_POINT; charValue <= Character.MAX_CODE_POINT; charValue++ ) {
                chars = Character.toChars( charValue );

                //we can't use these higher values, they're 2 chars
                if(chars.length > 1){
                    break;
                }

                builder.setCharAt( index, chars[0] );


                //now hash it
                 uuidHash = EdgeHasher.createEdgeHash( builder.toString() );

                 assertFalse( "Hash should be unique", hashed.contains( uuidHash ) );

            }
        }
    }

}
