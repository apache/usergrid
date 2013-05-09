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
import org.usergrid.utils.UUIDUtils;

/**
 * An iterator that unions 1 or more subsets. It makes the assuming that sub
 * iterators iterate from min(uuid) to max(uuid)
 * 
 * @author tnine
 * 
 */
public class UnionIterator implements ResultIterator {

  // pointer to set the finished value to
  private List<ResultIterator> iterators = new ArrayList<ResultIterator>();
  private UUID match;

  // The pointer to the last iterator we read, so we can advance and read from
  // the next one
  private int lastIterator;

  /**
   * We can't match any more
   */
  boolean complete = false;

  /**
   * 
   */
  public UnionIterator() {
  }

  /**
   * Add an iterator for our sub results
   * 
   * @param iterator
   */
  public void addIterator(ResultIterator iterator) {
    iterators.add(iterator);
  }

  /**
   * Advance our sub iterators until the UUID's all line up
   */
  private void intersect() {

    UUID current = match;
    UUID lastMax = null;

    int size = iterators.size();

    // edge case with only 1 iterator
    if (size == 1) {

      ResultIterator itr = iterators.get(0);

      if (!itr.hasNext()) {
        match = null;
        return;
      }

      match = itr.next();
      return;
    }

    int matchCount = 0;
    int iteratorIndex = lastIterator;

    while (matchCount != size) {

      iteratorIndex = iteratorIndex % iterators.size();

      ResultIterator itr = iterators.get(iteratorIndex);

      // we've run out, we can't match any more records
      if (!itr.hasNext()) {
        match = null;
        complete = true;
        return;
      }

      current = itr.next();

      // we haven't set a max yet, set it to our current and try the next
      // iterator
      if (lastMax == null) {
        matchCount = 1;
        iteratorIndex++;
        lastMax = current;
        continue;
      }

      // we have a last max, compare it to the current one
      int compare = UUIDUtils.compare(current, lastMax);

      // our current is larger than the last max, so set the last max , reset
      // our match count and keep
      // running to the next iterator
      if (compare > 0) {
        matchCount = 1;
        lastMax = current;
        iteratorIndex++;
        continue;
      }

      // our last max is still the largest, advance the current iterator again
      // for comparison
      if (compare < 0) {
        continue;
      }

      // we have a match, advance and check the next iterator vs this max

      matchCount++;
      iteratorIndex++;

    }

    match = lastMax;
    lastIterator = iteratorIndex;

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
    if (complete) {
      return false;
    }

    // else try to advance
    if (match == null) {
      intersect();
    }

    return match != null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#next()
   */
  @Override
  public UUID next() {
    UUID returnVal = match;

    match = null;

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

}
