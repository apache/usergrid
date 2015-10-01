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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.usergrid.persistence.core.astyanax.ColumnNameIterator;
import org.apache.usergrid.persistence.core.astyanax.MultiKeyColumnNameIterator;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.query.RowQuery;
import com.netflix.astyanax.util.RangeBuilder;


/**
 *
 * Iterator to keep iterating over multiple shard groups to stream results
 *
 * @param <T> The parsed return type
 */
public abstract class ShardGroupColumnIterator<T> implements Iterator<T> {


    private final Iterator<ShardEntryGroup> entryGroupIterator;
    private Iterator<T> elements;


    public ShardGroupColumnIterator( final Iterator<ShardEntryGroup> entryGroupIterator ){
        this.entryGroupIterator = entryGroupIterator;
    }


    @Override
    public boolean hasNext() {


        if(elements == null){
            return advance();
        }

        if(elements.hasNext()){
            return true;
        }

        //we've exhausted our shard groups and we don't have a next, we can't continue
        if(!entryGroupIterator.hasNext()){
            return false;
        }


        return advance();
    }


    @Override
    public T next() {
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
     * @param readShards the read shards to use
     * @return
     */
    protected abstract Iterator<T> getIterator(Collection<Shard> readShards);


    public boolean advance(){

        while(entryGroupIterator.hasNext()){

            final ShardEntryGroup group = entryGroupIterator.next();

            elements = getIterator( group.getReadShards() );

            /**
             * We're done, we have some columns to return
             */
            if(elements.hasNext()){
                return true;
            }

        }


        return false;

    }
}
