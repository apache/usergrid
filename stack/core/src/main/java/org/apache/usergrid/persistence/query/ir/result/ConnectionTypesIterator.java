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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.cassandra.CassandraService;

import me.prettyprint.hector.api.beans.HColumn;

import static org.apache.usergrid.persistence.Schema.DICTIONARY_CONNECTED_TYPES;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_CONNECTING_TYPES;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_DICTIONARIES;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.usergrid.persistence.cassandra.Serializers.*;

/** Iterator to iterate all types of connections the entity participates in */
public class ConnectionTypesIterator implements Iterator<String>, Iterable<String> {

    private final CassandraService cass;
    private final UUID applicationId;
    private final Object key;
    //  private final UUID entityId;
    private final int pageSize;
    //  private static final String dictionaryName;


    private boolean hasMore = true;
    private Object start = null;

    private Iterator<String> lastResults;


    /**
     * The connection types iterator.
     *
     * @param cass The cassandra service to use
     * @param applicationId The application id to use
     * @param entityId The entityId to use.  Can be a source for outgoing connections, or target for incoming
     * connections
     * @param outgoing True if this is a search from source->target on the edge, false if it is a search from
     * target<-source
     * @param pageSize The page size to use for batch fetching
     */
    public ConnectionTypesIterator( CassandraService cass, UUID applicationId, UUID entityId, boolean outgoing,
                                    int pageSize ) {
        this.cass = cass;
        this.applicationId = applicationId;
        this.pageSize = pageSize;

        this.key =
                outgoing ? key( entityId, DICTIONARY_CONNECTED_TYPES ) : key( entityId, DICTIONARY_CONNECTING_TYPES );
    }


    @Override
    public Iterator<String> iterator() {
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
        if ( ( lastResults == null || !lastResults.hasNext() ) && hasMore ) {
            try {
                return load();
            }
            catch ( Exception e ) {
                throw new RuntimeException( "Error loading next page of indexbucket scanner", e );
            }
        }

        return lastResults.hasNext();
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#next()
     */
    @Override
    public String next() {

        if ( !hasNext() ) {
            throw new NoSuchElementException( "There are no elements left in this iterator" );
        }

        return lastResults.next();
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


    /**
     * Search the collection index using all the buckets for the given collection. Load the next page. Return false if
     * nothing was loaded, true otherwise
     */

    public boolean load() throws Exception {

        // nothing left to load
        if ( !hasMore ) {
            return false;
        }

        // if we skip the first we need to set the load to page size +2, since we'll
        // discard the first
        // and start paging at the next entity, otherwise we'll just load the page
        // size we need
        int selectSize = pageSize + 1;


        List<HColumn<ByteBuffer, ByteBuffer>> results =
                cass.getColumns( cass.getApplicationKeyspace( applicationId ), ENTITY_DICTIONARIES, key, start, null,
                        selectSize, false );

        // we loaded a full page, there might be more
        if ( results.size() == selectSize ) {
            hasMore = true;

            // set the bytebuffer for the next pass
            start = results.get( results.size() - 1 ).getName();

            results.remove( results.size() - 1 );
        }
        else {
            hasMore = false;
        }


        List<String> stringResults = new ArrayList<String>( results.size() );

        //do the parse here
        for ( HColumn<ByteBuffer, ByteBuffer> col : results ) {
            final String value = se.fromByteBuffer( col.getName() );

            //always ignore loopback, this is legacy data that needs cleaned up, and it doesn't belong here
            if ( !Schema.TYPE_CONNECTION.equalsIgnoreCase( value ) ) {
                stringResults.add( value );
            }
        }


        lastResults = stringResults.iterator();


        return stringResults.size() > 0;
    }
}
