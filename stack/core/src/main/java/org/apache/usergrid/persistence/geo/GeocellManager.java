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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.usergrid.persistence.cassandra.GeoIndexManager;
import org.apache.usergrid.persistence.geo.model.BoundingBox;
import org.apache.usergrid.persistence.geo.model.CostFunction;
import org.apache.usergrid.persistence.geo.model.DefaultCostFunction;
import org.apache.usergrid.persistence.geo.model.Point;


/**
 #
 # Copyright 2010 Alexandre Gellibert
 #
 # Licensed under the Apache License, Version 2.0 (the "License");
 # you may not use this file except in compliance with the License.
 # You may obtain a copy of the License at
 #
 #     http://www.apache.org/licenses/LICENSE-2.0
 #
 # Unless required by applicable law or agreed to in writing, software
 # distributed under the License is distributed on an "AS IS" BASIS,
 # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 # See the License for the specific language governing permissions and
 # limitations under the License.
 */


/**
 * Ported java version of python geocell: http://code.google.com/p/geomodel/source/browse/trunk/geo/geocell.py
 * <p/>
 * Defines the notion of 'geocells' and exposes methods to operate on them.
 * <p/>
 * A geocell is a hexadecimal string that defines a two dimensional rectangular region inside the [-90,90] x [-180,180]
 * latitude/longitude space. A geocell's 'resolution' is its length. For most practical purposes, at high resolutions,
 * geocells can be treated as single points.
 * <p/>
 * Much like geohashes (see http://en.wikipedia.org/wiki/Geohash), geocells are hierarchical, in that any prefix of a
 * geocell is considered its ancestor, with geocell[:-1] being geocell's immediate parent cell.
 * <p/>
 * To calculate the rectangle of a given geocell string, first divide the [-90,90] x [-180,180] latitude/longitude space
 * evenly into a 4x4 grid like so:
 * <p/>
 * +---+---+---+---+ (90, 180) | a | b | e | f | +---+---+---+---+ | 8 | 9 | c | d | +---+---+---+---+ | 2 | 3 | 6 | 7 |
 * +---+---+---+---+ | 0 | 1 | 4 | 5 | (-90,-180) +---+---+---+---+
 * <p/>
 * NOTE: The point (0, 0) is at the intersection of grid cells 3, 6, 9 and c. And, for example, cell 7 should be the
 * sub-rectangle from (-45, 90) to (0, 180).
 * <p/>
 * Calculate the sub-rectangle for the first character of the geocell string and re-divide this sub-rectangle into
 * another 4x4 grid. For example, if the geocell string is '78a', we will re-divide the sub-rectangle like so:
 * <p/>
 * .                   . .                   . . . +----+----+----+----+ (0, 180) | 7a | 7b | 7e | 7f |
 * +----+----+----+----+ | 78 | 79 | 7c | 7d | +----+----+----+----+ | 72 | 73 | 76 | 77 | +----+----+----+----+ | 70 |
 * 71 | 74 | 75 | . . (-45,90) +----+----+----+----+ .                   . .                   .
 * <p/>
 * Continue to re-divide into sub-rectangles and 4x4 grids until the entire geocell string has been exhausted. The final
 * sub-rectangle is the rectangular region for the geocell.
 *
 * @author api.roman.public@gmail.com (Roman Nurik)
 * @author (java portage) Alexandre Gellibert
 */

public class GeocellManager {

    // The maximum *practical* geocell resolution.
    public static final int MAX_GEOCELL_RESOLUTION = GeoIndexManager.MAX_RESOLUTION;

    // The maximum number of geocells to consider for a bounding box search.
    private static final int MAX_FEASIBLE_BBOX_SEARCH_CELLS = 300;

    // Function used if no custom function is used in bestBboxSearchCells method
    private static final CostFunction DEFAULT_COST_FUNCTION = new DefaultCostFunction();

    //    private static final Logger logger = GeocellLogger.get();


