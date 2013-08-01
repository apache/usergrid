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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.usergrid.persistence.Query.SortPredicate;
import org.usergrid.persistence.cassandra.CursorCache;

/**
 * @author tnine
 * 
 */
public class OrderByIterator extends MergeIterator {

  private final ResultIterator firstOrder;
  private final List<SortPredicate> secondary;

  /**
   * @param pageSize
   */
  public OrderByIterator(int pageSize, ResultIterator firstOrder, List<SortPredicate> secondary) {
    super(pageSize);
    this.firstOrder = firstOrder;
    this.secondary = secondary;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.persistence.query.ir.result.ResultIterator#finalizeCursor(
   * org.usergrid.persistence.cassandra.CursorCache, java.util.UUID)
   */
  @Override
  public void finalizeCursor(CursorCache cache, UUID lastValue) {
    // we just want to finalize the cursor in our primary sort

    firstOrder.finalizeCursor(cache, lastValue);

    //no secondary sorts, we're done
    if (secondary.size() == 0) {
      return;
    }

    // construct our own cursor on top of the cursor for the primary seek
    throw new UnsupportedOperationException("Finish this");
    
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.persistence.query.ir.result.MergeIterator#advance()
   */
  @Override
  protected Set<UUID> advance() {
    // no secondary sorting
    if (secondary.size() == 0) {
      return firstOrder.hasNext() ? firstOrder.next() : null;
    }

    // load our first page of results

    throw new UnsupportedOperationException("Finish this");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.persistence.query.ir.result.MergeIterator#doReset()
   */
  @Override
  protected void doReset() {
    firstOrder.reset();
  }

}
