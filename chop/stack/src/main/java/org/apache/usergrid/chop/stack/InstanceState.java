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
package org.apache.usergrid.chop.stack;


/**
 * The states of an instance.
 */
public enum InstanceState {

    Pending("pending"),
    Running("running"),
    ShuttingDown("shutting-down"),
    Terminated("terminated"),
    Stopping("stopping"),
    Stopped("stopped");

    private String value;

    private InstanceState( String value ) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    /**
     * Use this in place of valueOf.
     *
     * @param value
     *            real value
     * @return InstanceState corresponding to the value
     */
    public static InstanceState fromValue( String value ) {
        if ( value == null || "".equals( value ) ) {
            throw new IllegalArgumentException( "Value cannot be null or empty!" );

        } else if ( "pending".equals( value ) ) {
            return InstanceState.Pending;
        } else if ( "running".equals( value ) ) {
            return InstanceState.Running;
        } else if ( "shutting-down".equals( value ) ) {
            return InstanceState.ShuttingDown;
        } else if ( "terminated".equals( value ) ) {
            return InstanceState.Terminated;
        } else if ( "stopping".equals( value ) ) {
            return InstanceState.Stopping;
        } else if ( "stopped".equals( value ) ) {
            return InstanceState.Stopped;
        } else {
            throw new IllegalArgumentException( "Cannot create enum from " + value + " value!" );
        }
    }



}
