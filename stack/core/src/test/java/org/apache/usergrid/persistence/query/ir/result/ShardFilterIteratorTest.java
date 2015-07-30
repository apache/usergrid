/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.usergrid.persistence.query.ir.result;


import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.cassandra.SimpleIndexBucketLocatorImpl;
import org.apache.usergrid.utils.UUIDUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import static org.junit.Assert.assertTrue;


/**
 * Simple test to test UUID
 */
public class ShardFilterIteratorTest {

    @Test
    public void testIndexValues() {

        int size = 100;

        final Multimap<String, ScanColumn> shards = HashMultimap.create();

        final IndexBucketLocator indexBucketLocator = new SimpleIndexBucketLocatorImpl( 20 );

        final Set<ScanColumn> allColumns = new LinkedHashSet<ScanColumn>( size );


        final UUIDCursorGenerator uuidCursorGenerator = new UUIDCursorGenerator( 1 );

        for ( int i = 0; i < size; i++ ) {
            final UUID entityId = UUIDUtils.newTimeUUID();

            final String shard = indexBucketLocator.getBucket(entityId );

            final UUIDColumn uuidColumn = new UUIDColumn( entityId, 1, uuidCursorGenerator );

            //add the shard to our assertion set
            shards.put( shard, uuidColumn );

            allColumns.add( uuidColumn );
        }

        //now create an iterator with all the uuid sand verity they're correct.


        for ( final String shard : shards.keySet() ) {
            //create a copy of our expected uuids
            final Set<ScanColumn> expected = new HashSet<ScanColumn>( shards.get( shard ) );


            final TestIterator testIterator = new TestIterator( new HashSet<ScanColumn>( shards.get( shard ) ) );


            final TestEntityFilter collectionSliceShardFilter =
                    new TestEntityFilter( indexBucketLocator, shard );


            //now iterate over everything and remove it from expected
            final ShardFilterIterator shardFilterIterator = new ShardFilterIterator(
                    collectionSliceShardFilter, testIterator, 10 );

            //keep removing
            while( shardFilterIterator.hasNext()){

                //check each scan column from our results
                for(final ScanColumn column : shardFilterIterator.next()){

                    final boolean contained = expected.remove( column );

                    assertTrue( "Column should be present", contained );

                }


            }

            assertTrue("expected should be empty", expected.isEmpty());
        }

    }

    private static final class TestEntityFilter implements ShardFilter{

        private final IndexBucketLocator indexBucketLocator;
        private final String expectedShard;


        private TestEntityFilter( final IndexBucketLocator indexBucketLocator, final String expectedShard ) {
            this.indexBucketLocator = indexBucketLocator;
            this.expectedShard = expectedShard;
        }


        @Override
        public boolean isInShard( final ScanColumn scanColumn ) {

            final String shard =  indexBucketLocator.getBucket( scanColumn.getUUID() );

            return expectedShard.equals( shard );
        }
    }


    private static final class TestIterator implements ResultIterator {


        private final Set<ScanColumn> scanColumns;
        private boolean completed;


        private TestIterator( final Set<ScanColumn> scanColumns ) {this.scanColumns = scanColumns;}


        @Override
        public void reset() {
            //no op
        }


        @Override
        public Iterator<Set<ScanColumn>> iterator() {
            return this;
        }


        @Override
        public boolean hasNext() {
            return !completed;
        }


        @Override
        public Set<ScanColumn> next() {
            completed = true;
            return scanColumns;
        }
    }
}
