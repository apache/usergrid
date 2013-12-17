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


import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.HashMultimap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 *
 *
 */
public class EdgeHasherTest {


    @Test
    public void consistentOutput() {

        final String simpleValue = "simpleValue";

        EdgeHasher.HashEdge hashed = EdgeHasher.createEdgeHash( simpleValue );

        EdgeHasher.HashEdge otherHash = EdgeHasher.createEdgeHash( simpleValue );

        assertEquals( "hashMatches", hashed, otherHash );
    }


    @Test
    public void collisionTest() {

        //test 10 million entries
        final int totalCount = 10000000;
        final int delta = Character.MAX_CODE_POINT - Character.MIN_CODE_POINT;
        final int lengthToTest = totalCount / delta;

        final HashMultimap< EdgeHasher.HashEdge, String> hashed = HashMultimap.create( totalCount, 2 );


        char[] chars;
        EdgeHasher.HashEdge uuidHash;

        StringBuilder builder = new StringBuilder();

        for ( int index = 0; index < lengthToTest; index++ ) {

            builder.append( 'a' );

            for ( int charValue = Character.MIN_CODE_POINT; charValue <= Character.MAX_CODE_POINT; charValue++ ) {
                chars = Character.toChars( charValue );

                //we can't use these higher values, they're 2 chars
                if ( chars.length > 1 ) {
                    continue;
                }

                builder.setCharAt( index, chars[0] );


                final String sourceString = builder.toString();

                //now hash it
                uuidHash = EdgeHasher.createEdgeHash( sourceString );

                hashed.put( uuidHash, sourceString );

                final Set<String> strings = hashed.get( uuidHash );

                if ( strings.size() > 1 ) {

                    //check if it's the same string.  If it is, the collision is fine
                    if(assertSameStrings(strings)){
                        continue;
                    }

                    final  EdgeHasher.HashEdge uuid = uuidHash;
                    final String value = builder.toString();

                    String error = String.format(
                            "Expected hash of '%s' to be unique, but hash of '%s' already exists.  Existing values " +
                                    "are: '",
                            value, uuid );

                    for ( String input : hashed.get( uuid ) ) {
                        error += input + ", ";
                    }

                    error = error.substring( 0, error.length() - 1 ).concat( "'" );

                    fail( error );
                }
            }
        }

        assertEquals( "Check the sizes are equal", totalCount, hashed.size() );
    }


    private boolean assertSameStrings( Set<String> set ) {

        Iterator<String> strings = set.iterator();

        if ( !strings.hasNext() ) {
            return false;
        }

        final String first = strings.next();

        while ( strings.hasNext() ) {

            final String next = strings.next();

            if ( !first.equals( next  ) ) {
                return false;
            }
        }


        return true;
    }
}
