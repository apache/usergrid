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
package org.apache.usergrid.chop.webapp.dao.model;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.usergrid.chop.api.RunResult;

import java.util.UUID;

public class BasicRunResult implements RunResult {

    private String id;
    private String runId;
    private int runCount;
    private int runTime;
    private int ignoreCount;
    private int failureCount;
    private String failures;


    public BasicRunResult( String id, String runId, int runCount, int runTime, int ignoreCount, int failureCount,
                          String failures ) {
        this.id = id;
        this.runId = runId;
        this.runCount = runCount;
        this.runTime = runTime;
        this.ignoreCount = ignoreCount;
        this.failureCount = failureCount;
        this.failures = failures;
    }


    public BasicRunResult( String runId, int runCount, int runTime, int ignoreCount, int failureCount ) {
        this( createId( runId ), runId, runCount, runTime, ignoreCount, failureCount, "" );
    }


    private static String createId( String runId ) {
        return "" + Math.abs(
                new HashCodeBuilder()
                .append( runId )
                .append( UUID.randomUUID() )
                .toHashCode()
                            );
    }


    public String toString() {
        return new ToStringBuilder( this, ToStringStyle.SHORT_PREFIX_STYLE )
                .append( "id", id )
                .append( "runId", runId )
                .append( "runCount", runCount )
                .append( "runTime", runTime )
                .append( "ignoreCount", ignoreCount )
                .append( "failureCount", failureCount )
                .append( "failures", failures )
                .toString();
    }


    @Override
    public String getId() {
        return id;
    }


    @Override
    public String getRunId() {
        return runId;
    }


    @Override
    public int getRunCount() {
        return runCount;
    }


    @Override
    public int getRunTime() {
        return runTime;
    }


    @Override
    public int getIgnoreCount() {
        return ignoreCount;
    }


    @Override
    public int getFailureCount() {
        return failureCount;
    }


    @Override
    public String getFailures() {
        return failures;
    }


    public void setFailures( String failures ) {
        this.failures = failures;
    }
}
