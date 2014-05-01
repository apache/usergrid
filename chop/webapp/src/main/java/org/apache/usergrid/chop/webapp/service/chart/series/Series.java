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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.usergrid.chop.webapp.service.chart.Point;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class Series {

    private String name;
    private List<Point> points;

    public Series(String name, List<Point> points) {
        this.name = name;
        this.points = points;
    }

    public String getName() {
        return name;
    }

    public Series(List<Point> points) {
        this("", points);
    }

    public List<Point> getPoints() {
        return points;
    }

    public List<Double[]> getDoubleArray() {

        ArrayList<Double[]> list = new ArrayList<Double[]>();

        for (Point p : points) {
            list.add(new Double[]{p.getX(), p.getY()});
        }

        return list;
    }

    public JSONArray getJsonArray() {

        JSONArray arr = new JSONArray();

        for (Point p : getPoints()) {
            arr.put(p.toJson());
        }

        return arr;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("name", name)
                .append("points", points)
                .toString();
    }
}
