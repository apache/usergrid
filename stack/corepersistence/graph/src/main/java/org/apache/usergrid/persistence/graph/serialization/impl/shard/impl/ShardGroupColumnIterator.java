/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardGroupDeletion;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;


/**
 * Iterator to keep iterating over multiple shard groups to stream results
 *
 * @param <T> The parsed return type
 */
public abstract class ShardGroupColumnIterator implements Iterator<MarkedEdge> {


    private static final Logger logger = LoggerFactory.getLogger( ShardGroupColumnIterator.class );

    private final ApplicationScope applicationScope;
    private final DirectedEdgeMeta directedEdgeMeta;
    private final ShardGroupDeletion shardGroupDeletion;
    private final Iterator<ShardEntryGroup> entryGroupIterator;


    private Iterator<MarkedEdge> elements;


    public ShardGroupColumnIterator( final ApplicationScope applicationScope, final DirectedEdgeMeta directedEdgeMeta,
                                     final ShardGroupDeletion shardGroupDeletion,
                                     final Iterator<ShardEntryGroup> entryGroupIterator ) {
        this.applicationScope = applicationScope;
        this.directedEdgeMeta = directedEdgeMeta;
        this.shardGroupDeletion = shardGroupDeletion;
        this.entryGroupIterator = entryGroupIterator;
    }


    @Override
    public boolean hasNext() {


        if ( elements == null ) {
            return advance();
        }

        if ( elements.hasNext() ) {
            return true;
        }

        //we've exhausted our shard groups and we don't have a next, we can't continue
        if ( !entryGroupIterator.hasNext() ) {
            return false;
        }


        return advance();
    }


    @Override
    public MarkedEdge next() {
        if ( !hasNext() ) {
            throw new NoSuchElementException( "There are no more rows or columns left to advance" );
        }

        return elements.next();
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException( "Remove is unsupported" );
    }


    /**
     * Get an iterator for the shard entry group
     *
     * @param readShards the read shards to use
     */
    protected abstract Iterator<MarkedEdge> getIterator( Collection<Shard> readShards );

    protected abstract Iterator<MarkedEdge> getIteratorFullRange( Collection<Shard> readShards );


    public boolean advance() {

        if (logger.isTraceEnabled()) logger.trace( "Advancing from shard entry group iterator" );

        while ( entryGroupIterator.hasNext() ) {

            final ShardEntryGroup group = entryGroupIterator.next();

            if (logger.isTraceEnabled()) logger.trace( "Shard entry group is {}.  Searching for edges in the shard", group );

            elements = getIterator( group.getReadShards() );

            /**
             * We're done, we have some columns to return
             */
            if ( elements.hasNext() ) {
                if (logger.isTraceEnabled()) logger.trace( "Found edges in shard entry group {}", group );
                return true;
            }
            else {
                if (logger.isTraceEnabled()) logger.trace( "Our shard is empty, we need to perform an audit on shard group {}", group );

                //fire and forget if we miss it here, we'll get it next read
                shardGroupDeletion.maybeDeleteShard(this.applicationScope, this.directedEdgeMeta, group, getIteratorFullRange( group.getReadShards() ) );


            }
        }

        if (logger.isTraceEnabled()) logger.trace( "Completed iterating shard group iterator" );

        return false;
    }
}
