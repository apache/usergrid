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
package org.apache.usergrid.chop.webapp.service.chart.value;

import org.apache.usergrid.chop.webapp.service.chart.Params;

public class ValueFactory {

    public static Value get(Params.Metric metric) {

        Value value;

//        if (metric == Metric.AVG"Avg Time".equals(metricType)) {
//            value = new AvgValue();
//        } else if ("Min Time".equals(metricType)) {
//            value = new MinValue();
//        } else if ("Max Time".equals(metricType)) {
//            value = new MaxValue();
//        } else {
//            value = new ActualValue();
//        }

        switch (metric) {
            case AVG:
                value = new AvgValue();
                break;
            case MIN:
                value = new MinValue();
                break;
            case MAX:
                value = new MaxValue();
                break;
            default:
                value = new ActualValue();
                break;

        }

        return value;
    }
}
