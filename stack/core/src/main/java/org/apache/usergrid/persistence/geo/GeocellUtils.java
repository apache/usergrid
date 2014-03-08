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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.usergrid.persistence.geo.comparator.DoubleTupleComparator;
import org.apache.usergrid.persistence.geo.model.BoundingBox;
import org.apache.usergrid.persistence.geo.model.Point;
import org.apache.usergrid.persistence.geo.model.Tuple;

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
 * Utils class to compute geocells.
 *
 * @author api.roman.public@gmail.com (Roman Nurik)
 * @author (java portage) Alexandre Gellibert
 */
public final class GeocellUtils {

    public static final float MIN_LONGITUDE = -180.0f;
    public static final float MAX_LONGITUDE = 180.0f;
    public static final float MIN_LATITUDE = -90.0f;
    public static final float MAX_LATITUDE = 90.0f;
    // Geocell algorithm constants.
    public static final int GEOCELL_GRID_SIZE = 4;
    private static final String GEOCELL_ALPHABET = "0123456789abcdef";

    // Direction enumerations.
    private static final int[] NORTHWEST = new int[] { -1, 1 };
    private static final int[] NORTH = new int[] { 0, 1 };
    private static final int[] NORTHEAST = new int[] { 1, 1 };
    private static final int[] EAST = new int[] { 1, 0 };
    private static final int[] SOUTHEAST = new int[] { 1, -1 };
    private static final int[] SOUTH = new int[] { 0, -1 };
    private static final int[] SOUTHWEST = new int[] { -1, -1 };
    private static final int[] WEST = new int[] { -1, 0 };

    private static final int RADIUS = 6378135;


    private GeocellUtils() {
        // no instantiation allowed
    }


    /**
     * Determines whether the given cells are collinear along a dimension.
     * <p/>
     * Returns True if the given cells are in the same row (columnTest=False) or in the same column (columnTest=True).
     *
     * @param cell1 : The first geocell string.
     * @param cell2 : The second geocell string.
     * @param columnTest : A boolean, where False invokes a row collinearity test and 1 invokes a column collinearity
     * test.
     *
     * @return A bool indicating whether or not the given cells are collinear in the given dimension.
     */
    public static boolean collinear( String cell1, String cell2, boolean columnTest ) {

        for ( int i = 0; i < Math.min( cell1.length(), cell2.length() ); i++ ) {
            int l1[] = subdivXY( cell1.charAt( i ) );
            int x1 = l1[0];
            int y1 = l1[1];
            int l2[] = subdivXY( cell2.charAt( i ) );
            int x2 = l2[0];
            int y2 = l2[1];

            // Check row collinearity (assure y's are always the same).
            if ( !columnTest && y1 != y2 ) {
                return false;
            }

            // Check column collinearity (assure x's are always the same).
            if ( columnTest && x1 != x2 ) {
                return false;
            }
        }
        return true;
    }


    /**
     * Calculates the grid of cells formed between the two given cells.
     * <p/>
     * Generates the set of cells in the grid created by interpolating from the given Northeast geocell to the given
     * Southwest geocell.
     * <p/>
     * Assumes the Northeast geocell is actually Northeast of Southwest geocell.
     *
     * @param cellNE : The Northeast geocell string.
     * @param cellSW : The Southwest geocell string.
     *
     * @return A list of geocell strings in the interpolation.
     */
    public static List<String> interpolate( String cellNE, String cellSW ) {
        // 2D array, will later be flattened.
        LinkedList<LinkedList<String>> cellSet = new LinkedList<LinkedList<String>>();
        LinkedList<String> cellFirst = new LinkedList<String>();
        cellFirst.add( cellSW );
        cellSet.add( cellFirst );

        // First get adjacent geocells across until Southeast--collinearity with
        // Northeast in vertical direction (0) means we're at Southeast.
        while ( !collinear( cellFirst.getLast(), cellNE, true ) ) {
            String cellTmp = adjacent( cellFirst.getLast(), EAST );
            if ( cellTmp == null ) {
                break;
            }
            cellFirst.add( cellTmp );
        }

        // Then get adjacent geocells upwards.
        while ( !cellSet.getLast().getLast().equalsIgnoreCase( cellNE ) ) {

            LinkedList<String> cellTmpRow = new LinkedList<String>();
            for ( String g : cellSet.getLast() ) {
                cellTmpRow.add( adjacent( g, NORTH ) );
            }
            if ( cellTmpRow.getFirst() == null ) {
                break;
            }
            cellSet.add( cellTmpRow );
        }

        // Flatten cellSet, since it's currently a 2D array.
        List<String> result = new ArrayList<String>();
        for ( LinkedList<String> list : cellSet ) {
            result.addAll( list );
        }
        return result;
    }


