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
package org.apache.usergrid.chop.api;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;


/** ... */
public class BaseResult implements Result {
    private String endpoint;
    private String message;
    private boolean status;
    private State state;
    private Project project;


    public BaseResult( String endpoint, boolean status, String message, State state ) {
        this.endpoint = endpoint;
        this.status = status;
        this.message = message;
        this.state = state;
    }


    public BaseResult( String endpoint, boolean status, String message, State state, Project project ) {
        this.endpoint = endpoint;
        this.status = status;
        this.message = message;
        this.state = state;
        this.project = project;
    }


    @SuppressWarnings("UnusedDeclaration")
    public BaseResult() {
        status = true;
    }


    @SuppressWarnings("UnusedDeclaration")
    public void setEndpoint( String endpoint ) {
        this.endpoint = endpoint;
    }


    @SuppressWarnings("UnusedDeclaration")
    public void setStatus( boolean status ) {
        this.status = status;
    }


    @SuppressWarnings("UnusedDeclaration")
    public void setMessage( String message ) {
        this.message = message;
    }


    @JsonProperty
    @Override
    public boolean getStatus() {
        return status;
    }


    @Override
    public State getState() {
        return state;
    }


    public void setState( State state ) {
        this.state = state;
    }


    @JsonProperty
    @Override
    public String getMessage() {
        return message;
    }


    @JsonProperty
    @Override
    public String getEndpoint() {
        return endpoint;
    }


    @Override
    @JsonProperty
    public Project getProject() {
        return project;
    }


    public void setProject( final Project project ) {
        this.project = project;
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("endpoint", endpoint)
                .append("status", status)
                .append("message", message)
                .append("state", state)
                .append("project", project)
                .toString();
    }
}
