/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence.query.ir.result;

import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.NULL_ID;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.UUIDSerializer;

import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.cassandra.ConnectedEntityRefImpl;
import org.usergrid.persistence.cassandra.ConnectionRefImpl;
import org.usergrid.persistence.cassandra.CursorCache;
import org.usergrid.persistence.cassandra.RelationManagerImpl;
import org.usergrid.persistence.query.ir.QuerySlice;
import org.usergrid.utils.UUIDUtils;

/**
 * An iterator that will take all slices and order them correctly
 * 
 * @author tnine
 * 
 */
public class ConnectionIterator implements ResultIterator {

  private static final UUIDSerializer UUID_SER = UUIDSerializer.get();

  private final QuerySlice slice;
  private final UUID cursorUUUID;
  private final RelationManagerImpl impl;
  private final EntityRef headEntity;

  /**
   * Pointer to the uuid set until it's returned
   */
  private Set<UUID> lastResult;



  /**
   * 
   * @param scanner
   *          The scanner to use to read the cols
   * @param slice
   *          The slice to use for cursors
   */
  public ConnectionIterator(EntityRef headEntity, QuerySlice slice, RelationManagerImpl impl) {
    this.slice = slice;
    this.impl = impl;
    this.cursorUUUID = slice.hasCursor() ? UUID_SER.fromByteBuffer(slice.getCursor()) : null;
    this.headEntity = headEntity;

  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<Set<UUID>> iterator() {
    return this;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#hasNext()
   */
  @Override
  public boolean hasNext() {
    if (lastResult == null) {
      return load();
    }

    // we can only ever load once with the reverse index
    return false;
  }

  private boolean load() {

    List<ConnectionRefImpl> refs = null;

    try {
      refs = impl.getConnections(new ConnectionRefImpl(headEntity, new ConnectedEntityRefImpl(NULL_ID),
          new ConnectedEntityRefImpl(null, null, null)), false);
    } catch (Exception e) {
      throw new RuntimeException("Error in loading connections", e);
    }

    if (refs == null) {
      return false;
    }

    lastResult = new LinkedHashSet<UUID>(refs.size());

    for (ConnectionRefImpl ref : refs) {
      UUID connectedId = ref.getConnectedEntityId();

      if (cursorUUUID != null && UUIDUtils.compare(cursorUUUID, connectedId) <= 0) {
        continue;
      }

      lastResult.add(ref.getConnectedEntityId());

    }

    return lastResult.size() > 0;

  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#next()
   */
  @Override
  public Set<UUID> next() {
    return lastResult;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#remove()
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove is not supported");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.persistence.query.ir.result.ResultIterator#reset()
   */
  @Override
  public void reset() {
    // Do nothing, we'll just return the first page again
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.persistence.query.ir.result.ResultIterator#finalizeCursor()
   */
  @Override
  public void finalizeCursor(CursorCache cache, UUID lastLoaded) {
    int sliceHash = slice.hashCode();
    cache.setNextCursor(sliceHash, UUID_SER.toByteBuffer(lastLoaded));

  }

}
