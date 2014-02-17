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
package org.apache.usergrid.persistence.geo.model;


import org.apache.commons.lang.Validate;


/** @author Alexandre Gellibert */
public class Point {

    private double lat;
    private double lon;


    public Point() {

    }


    public Point( double lat, double lon ) {
        Validate.isTrue( !( lat > 90.0 || lat < -90.0 ), "Latitude must be in [-90, 90]  but was ", lat );
        Validate.isTrue( !( lon > 180.0 || lon < -180.0 ), "Longitude must be in [-180, 180] but was ", lon );
        this.lat = lat;
        this.lon = lon;
    }


    public double getLat() {
        return lat;
    }


    public void setLat( double lat ) {
        this.lat = lat;
    }


    public double getLon() {
        return lon;
    }


    public void setLon( double lon ) {
        this.lon = lon;
    }
}
