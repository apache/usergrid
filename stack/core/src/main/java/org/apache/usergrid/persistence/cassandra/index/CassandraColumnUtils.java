/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.usergrid.persistence.cassandra.index;


import java.nio.ByteBuffer;
import java.util.List;

import org.apache.cassandra.utils.FBUtilities;

import me.prettyprint.hector.api.beans.HColumn;


/**
 * Utils for dealing with Pagination in cassandra
 */
public class CassandraColumnUtils {

    /**
     * Returns true if the 2 byte buffers contain the same bytes, false otherwise
     */
    public static boolean equals( final ByteBuffer first, final ByteBuffer second ) {
        int firstLength = first.remaining();
        int firstPosition = first.position();

        int secondLength = second.remaining();
        int secondPosition = second.position();

        final int compare = FBUtilities
                .compareUnsigned( first.array(), second.array(), firstPosition, secondPosition, firstLength,
                        secondLength );

        return compare == 0;
    }


    /**
     * Maybe remove the first if the byte buffers match
     * @param columns
     * @param startScan
     */
    public static void maybeRemoveFirst(final  List<HColumn<ByteBuffer, ByteBuffer>> columns, final ByteBuffer startScan){
           //remove the first element since it needs to be skipped AFTER the size check. Otherwise it will fail
        //we only want to skip if our byte value are the same as our expected start.  Since we aren't stateful you can't
        //be sure your start even comes back, and you don't want to erroneously remove columns
        if ( columns != null && columns.size() > 0  && startScan != null) {
            final ByteBuffer returnedBuffer = columns.get( 0 ).getName();

            //the byte buffers are the same as our seek (which may or may not be the case in the first seek)
            if( CassandraColumnUtils.equals( startScan, returnedBuffer ) ){
                columns.remove( 0 );
            }
        }
    }
}
