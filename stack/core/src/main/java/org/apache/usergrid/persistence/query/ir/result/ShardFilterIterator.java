/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package org.apache.usergrid.persistence.query.ir.result;


import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An iterator that will check if the parsed column is part of this shard.  This is required due to a legacy storage
 * format in both connection pointers, as well as geo points.
 *
 * Some formats are not sharded by target entity, as a result, we get data partition mismatches when performing
 * intersections and seeks.  This is meant to discard target entities that are not part of the current shard
 *
 * @author tnine
 */
public class ShardFilterIterator implements ResultIterator {

    private static final Logger logger = LoggerFactory.getLogger( ShardFilterIterator.class );

    private final ShardFilter shardFilter;
    private final ResultIterator resultsIterator;
    private final int pageSize;

    private Set<ScanColumn> current;


    /**
     * @param shardFilter The validator to use when validating results belong to a shard
     * @param resultsIterator The iterator to filter results from
     * @param pageSize
     */
    public ShardFilterIterator( final ShardFilter shardFilter, final ResultIterator resultsIterator,
                                final int pageSize ) {
        this.shardFilter = shardFilter;
        this.resultsIterator = resultsIterator;
        this.pageSize = pageSize;
    }



    @Override
    public void reset() {
        current = null;
        resultsIterator.reset();
    }



    @Override
    public Iterator<Set<ScanColumn>> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {
        if(current == null){
            advance();
        }

        return current != null && current.size() > 0;
    }


    @Override
    public Set<ScanColumn> next() {

        final Set<ScanColumn> toReturn = current;

        current = null;

        return toReturn;
    }


    /**
     * Advance the column pointers
     */
    private void advance(){

        final Set<ScanColumn> results = new LinkedHashSet<ScanColumn>(  );

        while(results.size() < pageSize && resultsIterator.hasNext() ){

            final Iterator<ScanColumn> scanColumns = resultsIterator.next().iterator();


            while(results.size() < pageSize && scanColumns.hasNext()){
                final ScanColumn scanColumn = scanColumns.next();

                if( shardFilter.isInShard( scanColumn )){
                   results.add( scanColumn );
                }
            }
        }

        current = results;


    }
}
