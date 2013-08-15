package org.usergrid.persistence.query.ir.result;

import org.usergrid.persistence.cassandra.CursorCache;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 *
 * Iterator that never returns results
 * @author: tnine
 *
 */
public class EmptyIterator implements ResultIterator {
  @Override
  public void reset() {
    //no op
  }

  @Override
  public void finalizeCursor(CursorCache cache, UUID lastValue) {
    //no op
  }

  @Override
  public Iterator<Set<UUID>> iterator() {
    return this;
  }

  @Override
  public boolean hasNext() {
    return false;
  }

  @Override
  public Set<UUID> next() {
    return null;
  }

  @Override
  public void remove() {
   throw new UnsupportedOperationException("Not supported");
  }
}