    /**
     * Returns the list of geocells (all resolutions) that are containing the point
     *
     * @return Returns the list of geocells (all resolutions) that are containing the point
     */
    public static List<String> generateGeoCell( Point point ) {
        List<String> geocells = new ArrayList<String>();
        String geocellMax = GeocellUtils.compute( point, GeocellManager.MAX_GEOCELL_RESOLUTION );
        for ( int i = 1; i < GeocellManager.MAX_GEOCELL_RESOLUTION; i++ ) {
            geocells.add( GeocellUtils.compute( point, i ) );
        }
        geocells.add( geocellMax );
        return geocells;
    }


    /**
     * Returns an efficient set of geocells to search in a bounding box query.
     * <p/>
     * This method is guaranteed to return a set of geocells having the same resolution (except in the case of
     * antimeridian search i.e when east < west).
     *
     * @param bbox: A geotypes.Box indicating the bounding box being searched.
     * @param costFunction: A function that accepts two arguments: numCells: the number of cells to search resolution:
     * the resolution of each cell to search and returns the 'cost' of querying against this number of cells at the
     * given resolution.)
     *
     * @return A list of geocell strings that contain the given box.
     */
    public static List<String> bestBboxSearchCells( BoundingBox bbox, CostFunction costFunction ) {
        if ( bbox.getEast() < bbox.getWest() ) {
            BoundingBox bboxAntimeridian1 =
                    new BoundingBox( bbox.getNorth(), bbox.getEast(), bbox.getSouth(), GeocellUtils.MIN_LONGITUDE );
            BoundingBox bboxAntimeridian2 =
                    new BoundingBox( bbox.getNorth(), GeocellUtils.MAX_LONGITUDE, bbox.getSouth(), bbox.getWest() );
            List<String> antimeridianList = bestBboxSearchCells( bboxAntimeridian1, costFunction );
            antimeridianList.addAll( bestBboxSearchCells( bboxAntimeridian2, costFunction ) );
            return antimeridianList;
        }

        String cellNE = GeocellUtils.compute( bbox.getNorthEast(), GeocellManager.MAX_GEOCELL_RESOLUTION );
        String cellSW = GeocellUtils.compute( bbox.getSouthWest(), GeocellManager.MAX_GEOCELL_RESOLUTION );

        // The current lowest BBOX-search cost found; start with practical infinity.
        double minCost = Double.MAX_VALUE;

        // The set of cells having the lowest calculated BBOX-search cost.
        List<String> minCostCellSet = new ArrayList<String>();

        // First find the common prefix, if there is one.. this will be the base
        // resolution.. i.e. we don't have to look at any higher resolution cells.
        int minResolution = 0;
        int maxResoltuion = Math.min( cellNE.length(), cellSW.length() );
        while ( minResolution < maxResoltuion && cellNE.substring( 0, minResolution + 1 )
                                                       .startsWith( cellSW.substring( 0, minResolution + 1 ) ) ) {
            minResolution++;
        }

        // Iteravely calculate all possible sets of cells that wholely contain
        // the requested bounding box.
        for ( int curResolution = minResolution; curResolution < GeocellManager.MAX_GEOCELL_RESOLUTION + 1;
              curResolution++ ) {
            String curNE = cellNE.substring( 0, curResolution );
            String curSW = cellSW.substring( 0, curResolution );

            int numCells = GeocellUtils.interpolationCount( curNE, curSW );
            if ( numCells > MAX_FEASIBLE_BBOX_SEARCH_CELLS ) {
                continue;
            }

            List<String> cellSet = GeocellUtils.interpolate( curNE, curSW );
            Collections.sort( cellSet );

            double cost;
            if ( costFunction == null ) {
                cost = DEFAULT_COST_FUNCTION.defaultCostFunction( cellSet.size(), curResolution );
            }
            else {
                cost = costFunction.defaultCostFunction( cellSet.size(), curResolution );
            }

            if ( cost <= minCost ) {
                minCost = cost;
                minCostCellSet = cellSet;
            }
            else {
                if ( minCostCellSet.size() == 0 ) {
                    minCostCellSet = cellSet;
                }
                // Once the cost starts rising, we won't be able to do better, so abort.
                break;
            }
        }
        //        logger.log(Level.INFO, "Calculate cells "+StringUtils.join(minCostCellSet, ",
        // ")+" in box ("+bbox.getSouth()+","+bbox.getWest()+") ("+bbox.getNorth()+","+bbox.getEast()+")");
        return minCostCellSet;
    }
}
