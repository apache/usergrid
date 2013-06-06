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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.usergrid.persistence.cassandra.CursorCache;
import org.usergrid.persistence.cassandra.GeoIndexManager.EntityLocationRef;

/**
 * Simple wrapper around list results until the geo library is updated so support iteration and set returns
 * @author tnine
 *
 */
public class GeoIterator implements ResultIterator {

  private List<EntityLocationRef> locations;
  private final int pageSize;
  private int lastIndex;
  
  /**
   * 
   */
  public GeoIterator(List<EntityLocationRef> locations, int pageSize) {
    this.locations = locations;
    this.pageSize = pageSize;
    lastIndex = 0;
    
  }

  /* (non-Javadoc)
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<Set<UUID>> iterator() {
    return this;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  @Override
  public boolean hasNext() {
    return lastIndex < locations.size();
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  @Override
  public Set<UUID> next() {
    
    
    
    int endIndex = Math.min(locations.size(), lastIndex+pageSize);
    
    Set<UUID> returnSet = new LinkedHashSet<UUID>(endIndex-lastIndex);
    
    for(EntityLocationRef loc: locations.subList(lastIndex, endIndex)){
      returnSet.add(loc.getUuid());
    }
    
    lastIndex = endIndex;
    
    
    return returnSet;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException("You cannot reove elements from this iterator");
  }

  /* (non-Javadoc)
   * @see org.usergrid.persistence.query.ir.result.ResultIterator#reset()
   */
  @Override
  public void reset() {
    lastIndex = 0;
  }

  /* (non-Javadoc)
   * @see org.usergrid.persistence.query.ir.result.ResultIterator#finalizeCursor(org.usergrid.persistence.cassandra.CursorCache, java.util.UUID)
   */
  @Override
  public void finalizeCursor(CursorCache cache, UUID lastValue) {
    //TODO TN fix this
  }

}
