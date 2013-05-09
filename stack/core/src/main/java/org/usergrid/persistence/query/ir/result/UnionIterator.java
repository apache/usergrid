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
public class UnionIterator extends MergeIterator {
  
    private TreeMap<UUID, ResultIterator> minMax = new TreeMap<UUID, ResultIterator>();
    private boolean initialized = false;

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.persistence.query.ir.result.MergeIterator#advance()
   */
  @Override
  protected UUID advance() {
    if (!initialized) {
      for (int i = 0; i < iterators.size(); i++) {
        addNext(iterators.get(i));
      }

    }

    //get the first entry, since it will be the min value
    Entry<UUID, ResultIterator> entry = minMax.firstEntry();

    if (entry == null) {
      return null;
    }

    //remove the entry
    minMax.remove(entry.getKey());

    //advance the iterator
    addNext(entry.getValue());

    return entry.getKey();

  }
  
  /**
   * Add the next value, advancing if this key already exists to avoid duplicates
   * @param itr
   */
  private void addNext(ResultIterator itr){
    //nothing to do
    if(!itr.hasNext()){
      return;
    }
    
    UUID next = itr.next();
    
    //we already have the entry via another iterator, we need to advance, or our entry is smaller than the first entry in the map
    while(minMax.containsKey(next)  && itr.hasNext()){
      next = itr.next();
    }
    
    minMax.put(next, itr);
    
  }

}
