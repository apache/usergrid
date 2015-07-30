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


import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.utils.UUIDUtils;

import me.prettyprint.hector.api.beans.DynamicComposite;


/**
 * Parser for reading and writing secondary index composites.  Instances of this class should not be shared among
 * iterators.
 *
 * It it designed with the following assumptions in mind.
 *
 * 1) The slice contains the same data type for every element 2) Evaluating the first parse call for a comparator is
 * sufficient for subsequent use
 *
 * @author tnine
 */
public class SecondaryIndexSliceParser implements SliceParser {

    //the type comparator
    private Comparator<SecondaryIndexColumn> typeComparator;

    private final SliceCursorGenerator sliceCursorGenerator;


    public SecondaryIndexSliceParser( final SliceCursorGenerator sliceCursorGenerator ) {
        this.sliceCursorGenerator = sliceCursorGenerator;
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.persistence.query.ir.result.SliceParser#parse(java.nio.ByteBuffer)
     */
    @Override
    public ScanColumn parse( ByteBuffer buff, final boolean isReversed ) {
        final DynamicComposite composite = DynamicComposite.fromByteBuffer( buff.duplicate() );

        final UUID uuid = ( UUID ) composite.get( 2 );
        final Object value = composite.get( 1 );

        if ( typeComparator == null ) {
            typeComparator = getTypeComparator( value, isReversed );
        }

        return new SecondaryIndexColumn( uuid, value, buff, typeComparator, sliceCursorGenerator );
    }


    private Comparator<SecondaryIndexColumn> getTypeComparator( final Object value, final boolean isReversed ) {

        final Class clazz = value.getClass();
        final Comparator<SecondaryIndexColumn> comparator = COMPARATOR_MAP.get( new MapKey( clazz, isReversed ) );

        if ( comparator == null ) {
            throw new NullPointerException( "comparator was not found for runtime type '" + clazz + "'" );
        }

        return comparator;
    }


    /**
     * Column for our secondary index type
     */
    public static class SecondaryIndexColumn extends AbstractScanColumn {

        private final Object value;
        private final Comparator<SecondaryIndexColumn> valueComparator;


        /**
         * Create the secondary index column
         *
         * @param valueComparator The comparator for the values
         */
        public SecondaryIndexColumn( final UUID uuid, final Object value, final ByteBuffer columnNameBuffer,
                                     final Comparator<SecondaryIndexColumn> valueComparator,
                                     final SliceCursorGenerator sliceCursorGenerator ) {
            super( uuid, columnNameBuffer, sliceCursorGenerator );
            this.value = value;
            this.valueComparator = valueComparator;
        }


        /** Get the value from the node */
        public Object getValue() {
            return this.value;
        }


        @Override
        public int compareTo( final ScanColumn other ) {
            if ( other == null ) {
                return 1;
            }

            return valueComparator.compare( this, ( SecondaryIndexColumn ) other );
        }
    }


    private static final Map<MapKey, Comparator<SecondaryIndexColumn>> COMPARATOR_MAP =
            new HashMap<MapKey, Comparator<SecondaryIndexColumn>>();

    static {

        final LongComparator longComparator = new LongComparator();
        COMPARATOR_MAP.put( new MapKey( Long.class, false ), new ForwardValueComparator( longComparator ) );
        COMPARATOR_MAP.put( new MapKey( Long.class, true ), new ReverseValueComparator( longComparator ) );

        final StringComparator stringComparator = new StringComparator();

        COMPARATOR_MAP.put( new MapKey( String.class, false ), new ForwardValueComparator( stringComparator ) );
        COMPARATOR_MAP.put( new MapKey( String.class, true ), new ReverseValueComparator( stringComparator ) );


        final UUIDComparator uuidComparator = new UUIDComparator();

        COMPARATOR_MAP.put( new MapKey( UUID.class, false ), new ForwardValueComparator( uuidComparator ) );
        COMPARATOR_MAP.put( new MapKey( UUID.class, true ), new ReverseValueComparator( uuidComparator ) );

        final BigIntegerComparator bigIntegerComparator = new BigIntegerComparator();

        COMPARATOR_MAP.put( new MapKey( BigInteger.class, false ), new ForwardValueComparator( bigIntegerComparator ) );
        COMPARATOR_MAP.put( new MapKey( BigInteger.class, true ), new ReverseValueComparator( bigIntegerComparator ) );
    }


    /**
     * The key for the map
     */
    private static final class MapKey {
        public final Class<?> clazz;
        public final boolean reversed;


        private MapKey( final Class<?> clazz, final boolean reversed ) {
            this.clazz = clazz;
            this.reversed = reversed;
        }


        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) {
                return true;
            }
            if ( !( o instanceof MapKey ) ) {
                return false;
            }

            final MapKey mapKey = ( MapKey ) o;

            if ( reversed != mapKey.reversed ) {
                return false;
            }
            return clazz.equals( mapKey.clazz );
        }


