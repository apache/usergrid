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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.usergrid.persistence.cassandra.CursorCache;

import com.google.common.collect.Sets;

/**
 * Simple iterator to perform Unions
 * 
 * @author tnine
 * 
 */
public class UnionIterator extends MultiIterator {

  /**
   * results that were left from our previous union. These are kept and returned
   * before advancing iterators
   */
  private Set<UUID> remainderResults;

  private int currentIndex = -1;

  /**
   * @param pageSize
   */
  public UnionIterator(int pageSize) {
    super(pageSize);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.persistence.query.ir.result.MergeIterator#advance()
   */
  @Override
  protected Set<UUID> advance() {
    
    int size = iterators.size();
    
    if(size == 0){
      return null;
    }

    Set<UUID> resultSet = null;

    if (remainderResults != null) {
      resultSet = remainderResults;
      remainderResults = null;
    } else {
      resultSet = new LinkedHashSet<UUID>();
    }

    /**
     * We have results from a previous merge
     */

   
    int complete = 0;
        
    while (resultSet.size() < pageSize && complete < size) {
      
      currentIndex = (currentIndex + 1) % iterators.size();
      
      ResultIterator itr = iterators.get(currentIndex);

      if (!itr.hasNext()) {
        complete++;
        continue;
      }
      
      resultSet = Sets.union(resultSet, itr.next());
       
      
    }

    // now check if we need to split our results if they went over the page size
    if (resultSet.size() > pageSize) {
      Set<UUID> returnSet = new LinkedHashSet<UUID>(pageSize);

      Iterator<UUID> itr = resultSet.iterator();

      for (int i = 0; i < pageSize && itr.hasNext(); i++) {
        returnSet.add(itr.next());
      }

      remainderResults = new LinkedHashSet<UUID>(pageSize);
      
      while(itr.hasNext()){
        remainderResults.add(itr.next());
      }
      
      resultSet = returnSet;
    }

    return resultSet;

  }
  

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.persistence.query.ir.result.ResultIterator#finalizeCursor(
   * org.usergrid.persistence.cassandra.CursorCache)
   */
  @Override
  public void finalizeCursor(CursorCache cache, UUID lastLoaded) {
    //we can create a cursor for every iterator in our union
    for (ResultIterator current : iterators) {
      current.finalizeCursor(cache, lastLoaded);
    }
  }

}
