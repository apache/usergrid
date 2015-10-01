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
package org.apache.usergrid.chop.webapp.service.chart.group;

import org.apache.usergrid.chop.api.Run;
import org.apache.usergrid.chop.webapp.service.chart.Params.Metric;
import org.apache.usergrid.chop.webapp.service.chart.value.Value;
import org.apache.usergrid.chop.webapp.service.chart.value.ValueFactory;

import java.util.Collection;
import java.util.HashMap;

public class GroupByRunNumber {

    // <runNumber, Value>
    private HashMap<Integer, Value> runNumberValues = new HashMap<Integer, Value>();
    private Metric metric;

    public GroupByRunNumber(Collection<Run> runs, Metric metric) {
        this.metric = metric;
        group(runs);
    }

    private void group(Collection<Run> runs) {
        for (Run run : runs) {
            put(run);
        }
    }

    private void put(Run run) {

        Value value = runNumberValues.get(run.getRunNumber());

        if (value == null) {
            value = ValueFactory.get(metric);
            runNumberValues.put(run.getRunNumber(), value);
        }

        value.merge(run);
    }

    public Collection<Value> get() {
        return runNumberValues.values();
    }

}
