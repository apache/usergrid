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
package org.apache.usergrid.chop.webapp.service.chart.filter;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.usergrid.chop.webapp.service.chart.value.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class PercentileFilter {

    public static Map<String, Collection<Value>> filter(Map<String, Collection<Value>> map, int percent) {

        double percentile = new DescriptiveStatistics(toArray(map)).getPercentile(percent);
        Map<String, Collection<Value>> resultMap = new LinkedHashMap<String, Collection<Value>>();

        for (String key : map.keySet()) {
            resultMap.put(key, doFilter(map.get(key), percentile));
        }

        return resultMap;
    }

    public static Collection<Value> filter(Collection<Value> values, double percent) {
        double percentile = new DescriptiveStatistics(toArray(values)).getPercentile(percent);
        return doFilter(values, percentile);
    }

    private static Collection<Value> doFilter(Collection<Value> values, double percentile) {

        ArrayList<Value> resultValues = new ArrayList<Value>();

        for (Value value : values) {
            if (value.getValue() <= percentile) {
                resultValues.add(value);
            }
        }

        return resultValues;
    }

    private static double[] toArray(Map<String, Collection<Value>> map) {

        double arr[] = {};

        for (Collection<Value> valueList : map.values()) {
            arr = ArrayUtils.addAll(arr, toArray(valueList));
        }

        return arr;
    }

    private static double[] toArray(Collection<Value> values) {

        double arr[] = new double[values.size()];
        int i = 0;

        for (Value value : values) {
            arr[i] = value.getValue();
            i++;
        }

        return arr;
    }

}
