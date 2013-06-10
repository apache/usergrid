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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.StringSerializer;

import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.cassandra.CursorCache;
import org.usergrid.persistence.cassandra.GeoIndexManager;
import org.usergrid.persistence.cassandra.GeoIndexManager.EntityLocationRef;
import org.usergrid.persistence.query.ir.QuerySlice;

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

  /**
   * 
   */
  private static final String DELIM = "+";

  private static final StringSerializer STR_SER = StringSerializer.get();

  private final GeoIndexSearcher searcher;
  private final int resultSize;
  private final QuerySlice slice;
  private final LinkedHashMap<UUID, Integer> idOrder;
  private Set<UUID> toReturn;

  // set when parsing cursor. If the cursor has gone to the end, this will be
  // true, we should return no results
  private boolean done = false;

  private List<Double> distances;
  private int nextResolution = GeoIndexManager.MAX_RESOLUTION;
  private UUID startId;

  /**
   * 
   */
  public GeoIterator(GeoIndexSearcher searcher, int resultSize, QuerySlice slice) {
    this.searcher = searcher;
    this.resultSize = resultSize;
    this.slice = slice;
    this.idOrder = new LinkedHashMap<UUID, Integer>(resultSize);
    parseCursor();

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
    return !done || toReturn != null;
  }

  private void advance() {
    // already loaded, do nothing
    if (done || toReturn != null) {
      return;
    }

    double nextDistance = 0;

    if (distances != null && distances.size() > 0) {
      nextDistance = distances.get(distances.size() - 1);
    }

    SearchResults<EntityLocationRef> results;

    try {
      results = searcher.doSearch(nextDistance, nextResolution, resultSize);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    idOrder.clear();

    /**
     * we have to do some screwy logic for paging since it's distance based.  A point could be the same
     * distance, and then get cut off by a page.  We need to discard results the have the same distance as
     * the last one and an id <= to the last uuid 
     */
    while (!done && idOrder.size() < resultSize) {
      List<EntityLocationRef> locations = results.getResults();

      for (int i = 0; i < locations.size(); i++) {
        idOrder.put(locations.get(i).getUuid(), i);
      }

      if (idOrder.size() > 0) {
        toReturn = idOrder.keySet();
      }

      distances = results.getDistances();
      nextResolution = results.getLastResolution();

      if (idOrder.size() < resultSize) {
        done = true;
      }

    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#next()
   */
  @Override
  public Set<UUID> next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    Set<UUID> temp = toReturn;

    toReturn = null;

    return temp;

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
    distances = null;
    idOrder.clear();
    nextResolution = GeoIndexManager.MAX_RESOLUTION;
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

    Integer index = idOrder.get(lastValue);

    if (index == null) {
      return;
    }

    int sliceHash = slice.hashCode();

    // get our next distance
    double distance = distances.get(index);

    // now create a string value for this
    StringBuilder builder = new StringBuilder();

    builder.append(lastValue).append(DELIM);
    builder.append(distance).append(DELIM);
    builder.append(nextResolution);

    ByteBuffer buff = STR_SER.toByteBuffer(builder.toString());

    cache.setNextCursor(sliceHash, buff);

  }

  private void parseCursor() {
    if (!slice.hasCursor()) {
      return;
    }

    String string = STR_SER.fromByteBuffer(slice.getCursor().duplicate());

    // was set to the end, set the no op flag
    if (string.length() == 0) {
      done = true;
      return;
    }

    String[] parts = string.split("\\" + DELIM);

    if (parts.length != 3) {
      throw new RuntimeException("Geo cursors must contain 3 parts.  Incorrect cursor, please execute the query again");
    }

    // discard the UUID for now

    // parse the double
    startId = UUID.fromString(parts[0]);
    distances = Collections.singletonList(Double.parseDouble(parts[1]));
    nextResolution = Integer.parseInt(parts[2]);
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

    abstract SearchResults<EntityLocationRef> doSearch(double minDistance, int resolution, int pageSize)
        throws Exception;
  }

  /**
   * Class for loading collection search data
   * 
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
    public SearchResults<EntityLocationRef> doSearch(double minDistance, int resolution, int pageSize) throws Exception {
      return geoIndexManager.proximitySearchCollection(headEntity, collectionName, propertyName, searchPoint,
          minDistance, distance, resolution, pageSize);

    }

  }

  /**
   * Class for loading connection data
   * 
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
    public SearchResults<EntityLocationRef> doSearch(double minDistance, int resolution, int pageSize) throws Exception {
      return geoIndexManager.proximitySearchConnections(connectionId, propertyName, searchPoint, minDistance, distance,
          resolution, pageSize);

    }

  }
}