    /**
     * Computes the number of cells in the grid formed between two given cells.
     * <p/>
     * Computes the number of cells in the grid created by interpolating from the given Northeast geocell to the given
     * Southwest geocell. Assumes the Northeast geocell is actually Northeast of Southwest geocell.
     *
     * @param cellNE : The Northeast geocell string.
     * @param cellSW : The Southwest geocell string.
     *
     * @return An int, indicating the number of geocells in the interpolation.
     */
    public static int interpolationCount( String cellNE, String cellSW ) {

        BoundingBox bboxNE = computeBox( cellNE );
        BoundingBox bboxSW = computeBox( cellSW );

        double cellLatSpan = bboxSW.getNorth() - bboxSW.getSouth();
        double cellLonSpan = bboxSW.getEast() - bboxSW.getWest();

        double numCols = ( ( bboxNE.getEast() - bboxSW.getWest() ) / cellLonSpan );
        double numRows = ( ( bboxNE.getNorth() - bboxSW.getSouth() ) / cellLatSpan );

        double totalCols = numCols * numRows * 1.0;
        if ( totalCols > Integer.MAX_VALUE ) {
            return Integer.MAX_VALUE;
        }
        return ( int ) totalCols;
    }


    /**
     * Calculates all of the given geocell's adjacent geocells.
     *
     * @param cell : The geocell string for which to calculate adjacent/neighboring cells.
     *
     * @return A list of 8 geocell strings and/or None values indicating adjacent cells.
     */

    public static List<String> allAdjacents( String cell ) {
        List<String> result = new ArrayList<String>();
        for ( int[] d : Arrays.asList( NORTHWEST, NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST ) ) {
            result.add( adjacent( cell, d ) );
        }
        return result;
    }


