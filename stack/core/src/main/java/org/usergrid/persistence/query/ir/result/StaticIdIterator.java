package org.usergrid.persistence.query.ir.result;


import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.usergrid.persistence.cassandra.CursorCache;


/** Simple iterator that just returns UUIDs that are set into it */
public class StaticIdIterator implements ResultIterator {

    private final Set<ScanColumn> ids;

    private boolean returnedOnce = false;


    /**
     *
     */
    public StaticIdIterator( UUID id ) {
        final ScanColumn col = new UUIDIndexSliceParser.UUIDColumn( id, ByteBuffer.allocate( 0 ) );

        ids = Collections.singleton( col );
    }


    @Override
    public void reset() {
        //no op
    }


    @Override
    public void finalizeCursor( CursorCache cache, UUID lastValue ) {
        //no cursor, it's a static list
    }


    @Override
    public Iterator<Set<ScanColumn>> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {
        return !returnedOnce;
    }


    @Override
    public Set<ScanColumn> next() {
        returnedOnce = true;
        return ids;
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException( "This iterator does not support remove" );
    }
}
