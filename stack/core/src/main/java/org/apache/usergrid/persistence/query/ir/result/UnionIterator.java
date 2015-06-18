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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.usergrid.persistence.cassandra.CursorCache;

import static org.apache.usergrid.persistence.cassandra.Serializers.*;

/**
 * Simple iterator to perform Unions
 *
 * @author tnine
 */
public class UnionIterator extends MultiIterator {

    private SortedColumnList list;

    private final int id;


    /**
     * @param pageSize The page size to return
     * @param id The id assigned to this node
     * @param minUuid The minimum UUID to return
     */
    public UnionIterator( int pageSize, int id, ByteBuffer minUuid ) {
        super( pageSize );

        this.id = id;

        UUID parseMinUuid = null;

        if(minUuid != null)      {
            parseMinUuid = ue.fromByteBuffer( minUuid );
        }

        list = new SortedColumnList( pageSize, parseMinUuid );
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.query.ir.result.MergeIterator#advance()
     */
    @Override
    protected Set<ScanColumn> advance() {

        int size = iterators.size();

        if ( size == 0 ) {
            return null;
        }


        list.clear();

        for ( ResultIterator itr : iterators ) {

            while ( itr.hasNext() ) {
                list.addAll( itr.next() );
            }

            itr.reset();
        }

        //mark us for the next page
        list.mark();


        return list.asSet();
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.persistence.query.ir.result.ResultIterator#finalizeCursor(
     * org.apache.usergrid.persistence.cassandra.CursorCache)
     */
    @Override
    public void finalizeCursor( CursorCache cache, UUID lastLoaded ) {

        ByteBuffer buff = ue.toByteBuffer( lastLoaded );
        cache.setNextCursor( id, buff );
        //get our scan column and put them in the cache
        //we finalize the cursor of the min
    }


    @Override
    public void doReset() {
        //reset sub iterators if we need to
        super.doReset();

        list.reset();

    }


    /**
     * A Sorted Set with a max size. When a new entry is added, the max is removed.  You can mark the next "min" by
     * calling the mark method.  Values > min are accepted.  Values > min and that are over size are discarded
     */
    public static final class SortedColumnList {

        private final int maxSize;

        private final List<ScanColumn> list;

//        private UUIDColumn min;


        public SortedColumnList( final int maxSize, final UUID minUuid ) {
            //we need to allocate the extra space if required
            this.list = new ArrayList<ScanColumn>( maxSize );
            this.maxSize = maxSize;

//            if ( minUuid != null ) {
//                min = new UUIDColumn( minUuid, 1 ) ;
//            }
        }


        /**
         * Add the column to this list
         */
        public void add( ScanColumn col ) {
            //less than our min, don't add
//            if ( min != null && min.compareTo( col ) >= 0 ) {
//                return;
//            }

            int index = Collections.binarySearch( this.list, col );

            //already present
            if ( index > -1 ) {
                return;
            }

            index = ( index * -1 ) - 1;

            //outside the range
            if ( index >= maxSize ) {
                return;
            }

            this.list.add( index, col );

            final int size = this.list.size();

            if ( size > maxSize ) {
                this.list.subList( maxSize, size ).clear();
            }
        }


        /**
         * Add all the elements to this list
         */
        public void addAll( final Collection<? extends ScanColumn> cols ) {
            for ( ScanColumn col : cols ) {
                add( col );
            }
        }


        /**
         * Returns a new list.  If no elements are present, returns null
         */
        public Set<ScanColumn> asSet() {
            if ( this.list.size() == 0 ) {
                return null;
            }

            return new LinkedHashSet<ScanColumn>( this.list );
        }


        /**
         * Mark our last element in the tree as the max
         */
        public void mark() {

            final int size = this.list.size();

            //we don't have any elements in the list, and we've never set a min
            if ( size == 0 ) {
                return;
            }

            final UUID oldMin = this.list.get( size - 1 ).getUUID();
//            min = new UUIDColumn( oldMin, 1 );
        }


        /**
         * Clear the list
         */
        public void clear() {
            this.list.clear();
        }

        public void reset(){
            clear();
//            this.min = null;
        }
    }
}
