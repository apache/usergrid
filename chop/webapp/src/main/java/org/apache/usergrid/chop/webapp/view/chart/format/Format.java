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
package org.apache.usergrid.chop.webapp.view.chart.format;

import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.chop.webapp.service.chart.series.Series;
import org.apache.usergrid.chop.webapp.service.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Set;

public class Format {

    public static String formatCategories(Set<String> categories) {

        String s = "";

        for (String category : categories) {
            if (!s.isEmpty()) {
                s += ", ";
            }

            s += String.format("'%s'", StringUtils.abbreviate(category, 10));
        }

        return String.format("[%s]", s);
    }

    public static String formatData(List<Series> seriesList) {

        JSONArray arr = new JSONArray();

        for (int i = 0; i < seriesList.size(); i++) {
            Series s = seriesList.get(i);

            JSONObject json = new JSONObject();
            JsonUtil.put(json, "data", s.getDoubleArray());
            JsonUtil.put(json, "label", s.getName());

            // Hard-code color indices to prevent them from shifting as the series are turned on/off.
            JsonUtil.put(json, "color", i);

            arr.put(json);
        }

        return arr.toString();
    }

    public static String formatPoints(List<Series> seriesList) {

        JSONArray arr = new JSONArray();

        for (Series s : seriesList) {
            arr.put(s.getJsonArray());
        }

        return arr.toString();
    }

}
