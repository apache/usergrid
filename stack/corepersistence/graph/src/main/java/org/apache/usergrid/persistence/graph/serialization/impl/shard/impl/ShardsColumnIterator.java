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
package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import java.util.*;

import org.apache.usergrid.persistence.core.shard.SmartShard;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.astyanax.MultiRowColumnIterator;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.util.RangeBuilder;


/**
 * Internal iterator to iterate over multiple row keys
 *
 * @param <R> The row type
 * @param <C> The column type
 * @param <T> The parsed return type
 */
public class ShardsColumnIterator<R, C, T> implements Iterator<T> {


    private static final Logger logger = LoggerFactory.getLogger( ShardsColumnIterator.class );

    private final EdgeSearcher<R, C, T> searcher;

    private final MultiTenantColumnFamily<ScopedRowKey<R>, C> cf;

    private Iterator<T> currentColumnIterator;

    private final Keyspace keyspace;

    private final int pageSize;

    private final ConsistencyLevel consistencyLevel;


    public ShardsColumnIterator(final EdgeSearcher<R, C, T> searcher,
                                final MultiTenantColumnFamily<ScopedRowKey<R>, C> cf, final Keyspace keyspace,
                                final ConsistencyLevel consistencyLevel, final int pageSize ) {
        this.searcher = searcher;
        this.cf = cf;
        this.keyspace = keyspace;
        this.pageSize = pageSize;
        this.consistencyLevel = consistencyLevel;
    }


    @Override
    public boolean hasNext() {

        /**
         * Iterator isn't initialized, start it
         */
        if(currentColumnIterator == null){
            startIterator();
        }

        return currentColumnIterator.hasNext();
    }


    @Override
    public T next() {
        if ( !hasNext() ) {
            throw new NoSuchElementException( "There are no more rows or columns left to advance" );
        }

        return currentColumnIterator.next();
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException( "Remove is unsupported" );
    }


    /**
     * Advance our iterator to the next row (assumes the check for row keys is elsewhere)
     */
    private void startIterator() {

        if (logger.isTraceEnabled()) {
            logger.trace("Starting shards column iterator");
        }


        /**
         * If the edge is present, we need to being seeking from this
         */

        final RangeBuilder rangeBuilder = new RangeBuilder().setLimit( pageSize );


        //set the range into the search
        searcher.buildRange( rangeBuilder );



        /**
         * Get our list of slices
         */
        final List<ScopedRowKey<R>> rowKeys = searcher.getRowKeys();

        final List<SmartShard> rowKeysWithShardEnd = searcher.getRowKeysWithShardEnd();

        final boolean ascending = searcher.getOrder() == SearchByEdgeType.Order.ASCENDING;

        if (logger.isTraceEnabled()) {
            logger.trace("Searching with row keys {}", rowKeys);
        }

        currentColumnIterator = new MultiRowColumnIterator<>( keyspace, cf,  consistencyLevel, searcher, searcher,
            searcher.getComparator(), rowKeys, pageSize, rowKeysWithShardEnd, ascending);


    }

}
