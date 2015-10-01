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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.usergrid.chop.api.Run;
import org.apache.usergrid.chop.api.RunResult;
import org.apache.usergrid.chop.webapp.service.util.JsonUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Value {

    protected static Logger LOG = LoggerFactory.getLogger(Value.class);

    protected double value;
    protected long failures;
    protected long ignores;

    protected JSONObject properties = new JSONObject();

    public Value() {
    }

    public Value(RunResult runResult) {
        this.value = runResult.getRunTime();
        this.failures = runResult.getFailureCount();
        this.ignores = runResult.getIgnoreCount();
        JsonUtil.put(properties, "id", runResult.getId());
    }

    public void merge(Value value) {
    }

    protected void calcValue(Run run) {
    }

    public void merge(Run run) {
        calcValue(run);
        inc(run.getFailures(), run.getIgnores());
        mergeProperties(run);
    }

    protected void inc(long failures, long ignores) {
        this.failures += failures;
        this.ignores += ignores;
    }

    private void mergeProperties(Run run) {
        JsonUtil.put(properties, "chopType", run.getChopType());
        JsonUtil.put(properties, "commitId", run.getCommitId());
        JsonUtil.put(properties, "runNumber", run.getRunNumber());

        JsonUtil.inc(properties, "runners", 1);
        JsonUtil.inc(properties, "totalTestsRun", run.getTotalTestsRun());
        JsonUtil.inc(properties, "iterations", run.getThreads() * run.getIterations());
    }

    public double getValue() {
        return value;
    }

    public long getFailures() {
        return failures;
    }

    public long getIgnores() {
        return ignores;
    }

    public JSONObject toJson() {

        JSONObject json = new JSONObject();

        JsonUtil.put(json, "value", getValue());
        JsonUtil.put(json, "failures", failures);
        JsonUtil.put(json, "ignores", ignores);
        JsonUtil.copy(properties, json);

        return json;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("value", getValue())
                .toString();
    }
}
