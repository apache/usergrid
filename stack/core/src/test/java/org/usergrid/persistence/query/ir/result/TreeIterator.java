package org.usergrid.persistence.query.ir.result;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.Ignore;
import org.usergrid.persistence.cassandra.CursorCache;

/**
 * Simple iterator for testing that orders UUIDs
 * @author tnine
 *
 */
@Ignore("not a test")
public class TreeIterator implements ResultIterator {

    private TreeSet<UUID> uuids = new TreeSet<UUID>();
    private Iterator<UUID> iterator;
    

    /**
     * Add a uuid to the list
     * @param ids
     */
    public void add(UUID... ids) {
      for (UUID current : ids) {
        uuids.add(current);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<UUID> iterator() {
      if(iterator == null){
        this.iterator = uuids.iterator();
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
      if(iterator == null){
        this.iterator = uuids.iterator();
      }
      
      return iterator.hasNext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#next()
     */
    @Override
    public UUID next() {
      if(iterator == null){
        this.iterator = uuids.iterator();
      }
      
      return iterator.next();
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
     * @see org.usergrid.persistence.query.ir.result.ResultIterator#finalizeCursor(org.usergrid.persistence.cassandra.CursorCache)
     */
    @Override
    public void finalizeCursor(CursorCache cache) {
      
    }

  }
