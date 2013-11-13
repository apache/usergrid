package org.usergrid.persistence.query.ir.result;


import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Ignore;
import org.usergrid.persistence.cassandra.CursorCache;

import com.google.common.collect.Iterables;


/**
 * Simple iterator for testing that iterates UUIDs in the order returned
 *
 * @author tnine
 */
@Ignore("not a test")
public class InOrderIterator implements ResultIterator {

    private LinkedHashSet<ScanColumn> uuids = new LinkedHashSet<ScanColumn>();
    private Iterator<List<ScanColumn>> iterator;
    private int pageSize = 1000;


    public InOrderIterator( int pageSize ) {
        this.pageSize = pageSize;
    }


    /** Add a uuid to the list */
    public void add( UUID... ids ) {
        for ( UUID current : ids ) {
            uuids.add( new UUIDIndexSliceParser.UUIDColumn( current, ByteBuffer.allocate( 0 ) ) );
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Set<ScanColumn>> iterator() {
        if ( iterator == null ) {
            reset();
        }

        return this;
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        if ( iterator == null ) {
            reset();
        }

        return iterator.hasNext();
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#next()
     */
    @Override
    public Set<ScanColumn> next() {
        if ( iterator == null ) {
            reset();
        }

        return new LinkedHashSet<ScanColumn>( iterator.next() );
    }


    /* (non-Javadoc)
     * @see org.usergrid.persistence.query.ir.result.ResultIterator#reset()
     */
    @Override
    public void reset() {
        this.iterator = Iterables.partition( uuids, pageSize ).iterator();
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove() {
    }


    /* (non-Javadoc)
     * @see org.usergrid.persistence.query.ir.result.ResultIterator#finalizeCursor(org.usergrid.persistence.cassandra
     * .CursorCache)
     */
    @Override
    public void finalizeCursor( CursorCache cache, UUID lastLoaded ) {

    }
}
