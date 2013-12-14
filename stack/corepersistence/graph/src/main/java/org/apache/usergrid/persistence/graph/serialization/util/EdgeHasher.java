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


import java.nio.charset.Charset;
import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Id;


/**
 *
 *
 */
public class EdgeHasher {

    //just re-use the string serializer.  It already does string -> bytes
    private static final String UTF_8 = "UTF-8";
    private static final Charset CHARSET = Charset.forName( UTF_8 );


    /**
     * Create a UUID based on the edge type and the type of the id that will be inserted into the column
     *
     * @param edgeType The name of the edge type
     * @param idForColumn The id of the value that will be in the column
     *
     * @return A UUID that represents a consistent one way hash of the fields
     */
    public static UUID createEdgeHash( final String edgeType, final Id idForColumn ) {

        final String hashString =
                new StringBuilder( edgeType ).append( idForColumn.getType() ).toString().toLowerCase();

        final byte[] hashBytes = hashString.getBytes( CHARSET );

        return UUID.nameUUIDFromBytes( hashBytes );
    }


    /**
     * Create a UUID based on the edge type and the type of the id that will be inserted into the column
     *
     * @return A UUID that represents a consistent one way hash of the fields
     */
    public static UUID createEdgeHash( final String edgeType ) {

        final String hashString = edgeType.toLowerCase();

        final byte[] hashBytes = hashString.getBytes( CHARSET );

        return UUID.nameUUIDFromBytes( hashBytes );
    }
}