    /**
     * Calculates the geocell adjacent to the given cell in the given direction.
     *
     * @param cell : The geocell string whose neighbor is being calculated.
     * @param dir : An (x, y) tuple indicating direction, where x and y can be -1, 0, or 1. -1 corresponds to West for x
     * and South for y, and 1 corresponds to East for x and North for y. Available helper constants are NORTH, EAST,
     * SOUTH, WEST, NORTHEAST, NORTHWEST, SOUTHEAST, and SOUTHWEST.
     *
     * @return The geocell adjacent to the given cell in the given direction, or None if there is no such cell.
     */
    public static String adjacent( String cell, int[] dir ) {
        if ( cell == null ) {
            return null;
        }
        int dx = dir[0];
        int dy = dir[1];
        char[] cellAdjArr = cell.toCharArray(); // Split the geocell string
        // characters into a list.
        int i = cellAdjArr.length - 1;

        while ( i >= 0 && ( dx != 0 || dy != 0 ) ) {
            int l[] = subdivXY( cellAdjArr[i] );
            int x = l[0];
            int y = l[1];

            // Horizontal adjacency.
            if ( dx == -1 ) { // Asking for left.
                if ( x == 0 ) { // At left of parent cell.
                    x = GEOCELL_GRID_SIZE - 1; // Becomes right edge of adjacent parent.
                }
                else {
                    x--; // Adjacent, same parent.
                    dx = 0; // Done with x.
                }
            }
            else if ( dx == 1 ) { // Asking for right.
                if ( x == GEOCELL_GRID_SIZE - 1 ) { // At right of parent cell.
                    x = 0; // Becomes left edge of adjacent parent.
                }
                else {
                    x++; // Adjacent, same parent.
                    dx = 0; // Done with x.
                }
            }

            // Vertical adjacency.
            if ( dy == 1 ) { // Asking for above.
                if ( y == GEOCELL_GRID_SIZE - 1 ) { // At top of parent cell.
                    y = 0; // Becomes bottom edge of adjacent parent.
                }
                else {
                    y++; // Adjacent, same parent.
                    dy = 0; // Done with y.
                }
            }
            else if ( dy == -1 ) { // Asking for below.
                if ( y == 0 ) { // At bottom of parent cell.
                    y = GEOCELL_GRID_SIZE - 1; // Becomes top edge of adjacent parent.
                }
                else {
                    y--; // Adjacent, same parent.
                    dy = 0; // Done with y.
                }
            }

            int l2[] = { x, y };
            cellAdjArr[i] = subdivChar( l2 );
            i--;
        }
        // If we're not done with y then it's trying to wrap vertically,
        // which is a failure.
        if ( dy != 0 ) {
            return null;
        }

        // At this point, horizontal wrapping is done inherently.
        return new String( cellAdjArr );
    }


    /**
     * Returns whether or not the given cell contains the given point.
     *
     * @return Returns whether or not the given cell contains the given point.
     */
    public static boolean containsPoint( String cell, Point point ) {
        return compute( point, cell.length() ).equalsIgnoreCase( cell );
    }


    /**
     * Returns the shortest distance between a point and a geocell bounding box.
     * <p/>
     * If the point is inside the cell, the shortest distance is always to a 'edge' of the cell rectangle. If the point
     * is outside the cell, the shortest distance will be to either a 'edge' or 'corner' of the cell rectangle.
     *
     * @return The shortest distance from the point to the geocell's rectangle, in meters.
     */
    public static double pointDistance( String cell, Point point ) {
        BoundingBox bbox = computeBox( cell );

        boolean betweenWE = bbox.getWest() <= point.getLon() && point.getLon() <= bbox.getEast();
        boolean betweenNS = bbox.getSouth() <= point.getLat() && point.getLat() <= bbox.getNorth();

        if ( betweenWE ) {
            if ( betweenNS ) {
                // Inside the geocell.
                return Math.min( Math.min( distance( point, new Point( bbox.getSouth(), point.getLon() ) ),
                        distance( point, new Point( bbox.getNorth(), point.getLon() ) ) ),
                        Math.min( distance( point, new Point( point.getLat(), bbox.getEast() ) ),
                                distance( point, new Point( point.getLat(), bbox.getWest() ) ) ) );
            }
            else {
                return Math.min( distance( point, new Point( bbox.getSouth(), point.getLon() ) ),
                        distance( point, new Point( bbox.getNorth(), point.getLon() ) ) );
            }
        }
        else {
            if ( betweenNS ) {
                return Math.min( distance( point, new Point( point.getLat(), bbox.getEast() ) ),
                        distance( point, new Point( point.getLat(), bbox.getWest() ) ) );
            }
            else {
                // TODO(romannurik): optimize
                return Math.min( Math.min( distance( point, new Point( bbox.getSouth(), bbox.getEast() ) ),
                        distance( point, new Point( bbox.getNorth(), bbox.getEast() ) ) ),
                        Math.min( distance( point, new Point( bbox.getSouth(), bbox.getWest() ) ),
                                distance( point, new Point( bbox.getNorth(), bbox.getWest() ) ) ) );
            }
        }
    }


