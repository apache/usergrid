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
package org.apache.usergrid.persistence.cassandra.index;


import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.util.Assert;
import org.apache.usergrid.persistence.cassandra.CassandraService;


import me.prettyprint.hector.api.beans.HColumn;

import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_COMPOSITE_DICTIONARIES;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;


/**
 * @author tnine
 */
public class ConnectedIndexScanner implements IndexScanner {

    private final CassandraService cass;
    private final UUID applicationId;
    private final boolean reversed;
    private final int pageSize;
    private final String dictionaryType;
    private final UUID entityId;
    private final Iterator<String> connectionTypes;
    private final boolean skipFirst;


    /**
     * Pointer to our next start read
     */
    private ByteBuffer start;

    /**
     * Set to the original value to start scanning from
     */
    private ByteBuffer scanStart;

    /**
     * Iterator for our results from the last page load
     */
    private LinkedHashSet<HColumn<ByteBuffer, ByteBuffer>> lastResults;

    /**
     * True if our last load loaded a full page size.
     */
    private boolean hasMore = true;

    private String currentConnectionType;


    public ConnectedIndexScanner( CassandraService cass, String dictionaryType, UUID applicationId, UUID entityId,
                                  Iterator<String> connectionTypes, ByteBuffer start, boolean reversed, int pageSize,
                                  boolean skipFirst ) {

        Assert.notNull( entityId, "Entity id for row key construction must be specified when searching graph indexes" );
        // create our start and end ranges
        this.scanStart = start;
        this.cass = cass;
        this.applicationId = applicationId;
        this.entityId = entityId;
        this.start = scanStart;
        this.reversed = reversed;
        this.pageSize = pageSize;
        this.dictionaryType = dictionaryType;
        this.connectionTypes = connectionTypes;
        this.skipFirst = skipFirst;


        if ( connectionTypes.hasNext() ) {
            currentConnectionType = connectionTypes.next();
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.cassandra.index.IndexScanner#reset()
     */
    @Override
    public void reset() {
        hasMore = true;
        start = scanStart;
    }


    /**
     * Search the collection index using all the buckets for the given collection. Load the next page. Return false if
     * nothing was loaded, true otherwise
     */

    public boolean load() throws Exception {

        // nothing left to load
        if ( !hasMore ) {
            return false;
        }

        boolean skipFirst = this.skipFirst && start == scanStart;

        int totalSelectSize = pageSize + 1;

        //we're discarding the first, so increase our total size by 1 since this value will be inclusive in the seek
        if ( skipFirst ) {
            totalSelectSize++;
        }


        lastResults = new LinkedHashSet<HColumn<ByteBuffer, ByteBuffer>>();


        //cleanup columns for later logic
        //pointer to the first col we load
        HColumn<ByteBuffer, ByteBuffer> first = null;

        //pointer to the last column we load
        HColumn<ByteBuffer, ByteBuffer> last = null;

        //go through each connection type until we exhaust the result sets
        while ( currentConnectionType != null ) {

            //only load a delta size to get this next page
            int selectSize = totalSelectSize - lastResults.size();


            Object key = key( entityId, dictionaryType, currentConnectionType );


            List<HColumn<ByteBuffer, ByteBuffer>> results =
                    cass.getColumns( cass.getApplicationKeyspace( applicationId ), ENTITY_COMPOSITE_DICTIONARIES, key,
                            start, null, selectSize, reversed );

            final int resultSize = results.size();

            if(resultSize > 0){

                last = results.get( resultSize -1 );

                if(first == null ){
                    first = results.get( 0 );
                }
            }

            lastResults.addAll( results );


            // we loaded a full page, there might be more
            if ( resultSize == selectSize ) {
                hasMore = true;

                //we've loaded a full page
                break;
            }
            else {

                //we're done, there's no more connection types and we've loaded all cols for this type.
                if ( !connectionTypes.hasNext() ) {
                    hasMore = false;
                    currentConnectionType = null;
                    break;
                }

                //we have more connection types, but we've reached the end of this type,
                // keep going in the loop to load the next page

                currentConnectionType = connectionTypes.next();
            }
        }

        //remove the first element, we need to skip it
        if ( skipFirst && first != null) {
            lastResults.remove( first  );
        }

        if ( hasMore && last != null ) {
            // set the bytebuffer for the next pass
            start = last.getName();
            lastResults.remove( last );
        }

        return lastResults != null && lastResults.size() > 0;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Set<HColumn<ByteBuffer, ByteBuffer>>> iterator() {
        return this;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {

        // We've either 1) paged everything we should and have 1 left from our
        // "next page" pointer
        // Our currently buffered results don't exist or don't have a next. Try to
        // load them again if they're less than the page size
        if ( lastResults == null && hasMore ) {
            try {
                return load();
            }
            catch ( Exception e ) {
                throw new RuntimeException( "Error loading next page of indexbucket scanner", e );
            }
        }

        return false;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#next()
     */
    @Override
    public Set<HColumn<ByteBuffer, ByteBuffer>> next() {
        Set<HColumn<ByteBuffer, ByteBuffer>> returnVal = lastResults;

        lastResults = null;

        return returnVal;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException( "You can't remove from a result set, only advance" );
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.cassandra.index.IndexScanner#getPageSize()
     */
    @Override
    public int getPageSize() {
        return pageSize;
    }
}
