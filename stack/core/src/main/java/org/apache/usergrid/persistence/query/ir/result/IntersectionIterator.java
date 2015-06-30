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
 * An iterator that unions 1 or more subsets. It makes the assuming that sub iterators iterate from min(uuid) to
 * max(uuid)
 *
 * @author tnine
 */
public class IntersectionIterator extends MultiIterator {


    /**
     *
     */
    public IntersectionIterator( int pageSize ) {
        super( pageSize );
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.query.ir.result.ResultIterator#reset()
     */
    @Override
    public void doReset() {
        for ( ResultIterator itr : iterators ) {
            itr.reset();
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.query.ir.result.MergeIterator#advance()
     */
    @Override
    protected Set<ScanColumn> advance() {
        /**
         * Advance our sub iterators until the UUID's all line up
         */

        int size = iterators.size();

        if ( size == 0 ) {
            return null;
        }

        // edge case with only 1 iterator
        if ( size == 1 ) {

            ResultIterator itr = iterators.get( 0 );

            if ( !itr.hasNext() ) {
                return null;
            }

            return itr.next();
        }

        // begin our tree merge of the iterators

        return merge();
    }


    private Set<ScanColumn> merge() {

        Set<ScanColumn> results = new LinkedHashSet<ScanColumn>();
        ResultIterator rootIterator = iterators.get( 0 );


        //we've matched to the end
        if ( !rootIterator.hasNext() ) {
            return null;
        }


        //purposely check size first, that way we avoid another round trip if we can
        while ( results.size() < pageSize && rootIterator.hasNext() ) {

            Set<ScanColumn> intersection = rootIterator.next();

            for ( int i = 1; i < iterators.size(); i++ ) {

                ResultIterator joinIterator = iterators.get( i );

                intersection = merge( intersection, joinIterator );

                //nothing left short circuit, there is no point in advancing to further join iterators
                if ( intersection.size() == 0 ) {
                    break;
                }
            }

            //now add the intermediate results and continue
            results.addAll( intersection );
        }

        return results;
    }


    private Set<ScanColumn> merge( Set<ScanColumn> current, ResultIterator child ) {

        Set<ScanColumn> results = new LinkedHashSet<ScanColumn>( pageSize );

        while ( results.size() < pageSize ) {
            if ( !child.hasNext() ) {
                // we've iterated to the end, reset for next pass
                child.reset();
                return results;
            }


            final Set<ScanColumn> childResults = child.next();

            final Set<ScanColumn> intersection =  Sets.intersection( current, childResults );

            results.addAll( intersection );
        }

        return results;
    }

    //TODO, replace columns with slice parser here

//    /*
//     * (non-Javadoc)
//     *
//     * @see
//     * org.apache.usergrid.persistence.query.ir.result.ResultIterator#finalizeCursor(
//     * org.apache.usergrid.persistence.cassandra.CursorCache)
//     */
//    @Override
//    public void finalizeCursor( CursorCache cache, UUID lastLoaded ) {
//        ResultIterator itr = iterators.get( 0 );
//
//        //We can only create a cursor on our root level value in the intersection iterator.
//        if ( itr != null ) {
//            itr.finalizeCursor( cache, lastLoaded );
//        }
//    }
}
