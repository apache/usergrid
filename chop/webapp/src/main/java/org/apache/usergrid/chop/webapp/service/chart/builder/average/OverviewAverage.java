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
package org.apache.usergrid.chop.webapp.service.chart.builder.average;

import org.apache.usergrid.chop.webapp.service.chart.value.AvgValue;
import org.apache.usergrid.chop.webapp.service.chart.value.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class OverviewAverage {

    public static Collection<Value> calc(Map<String, Collection<Value>> commitRuns) {

        ArrayList<Value> avgValues = new ArrayList<Value>();

        for (String commitId : commitRuns.keySet()) {
            Collection<Value> values = commitRuns.get(commitId);
            avgValues.add(getAvg(values));
        }

        return avgValues;
    }

    private static Value getAvg(Collection<Value> values) {

        Value avg = new AvgValue();

        for (Value value : values) {
            avg.merge(value);
        }

        return avg;
    }

}
