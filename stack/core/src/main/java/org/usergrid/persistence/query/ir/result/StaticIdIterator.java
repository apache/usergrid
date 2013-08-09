package org.usergrid.persistence.query.ir.result;

import org.usergrid.persistence.cassandra.CursorCache;

import java.util.*;

/**
 *
 * Simple iterator that just returns UUIDs that are set into it
 * @author: tnine
 *
 */
public class StaticIdIterator implements ResultIterator{

  private final Set<UUID> ids;

  private boolean returnedOnce = false;

  /**
   *
   */
  public StaticIdIterator(UUID id) {
    ids = Collections.singleton(id);
  }


  @Override
  public void reset() {
    //no op
  }

  @Override
  public void finalizeCursor(CursorCache cache, UUID lastValue) {
    //no cursor, it's a static list
  }

  @Override
  public Iterator<Set<UUID>> iterator() {
    return this;
  }

  @Override
  public boolean hasNext() {
    return !returnedOnce;
  }

  @Override
  public Set<UUID> next() {
    returnedOnce = true;
    return ids;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("This iterator does not support remove");
  }
}
