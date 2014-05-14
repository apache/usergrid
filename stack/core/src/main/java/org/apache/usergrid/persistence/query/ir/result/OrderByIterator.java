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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityPropertyComparator;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Query.SortPredicate;
import org.apache.usergrid.persistence.cassandra.CursorCache;
import org.apache.usergrid.persistence.query.ir.QuerySlice;

import org.apache.commons.collections.comparators.ComparatorChain;

import static org.apache.usergrid.persistence.cassandra.Serializers.*;

/**
 * 1) Take a result set iterator as the child 2) Iterate only over candidates and create a cursor from the candidates
 *
 * @author tnine
 */

public class OrderByIterator extends MergeIterator {

    private static final String NAME_UUID = "uuid";
    private static final Logger logger = LoggerFactory.getLogger( OrderByIterator.class );
    private final QuerySlice slice;
    private final ResultIterator candidates;
    private final ComparatorChain subSortCompare;
    private final List<String> secondaryFields;
    private final EntityManager em;

    //our last result from in memory sorting
    private SortedEntitySet entries;


    /**
     * @param pageSize
     */
    public OrderByIterator( QuerySlice slice, List<Query.SortPredicate> secondary, ResultIterator candidates,
                            EntityManager em, int pageSize ) {
        super( pageSize );
        this.slice = slice;
        this.em = em;
        this.candidates = candidates;
        this.subSortCompare = new ComparatorChain();
        this.secondaryFields = new ArrayList<String>( 1 + secondary.size() );

        //add the sort of the primary column
        this.secondaryFields.add( slice.getPropertyName() );
        this.subSortCompare
                .addComparator( new EntityPropertyComparator( slice.getPropertyName(), slice.isReversed() ) );

        for ( SortPredicate sort : secondary ) {
            this.subSortCompare.addComparator( new EntityPropertyComparator( sort.getPropertyName(),
                    sort.getDirection() == Query.SortDirection.DESCENDING ) );
            this.secondaryFields.add( sort.getPropertyName() );
        }

        //do uuid sorting last, this way if all our previous sorts are equal, we'll have a reproducible sort order for
        // paging
        this.secondaryFields.add( NAME_UUID );
        this.subSortCompare.addComparator( new EntityPropertyComparator( NAME_UUID, false ) );
    }


    @Override
    protected Set<ScanColumn> advance() {

        ByteBuffer cursor = slice.getCursor();

        UUID minEntryId = null;

        if ( cursor != null ) {
            minEntryId = ue.fromByteBuffer( cursor );
        }

        entries = new SortedEntitySet( subSortCompare, em, secondaryFields, pageSize, minEntryId );

        /**
         *  keep looping through our peek iterator.  We need to inspect each forward page to ensure we have performed a
         *  seek to the end of our primary range.  Otherwise we need to keep aggregating. I.E  if the value is a boolean
         *  and we order by "true
         *  asc, timestamp desc" we must load every entity that has the value "true" before sub sorting,
         *  then drop all values that fall out of the sort.
         */
        while ( candidates.hasNext() ) {


            for ( ScanColumn id : candidates.next() ) {
                entries.add( id );
            }

            entries.load();
        }


        return entries.toIds();
    }


    @Override
    protected void doReset() {
        // no op
    }


    @Override
    public void finalizeCursor( CursorCache cache, UUID lastValue ) {
        int sliceHash = slice.hashCode();

        ByteBuffer bytes = ue.toByteBuffer( lastValue );

        if ( bytes == null ) {
            return;
        }

        cache.setNextCursor( sliceHash, bytes );
    }


    /** A Sorted set with a max size. When a new entry is added, the max is removed */
    public static final class SortedEntitySet extends TreeSet<Entity> {

        private final int maxSize;
        private final Map<UUID, ScanColumn> cursorVal = new HashMap<UUID, ScanColumn>();
        private final EntityManager em;
        private final List<String> fields;
        private final Entity minEntity;
        private final Comparator<Entity> comparator;


        public SortedEntitySet( Comparator<Entity> comparator, EntityManager em, List<String> fields, int maxSize,
                                UUID minEntityId ) {
            super( comparator );
            this.maxSize = maxSize;
            this.em = em;
            this.fields = fields;
            this.comparator = comparator;
            this.minEntity = getPartialEntity( minEntityId );
        }


        @Override
        public boolean add( Entity entity ) {

            // don't add this entity.  We get it in our scan range, but it's <= the minimum value that
            //should be allowed in the result set
            if ( minEntity != null && comparator.compare( entity, minEntity ) <= 0 ) {
                return false;
            }

            boolean added = super.add( entity );

            while ( size() > maxSize ) {
                //remove our last element, we're over size
                Entity e = this.pollLast();
                //remove it from the cursors as well
                cursorVal.remove( e.getUuid() );
            }

            return added;
        }


        /** add the id to be loaded, and the dynamiccomposite column that belongs with it */
        public void add( ScanColumn col ) {
            cursorVal.put( col.getUUID(), col );
        }


        private Entity getPartialEntity( UUID minEntityId ) {
            List<Entity> entities;

            try {
                entities = em.getPartialEntities( Collections.singletonList( minEntityId ), fields );
            }
            catch ( Exception e ) {
                logger.error( "Unable to load partial entities", e );
                throw new RuntimeException( e );
            }

            if ( entities == null || entities.size() == 0 ) {
                return null;
            }

            return entities.get( 0 );
        }


        public void load() {
            try {
                for ( Entity e : em.getPartialEntities( cursorVal.keySet(), fields ) ) {
                    add( e );
                }
            }
            catch ( Exception e ) {
                logger.error( "Unable to load partial entities", e );
                throw new RuntimeException( e );
            }
        }


        /** Turn our sorted entities into a set of ids */
        public Set<ScanColumn> toIds() {
            Iterator<Entity> itr = iterator();

            Set<ScanColumn> columns = new LinkedHashSet<ScanColumn>( this.size() );

            while ( itr.hasNext() ) {

                UUID id = itr.next().getUuid();

                columns.add( cursorVal.get( id ) );
            }

            return columns;
        }
    }
}