    /**
     * Computes the geocell containing the given point to the given resolution.
     * <p/>
     * This is a simple 16-tree lookup to an arbitrary depth (resolution).
     *
     * @param point : The geotypes.Point to compute the cell for.
     * @param resolution : An int indicating the resolution of the cell to compute.
     *
     * @return The geocell string containing the given point, of length resolution.
     */
    public static String compute( Point point, int resolution ) {
        float north = MAX_LATITUDE;
        float south = MIN_LATITUDE;
        float east = MAX_LONGITUDE;
        float west = MIN_LONGITUDE;

        StringBuilder cell = new StringBuilder();
        while ( cell.length() < resolution ) {
            float subcellLonSpan = ( east - west ) / GEOCELL_GRID_SIZE;
            float subcellLatSpan = ( north - south ) / GEOCELL_GRID_SIZE;

            int x = Math.min( ( int ) ( GEOCELL_GRID_SIZE * ( point.getLon() - west ) / ( east - west ) ),
                    GEOCELL_GRID_SIZE - 1 );
            int y = Math.min( ( int ) ( GEOCELL_GRID_SIZE * ( point.getLat() - south ) / ( north - south ) ),
                    GEOCELL_GRID_SIZE - 1 );

            int l[] = { x, y };
            cell.append( subdivChar( l ) );

            south += subcellLatSpan * y;
            north = south + subcellLatSpan;

            west += subcellLonSpan * x;
            east = west + subcellLonSpan;
        }
        return cell.toString();
    }


    /**
     * Computes the rectangular boundaries (bounding box) of the given geocell.
     *
     * @param cell_ : The geocell string whose boundaries are to be computed.
     *
     * @return A geotypes.Box corresponding to the rectangular boundaries of the geocell.
     */
    public static BoundingBox computeBox( String cell_ ) {
        if ( cell_ == null ) {
            return null;
        }

        BoundingBox bbox = new BoundingBox( 90.0, 180.0, -90.0, -180.0 );
        StringBuilder cell = new StringBuilder( cell_ );
        while ( cell.length() > 0 ) {
            double subcellLonSpan = ( bbox.getEast() - bbox.getWest() ) / GEOCELL_GRID_SIZE;
            double subcellLatSpan = ( bbox.getNorth() - bbox.getSouth() ) / GEOCELL_GRID_SIZE;

            int l[] = subdivXY( cell.charAt( 0 ) );
            int x = l[0];
            int y = l[1];

            bbox = new BoundingBox( bbox.getSouth() + subcellLatSpan * ( y + 1 ),
                    bbox.getWest() + subcellLonSpan * ( x + 1 ), bbox.getSouth() + subcellLatSpan * y,
                    bbox.getWest() + subcellLonSpan * x );

            cell.deleteCharAt( 0 );
        }

        return bbox;
    }


    /**
     * Returns whether or not the given geocell string defines a valid geocell.
     *
     * @return Returns whether or not the given geocell string defines a valid geocell.
     */
    public static boolean isValid( String cell ) {
        if ( cell == null || cell.trim().length() == 0 ) {
            return false;
        }
        for ( char c : cell.toCharArray() ) {
            if ( GEOCELL_ALPHABET.indexOf( c ) < 0 ) {
                return false;
            }
        }
        return true;
    }


    /**
     * Returns the (x, y) of the geocell character in the 4x4 alphabet grid.
     *
     * @return Returns the (x, y) of the geocell character in the 4x4 alphabet grid.
     */
    public static int[] subdivXY( char char_ ) {
        // NOTE: This only works for grid size 4.
        int charI = GEOCELL_ALPHABET.indexOf( char_ );
        return new int[] {
                ( charI & 4 ) >> 1 | ( charI & 1 ) >> 0, ( charI & 8 ) >> 2 | ( charI & 2 ) >> 1
        };
    }


