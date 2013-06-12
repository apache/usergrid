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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
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

  private final LinkedHashMap<UUID, Integer> idOrder;
  private final List<ByteBuffer> cols;
  private final QuerySlice slice;
  private final SliceParser<T> parser;
  private final IndexScanner scanner;
  private final int pageSize;

  /**
   * Pointer to the uuid set until it's returned
   */
  private Set<UUID> lastResult;

  /**
   *  counter that's incremented as we load pages. If pages loaded = 1 when
   *  reset, we don't have to reload from cass
   */
 
  private int pagesLoaded = 0;

  /**
   * 
   */
  public SliceIterator(IndexScanner scanner, QuerySlice slice, SliceParser<T> parser) {
    this.slice = slice;
    this.parser = parser;
    this.scanner = scanner;
    this.pageSize = scanner.getPageSize();
    this.idOrder = new LinkedHashMap<UUID, Integer>(this.pageSize);
    this.cols = new ArrayList<ByteBuffer>(this.pageSize);
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

    idOrder.clear();
    cols.clear();

    for (int i = 0; results.hasNext(); i++) {

      ByteBuffer colName = results.next().getName().duplicate();

      UUID id = parser.getUUID(parser.parse(colName));

      idOrder.put(id, i);

      cols.add(i, colName);
    }

    lastResult = idOrder.keySet();
    
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
    //Do nothing, we'll just return the first page again
    if(pagesLoaded == 1){
      lastResult = idOrder.keySet();
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

    // if nothing was loaded we still want to set the cursor to empty
    Integer loadedIndex = idOrder.get(lastLoaded);

    // the id did not come from this iterator, it's a no-op
    if (loadedIndex == null) {
      return;
    }

    ByteBuffer bytes = null;

    // edge case where the uuid is the last one loaded. In this case we need to
    // advance to the next page, then finalize our cursor
    if (loadedIndex == pageSize - 1) {
      // couldn't load the next page, nothing to page next time
      if (!load()) {
        bytes = ByteBuffer.allocate(0);
      }
      // set it to our first uuid from the new set
      else {
        bytes = cols.get(0);
      }
    }
    // last one we loaded, but not a full page. This slice is complete
    else if (loadedIndex == idOrder.size() - 1) {
      bytes = ByteBuffer.allocate(0);
    } else {
      bytes = cols.get(loadedIndex + 1);
    }

    cache.setNextCursor(sliceHash, bytes);

  }

}
