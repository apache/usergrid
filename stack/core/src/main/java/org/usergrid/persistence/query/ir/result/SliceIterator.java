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
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;

import me.prettyprint.hector.api.beans.HColumn;

import org.usergrid.persistence.cassandra.CursorCache;
import org.usergrid.persistence.cassandra.index.IndexScanner;
import org.usergrid.persistence.query.ir.QuerySlice;

/**
 * An iterator that will take all slices and order them correctly
 * 
 * @author tnine
 * 
 */
public class SliceIterator<T> implements ResultIterator {

  private final LinkedHashMap<UUID, ByteBuffer> cols;
  private final QuerySlice slice;
  private final SliceParser<T> parser;
  private final IndexScanner scanner;
  private final int pageSize;
  private final boolean skipFirst;

  /**
   * Pointer to the uuid set until it's returned
   */
  private Set<UUID> lastResult;

  /**
   * counter that's incremented as we load pages. If pages loaded = 1 when
   * reset, we don't have to reload from cass
   */

  private int pagesLoaded = 0;

  /**
   * 
   * @param scanner The scanner to use to read the cols
   * @param slice The slice used in the scanner
   * @param parser The parser for the scanner results
   * @param skipFirst True if the first record should be skipped, used with cursors
   */
  public SliceIterator(QuerySlice slice, IndexScanner scanner,  SliceParser<T> parser, boolean skipFirst) {
    this.slice = slice;
    this.parser = parser;
    this.scanner = scanner;
    this.skipFirst = skipFirst;
    this.pageSize = scanner.getPageSize();
    this.cols = new LinkedHashMap<UUID, ByteBuffer>(this.pageSize);
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

    Iterator<HColumn<ByteBuffer, ByteBuffer>> results = scanner.next().iterator();

    cols.clear();

    /**
     * Skip the first value, it's from the previous cursor
     */
    if(skipFirst && pagesLoaded == 0  && results.hasNext()){
      results.next();
    }
    
    while (results.hasNext()) {

      ByteBuffer colName = results.next().getName().duplicate();  
    
      T parsed = parser.parse(colName);
      
      //skip this value, the parser has discarded it
      if(parsed == null){
        continue;
      }
          
      UUID id = parser.getUUID(parsed);

      cols.put(id, colName);
    }

    lastResult = cols.keySet();

    pagesLoaded++;

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
    // Do nothing, we'll just return the first page again
    if (pagesLoaded == 1) {
      lastResult = cols.keySet();
      return;
    }
    scanner.reset();
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

    ByteBuffer bytes = cols.get(lastLoaded);

    if (bytes == null) {
      return;
    }
    
    cache.setNextCursor(sliceHash, bytes);

  }

}
