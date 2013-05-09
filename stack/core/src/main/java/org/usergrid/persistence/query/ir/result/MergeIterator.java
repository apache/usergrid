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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.usergrid.persistence.cassandra.CursorCache;

/**
 * @author tnine
 *
 */
public abstract class MergeIterator implements ResultIterator {

  protected List<ResultIterator> iterators = new ArrayList<ResultIterator>();

  
  //kept private on purpose so advance must return the correct value
  private UUID next;
  
  /**
   * 
   */
  public MergeIterator() {
  }

  /**
   * Add an iterator for our sub results
   * 
   * @param iterator
   */
  public void addIterator(ResultIterator iterator) {
    iterators.add(iterator);
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<UUID> iterator() {
    return this;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#hasNext()
   */
  @Override
  public boolean hasNext() {
    //if next isn't set, try to advance
    if (next == null) {
      next = advance();
    }

    return next != null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#next()
   */
  @Override
  public UUID next() {
    UUID returnVal = next;

    next = null;

    return returnVal;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#remove()
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException("You can't remove from a union iterator");
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.persistence.query.ir.result.ResultIterator#finalizeCursor(
   * org.usergrid.persistence.cassandra.CursorCache)
   */
  @Override
  public void finalizeCursor(CursorCache cache) {
    for (ResultIterator current : iterators) {
      current.finalizeCursor(cache);
    }
  }
  
  /**
   * Advance the iterator to the next value
   */
  protected abstract UUID advance();


}
