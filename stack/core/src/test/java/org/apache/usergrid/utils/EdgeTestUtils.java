/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.utils;


import org.apache.usergrid.corepersistence.util.CpNamingUtils;

import static org.junit.Assert.assertEquals;


public class EdgeTestUtils {

    /**
     * Get the name for an edge
     */
    public static String getNameForEdge( final String edgeName ) {
        final String[] parts = edgeName.split( "\\|" );

        assertEquals( "there should be 2 parts", parts.length, 2 );

        return parts[1];
    }


    public static boolean isCollectionEdgeType( String type ) {
        return type.startsWith( CpNamingUtils.EDGE_COLL_SUFFIX );
    }


    public static boolean isConnectionEdgeType( String type ) {
        return type.startsWith( CpNamingUtils.EDGE_CONN_SUFFIX );
    }
}
