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
package org.apache.usergrid.persistence.cassandra.index;


import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.usergrid.persistence.cassandra.ApplicationCF;
import org.apache.usergrid.persistence.cassandra.CassandraService;

import me.prettyprint.hector.api.beans.HColumn;


/** @author tnine */
public class IndexMultiBucketSetLoader {


    /**
     *
     */
    private static final long serialVersionUID = 1L;


    /**
     * Loads and sorts columns from each bucket in memory.  This will return a contiguous set of columns as if they'd
     * been
     * read from a single row
     */
    public static TreeSet<HColumn<ByteBuffer, ByteBuffer>> load( CassandraService cass, ApplicationCF columnFamily,
                                                                 UUID applicationId, List<Object> rowKeys, Object start,
                                                                 Object finish, int resultSize, boolean reversed )
            throws Exception {
        Map<ByteBuffer, List<HColumn<ByteBuffer, ByteBuffer>>> results =
                cass.multiGetColumns( cass.getApplicationKeyspace( applicationId ), columnFamily, rowKeys, start,
                        finish, resultSize, reversed );

        final Comparator<ByteBuffer> comparator = reversed ? new DynamicCompositeReverseComparator( columnFamily ) :
                                                  new DynamicCompositeForwardComparator( columnFamily );

        TreeSet<HColumn<ByteBuffer, ByteBuffer>> resultsTree =
                new TreeSet<HColumn<ByteBuffer, ByteBuffer>>( new Comparator<HColumn<ByteBuffer, ByteBuffer>>() {

                    @Override
                    public int compare( HColumn<ByteBuffer, ByteBuffer> first,
                                        HColumn<ByteBuffer, ByteBuffer> second ) {

                        return comparator.compare( first.getName(), second.getName() );
                    }
                } );

        for ( List<HColumn<ByteBuffer, ByteBuffer>> cols : results.values() ) {

            for ( HColumn<ByteBuffer, ByteBuffer> col : cols ) {
                resultsTree.add( col );

                // trim if we're over size
                if ( resultsTree.size() > resultSize ) {
                    resultsTree.pollLast();
                }
            }
        }

        return resultsTree;
    }
}
