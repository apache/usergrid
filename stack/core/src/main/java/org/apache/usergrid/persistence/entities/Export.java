/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.persistence.entities;


import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import org.apache.usergrid.persistence.TypedEntity;
import org.apache.usergrid.persistence.annotations.EntityProperty;


/**
 * Contains state information for an Entity Job
 */
@XmlRootElement
public class Export extends TypedEntity {
    //canceled , and expired states aren't used in current iteration.
    public static enum State {
        CREATED, FAILED, SCHEDULED, STARTED, FINISHED, CANCELED, EXPIRED
    }


    @EntityProperty
    protected State curState;

    @EntityProperty
    protected Long queued;

    /**
     * Time send started
     */
    @EntityProperty
    protected Long started;

    /**
     * Time processed
     */
    @EntityProperty
    protected Long finished;


    /**
     * Time to expire the exportJob
     */
    @EntityProperty
    protected Long expire;

    /**
     * True if exportJob is canceled
     */
    @EntityProperty
    protected Boolean canceled;

    /**
     * Error message
     */
    @EntityProperty
    protected String errorMessage;


    public Export() {
    }


    public boolean isExpired() {
        return ( expire != null && expire > System.currentTimeMillis() );
    }


    public Long getStarted() {
        return started;
    }


    public void setStarted( final Long started ) {
        this.started = started;
    }


    public Long getFinished() {
        return finished;
    }


    public void setFinished( final Long finished ) {
        this.finished = finished;
    }


    public Long getExpire() {
        return expire;
    }


    public void setExpire( final Long expire ) {
        this.expire = expire;
    }


    public Boolean getCanceled() {
        return canceled;
    }


    //state should moved to a derived state, but it is not there yet.
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @EntityProperty
    public void setState( State setter ) {
        curState = setter;
    }


    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @EntityProperty
    public State getState() { return curState; }


    public void setCanceled( final Boolean canceled ) {
        this.canceled = canceled;
    }


    public String getErrorMessage() {
        return errorMessage;
    }


    public void setErrorMessage( final String errorMessage ) {
        this.errorMessage = errorMessage;
    }


    public Long getQueued() {
        return queued;
    }


    public void setQueued( final Long queued ) {
        this.queued = queued;
    }
}
