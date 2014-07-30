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
 * Contains state information for an Entity Job
 */
@XmlRootElement
public class FileImport extends TypedEntity {
    //canceled , and expired states aren't used in current iteration.
    public static enum State {
        CREATED, FAILED, SCHEDULED, STARTED, FINISHED, CANCELED, EXPIRED
    }

    @EntityProperty
    protected State curState;

    /**
     * Error message
     */
    @EntityProperty
    protected String errorMessage;


    /**
     * file name
     */
    @EntityProperty
    protected String fileName;

    /**
     * File completion Status
     */
    @EntityProperty
    protected Boolean completed;

    /**
     * LastUpdatedUUID
     */
    @EntityProperty
    protected String lastUpdatedUUID;

    public FileImport() {
    }


    public String getLastUpdatedUUID() {
        return lastUpdatedUUID;
    }


    public void setLastUpdatedUUID( final String lastUpdatedUUID ) {
        this.lastUpdatedUUID = lastUpdatedUUID;
    }

    public Boolean getCompleted() {
        return completed;
    }


    public void setCompleted( final Boolean completed ) {
        this.completed  = completed;
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

    public String getErrorMessage() {
        return errorMessage;
    }


    public void setErrorMessage( final String errorMessage ) {
        this.errorMessage = errorMessage;
    }

    public String getFileName() {
        return fileName;
    }


    public void setFileName( final String fileName ) {
        this.fileName = fileName;
    }



}
