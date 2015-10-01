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

import org.apache.usergrid.chop.webapp.service.chart.Params;
import org.apache.usergrid.chop.webapp.service.chart.value.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class FailureFilter {

    public static Map<String, Collection<Value>> filter(Map<String, Collection<Value>> map, Params.FailureType failureType) {

        Map<String, Collection<Value>> resultMap = new LinkedHashMap<String, Collection<Value>>();

        for (String key : map.keySet()) {
            resultMap.put(key, filter(map.get(key), failureType));
        }

        return resultMap;
    }

    public static Collection<Value> filter(Collection<Value> values, Params.FailureType failureType) {

        ArrayList<Value> resultValues = new ArrayList<Value>();

        for (Value value : values) {
            Value newValue = isValid(value, failureType) ? value : null;
            resultValues.add(newValue);
        }

        return resultValues;
    }

    private static boolean isValid(Value value, Params.FailureType failureType) {
        return failureType == Params.FailureType.ALL
                || failureType == Params.FailureType.FAILED && value.getFailures() > 0
                || failureType == Params.FailureType.SUCCESS && value.getFailures() == 0;
    }
}
