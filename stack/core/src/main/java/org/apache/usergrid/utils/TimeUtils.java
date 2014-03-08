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
package org.apache.usergrid.utils;


import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;


public class TimeUtils {
    /**
     * Jira-style duration parser. Supported duration strings are: <ul> <li>'S': milliseconds</li> <li>'s': seconds</li>
     * <li>'m': minutes</li> <li>'h': hours</li> <li>'d': days</li> </ul>
     * <p/>
     * Durations can be compound statements in any order as long as they are separated by a ',' (comma). Eg. "1d,14h,3s"
     * to get the millisecond equivalent of one day, fourteen hours and 3 seconds.
     * <p/>
     * Numbers with no durations will be treated as raw millisecond values
     *
     * @return the number of milliseconds representing the duration
     */
    public static long millisFromDuration( String durationStr ) {
        long total = 0;
        MultiplierToken mt;
        long dur;
        for ( String val : Splitter.on( ',' ).trimResults().omitEmptyStrings().split( durationStr ) ) {
            dur = Long.parseLong( CharMatcher.DIGIT.retainFrom( val ) );
            mt = MultiplierToken.from( val.charAt( val.length() - 1 ) );
            total += ( mt.multiplier * dur );
        }
        return total;
    }


    private enum MultiplierToken {
        MILSEC_TOKEN( 'S', 1L ),
        SEC_TOKEN( 's', 1000L ),
        MIN_TOKEN( 'm', 60000L ),
        HOUR_TOKEN( 'h', 3600000L ),
        DAY_TOKEN( 'd', 86400000L );

        final char token;
        final long multiplier;


        MultiplierToken( char token, long multiplier ) {
            this.token = token;
            this.multiplier = multiplier;
        }


        static MultiplierToken from( char c ) {
            switch ( c ) {
                case 's':
                    return SEC_TOKEN;
                case 'm':
                    return MIN_TOKEN;
                case 'h':
                    return HOUR_TOKEN;
                case 'd':
                    return DAY_TOKEN;
                case 'S':
                    return MILSEC_TOKEN;
                default:
                    break;
            }

            if ( CharMatcher.DIGIT.matches( c ) ) {
                return MILSEC_TOKEN;
            }
            throw new IllegalArgumentException( "Duration token was not on of [S,s,m,h,d] but was " + c );
        }
    }
}
