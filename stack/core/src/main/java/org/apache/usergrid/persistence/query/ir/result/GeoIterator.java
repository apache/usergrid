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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.apache.usergrid.persistence.cassandra.CursorCache;
import org.apache.usergrid.persistence.geo.EntityLocationRef;
import org.apache.usergrid.persistence.geo.GeoIndexSearcher;
import org.apache.usergrid.persistence.geo.GeoIndexSearcher.SearchResults;
import org.apache.usergrid.persistence.geo.model.Point;
import org.apache.usergrid.persistence.query.ir.QuerySlice;

import static org.apache.usergrid.persistence.cassandra.Serializers.*;

/**
 * Simple wrapper around list results until the geo library is updated so support iteration and set returns
 *
 * @author tnine
 */
public class GeoIterator implements ResultIterator {

    /**
     *
     */
    private static final String DELIM = "+";
    private static final String TILE_DELIM = "TILE";

    private final GeoIndexSearcher searcher;
    private final int resultSize;
    private final QuerySlice slice;
    private final LinkedHashMap<UUID, LocationScanColumn> idOrder;
    private final Point center;
    private final double distance;
    private final String propertyName;

    private Set<ScanColumn> toReturn;
    private Set<ScanColumn> lastLoaded;

    // set when parsing cursor. If the cursor has gone to the end, this will be
    // true, we should return no results
    private boolean done = false;

    /** Moved and used as part of cursors */
    private EntityLocationRef last;
    private List<String> lastCellsSearched;

    /** counter that's incremented as we load pages. If pages loaded = 1 when reset,
     * we don't have to reload from cass */
    private int pagesLoaded = 0;


    /**
     *
     */
    public GeoIterator( GeoIndexSearcher searcher, int resultSize, QuerySlice slice, String propertyName, Point center,
                        double distance ) {
        this.searcher = searcher;
        this.resultSize = resultSize;
        this.slice = slice;
        this.propertyName = propertyName;
        this.center = center;
        this.distance = distance;
        this.idOrder = new LinkedHashMap<UUID, LocationScanColumn>( resultSize );
        this.lastLoaded = new LinkedHashSet<ScanColumn>( resultSize );
        parseCursor();
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Set<ScanColumn>> iterator() {
        return this;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        advance();
        return !done || toReturn != null;
    }


    private void advance() {
        // already loaded, do nothing
        if ( done || toReturn != null ) {
            return;
        }

        idOrder.clear();
        lastLoaded.clear();


        SearchResults results;

        try {
            results =
                    searcher.proximitySearch( last, lastCellsSearched, center, propertyName, 0, distance, resultSize );
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Unable to search geo locations", e );
        }

        List<EntityLocationRef> locations = results.entityLocations;

        lastCellsSearched = results.lastSearchedGeoCells;

        for ( int i = 0; i < locations.size(); i++ ) {

            final EntityLocationRef location = locations.get( i );
            final UUID id = location.getUuid();

            final LocationScanColumn locationScan = new LocationScanColumn( location );

            idOrder.put( id, locationScan );
            lastLoaded.add( locationScan );

            last = location;
        }

        if ( locations.size() < resultSize ) {
            done = true;
        }

        if ( lastLoaded.size() > 0 ) {
            toReturn = lastLoaded;
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#next()
     */
    @Override
    public Set<ScanColumn> next() {
        if ( !hasNext() ) {
            throw new NoSuchElementException();
        }

        Set<ScanColumn> temp = toReturn;

        toReturn = null;

        return temp;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException( "You cannot reove elements from this iterator" );
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.query.ir.result.ResultIterator#reset()
     */
    @Override
    public void reset() {
        //only 1 iteration was invoked.  Just reset the pointer rather than re-search
        if ( pagesLoaded == 1 ) {
            toReturn = lastLoaded;
            return;
        }

        idOrder.clear();
        lastLoaded.clear();
        lastCellsSearched = null;
        last = null;
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.persistence.query.ir.result.ResultIterator#finalizeCursor(
     * org.apache.usergrid.persistence.cassandra.CursorCache, java.util.UUID)
     */
    @Override
    public void finalizeCursor( CursorCache cache, UUID uuid ) {

        LocationScanColumn col = idOrder.get( uuid );

        if ( col == null ) {
            return;
        }

        final EntityLocationRef location = col.location;

        if ( location == null ) {
            return;
        }

        final int sliceHash = slice.hashCode();

        // get our next distance
        final double latitude = location.getLatitude();

        final double longitude = location.getLongitude();

        // now create a string value for this
        final StringBuilder builder = new StringBuilder();

        builder.append( uuid ).append( DELIM );
        builder.append( latitude ).append( DELIM );
        builder.append( longitude );

        if ( lastCellsSearched != null ) {
            builder.append( DELIM );

            for ( String geoCell : lastCellsSearched ) {
                builder.append( geoCell ).append( TILE_DELIM );
            }

            int length = builder.length();

            builder.delete( length - TILE_DELIM.length() - 1, length );
        }

        ByteBuffer buff = se.toByteBuffer( builder.toString() );


        cache.setNextCursor( sliceHash, buff );
    }


    /** Get the last cells searched in the iteraton */
    public List<String> getLastCellsSearched() {
        return Collections.unmodifiableList( lastCellsSearched );
    }


    private void parseCursor() {
        if ( !slice.hasCursor() ) {
            return;
        }

        String string = se.fromByteBuffer( slice.getCursor() );

        // was set to the end, set the no op flag
        if ( string.length() == 0 ) {
            done = true;
            return;
        }

        String[] parts = string.split( "\\" + DELIM );

        if ( parts.length < 3 ) {
            throw new RuntimeException(
                    "Geo cursors must contain 3 or more parts.  Incorrect cursor, please execute the query again" );
        }

        UUID startId = UUID.fromString( parts[0] );
        double latitude = Double.parseDouble( parts[1] );
        double longitude = Double.parseDouble( parts[2] );

        if ( parts.length >= 4 ) {
            String[] geoCells = parts[3].split( TILE_DELIM );

            lastCellsSearched = Arrays.asList( geoCells );
        }

        last = new EntityLocationRef( ( String ) null, startId, latitude, longitude );
    }


    private class LocationScanColumn implements ScanColumn {

        private final EntityLocationRef location;


        public LocationScanColumn( EntityLocationRef location ) {
            this.location = location;
        }


        @Override
        public UUID getUUID() {
            return location.getUuid();
        }


        @Override
        public ByteBuffer getCursorValue() {
            throw new UnsupportedOperationException(
                    "This is not supported for location scan columns.  It requires iterator information" );
        }


        @Override
        public boolean equals( Object o ) {
            if ( this == o ) {
                return true;
            }
            if ( !( o instanceof ScanColumn ) ) {
                return false;
            }

            ScanColumn that = ( ScanColumn ) o;

            return location.getUuid().equals( that.getUUID() );
        }


        @Override
        public int hashCode() {
            return location.getUuid().hashCode();
        }
    }
}
