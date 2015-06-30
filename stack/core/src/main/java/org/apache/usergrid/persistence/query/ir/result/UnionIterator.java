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

import com.fasterxml.uuid.UUIDComparator;

import static org.apache.usergrid.persistence.cassandra.Serializers.*;

/**
 * Simple iterator to perform Unions
 *
 * @author tnine
 */
public class UnionIterator extends MultiIterator {

    private SortedColumnList list;


    /**
     * @param pageSize The page size to return
     * @param id The id assigned to this node
     * @param minUuid The minimum UUID to return
     */
    public UnionIterator( int pageSize, int id, ByteBuffer minUuid ) {
        super( pageSize );
        UUID parseMinUuid = null;

        if(minUuid != null)      {
            parseMinUuid = ue.fromByteBuffer( minUuid );
        }

        final UUIDCursorGenerator uuidCursorGenerator = new UUIDCursorGenerator( id );

        list = new SortedColumnList( pageSize, parseMinUuid, uuidCursorGenerator );


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

        private final List<UnionScanColumn> list;

        private final UUIDCursorGenerator uuidCursorGenerator;

        private UnionScanColumn min;


        public SortedColumnList( final int maxSize, final UUID minUuid, final UUIDCursorGenerator uuidCursorGenerator ) {


            this.uuidCursorGenerator = uuidCursorGenerator;
            //we need to allocate the extra space if required
            this.list = new ArrayList<UnionScanColumn>( maxSize );
            this.maxSize = maxSize;

            if ( minUuid != null ) {
                min = new UnionScanColumn(new UUIDColumn( minUuid, 1, uuidCursorGenerator), uuidCursorGenerator ) ;
            }
        }


        /**
         * Add the column to this list
         */
        public void add( ScanColumn col ) {

            final UnionScanColumn unionScanColumn = new UnionScanColumn(col, uuidCursorGenerator );

            //less than our min, don't add
            if ( min != null && min.compareTo( unionScanColumn ) >= 0 ) {
                return;
            }

            int index = Collections.binarySearch( this.list, unionScanColumn );

            //already present
            if ( index > -1 ) {
                return;
            }

            index = ( index * -1 ) - 1;

            //outside the range
            if ( index >= maxSize ) {
                return;
            }

            this.list.add( index, unionScanColumn );

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

            min = this.list.get( size - 1 );
        }


        /**
         * Clear the list
         */
        public void clear() {
            this.list.clear();
        }

        public void reset(){
            clear();
            this.min = null;
        }
    }

    private static class UnionScanColumn implements ScanColumn{

        private final ScanColumn delegate;
        private final UUIDCursorGenerator uuidCursorGenerator;
        private ScanColumn child;



        private UnionScanColumn( final ScanColumn delegate, final UUIDCursorGenerator uuidCursorGenerator ) {
            super();
            this.delegate = delegate;
            this.uuidCursorGenerator = uuidCursorGenerator;
        }


        @Override
        public int compareTo( final ScanColumn o ) {
            return UUIDComparator.staticCompare( delegate.getUUID(), o.getUUID() );
        }


        @Override
        public UUID getUUID() {
            return delegate.getUUID();
        }


        @Override
        public ByteBuffer getCursorValue() {
            return ue.toByteBuffer( delegate.getUUID() );
        }


        @Override
        public void setChild( final ScanColumn childColumn ) {
           //intentionally a no-op, since child is on the delegate
        }


        @Override
        public void addToCursor( final CursorCache cache ) {
            this.uuidCursorGenerator.addToCursor( cache, this );
        }


        @Override
        public ScanColumn getChild() {
            return delegate.getChild();
        }


        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) {
                return true;
            }
            if ( !( o instanceof UnionScanColumn ) ) {
                return false;
            }

            final UnionScanColumn that = ( UnionScanColumn ) o;

            return delegate.getUUID().equals( that.delegate.getUUID() );
        }


        @Override
        public int hashCode() {
            return delegate.getUUID().hashCode();
        }
    }
}
