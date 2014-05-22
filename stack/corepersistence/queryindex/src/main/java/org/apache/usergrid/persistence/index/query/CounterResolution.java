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
package org.apache.usergrid.persistence.index.query;


public enum CounterResolution {
    ALL( 0 ), MINUTE( 1 ), FIVE_MINUTES( 5 ), HALF_HOUR( 30 ), HOUR( 60 ), SIX_HOUR( 60 * 6 ), HALF_DAY( 60 * 12 ),
    DAY( 60 * 24 ), WEEK( 60 * 24 * 7 ), MONTH( 60 * 24 * ( 365 / 12 ) );

    private final long interval;


    CounterResolution( long minutes ) {
        interval = minutes * 60 * 1000;
    }


    public long interval() {
        return interval;
    }


    public long round( long timestamp ) {
        if ( interval == 0 ) {
            return 1;
        }
        return ( timestamp / interval ) * interval;
    }


    public long next( long timestamp ) {
        return round( timestamp ) + interval;
    }


    public static CounterResolution fromOrdinal( int i ) {
        if ( ( i < 0 ) || ( i >= CounterResolution.values().length ) ) {
            throw new IndexOutOfBoundsException( "Invalid ordinal" );
        }
        return CounterResolution.values()[i];
    }


    public static CounterResolution fromMinutes( int m ) {
        m = m * 60 * 1000;
        for ( int i = CounterResolution.values().length - 1; i >= 0; i-- ) {
            if ( CounterResolution.values()[i].interval <= m ) {
                return CounterResolution.values()[i];
            }
        }
        return ALL;
    }


    public static CounterResolution fromString( String s ) {
        if ( s == null ) {
            return ALL;
        }
        try {
            return CounterResolution.valueOf( s.toUpperCase() );
        }
        catch ( IllegalArgumentException e ) {
        }
        try {
            return fromMinutes( Integer.valueOf( s ) );
        }
        catch ( NumberFormatException e ) {
        }
        return ALL;
    }
}
