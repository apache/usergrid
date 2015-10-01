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

import org.apache.usergrid.chop.api.Run;
import org.apache.usergrid.chop.webapp.service.util.JsonUtil;

public class AvgValue extends Value {

    private int count;

    private void doCalc(double d) {
        value += d;
        count++;
    }

    @Override
    protected void calcValue(Run run) {
        doCalc(run.getAvgTime());
    }

    @Override
    public void merge(Value other) {
        if (other == null) {
            return;
        }

        doCalc(other.getValue());
        inc(other.getFailures(), other.getIgnores());
        copyProperties(other);
    }

    private void copyProperties(Value other) {
        JsonUtil.copy(other.properties, properties, "chopType");
        JsonUtil.copy(other.properties, properties, "commitId");
    }

    @Override
    public double getValue() {
        return value / count;
    }
}
