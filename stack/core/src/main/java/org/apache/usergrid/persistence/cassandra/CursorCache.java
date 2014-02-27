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
package org.apache.usergrid.persistence.cassandra;


import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static java.lang.Integer.parseInt;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.commons.lang.StringUtils.split;
import static org.apache.usergrid.utils.ConversionUtils.bytes;


/**
 * Internal cursor parsing
 *
 * @author tnine
 */
public class CursorCache {

    private Map<Integer, ByteBuffer> cursors = new HashMap<Integer, ByteBuffer>();


    public CursorCache() {

    }


    /** Create a new cursor cache from the string if passed */
    public CursorCache( String cursorString ) {

        if ( cursorString == null ) {
            return;
        }

        String decoded = new String( decodeBase64( cursorString ) );

        // nothing to do
        if ( decoded.indexOf( ':' ) < 0 ) {
            return;
        }

        String[] cursorTokens = split( decoded, '|' );

        for ( String c : cursorTokens ) {

            String[] parts = split( c, ':' );

            if ( parts.length >= 1 ) {

                int hashCode = parseInt( parts[0] );

                ByteBuffer cursorBytes = null;

                if ( parts.length == 2 ) {
                    cursorBytes = ByteBuffer.wrap( decodeBase64( parts[1] ) );
                }
                else {
                    cursorBytes = ByteBuffer.allocate( 0 );
                }

                cursors.put( hashCode, cursorBytes );
            }
        }
    }


    /** Set the cursor with the given hash and the new byte buffer */
    public void setNextCursor( int sliceHash, ByteBuffer newCursor ) {
        cursors.put( sliceHash, newCursor );
    }


    /** Get the cursor by the hashcode of the slice */
    public ByteBuffer getCursorBytes( int sliceHash ) {
        return cursors.get( sliceHash );
    }


    /** Turn the cursor cache into a string */
    public String asString() {
        /**
         * No cursors to return
         */
        if ( cursors.size() == 0 ) {
            return null;
        }

        StringBuffer buff = new StringBuffer();

        int nullCount = 0;
        ByteBuffer value = null;

        for ( Entry<Integer, ByteBuffer> entry : cursors.entrySet() ) {
            value = entry.getValue();

            buff.append( entry.getKey() );
            buff.append( ":" );
            buff.append( encodeBase64URLSafeString( bytes( value ) ) );
            buff.append( "|" );

            // this range was empty, mark it as a null
            if ( value == null || value.remaining() == 0 ) {
                nullCount++;
            }
        }

        // all cursors are complete, return null
        if ( nullCount == cursors.size() ) {
            return null;
        }

        // trim off the last pipe
        buff.setLength( buff.length() - 1 );

        return encodeBase64URLSafeString( buff.toString().getBytes() );
    }
}
