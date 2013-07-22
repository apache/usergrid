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
package org.usergrid.persistence.geo;

import java.util.ArrayList;
import java.util.List;

import org.usergrid.persistence.geo.comparator.EntityLocationComparableTuple;


/**
 * A simple wrapper class that contains the results. It also contains the last
 * tile we searched (to resume searching at this tile) and the last match within
 * that tile (to discard all values less than this on the next pass)
 * 
 * @author tnine
 * 
 */
public class SearchResults<T> {

  public static final double NO_RESULTS = -1;
  
  private final List<EntityLocationComparableTuple<T>> results;
  private final int lastResolution;
  
  private List<T> entityResults;

  /**
   * @param results
   * @param nextStartTile
   * @param lastMatched
   */
  public SearchResults(List<EntityLocationComparableTuple<T>> results,  int lastResolution) {
    this.results = results;
    this.lastResolution = lastResolution;
  }

  public List<EntityLocationComparableTuple<T>> getResults() {
    return results;
  }

  /**
   * Return the last resolution that was searched
   * @return the lastResolution 
   */
  public int getLastResolution() {
    return lastResolution;
  }

  public List<T> entityResults(){
    if(entityResults != null){
      return entityResults;
    }
    
    entityResults = new ArrayList<T>(results.size());
    
    for(EntityLocationComparableTuple<T> t: results){
      entityResults.add(t.getEntity());
    }
    
    return entityResults;
  }
  
}
