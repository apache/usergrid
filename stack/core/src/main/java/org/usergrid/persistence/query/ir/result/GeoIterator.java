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
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.cassandra.CursorCache;
import org.usergrid.persistence.cassandra.GeoIndexManager;
import org.usergrid.persistence.cassandra.GeoIndexManager.EntityLocationRef;

import com.beoui.geocell.SearchResults;
import com.beoui.geocell.model.Point;

/**
 * Simple wrapper around list results until the geo library is updated so
 * support iteration and set returns
 * 
 * @author tnine
 * 
 */
public class GeoIterator implements ResultIterator {

  private List<EntityLocationRef> locations;
  private String nextStartTile;
  private UUID nextUUID;

  private final GeoIndexSearcher searcher;
  private final int resultSize;
  
  /**
   * 
   */
  public GeoIterator(GeoIndexSearcher searcher, int resultSize) {
    this.searcher = searcher;
    this.resultSize = resultSize;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<Set<UUID>> iterator() {
    return this;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#hasNext()
   */
  @Override
  public boolean hasNext() {
    advance();
    return locations != null && locations.size() > 0;
  }

  private void advance() {
    if (locations != null) {
      return;
    }

    SearchResults<EntityLocationRef> results;

    try {
      results = searcher.doSearch(nextUUID, nextStartTile, resultSize+1);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    locations = results.getResults();
    nextStartTile = results.getLastSearchedTile();
    nextUUID = null;
    
    //we have a +1 for the next page, set it then drop it
    if(locations.size() == resultSize+1){
      nextUUID = locations.get(resultSize).getUuid();
      locations.remove(resultSize);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#next()
   */
  @Override
  public Set<UUID> next() {
    advance();

    if (locations == null) {
      throw new NoSuchElementException();
    }

    Set<UUID> results = new LinkedHashSet<UUID>(locations.size());

    for (EntityLocationRef location : locations) {
      results.add(location.getUuid());
    }

    return results;

  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#remove()
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException("You cannot reove elements from this iterator");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.persistence.query.ir.result.ResultIterator#reset()
   */
  @Override
  public void reset() {
    locations = null;
    nextStartTile = null;
    nextUUID = null;
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
    // TODO TN fix this
  }

  public static abstract class GeoIndexSearcher {

    protected GeoIndexManager geoIndexManager;
    protected final Point searchPoint;
    protected final String propertyName;
    protected final float distance;

    /**
     * @param geoIndexManager
     * @param pageSize
     * @param headEntity
     * @param searchPoint
     * @param propertyName
     * @param distance
     */
    public GeoIndexSearcher(GeoIndexManager geoIndexManager, Point searchPoint, String propertyName, float distance) {
      this.geoIndexManager = geoIndexManager;
      this.searchPoint = searchPoint;
      this.propertyName = propertyName;
      this.distance = distance;
    }

    abstract SearchResults<EntityLocationRef> doSearch(UUID nextId, String startTile, int pageSize) throws Exception;
  }

  /**
   * Class for loading collection search data
   * @author tnine
   *
   */
  public static class CollectionGeoSearch extends GeoIndexSearcher {

    private final String collectionName;
    private final EntityRef headEntity;

    public CollectionGeoSearch(GeoIndexManager geoIndexManager, EntityRef headEntity, String collectionName,
        Point searchPoint, String propertyName, float distance) {
      super(geoIndexManager, searchPoint, propertyName, distance);
      this.collectionName = collectionName;
      this.headEntity = headEntity;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.persistence.query.ir.result.GeoIterator.GeoIndexSearcher
     * #doSearch()
     */
    @Override
    public SearchResults<EntityLocationRef> doSearch(UUID nextId, String startTile, int pageSize) throws Exception {
      return geoIndexManager.proximitySearchCollection(headEntity, collectionName, propertyName, searchPoint, distance,
          nextId, startTile, pageSize, false);

    }

  }

  /**
   * Class for loading connection data
   * @author tnine
   *
   */
  public static class ConnectionGeoSearch extends GeoIndexSearcher {

    private final UUID connectionId;

    public ConnectionGeoSearch(GeoIndexManager geoIndexManager, UUID connectionId, Point searchPoint,
        String propertyName, float distance) {
      super(geoIndexManager, searchPoint, propertyName, distance);

      this.connectionId = connectionId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.persistence.query.ir.result.GeoIterator.GeoIndexSearcher
     * #doSearch()
     */
    @Override
    public SearchResults<EntityLocationRef> doSearch(UUID nextId, String startTile, int pageSize) throws Exception {
      return geoIndexManager.proximitySearchConnections(connectionId, propertyName, searchPoint, distance, nextId,
          startTile, pageSize, false);

    }

  }
}
