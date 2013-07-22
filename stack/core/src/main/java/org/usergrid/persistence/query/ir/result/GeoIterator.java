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

import static org.usergrid.persistence.Schema.DICTIONARY_GEOCELL;
import static org.usergrid.persistence.Schema.INDEX_CONNECTIONS;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.usergrid.utils.CompositeUtils.setEqualityFlag;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.beans.AbstractComposite.Component;
import me.prettyprint.hector.api.beans.AbstractComposite.ComponentEquality;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.IndexBucketLocator.IndexType;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.cassandra.CursorCache;
import org.usergrid.persistence.cassandra.EntityManagerImpl;
import org.usergrid.persistence.cassandra.GeoIndexManager;
import org.usergrid.persistence.cassandra.GeoIndexManager.EntityLocationRef;
import org.usergrid.persistence.geo.GeocellUtils;
import org.usergrid.persistence.geo.model.Point;
import org.usergrid.persistence.geo.model.Tuple;
import org.usergrid.persistence.query.ir.QuerySlice;
import org.usergrid.utils.UUIDUtils;

/**
 * Simple wrapper around list results until the geo library is updated so
 * support iteration and set returns
 * 
 * @author tnine
 * 
 */
public class GeoIterator implements ResultIterator {

  private static final Logger logger = LoggerFactory.getLogger(GeoIterator.class);
  /**
   * 
   */
  private static final String DELIM = "+";
  private static final String TILE_DELIM = "TILE";

  private static final StringSerializer STR_SER = StringSerializer.get();

  // The maximum *practical* geocell resolution.
  public static final int MAX_GEOCELL_RESOLUTION = GeoIndexManager.MAX_RESOLUTION;

  // The maximum number of geocells to consider for a bounding box search.
  private static final int MAX_FEASIBLE_BBOX_SEARCH_CELLS = 300;

