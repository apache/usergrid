/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.webapp.service.chart.series;

import org.apache.usergrid.chop.webapp.service.chart.Point;
import org.apache.usergrid.chop.webapp.service.chart.value.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SeriesBuilder {

    public static List<Point> toPoints(Collection<Value> values, int startX) {

        ArrayList<Point> points = new ArrayList<Point>();
        int x = startX;

        for (Value value : values) {
            if (value != null && !Double.isNaN(value.getValue())) {
                points.add(new Point(x, value));
            }
            x++;
        }

        return points;
    }

    private static List<Point> toPointsStaticX(Collection<Value> values, int x) {

        ArrayList<Point> points = new ArrayList<Point>();

        for (Value value : values) {
            if (value != null) {
                points.add(new Point(x, value));
            }
        }

        return points;
    }

    public static List<Series> toSeriesStaticX(Map<String, Collection<Value>> map) {

        ArrayList<Series> seriesList = new ArrayList<Series>();

        for (String key : map.keySet()) {
            Collection<Value> values = map.get(key);
            seriesList.add(new Series(key, SeriesBuilder.toPoints(values, 1)));
        }

        return seriesList;
    }

    public static List<Series> toSeries(Map<String, Collection<Value>> map) {

        ArrayList<Series> seriesList = new ArrayList<Series>();
        int x = 0;

        for (String key : map.keySet()) {
            Collection<Value> values = map.get(key);
            seriesList.add(new Series(SeriesBuilder.toPointsStaticX(values, x)));
            x++;
        }

        return seriesList;
    }

}
