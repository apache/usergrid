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
import java.util.List;
import java.util.UUID;

import org.usergrid.persistence.cassandra.CursorCache;

/**
 * @author tnine
 * 
 */
public abstract class MultiIterator extends MergeIterator {

  protected List<ResultIterator> iterators = new ArrayList<ResultIterator>();

  /**
   * @param pageSize
   */
  public MultiIterator(int pageSize) {
    super(pageSize);
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
   * @see org.usergrid.persistence.query.ir.result.ResultIterator#reset()
   */
  @Override
  public void reset() {
    for (ResultIterator itr : iterators) {
      itr.reset();
    }
  }


}