        @Override
        public int hashCode() {
            int result = clazz.hashCode();
            result = 31 * result + ( reversed ? 1 : 0 );
            return result;
        }
    }


    private static final class LongComparator implements Comparator<SecondaryIndexColumn> {

        @Override
        public int compare( final SecondaryIndexColumn first, final SecondaryIndexColumn second ) {

            final Long firstLong = ( Long ) first.value;
            final Long secondLong = ( Long ) second.value;


            return Long.compare( firstLong, secondLong );
        }
    }


    private static final class StringComparator implements Comparator<SecondaryIndexColumn> {
        @Override
        public int compare( final SecondaryIndexColumn first, final SecondaryIndexColumn second ) {

            if ( first == null && second != null ) {
                return -1;
            }

            final String firstString = ( String ) first.value;
            final String secondString = ( String ) second.value;


            return firstString.compareTo( secondString );
        }
    }


    private static final class UUIDComparator implements Comparator<SecondaryIndexColumn> {
        @Override
        public int compare( final SecondaryIndexColumn first, final SecondaryIndexColumn second ) {
            final UUID firstUUID = ( UUID ) first.value;
            final UUID secondUUID = ( UUID ) second.value;


            return UUIDUtils.compare( firstUUID, secondUUID );
        }
    }


    private static final class BigIntegerComparator implements Comparator<SecondaryIndexColumn> {
        @Override
        public int compare( final SecondaryIndexColumn first, final SecondaryIndexColumn second ) {
            final BigInteger firstInt = ( BigInteger ) first.value;
            final BigInteger secondInt = ( BigInteger ) second.value;


            return firstInt.compareTo( secondInt );
        }
    }


    /**
     * Delegates to the type comparator, if equal, sorts by UUID ascending always
     */
    private static final class ForwardValueComparator implements Comparator<SecondaryIndexColumn> {

        private final Comparator<SecondaryIndexColumn> comparator;


        private ForwardValueComparator( final Comparator<SecondaryIndexColumn> comparator ) {
            this.comparator = comparator;
        }


        @Override
        public int compare( final SecondaryIndexColumn first, final SecondaryIndexColumn second ) {

            int compare = comparator.compare( first, second );

            if ( compare == 0 ) {
                return com.fasterxml.uuid.UUIDComparator.staticCompare( first.uuid, second.uuid );
            }

            return compare;
        }
    }


    /**
     * Reversed our delegate comparator, if equal, compares by uuid ascending
     */
    private static final class ReverseValueComparator implements Comparator<SecondaryIndexColumn> {

        private final Comparator<SecondaryIndexColumn> comparator;


        private ReverseValueComparator( final Comparator<SecondaryIndexColumn> comparator ) {
            this.comparator = comparator;
        }


        @Override
        public int compare( final SecondaryIndexColumn first, final SecondaryIndexColumn second ) {
            int compare = comparator.compare( first, second ) * -1;

            if ( compare == 0 ) {
                return com.fasterxml.uuid.UUIDComparator.staticCompare( first.uuid, second.uuid );
            }

            return compare;
        }
    }
}
