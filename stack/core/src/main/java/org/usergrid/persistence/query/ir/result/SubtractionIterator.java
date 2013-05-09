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

import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.usergrid.utils.UUIDUtils;

import com.google.common.collect.Multimap;

/**
 * Simple iterator to perform Unions
 * 
 * @author tnine
 * 
 */
public class SubtractionIterator extends MergeIterator {

  private ResultIterator keepIterator;
  private ResultIterator subtractIterator;

  private UUID lastKeep;
  private UUID lastSubtract;

  /**
   * @param subtractIterator
   *          the subtractIterator to set
   */
  public void setSubtractIterator(ResultIterator subtractIterator) {
    this.subtractIterator = subtractIterator;
  }

  /**
   * @param keepIterator
   *          the keepIterator to set
   */
  public void setKeepIterator(ResultIterator keepIterator) {
    this.keepIterator = keepIterator;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.persistence.query.ir.result.MergeIterator#advance()
   */
  @Override
  protected UUID advance() {
    if (!keepIterator.hasNext()) {
      return null;
    }

    lastKeep = keepIterator.next();

    if (lastSubtract == null && subtractIterator.hasNext()) {
      lastSubtract = subtractIterator.next();
    }

    int compare = UUIDUtils.compare(lastSubtract, lastKeep);

    // the next uuid to remove is smaller than our current keep uuid, advance
    // our remove until it's >= to the current keep
    while (compare < 0) {
      lastSubtract = advance(subtractIterator);
      compare = UUIDUtils.compare(lastSubtract, lastKeep);
    }

    // they're the same, advance them both since we skip this value
    while (compare == 0) {
      // nothing left to keep return
      if (!keepIterator.hasNext()) {
        return null;
      }

      lastKeep = keepIterator.next();
      lastSubtract = advance(subtractIterator);

      compare = UUIDUtils.compare(lastSubtract, lastKeep);
    }

    UUID next = null;

    if (compare > 0) {
      next = lastKeep;
      lastKeep = advance(keepIterator);
    }

    return next;
  }

  /**
   * Advance the iterator
   * 
   * @param itr
   * @return
   */
  private UUID advance(ResultIterator itr) {
    return itr.hasNext() ? itr.next() : null;
  }

}
