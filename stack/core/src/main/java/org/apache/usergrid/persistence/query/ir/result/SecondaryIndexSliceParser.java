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


import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.UUID;

import me.prettyprint.hector.api.beans.DynamicComposite;


/**
 * Parser for reading and writing secondary index composites
 *
 * @author tnine
 */
public class SecondaryIndexSliceParser implements SliceParser {


    /* (non-Javadoc)
     * @see org.apache.usergrid.persistence.query.ir.result.SliceParser#parse(java.nio.ByteBuffer)
     */
    @Override
    public ScanColumn parse( ByteBuffer buff, final boolean isReversed) {
        DynamicComposite composite = DynamicComposite.fromByteBuffer( buff.duplicate() );

        throw new UnsupportedOperationException( "Implement me with static comparators" );

//        return new SecondaryIndexColumn( ( UUID ) composite.get( 2 ), composite.get( 1 ), buff, null );
    }



    public static class SecondaryIndexColumn extends AbstractScanColumn {

        private final Object value;
        private final Comparator<Object> valueComparator;


        /**
         * Create the secondary index column
         * @param uuid
         * @param value
         * @param columnNameBuffer
         * @param valueComparator The comparator for the values
         */
        public SecondaryIndexColumn( final UUID uuid, final Object value, final ByteBuffer columnNameBuffer,
                                  final Comparator<Object> valueComparator ) {
            super( uuid, columnNameBuffer );
            this.value = value;
            this.valueComparator = valueComparator;
        }


        /** Get the value from the node */
        public Object getValue() {
            return this.value;
        }


        @Override
        public int compareTo( final ScanColumn o ) {
            throw new UnsupportedOperationException( "Impelment me" );
        }
    }
}