    /**
     * Returns the geocell character in the 4x4 alphabet grid at pos. (x, y).
     *
     * @return Returns the geocell character in the 4x4 alphabet grid at pos. (x, y).
     */
    public static char subdivChar( int[] pos ) {
        // NOTE: This only works for grid size 4.
        return GEOCELL_ALPHABET.charAt( ( pos[1] & 2 ) << 2 |
                ( pos[0] & 2 ) << 1 |
                ( pos[1] & 1 ) << 1 |
                ( pos[0] & 1 ) << 0 );
    }


    /**
     * Calculates the great circle distance between two points (law of cosines).
     *
     * @param p1 : indicating the first point.
     * @param p2 : indicating the second point.
     *
     * @return The 2D great-circle distance between the two given points, in meters.
     */
    public static double distance( Point p1, Point p2 ) {
        double p1lat = Math.toRadians( p1.getLat() );
        double p1lon = Math.toRadians( p1.getLon() );
        double p2lat = Math.toRadians( p2.getLat() );
        double p2lon = Math.toRadians( p2.getLon() );
        return RADIUS * Math.acos( makeDoubleInRange(
                Math.sin( p1lat ) * Math.sin( p2lat ) + Math.cos( p1lat ) * Math.cos( p2lat ) * Math
                        .cos( p2lon - p1lon ) ) );
    }


    /**
     * This function is used to fix issue 10: GeocellUtils.distance(...) uses Math.acos(arg) method. In some cases arg >
     * 1 (i.e 1.0000000002), so acos cannot be calculated and the method returns NaN.
     *
     * @return a double between -1 and 1
     */
    public static double makeDoubleInRange( double d ) {
        double result = d;
        if ( d > 1 ) {
            result = 1;
        }
        else if ( d < -1 ) {
            result = -1;
        }
        return result;
    }


    /**
     * Returns the edges of the rectangular region containing all of the given geocells, sorted by distance from the
     * given point, along with the actual distances from the point to these edges.
     *
     * @param cells : The cells (should be adjacent) defining the rectangular region whose edge distances are
     * requested.
     * @param point : The point that should determine the edge sort order.
     *
     * @return A list of (direction, distance) tuples, where direction is the edge and distance is the distance from the
     *         point to that edge. A direction value of (0,-1), for example, corresponds to the South edge of the
     *         rectangular region containing all of the given geocells.
     *         <p/>
     *         TODO(romannurik): Assert that lat,lon are actually inside the geocell.
     */
    public static List<Tuple<int[], Double>> distanceSortedEdges( List<String> cells, Point point ) {
        List<BoundingBox> boxes = new ArrayList<BoundingBox>();
        for ( String cell : cells ) {
            boxes.add( computeBox( cell ) );
        }
        double maxNorth = Double.NEGATIVE_INFINITY;
        double maxEast = Double.NEGATIVE_INFINITY;
        double maxSouth = Double.POSITIVE_INFINITY;
        double maxWest = Double.POSITIVE_INFINITY;
        for ( BoundingBox box : boxes ) {
            maxNorth = Math.max( maxNorth, box.getNorth() );
            maxEast = Math.max( maxEast, box.getEast() );
            maxSouth = Math.min( maxSouth, box.getSouth() );
            maxWest = Math.min( maxWest, box.getWest() );
        }
        List<Tuple<int[], Double>> result = new ArrayList<Tuple<int[], Double>>();
        result.add( new Tuple<int[], Double>( SOUTH, distance( new Point( maxSouth, point.getLon() ), point ) ) );
        result.add( new Tuple<int[], Double>( NORTH, distance( new Point( maxNorth, point.getLon() ), point ) ) );
        result.add( new Tuple<int[], Double>( WEST, distance( new Point( point.getLat(), maxWest ), point ) ) );
        result.add( new Tuple<int[], Double>( EAST, distance( new Point( point.getLat(), maxEast ), point ) ) );
        Collections.sort( result, new DoubleTupleComparator() );
        return result;
    }
}
