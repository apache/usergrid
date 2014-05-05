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
package org.apache.usergrid.chop.webapp.service.chart;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.usergrid.chop.webapp.service.chart.value.Value;
import org.apache.usergrid.chop.webapp.service.util.JsonUtil;
import org.json.JSONObject;

public class Point {

    private double x;
    private double y;
    private long failures;
    private long ignores;
    private JSONObject properties;

    public Point(int x, Value value) {
        this.x = x;
        this.y = value.getValue();
        this.failures = value.getFailures();
        this.ignores = value.getIgnores();
        this.properties = value.toJson();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public long getFailures() {
        return failures;
    }

    public long getIgnores() {
        return ignores;
    }

    public JSONObject getProperties() {
        return properties;
    }

    public JSONObject toJson() {

        JSONObject json = new JSONObject();

        JsonUtil.put(json, "failures", failures);
        JsonUtil.put(json, "ignores", ignores);
        JsonUtil.put(json, "properties", properties);

        return json;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("x", x)
                .append("y", y)
                .toString();
    }
}
