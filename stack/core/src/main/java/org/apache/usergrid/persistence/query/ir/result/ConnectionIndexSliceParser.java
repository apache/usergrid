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
import java.util.UUID;

import org.apache.usergrid.persistence.Schema;

import com.fasterxml.uuid.UUIDComparator;

import me.prettyprint.hector.api.beans.DynamicComposite;


/**
 * Parser for reading uuid connections from ENTITY_COMPOSITE_DICTIONARIES and DICTIONARY_CONNECTED_ENTITIES type
 *
 * @author tnine
 */
public class ConnectionIndexSliceParser implements SliceParser {

    private final String connectedEntityType;
    private final SliceCursorGenerator sliceCurosrGenerator;


    /**
     * @param connectedEntityType Could be null if we want to return all types
     * @param sliceCurosrGenerator */
    public ConnectionIndexSliceParser( String connectedEntityType, final SliceCursorGenerator sliceCurosrGenerator ) {
        this.connectedEntityType = connectedEntityType;
        this.sliceCurosrGenerator = sliceCurosrGenerator;
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.persistence.query.ir.result.SliceParser#parse(java.nio.ByteBuffer)
     */
    @Override
    public ScanColumn parse( ByteBuffer buff, final boolean isReversed ) {
        DynamicComposite composite = DynamicComposite.fromByteBuffer( buff.duplicate() );

        String connectedType = ( String ) composite.get( 1 );


        //connection type has been defined and it doesn't match, skip it
        if ( connectedEntityType != null && !connectedEntityType.equals( connectedType ) ) {
            return null;
        }

        //we're checking a loopback and it wasn't specified, skip it
        if ( ( connectedEntityType != null && !connectedEntityType.equalsIgnoreCase( connectedType ) ) || Schema
                .TYPE_CONNECTION.equalsIgnoreCase( connectedType ) ) {
            return null;
        }

        return new ConnectionColumn( ( UUID ) composite.get( 0 ), connectedType, buff, sliceCurosrGenerator );
        //    return composite;
        //    return null;
    }




    public static class ConnectionColumn extends AbstractScanColumn {

        private final String connectedType;


        public ConnectionColumn( UUID uuid, String connectedType, ByteBuffer column,
                                 final SliceCursorGenerator sliceCursorGenerator ) {
            super( uuid, column, sliceCursorGenerator );
            this.connectedType = connectedType;
        }


        /** Get the target type from teh column */
        public String getTargetType() {
            return connectedType;
        }


        @Override
        public int compareTo( final ScanColumn o ) {
            if(o == null){
                return 1;
            }

            final int compare =  UUIDComparator.staticCompare( uuid, o.getUUID() );

            if(compare == 0){
                return connectedType.compareTo( ((ConnectionColumn)o).connectedType );
            }

            return compare;
        }
    }
}
