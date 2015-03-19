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

package org.apache.usergrid.persistence.graph.test.util;


import java.util.Random;

import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleMarkedEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByIdType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchIdType;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Simple class for edge testing generation
 */
public class EdgeTestUtils {


    private static final long KCLOCK_OFFSET = 0x01b21dd213814000L;
    private static final long KCLOCK_MULTIPLIER_L = 10000L;
    private static final Random CLOCK_SEQ_RANDOM = new Random();


    /**
     * Create an edge for testing
     *
     * @param sourceType The source type to use in the id
     * @param edgeType The edge type to use
     * @param targetType The target type to use
     *
     * @return an Edge for testing
     */
    public static MarkedEdge createEdge( final String sourceType, final String edgeType, final String targetType ) {
        return createEdge( IdGenerator.createId( sourceType ), edgeType, IdGenerator.createId( targetType ), System.currentTimeMillis() );
    }


    /**
     * Create an edge for testing
     *
     * @param sourceType The source type to use in the id
     * @param edgeType The edge type to use
     * @param targetType The target type to use
     * @param timestamp the edge's timestamp
     *
     * @return an Edge for testing
     */
    public static MarkedEdge createEdge( final String sourceType, final String edgeType, final String targetType, final long timestamp ) {
        return createEdge( IdGenerator.createId( sourceType ), edgeType, IdGenerator.createId( targetType ), timestamp );
    }



    /**
     * Create an edge for testing
     *
     * @param sourceType The source type to use in the id
     * @param edgeType The edge type to use
     * @param targetType The target type to use
     *
     * @return an Edge for testing
     */
    public static MarkedEdge createMarkedEdge( final String sourceType, final String edgeType,
                                               final String targetType ) {
        return createEdge( IdGenerator.createId( sourceType ), edgeType, IdGenerator.createId( targetType ), System.currentTimeMillis(),
                true );
    }


    /**
     * Create an edge for testing
     */
    public static MarkedEdge createEdge( final Id sourceId, final String edgeType, final Id targetId ) {
        return createEdge( sourceId, edgeType, targetId, System.currentTimeMillis() );
    }


    /**
     * Create an edge that is marked
     */
    public static MarkedEdge createMarkedEdge( final Id sourceId, final String edgeType, final Id targetId ) {
        return createEdge( sourceId, edgeType, targetId, System.currentTimeMillis(), true );
    }


    /**
        * Create an edge that is marked
        */
       public static MarkedEdge createMarkedEdge( final Id sourceId, final String edgeType, final Id targetId, final long timestamp) {
           return createEdge( sourceId, edgeType, targetId, timestamp, true );
       }


    /**
     * Create an edge with the specified params
     */
    public static MarkedEdge createEdge( final Id sourceId, final String edgeType, final Id targetId,
                                         final long timestamp ) {
        return createEdge( sourceId, edgeType, targetId, timestamp, false );
    }


    /**
     * Create an edge with the specified params
     */
    public static MarkedEdge createEdge( final Id sourceId, final String edgeType, final Id targetId,
                                         final long timestamp, final boolean deleted ) {
        return new SimpleMarkedEdge( sourceId, edgeType, targetId, timestamp, deleted );
    }


    /**
     *
     * @param sourceId
     * @param type
     * @param maxVersion
     * @param last
     * @return
     */
    public static SearchByEdgeType createSearchByEdge( final Id sourceId, final String type, final long maxVersion,
                                                       final Edge last ) {
        return new SimpleSearchByEdgeType( sourceId, type, maxVersion, SearchByEdgeType.Order.DESCENDING, last );
    }


    /**
     *
     * @param sourceId
     * @param type
     * @param maxVersion
     * @param idType
     * @param last
     * @return
     */
    public static SearchByIdType createSearchByEdgeAndId( final Id sourceId, final String type, final long maxVersion,
                                                          final String idType, final Edge last ) {
        return new SimpleSearchByIdType( sourceId, type, maxVersion, SearchByEdgeType.Order.DESCENDING, idType, last );
    }


    /**
     *
     * @param sourceId
     * @param last
     * @return
     */
    public static SearchEdgeType createSearchEdge( final Id sourceId, final String last ) {
        return new SimpleSearchEdgeType( sourceId, null, last );
    }


