/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.corepersistence.service;

import rx.Observable;

import java.util.Map;
import java.util.UUID;

/**
 * serializing job status or any kind
 */
public interface StatusService  {
    /**
     * set status
     * @param applicationdId
     * @param jobStatusId
     * @param status
     * @param data
     * @return job id
     */
    Observable<UUID> setStatus(
        final UUID applicationdId, final UUID jobStatusId, final Status status, final Map<String,Object> data );

    /**
     * Get status based on app and job
     * @param applicationId
     * @param jobId
     * @return
     */
    Observable<JobStatus> getStatus(final UUID applicationId, final UUID jobId);

    enum Status{
        STARTED, FAILED, INPROGRESS, COMPLETE, UNKNOWN;
    }

    /**
     * Encapsulate job return
     */
    class JobStatus{
        private final UUID jobStatusId;
        private final Status status;
        private final Map<String, Object> data;

        public JobStatus(final UUID jobStatusId, final Status status, final Map<String,Object> data){

            this.jobStatusId = jobStatusId;
            this.status = status;
            this.data = data;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public Status getStatus() {
            return status;
        }

        public UUID getJobStatusId() {
            return jobStatusId;
        }
    }
}
