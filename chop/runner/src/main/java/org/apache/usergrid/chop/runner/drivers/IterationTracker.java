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


import org.apache.usergrid.chop.api.IterationChop;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Gather all data in one place for an IterationChop.
 */
@JsonPropertyOrder( { "testClass", "iterationChop", "tracker" } )
public class IterationTracker extends Tracker {
    private final IterationChop iterationChop;


    public IterationTracker( final Class<?> testClass ) {
        super( testClass );
        this.iterationChop = testClass.getAnnotation( IterationChop.class );
    }


    @Override
    public long getDelay() {
        return iterationChop.delay();
    }


    @Override
    public int getThreads() {
        return iterationChop.threads();
    }


    @Override
    public boolean getSaturate() {
        return iterationChop.saturate();
    }


    @Override
    public int getPercentCompleted() {
        double percent = ( double ) getActualIterations() /
                ( ( double ) iterationChop.iterations() * iterationChop.threads() );
        return ( int ) Math.floor( percent * 100 );
    }


    @JsonProperty
    public IterationChop getIterationChop() {
        return iterationChop;
    }
}
