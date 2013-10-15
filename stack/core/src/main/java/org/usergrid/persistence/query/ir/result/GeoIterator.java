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
import java.util.*;

import me.prettyprint.cassandra.serializers.StringSerializer;

import org.usergrid.persistence.cassandra.CursorCache;
import org.usergrid.persistence.geo.EntityLocationRef;
import org.usergrid.persistence.geo.GeoIndexSearcher;
import org.usergrid.persistence.geo.GeoIndexSearcher.SearchResults;
import org.usergrid.persistence.geo.model.Point;
import org.usergrid.persistence.query.ir.QuerySlice;

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
  private static final String TILE_DELIM = "TILE";

  private static final StringSerializer STR_SER = StringSerializer.get();


  private final GeoIndexSearcher searcher;
  private final int resultSize;
  private final QuerySlice slice;
  private final LinkedHashMap<UUID, EntityLocationRef> idOrder;
  private final Point center;
  private final double distance;
  private final String propertyName;
  
  private Set<UUID> toReturn;

  // set when parsing cursor. If the cursor has gone to the end, this will be
  // true, we should return no results
  private boolean done = false;
  //
  // private List<Double> distances;

  /**
   * Moved and used as part of cursors
   */
  // private int nextResolution = GeoIndexManager.MAX_RESOLUTION;
  // private UUID startId;
  // private double cursorDistance;
  private EntityLocationRef last;
  private List<String> lastCellsSearched;

  
  
  /**
   * 
   */
  public GeoIterator(GeoIndexSearcher searcher, int resultSize, QuerySlice slice, String propertyName, Point center, double distance) {
    this.searcher = searcher;
    this.resultSize = resultSize;
    this.slice = slice;
    this.propertyName = propertyName;
    this.center = center;
    this.distance = distance;
    this.idOrder = new LinkedHashMap<UUID, EntityLocationRef>(resultSize);
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

    idOrder.clear();
  

    SearchResults results;

    try {
      results = searcher.proximitySearch(last, lastCellsSearched, center, propertyName, 0, distance, resultSize);
      

    } catch (Exception e) {
      throw new RuntimeException("Unable to search geo locations", e);
    }

    List<EntityLocationRef> locations = results.entityLocations;

    lastCellsSearched = results.lastSearchedGeoCells;

    for (int i = 0; i < locations.size(); i++) {

      EntityLocationRef location = locations.get(i);
      UUID id = location.getUuid();

      idOrder.put(id, location);
      // distances.add(distance);

      last = location;

      // count++;
    }

    if (locations.size() < resultSize) {
      done = true;
    }

    if (idOrder.size() > 0) {
      toReturn = idOrder.keySet();
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
    idOrder.clear();
    lastCellsSearched = null;
    last = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.persistence.query.ir.result.ResultIterator#finalizeCursor(
   * org.usergrid.persistence.cassandra.CursorCache, java.util.UUID)
   */
  @Override
  public void finalizeCursor(CursorCache cache, UUID uuid) {

    EntityLocationRef location = idOrder.get(uuid);

    if (location == null) {
      return;
    }

    int sliceHash = slice.hashCode();

    // get our next distance
    double lattitude = location.getLatitude();

    double longitude = location.getLongitude();

    // now create a string value for this
    StringBuilder builder = new StringBuilder();

    builder.append(uuid).append(DELIM);
    builder.append(lattitude).append(DELIM);
    builder.append(longitude);

    if (lastCellsSearched != null) {
      builder.append(DELIM);

      for (String geoCell : lastCellsSearched) {
        builder.append(geoCell).append(TILE_DELIM);
      }

      int length = builder.length();

      builder.delete(length - TILE_DELIM.length() - 1, length);
    }

    ByteBuffer buff = STR_SER.toByteBuffer(builder.toString());

    cache.setNextCursor(sliceHash, buff);

  }

  /**
   * Get the last cells searched in the iteraton
   * @return
   */
  public List<String> getLastCellsSearched(){
    return Collections.unmodifiableList(lastCellsSearched);
  }

  private void parseCursor() {
    if (!slice.hasCursor()) {
      return;
    }

    String string = STR_SER.fromByteBuffer(slice.getCursor());

    // was set to the end, set the no op flag
    if (string.length() == 0) {
      done = true;
      return;
    }

    String[] parts = string.split("\\" + DELIM);

    if (parts.length < 3) {
      throw new RuntimeException(
          "Geo cursors must contain 3 or more parts.  Incorrect cursor, please execute the query again");
    }

    UUID startId = UUID.fromString(parts[0]);
    double lattitude = Double.parseDouble(parts[1]);
    double longtitude = Double.parseDouble(parts[2]);

    if (parts.length >= 4) {
      String[] geoCells = parts[3].split(TILE_DELIM);

      lastCellsSearched = Arrays.asList(geoCells);
    }

    last = new EntityLocationRef((String) null, startId, lattitude, longtitude);

  }



  
}
