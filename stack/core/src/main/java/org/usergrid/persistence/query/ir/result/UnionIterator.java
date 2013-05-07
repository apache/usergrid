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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import org.usergrid.utils.UUIDUtils;

/**
 * An iterator that unions 1 or more subsets. It makes the assuming that sub
 * iterators iterate from min(uuid) to max(uuid)
 * 
 * @author tnine
 * 
 */
public class UnionIterator implements ResultIterator {

  private List<ResultIterator> iterators = new ArrayList<ResultIterator>();
  private UUID[] last;

  /**
   * Set to true if one of our iterators runs to the end
   */
  boolean completed = false;

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

    UUID max = null;
    
    while(true){
      for(int i = 0; i < last.length; i ++){
        max = UUIDUtils.max(first, second)
      }
      
    }
      
    

  }
  
  
  
  private UUID next(ResultIterator itr){
    if(!itr.hasNext()){
      return null;
    }
    
    return itr.next();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<UUID> iterator() {
    // once someone has set the iterators, we construct our min array
    last = new UUID[iterators.size()];

    for (int i = 0; i < last.length; i++) {
      Iterator<UUID> itr = iterators.get(i);

      if (!itr.hasNext()) {
        completed = true;
        break;
      }

      last[i] = itr.next();
    }
    
    Arrays.sort(last);

    return this;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#hasNext()
   */
  @Override
  public boolean hasNext() {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#next()
   */
  @Override
  public UUID next() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#remove()
   */
  @Override
  public void remove() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.persistence.query.ir.result.ResultIterator#finalizeCursor()
   */
  @Override
  public void finalizeCursor() {
  }

}
