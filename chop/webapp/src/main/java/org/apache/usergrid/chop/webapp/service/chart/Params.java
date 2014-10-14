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

public class Params {

    public enum Metric {
        AVG, MIN, MAX, ACTUAL
    }

    public enum FailureType {
        ALL, FAILED, SUCCESS
    }

    private String moduleId;
    private String testName;
    private String commitId;
    private int runNumber;
    private Metric metric;
    private int percentile = 100;
    public FailureType failureType;

    public Params(String moduleId) {
        this.moduleId = moduleId;
    }

    public Params(String moduleId, String testName, String commitId, int runNumber, Metric metric, int percentile, FailureType failureType) {
        this.moduleId = moduleId;
        this.testName = testName;
        this.commitId = commitId;
        this.runNumber = runNumber;
        this.metric = metric;
        this.percentile = percentile;
        this.failureType = failureType;
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getTestName() {
        return testName;
    }

    public String getCommitId() {
        return commitId;
    }

    public int getRunNumber() {
        return runNumber;
    }

    public Metric getMetric() {
        return metric;
    }

    public int getPercentile() {
        return percentile;
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public Params setModuleId(String moduleId) {
        this.moduleId = moduleId;
        return this;
    }

    public Params setCommitId(String commitId) {
        this.commitId = commitId;
        return this;
    }

    public Params setRunNumber(int runNumber) {
        this.runNumber = runNumber;
        return this;
    }

    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("moduleId", moduleId)
                .append("testName", testName)
                .append("commitId", commitId)
                .append("runNumber", runNumber)
                .append("metricType", metric)
                .append("percentile", percentile)
                .append("failureType", failureType)
                .toString();
    }
}
