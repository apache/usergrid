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
import java.util.UUID;


/**
 * Contains state information for an Entity FileImport Job
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
     * Name of file to import
     */
    @EntityProperty
    protected String fileName;

    @EntityProperty
    private UUID applicationId;


    /**
     * LastUpdatedUUID
     */
    @EntityProperty
    protected String lastUpdatedUUID;

    @EntityProperty
    protected long importedEntityCount;

    @EntityProperty
    protected long failedEntityCount;


    /**
     * File completion Status
     */
    @EntityProperty
    protected long importedConnectionCount;

    @EntityProperty
    protected long failedConnectionCount;


    public FileImport() {
        setLastUpdatedUUID(" ");
        setErrorMessage(" ");
        setState(FileImport.State.CREATED);
    }


    public FileImport( String fileName, UUID applicationId ) {
        this();
        this.fileName = fileName;
        this.setApplicationId(applicationId);
    }

    /**
     * Gets the last updated entity UUID for the File being handled by the File Import Job
     * @return last updated entity UUID
     */
    public String getLastUpdatedUUID() {
        return lastUpdatedUUID;
    }

    /**
     * Sets the lastupdated UUID
     * @param lastUpdatedUUID entity UUID
     */
    public void setLastUpdatedUUID(final String lastUpdatedUUID) {
        this.lastUpdatedUUID = lastUpdatedUUID;
    }

    /**
     * gets the state of the current job
     * @return state
     */
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @EntityProperty
    public State getState() {
        return curState;
    }

    /**
     * sets the state of the current job
     * @param setter state of the job
     */
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @EntityProperty
    public void setState(State setter) {
        curState = setter;
    }

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
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Get the filename of the file being imported by this FileImport Job
     * @return filename
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the filename of the file being imported by this FileImport Job
     * @param fileName file name
     */
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }


    /**
     * Target application name
     */
    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public long getImportedEntityCount() {
        return importedEntityCount;
    }


    public void setImportedEntityCount( final long importedEntityCount ) {
        this.importedEntityCount = importedEntityCount;
    }


    public long getFailedEntityCount() {
        return failedEntityCount;
    }


    public void setFailedEntityCount( final long failedEntityCount ) {
        this.failedEntityCount = failedEntityCount;
    }


    public long getImportedConnectionCount() {
        return importedConnectionCount;
    }


    public void setImportedConnectionCount( final long importedConnectionCount ) {
        this.importedConnectionCount = importedConnectionCount;
    }


    public long getFailedConnectionCount() {
        return failedConnectionCount;
    }


    public void setFailedConnectionCount( final long failedConnectionCount ) {
        this.failedConnectionCount = failedConnectionCount;
    }
}