  private final GeoIndexSearcher searcher;
  private final int resultSize;
  private final QuerySlice slice;
  private final LinkedHashMap<UUID, EntityLocationRef> idOrder;
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
  public GeoIterator(GeoIndexSearcher searcher, int resultSize, QuerySlice slice) {
    this.searcher = searcher;
    this.resultSize = resultSize;
    this.slice = slice;
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
    // distances = new ArrayList<Double>(resultSize);

    // int count = 0;

    // SearchResults last = new SearchResults(entityLocations,
    // lastSearchedGeoCells);

    SearchResults results;

    try {
      results = searcher.proximitySearch(last, lastCellsSearched, resultSize);
    } catch (Exception e) {
      throw new RuntimeException("Unable to search geo locations", e);
    }
    // results = searcher.doSearch(last.getDistance(), last.getUuid(),
    // nextResolution, queriedSize);

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

  public static abstract class GeoIndexSearcher {

    protected final EntityManagerImpl em;
    protected final Point searchPoint;
    protected final String propertyName;
    protected final float maxDistance;
    protected final float minDistance;

    /**
     * @param entityManager
     * @param pageSize
     * @param headEntity
     * @param searchPoint
     * @param propertyName
     * @param distance
     */
    public GeoIndexSearcher(EntityManagerImpl entityManager, Point searchPoint, String propertyName, float minDistance,
        float maxDistance) {
      this.em = entityManager;
      this.searchPoint = searchPoint;
      this.propertyName = propertyName;
      this.minDistance = minDistance;
      this.maxDistance = maxDistance;
    }

    /**
     * Perform a search from the center. The corresponding entities returned
     * must be >= minDistance(inclusive) and < maxDistance (exclusive)
     * 
     * @param center
     * @param maxResults
     *          The maximum number of results to include
     * @param minDistance
     *          The minimum distance (inclusive)
     * @param maxDistance
     *          The maximum distance (exclusive)
     * @param entityClass
     *          The entity class
     * @param baseQuery
     *          The base query
     * @param queryEngine
     *          The query engine to use
     * @param maxGeocellResolution
     *          The max resolution to use when searching
     * @return
     * @throws Exception
     */
    public final SearchResults proximitySearch(final EntityLocationRef startResult, final List<String> geoCells,
        final int maxResults) throws Exception {

      List<EntityLocationRef> entityLocations = new ArrayList<EntityLocationRef>(maxResults);

      // set our startresult if it's not set
      EntityLocationRef minMatch = startResult;

      if (minMatch == null) {
        minMatch = new EntityLocationRef((String) null, UUIDUtils.MIN_TIME_UUID, searchPoint.getLat(),
            searchPoint.getLon());
        minMatch.calcDistance(searchPoint);
      }

      List<String> curGeocells = new ArrayList<String>();
      String curContainingGeocell = null;

      // we have some cells used from last time, re-use them
      if (geoCells != null && geoCells.size() > 0) {
        curGeocells.addAll(geoCells);
        curContainingGeocell = geoCells.get(0);
      }
      // start at the bottom
      else {

        /*
         * The currently-being-searched geocells. NOTES: Start with max
         * possible. Must always be of the same resolution. Must always form a
         * rectangular region. One of these must be equal to the
         * cur_containing_geocell.
         */
        curContainingGeocell = GeocellUtils.compute(searchPoint, MAX_GEOCELL_RESOLUTION);
        curGeocells.add(curContainingGeocell);
      }

      // Set of already searched cells
      Set<String> searchedCells = new HashSet<String>();

      List<String> curGeocellsUnique = null;

      double closestPossibleNextResultDist = 0;

      /*
       * Assumes both a and b are lists of (entity, dist) tuples, *sorted by
       * dist*. NOTE: This is an in-place merge, and there are guaranteed no
       * duplicates in the resulting list.
       */

      int noDirection[] = { 0, 0 };
      List<Tuple<int[], Double>> sortedEdgesDistances = Arrays.asList(new Tuple<int[], Double>(noDirection, 0d));
      boolean done = false;
      UUID lastReturned = null;

      while (!curGeocells.isEmpty() && entityLocations.size() < maxResults) {
        closestPossibleNextResultDist = sortedEdgesDistances.get(0).getSecond();
        if (maxDistance > 0 && closestPossibleNextResultDist > maxDistance) {
          break;
        }

        Set<String> curTempUnique = new HashSet<String>(curGeocells);
        curTempUnique.removeAll(searchedCells);
        curGeocellsUnique = new ArrayList<String>(curTempUnique);

        Set<EntityLocationRef> queryResults = null;

        // we need to keep searching everything in our tiles until we don't get
        // any more results, then we'll have the closest points and can move on
        // do the next tiles
        do {
          queryResults = doSearch(curGeocellsUnique, lastReturned, 1000);

          if (logger.isDebugEnabled()) {
            logger.debug("fetch complete for: {}", StringUtils.join(curGeocellsUnique, ", "));
          }

          searchedCells.addAll(curGeocells);

          // Begin storing distance from the search result entity to the
          // search center along with the search result itself, in a tuple.

          // Merge new_results into results
          for (EntityLocationRef entityLocation : queryResults) {

            lastReturned = entityLocation.getUuid();

            double distance = entityLocation.getDistance();

            // discard, it's too close or too far
            if (distance < minDistance || (maxDistance != 0 && distance > maxDistance)) {
              continue;
            }

            int index = Collections.binarySearch(entityLocations, entityLocation);

            // already in the index
            if (index > -1) {
              // check if it's the same point, if it is, skip it. Otherwise
              // continue
              // below
              // set the insert index

              if (entityLocations.get(index).equals(entityLocation)) {
                continue;
              }

              index++;

            } else {

              // set the insert index
              index = (index + 1) * -1;
            }

            // no point in adding it
            if (index >= maxResults) {
              continue;
            }

            // results.add(index, entity);
            // distances.add(index, distance);
            entityLocations.add(index, entityLocation);

            /**
             * Discard an additional entries as we iterate to avoid holding them
             * all in ram
             */
            while (entityLocations.size() > maxResults) {
              entityLocations.remove(entityLocations.size() - 1);
            }

          }

        } while (queryResults != null && queryResults.size() > 0);
        

        /**
         * We've searched everything and have a full set, we want to return the "current" tiles to search
         * next time for the cursor, since cass could contain more results
         */
        if (done ||  entityLocations.size() == maxResults) {
          break;
        }

        sortedEdgesDistances = GeocellUtils.distanceSortedEdges(curGeocells, searchPoint);

        if (queryResults.size() == 0 || curGeocells.size() == 4) {
          /*
           * Either no results (in which case we optimize by not looking at
           * adjacents, go straight to the parent) or we've searched 4 adjacent
           * geocells, in which case we should now search the parents of those
           * geocells.
           */
          curContainingGeocell = curContainingGeocell.substring(0, Math.max(curContainingGeocell.length() - 1, 0));
          if (curContainingGeocell.length() == 0) {
            // final check - top level tiles
            curGeocells.clear();
            String[] items = "0123456789abcdef".split("(?!^)");
            for (String item : items)
              curGeocells.add(item);
            done = true;
          }
          else {
            List<String> oldCurGeocells = new ArrayList<String>(curGeocells);
            curGeocells.clear();
            for (String cell : oldCurGeocells) {
              if (cell.length() > 0) {
                String newCell = cell.substring(0, cell.length() - 1);
                if (!curGeocells.contains(newCell)) {
                  curGeocells.add(newCell);
                }
              }
            }
          }

        } else if (curGeocells.size() == 1) {
          // Get adjacent in one direction.
          // TODO(romannurik): Watch for +/- 90 degree latitude edge case
          // geocells.
          for (int i = 0; i < sortedEdgesDistances.size(); i++) {

            int nearestEdge[] = sortedEdgesDistances.get(i).getFirst();
            String edge = GeocellUtils.adjacent(curGeocells.get(0), nearestEdge);

            // we're at the edge of the world, search in a different direction
            if (edge == null) {
              continue;
            }

            curGeocells.add(edge);
            break;
          }

        } else if (curGeocells.size() == 2) {
          // Get adjacents in perpendicular direction.
          int nearestEdge[] = GeocellUtils.distanceSortedEdges(Arrays.asList(curContainingGeocell), searchPoint).get(0)
              .getFirst();
          int[] perpendicularNearestEdge = { 0, 0 };
          if (nearestEdge[0] == 0) {
            // Was vertical, perpendicular is horizontal.
            for (Tuple<int[], Double> edgeDistance : sortedEdgesDistances) {
              if (edgeDistance.getFirst()[0] != 0) {
                perpendicularNearestEdge = edgeDistance.getFirst();
                break;
              }
            }
          } else {
            // Was horizontal, perpendicular is vertical.
            for (Tuple<int[], Double> edgeDistance : sortedEdgesDistances) {
              if (edgeDistance.getFirst()[0] == 0) {
                perpendicularNearestEdge = edgeDistance.getFirst();
                break;
              }
            }
          }
          List<String> tempCells = new ArrayList<String>();
          for (String cell : curGeocells) {
            tempCells.add(GeocellUtils.adjacent(cell, perpendicularNearestEdge));
          }
          curGeocells.addAll(tempCells);
        }

        // We don't have enough items yet, keep searching.
        if (entityLocations.size() < maxResults) {
          logger.debug("{} results found but want {} results, continuing search.", entityLocations.size(), maxResults);
          continue;
        }

        logger.debug("{} results found.", entityLocations.size());
        
       
      }

      // now we have our final sets, construct the results

      return new SearchResults(entityLocations, curGeocells);
    }

    protected Set<EntityLocationRef> query(Object key, List<String> curGeocellsUnique, UUID startId, int count)
        throws Exception {

      Set<EntityLocationRef> locations = new TreeSet<EntityLocationRef>();

      List<Object> keys = new ArrayList<Object>();

      IndexBucketLocator locator = em.getIndexBucketLocator();
      UUID appId = em.getApplicationId();

      for (String geoCell : curGeocellsUnique) {

        // add buckets for each geoCell

        for (String indexBucket : locator.getBuckets(appId, IndexType.GEO, geoCell)) {
          keys.add(key(key, DICTIONARY_GEOCELL, geoCell, indexBucket));
        }
      }

      CassandraService cass = em.getCass();
      
      
      DynamicComposite composite = null;
      
      if(startId != null){
       composite =  new DynamicComposite(startId);
       setEqualityFlag(composite, ComponentEquality.GREATER_THAN_EQUAL);
      }
      
      

      Map<ByteBuffer, List<HColumn<ByteBuffer, ByteBuffer>>> rows = cass.multiGetColumns(
          cass.getApplicationKeyspace(em.getApplicationId()), ENTITY_INDEX, keys, composite, null, count, false);

      for (List<HColumn<ByteBuffer, ByteBuffer>> columns : rows.values()) {
        addLocationIndexEntries(columns, locations, searchPoint);
      }

      return locations;

    }

    private static void addLocationIndexEntries(List<HColumn<ByteBuffer, ByteBuffer>> columns,
        Set<EntityLocationRef> locations, Point search) {

      if (columns == null) {
        return;
      }

      EntityLocationRef location = null;

      for (HColumn<ByteBuffer, ByteBuffer> column : columns) {
        DynamicComposite composite = DynamicComposite.fromByteBuffer(column.getName());

        UUID uuid = composite.get(0, UUIDSerializer.get());
        String type = composite.get(1, StringSerializer.get());
        UUID timestampUuid = composite.get(2, UUIDSerializer.get());
        composite = DynamicComposite.fromByteBuffer(column.getValue());
        Double latitude = composite.get(0, DoubleSerializer.get());
        Double longitude = composite.get(1, DoubleSerializer.get());

        location = new EntityLocationRef(type, uuid, timestampUuid, latitude, longitude);
        location.calcDistance(search);

        locations.add(location);

      }
    }

    protected abstract Set<EntityLocationRef> doSearch(List<String> geoCells, UUID startId, int pageSize)
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

    public CollectionGeoSearch(EntityManagerImpl entityManager, EntityRef headEntity, String collectionName,
        Point searchPoint, String propertyName, float minDistance, float maxDistance) {
      super(entityManager, searchPoint, propertyName, minDistance, maxDistance);
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
    public Set<EntityLocationRef> doSearch(List<String> geoCells, UUID startId, int pageSize)
        throws Exception {

      return query(key(headEntity.getUuid(), collectionName, propertyName), geoCells, startId, pageSize);
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

    public ConnectionGeoSearch(EntityManagerImpl entityManager, UUID connectionId, Point searchPoint,
        String propertyName, float minDistance, float maxDistance) {
      super(entityManager, searchPoint, propertyName, minDistance, maxDistance);

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
    public Set<EntityLocationRef> doSearch(List<String> geoCells, UUID startId, int pageSize)
        throws Exception {

      return query(key(connectionId, INDEX_CONNECTIONS, propertyName), geoCells, startId, pageSize);
    }

  }

  private static class SearchResults {

    private final List<EntityLocationRef> entityLocations;
    private final List<String> lastSearchedGeoCells;

    /**
     * @param entityLocations
     * @param curGeocells
     */
    public SearchResults(List<EntityLocationRef> entityLocations, List<String> lastSearchedGeoCells) {
      this.entityLocations = entityLocations;
      this.lastSearchedGeoCells = lastSearchedGeoCells;
    }

  }

}
