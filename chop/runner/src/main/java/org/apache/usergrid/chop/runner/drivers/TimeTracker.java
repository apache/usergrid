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
package org.apache.usergrid.chop.runner.drivers;


import org.apache.usergrid.chop.api.TimeChop;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Gather all data in one place for a TimeChop.
 */
@JsonPropertyOrder( { "testClass", "iterationChop", "tracker" } )
public class TimeTracker extends Tracker {
    private final TimeChop timeChop;


    public TimeTracker( Class<?> testClass ) {
        super( testClass );
        this.timeChop = testClass.getAnnotation( TimeChop.class );
    }


    @Override
    public long getDelay() {
        return timeChop.delay();
    }


    @Override
    public int getThreads() {
        return timeChop.threads();
    }


    @Override
    public boolean getSaturate() {
        return timeChop.saturate();
    }


    @Override
    public int getPercentCompleted() {
        double percent = ( double ) ( System.currentTimeMillis() - getStartTime() ) / ( double ) timeChop.time();
        return ( int ) Math.floor( Math.min( 100, percent * 100 ) );
    }


    @JsonProperty
    public TimeChop getTimeChop() {
        return timeChop;
    }
}
