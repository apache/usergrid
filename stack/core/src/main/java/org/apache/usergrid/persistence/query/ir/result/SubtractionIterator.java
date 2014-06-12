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


import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.usergrid.persistence.cassandra.CursorCache;

import com.google.common.collect.Sets;


/**
 * Simple iterator to perform Unions
 *
 * @author tnine
 */
public class SubtractionIterator extends MergeIterator {

    private ResultIterator keepIterator;
    private ResultIterator subtractIterator;


    public SubtractionIterator( int pageSize ) {
        super( pageSize );
    }


    /** @param subtractIterator the subtractIterator to set */
    public void setSubtractIterator( ResultIterator subtractIterator ) {
        this.subtractIterator = subtractIterator;
    }


    /** @param keepIterator the keepIterator to set */
    public void setKeepIterator( ResultIterator keepIterator ) {
        this.keepIterator = keepIterator;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.query.ir.result.ResultIterator#reset()
     */
    @Override
    public void doReset() {
        keepIterator.reset();
        subtractIterator.reset();
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.query.ir.result.MergeIterator#advance()
     */
    @Override
    protected Set<ScanColumn> advance() {
        if ( !keepIterator.hasNext() ) {
            return null;
        }

        Set<ScanColumn> results = new LinkedHashSet<ScanColumn>( pageSize );

        /**
         * The order here is important.  We don't want to check the advance unless we're less than our result size
         * Otherwise we have issues with side effects of cursor construction.
         */
        while (results.size() < pageSize && keepIterator.hasNext() ) {

            Set<ScanColumn> keepPage = keepIterator.next();

            while ( subtractIterator.hasNext() && keepPage.size() > 0 ) {
                keepPage = Sets.difference( keepPage, subtractIterator.next() );
            }

            subtractIterator.reset();

            results.addAll( keepPage );
        }

        return results;
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.persistence.query.ir.result.ResultIterator#finalizeCursor(org.apache.usergrid.persistence.cassandra
     * .CursorCache)
     */
    @Override
    public void finalizeCursor( CursorCache cache, UUID lastLoaded ) {
        //we can only keep a cursor on our keep result set, we must subtract from every page of keep when loading
        // results
        keepIterator.finalizeCursor( cache, lastLoaded );
    }
}
