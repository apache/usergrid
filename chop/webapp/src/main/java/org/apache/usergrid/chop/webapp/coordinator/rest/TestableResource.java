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
package org.apache.usergrid.chop.webapp.coordinator.rest;


import org.safehaus.jettyjam.utils.TestMode;


/**
 * A base class for all signal resources.
 */
public abstract class TestableResource {
    public static final String TEST_PARAM = TestMode.TEST_MODE_PROPERTY;

    public final static String SUCCESSFUL_TEST_MESSAGE = "Test parameters are OK";

    private final String endpoint;


    protected TestableResource(String endpoint) {
        this.endpoint = endpoint;
    }


    public String getTestMessage() {
        return endpoint + " resource called in test mode.";
    }


    public boolean inTestMode(String testMode) {
        return testMode != null &&
                (testMode.equals(TestMode.INTEG.toString()) || testMode.equals(TestMode.UNIT.toString()));
    }
}
