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
import org.apache.usergrid.chop.api.RunResult;
import org.apache.usergrid.chop.webapp.service.chart.value.Value;

import java.util.*;

public class GroupByRunner {

    // <runner, List<Value>>
    public static Map<String, Collection<Value>> group(Map<Run, List<RunResult>> runResults) {

        Map<String, Collection<Value>> runnerValues = new HashMap<String, Collection<Value>>();

        for (Run run : runResults.keySet()) {
            runnerValues.put(run.getRunner(), toValueList(runResults.get(run)));
        }

        return runnerValues;
    }

    private static List<Value> toValueList(List<RunResult> runResults) {

        ArrayList<Value> values = new ArrayList<Value>();

        for (RunResult runResult : runResults) {
            values.add(new Value(runResult));
        }

        return values;
    }
}
