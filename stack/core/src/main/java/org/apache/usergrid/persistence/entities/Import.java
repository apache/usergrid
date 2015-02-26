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

import org.apache.usergrid.persistence.TypedEntity;
import org.apache.usergrid.persistence.annotations.EntityProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Contains state information for an Entity Import Job
 */
@XmlRootElement
public class Import extends TypedEntity {

   //canceled  and expired states aren't used in current iteration.
    public static enum State {
        CREATED, FAILED, SCHEDULED, STARTED, FINISHED, CANCELED, EXPIRED
    }

    @EntityProperty
    private int fileCount;

    @EntityProperty
    protected State curState;

    /**
     * Time job started
     */
    @EntityProperty
    protected Long started;

    /**
     * Error message
     */
    @EntityProperty
    protected String errorMessage;


    public Import() {}


    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @EntityProperty
    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @EntityProperty
    public int getFileCount() {
        return fileCount;
    }


    /**
     * get the started time for the import job
     */
    public Long getStarted() {
        return started;
    }

    /**
     * set the started time for the import job
     */
    public void setStarted( final Long started ) {
        this.started = started;
    }

    /**
     * sets the state of the current job
     * @param setter state of the job
     */
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @EntityProperty
    public void setState( State setter ) {
        curState = setter;
    }


    /**
     * gets the state of the current job
     * @return state
     */
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @EntityProperty
    public State getState() { return curState; }

    /**
     * Get error message for the job
     * @return
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message for the job
     * @param errorMessage error message
     */
    public void setErrorMessage( final String errorMessage ) {
        this.errorMessage = errorMessage;
    }

}
