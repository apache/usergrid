/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.geo;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.IndexBucketLocator.IndexType;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.GeoIndexManager;
import org.apache.usergrid.persistence.cassandra.index.IndexMultiBucketSetLoader;
import org.apache.usergrid.persistence.geo.model.Point;
import org.apache.usergrid.persistence.geo.model.Tuple;

import org.apache.commons.lang.StringUtils;

import me.prettyprint.hector.api.beans.AbstractComposite.ComponentEquality;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;

import static org.apache.usergrid.persistence.Schema.DICTIONARY_GEOCELL;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.apache.usergrid.utils.CompositeUtils.setEqualityFlag;
import static org.apache.usergrid.persistence.cassandra.Serializers.*;

public abstract class GeoIndexSearcher {

    private static final Logger logger = LoggerFactory.getLogger( GeoIndexSearcher.class );

    private static final EntityLocationRefDistanceComparator COMP = new EntityLocationRefDistanceComparator();

    // The maximum *practical* geocell resolution.
    private static final int MAX_GEOCELL_RESOLUTION = GeoIndexManager.MAX_RESOLUTION;

    /** Max number of records to read+parse from cass per tile */
    private static final int MAX_FETCH_SIZE = 1000;

    protected final EntityManager em;
    protected final IndexBucketLocator locator;
    protected final CassandraService cass;

    public GeoIndexSearcher( EntityManager entityManager, IndexBucketLocator locator, CassandraService cass ) {
        this.em = entityManager;
        this.locator = locator;
        this.cass = cass;
    }