    /**
     * Create the search by Id type
     */
    public static SimpleSearchIdType createSearchIdType( final Id sourceId, final String type, final String last ) {
        return new SimpleSearchIdType( sourceId, type, null, last );
    }


    /**
     * Get the edge by type
     */
    public static SearchByEdge createGetByEdge( final Id sourceId, final String type, final Id targetId,
                                                final long maxVersion, final Edge last ) {
        return new SimpleSearchByEdge( sourceId, type, targetId, maxVersion, SearchByEdgeType.Order.DESCENDING, last );
    }

//
//    /**
//     * NEVER USE THIS IN A REAL ENV.  Setting timestamps in anything but the present can result in collections Copied
//     * from fasterxml uuid utils
//     */
//    public static UUID setTimestamp( long timestamp ) {
//
//        byte[] uuidBytes = new byte[16];
//        EthernetAddress _ethernetAddress = EthernetAddress.constructMulticastAddress();
//        _ethernetAddress.toByteArray( uuidBytes, 10 );
//        // and add clock sequence
//        int clockSeq = timer.getClockSequence();
//        uuidBytes[UUIDUtil.BYTE_OFFSET_CLOCK_SEQUENCE] = ( byte ) ( clockSeq >> 8 );
//        uuidBytes[UUIDUtil.BYTE_OFFSET_CLOCK_SEQUENCE + 1] = ( byte ) clockSeq;
//        long l2 = gatherLong( uuidBytes, 8 );
//        long _uuidL2 = UUIDUtil.initUUIDSecondLong( l2 );
//
//
//        final long rawTimestamp = timestamp;
//        // Time field components are kind of shuffled, need to slice:
//        int clockHi = ( int ) ( rawTimestamp >>> 32 );
//        int clockLo = ( int ) rawTimestamp;
//        // and dice
//        int midhi = ( clockHi << 16 ) | ( clockHi >>> 16 );
//        // need to squeeze in type (4 MSBs in byte 6, clock hi)
//        midhi &= ~0xF000; // remove high nibble of 6th byte
//        midhi |= 0x1000; // type 1
//        long midhiL = ( long ) midhi;
//        midhiL = ( ( midhiL << 32 ) >>> 32 ); // to get rid of sign extension
//        // and reconstruct
//        long l1 = ( ( ( long ) clockLo ) << 32 ) | midhiL;
//        // last detail: must force 2 MSB to be '10'
//        return new UUID( l1, _uuidL2 );
//    }
//
//    /*
//    /********************************************************************************
//    /* Internal helper methods
//    /********************************************************************************
//     */
//
//
//    protected final static long gatherLong( byte[] buffer, int offset ) {
//        long hi = ( ( long ) _gatherInt( buffer, offset ) ) << 32;
//        //long lo = ((long) _gatherInt(buffer, offset+4)) & MASK_LOW_INT;
//        long lo = ( ( ( long ) _gatherInt( buffer, offset + 4 ) ) << 32 ) >>> 32;
//        return hi | lo;
//    }
//
//
//    private final static int _gatherInt( byte[] buffer, int offset ) {
//        return ( buffer[offset] << 24 ) | ( ( buffer[offset + 1] & 0xFF ) << 16 ) | ( ( buffer[offset + 2] & 0xFF )
//                << 8 ) | ( buffer[offset + 3] & 0xFF );
//    }
//
//
//    private static final Random random = new Random();
//    private static final UUIDTimer timer;
//
//
//    /**
//     * Lame, but required
//     */
//    static {
//        try {
//            timer = new UUIDTimer( random, new TimestampSynchronizer() {
//                @Override
//                protected long initialize() throws IOException {
//                    return System.currentTimeMillis();
//                }
//
//
//                @Override
//                protected void deactivate() throws IOException {
//
//                }
//
//
//                @Override
//                protected long update( final long now ) throws IOException {
//                    return now;
//                }
//            } );
//        }
//        catch ( IOException e ) {
//            throw new RuntimeException( "Couldn't intialize timer", e );
//        }
//    }
}


