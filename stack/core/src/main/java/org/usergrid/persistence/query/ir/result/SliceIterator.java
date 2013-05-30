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

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;

import me.prettyprint.hector.api.beans.HColumn;

import org.usergrid.persistence.cassandra.CursorCache;
import org.usergrid.persistence.cassandra.index.IndexScanner;
import org.usergrid.persistence.query.ir.SliceNode;

/**
 * An iterator that will take all slices and order them correctly
 * 
 * @author tnine
 * 
 */
public class SliceIterator<T> implements ResultIterator {

  private LinkedHashSet<UUID> lastResult;
  private SliceNode slice;
  private SliceParser<T> parser;
  private IndexScanner scanner;

  /**
   * 
   */
  public SliceIterator(IndexScanner scanner, SliceNode slice, SliceParser<T> parser) {
    this.slice = slice;
    this.parser = parser;
    this.scanner = scanner;
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

    return true;
  }

  private boolean load() {
    if (!scanner.hasNext()) {
      return false;
    }

    NavigableSet<HColumn<ByteBuffer, ByteBuffer>> results = scanner.next();

    LinkedHashSet<UUID> ids = new LinkedHashSet<UUID>();

    for (HColumn<ByteBuffer, ByteBuffer> col : results) {
      ids.add(parser.getUUID(parser.parse(col.getName())));
    }

    lastResult = ids;

    return lastResult != null && lastResult.size() > 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#next()
   */
  @Override
  public Set<UUID> next() {
    Set<UUID> temp = lastResult;
    lastResult = null;
    return temp;
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
    scanner.reset();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.persistence.query.ir.result.ResultIterator#finalizeCursor()
   */
  @Override
  public void finalizeCursor(CursorCache cache,UUID lastLoaded) {

    int cursorId = slice.hashCode();
   
    //TODO finish cursors
    // ByteBuffer bytes = ByteBufferUtil.EMPTY_BYTE_BUFFER;
    //
    // if (hasNext()) {
    // bytes = parser.serialize(idIterator.next());
    // }
    //
    // // otherwise it's an empty buffer
    // cache.setNextCursor(slice.hashCode(), bytes);
  }

}