    /**
     * Perform a search from the center. The corresponding entities returned must be >= minDistance(inclusive) and <
     * maxDistance (exclusive)
     *
     * @param maxResults The maximum number of results to include
     * @param minDistance The minimum distance (inclusive)
     * @param maxDistance The maximum distance (exclusive)
     * @param entityClass The entity class
     * @param baseQuery The base query
     * @param queryEngine The query engine to use
     * @param maxGeocellResolution The max resolution to use when searching
     */
    public final SearchResults proximitySearch( final EntityLocationRef minMatch, final List<String> geoCells,
                                                Point searchPoint, String propertyName, double minDistance,
                                                double maxDistance, final int maxResults ) throws Exception {

        List<EntityLocationRef> entityLocations = new ArrayList<EntityLocationRef>( maxResults );

        List<String> curGeocells = new ArrayList<String>();
        String curContainingGeocell = null;

        // we have some cells used from last time, re-use them
        if ( geoCells != null && geoCells.size() > 0 ) {
            curGeocells.addAll( geoCells );
            curContainingGeocell = geoCells.get( 0 );
        }
        // start at the bottom
        else {

      /*
       * The currently-being-searched geocells. NOTES: Start with max possible.
       * Must always be of the same resolution. Must always form a rectangular
       * region. One of these must be equal to the cur_containing_geocell.
       */
            curContainingGeocell = GeocellUtils.compute( searchPoint, MAX_GEOCELL_RESOLUTION );
            curGeocells.add( curContainingGeocell );
        }

        if ( minMatch != null ) {
            minMatch.calcDistance( searchPoint );
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
        List<Tuple<int[], Double>> sortedEdgesDistances = Arrays.asList( new Tuple<int[], Double>( noDirection, 0d ) );
        boolean done = false;
        UUID lastReturned = null;

        while ( !curGeocells.isEmpty() && entityLocations.size() < maxResults ) {
            closestPossibleNextResultDist = sortedEdgesDistances.get( 0 ).getSecond();
            if ( maxDistance > 0 && closestPossibleNextResultDist > maxDistance ) {
                break;
            }

            Set<String> curTempUnique = new HashSet<String>( curGeocells );
            curTempUnique.removeAll( searchedCells );
            curGeocellsUnique = new ArrayList<String>( curTempUnique );

            Set<HColumn<ByteBuffer, ByteBuffer>> queryResults = null;

            lastReturned = null;

            // we need to keep searching everything in our tiles until we don't get
            // any more results, then we'll have the closest points and can move on
            // do the next tiles
            do {
                queryResults = doSearch( curGeocellsUnique, lastReturned, searchPoint, propertyName, MAX_FETCH_SIZE );

                if ( logger.isDebugEnabled() ) {
                    logger.debug( "fetch complete for: {}", StringUtils.join( curGeocellsUnique, ", " ) );
                }

                searchedCells.addAll( curGeocells );

                // Begin storing distance from the search result entity to the
                // search center along with the search result itself, in a tuple.

                // Merge new_results into results
                for ( HColumn<ByteBuffer, ByteBuffer> column : queryResults ) {

                    DynamicComposite composite = DynamicComposite.fromByteBuffer( column.getName() );

                    UUID uuid = composite.get( 0, ue );

                    lastReturned = uuid;

                    String type = composite.get( 1, se );
                    UUID timestampUuid = composite.get( 2, ue );
                    composite = DynamicComposite.fromByteBuffer( column.getValue() );
                    Double latitude = composite.get( 0, de );
                    Double longitude = composite.get( 1, de );

                    EntityLocationRef entityLocation =
                            new EntityLocationRef( type, uuid, timestampUuid, latitude, longitude );

                    double distance = entityLocation.calcDistance( searchPoint );

                    // discard, it's too close or too far, of closer than the minimum we
                    // should match, skip it
                    if ( distance < minDistance || ( maxDistance != 0 && distance > maxDistance ) || ( minMatch != null
                            && COMP.compare( entityLocation, minMatch ) <= 0 ) ) {
                        continue;
                    }

                    int index = Collections.binarySearch( entityLocations, entityLocation, COMP );

                    // already in the index
                    if ( index > -1 ) {
                        continue;
                    }

                    // set the insert index
                    index = ( index + 1 ) * -1;

                    // no point in adding it
                    if ( index >= maxResults ) {
                        continue;
                    }

                    // results.add(index, entity);
                    // distances.add(index, distance);
                    entityLocations.add( index, entityLocation );

                    /**
                     * Discard an additional entries as we iterate to avoid holding them
                     * all in ram
                     */
                    while ( entityLocations.size() > maxResults ) {
                        entityLocations.remove( entityLocations.size() - 1 );
                    }
                }
            }
            while ( queryResults != null && queryResults.size() == MAX_FETCH_SIZE );

            /**
             * We've searched everything and have a full set, we want to return the
             * "current" tiles to search next time for the cursor, since cass could
             * contain more results
             */
            if ( done || entityLocations.size() == maxResults ) {
                break;
            }

            sortedEdgesDistances = GeocellUtils.distanceSortedEdges( curGeocells, searchPoint );

            if ( queryResults.size() == 0 || curGeocells.size() == 4 ) {
        /*
         * Either no results (in which case we optimize by not looking at
         * adjacents, go straight to the parent) or we've searched 4 adjacent
         * geocells, in which case we should now search the parents of those
         * geocells.
         */
                curContainingGeocell =
                        curContainingGeocell.substring( 0, Math.max( curContainingGeocell.length() - 1, 0 ) );
                if ( curContainingGeocell.length() == 0 ) {
                    // final check - top level tiles
                    curGeocells.clear();
                    String[] items = "0123456789abcdef".split( "(?!^)" );
                    for ( String item : items ) {
                        curGeocells.add( item );
                    }
                    done = true;
                }
                else {
                    List<String> oldCurGeocells = new ArrayList<String>( curGeocells );
                    curGeocells.clear();
                    for ( String cell : oldCurGeocells ) {
                        if ( cell.length() > 0 ) {
                            String newCell = cell.substring( 0, cell.length() - 1 );
                            if ( !curGeocells.contains( newCell ) ) {
                                curGeocells.add( newCell );
                            }
                        }
                    }
                }
            }
            else if ( curGeocells.size() == 1 ) {
                // Get adjacent in one direction.
                // TODO(romannurik): Watch for +/- 90 degree latitude edge case
                // geocells.
                for ( int i = 0; i < sortedEdgesDistances.size(); i++ ) {

                    int nearestEdge[] = sortedEdgesDistances.get( i ).getFirst();
                    String edge = GeocellUtils.adjacent( curGeocells.get( 0 ), nearestEdge );

                    // we're at the edge of the world, search in a different direction
                    if ( edge == null ) {
                        continue;
                    }

                    curGeocells.add( edge );
                    break;
                }
            }
            else if ( curGeocells.size() == 2 ) {
                // Get adjacents in perpendicular direction.
                int nearestEdge[] =
                        GeocellUtils.distanceSortedEdges( Arrays.asList( curContainingGeocell ), searchPoint ).get( 0 )
                                    .getFirst();
                int[] perpendicularNearestEdge = { 0, 0 };
                if ( nearestEdge[0] == 0 ) {
                    // Was vertical, perpendicular is horizontal.
                    for ( Tuple<int[], Double> edgeDistance : sortedEdgesDistances ) {
                        if ( edgeDistance.getFirst()[0] != 0 ) {
                            perpendicularNearestEdge = edgeDistance.getFirst();
                            break;
                        }
                    }
                }
                else {
                    // Was horizontal, perpendicular is vertical.
                    for ( Tuple<int[], Double> edgeDistance : sortedEdgesDistances ) {
                        if ( edgeDistance.getFirst()[0] == 0 ) {
                            perpendicularNearestEdge = edgeDistance.getFirst();
                            break;
                        }
                    }
                }
                List<String> tempCells = new ArrayList<String>();
                for ( String cell : curGeocells ) {
                    tempCells.add( GeocellUtils.adjacent( cell, perpendicularNearestEdge ) );
                }
                curGeocells.addAll( tempCells );
            }

            logger.debug( "{} results found.", entityLocations.size() );
        }

        // now we have our final sets, construct the results

        return new SearchResults( entityLocations, curGeocells );
    }


    protected TreeSet<HColumn<ByteBuffer, ByteBuffer>> query( Object key, List<String> curGeocellsUnique,
                                                              Point searchPoint, UUID startId, int count )
            throws Exception {

        List<Object> keys = new ArrayList<Object>();

        UUID appId = em.getApplicationRef().getUuid();

        for ( String geoCell : curGeocellsUnique ) {

            // add buckets for each geoCell

            for ( String indexBucket : locator.getBuckets( appId, IndexType.GEO, geoCell ) ) {
                keys.add( key( key, DICTIONARY_GEOCELL, geoCell, indexBucket ) );
            }
        }

        DynamicComposite start = null;

        if ( startId != null ) {
            start = new DynamicComposite( startId );
            setEqualityFlag( start, ComponentEquality.GREATER_THAN_EQUAL );
        }

        TreeSet<HColumn<ByteBuffer, ByteBuffer>> columns =
                IndexMultiBucketSetLoader.load( cass, ENTITY_INDEX, appId, keys, start, null, count, false );

        return columns;
    }


    protected abstract TreeSet<HColumn<ByteBuffer, ByteBuffer>> doSearch( List<String> geoCells, UUID startId,
                                                                          Point searchPoint, String propertyName,
                                                                          int pageSize ) throws Exception;


    public static class SearchResults {

        public final List<EntityLocationRef> entityLocations;
        public final List<String> lastSearchedGeoCells;


        /**
         * @param entityLocations
         * @param lastSearchedGeoCells
         */
        public SearchResults( List<EntityLocationRef> entityLocations, List<String> lastSearchedGeoCells ) {
            this.entityLocations = entityLocations;
            this.lastSearchedGeoCells = lastSearchedGeoCells;
        }
    }
}
