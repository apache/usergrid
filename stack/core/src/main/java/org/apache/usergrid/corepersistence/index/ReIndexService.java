/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.index;


/**
 * An interface for re-indexing all entities in an application
 */
public interface ReIndexService {


    /**
     * Perform an index rebuild
     *
     * @param reIndexRequestBuilder The builder to build the request
     */
    ReIndexStatus rebuildIndex( final ReIndexRequestBuilder reIndexRequestBuilder );


    /**
     * Generate a build for the index
     */
    ReIndexRequestBuilder getBuilder();


    /**
     * Get the status of a job
     * @param jobId The jobId returned during the rebuild index
     * @return
     */
    ReIndexStatus getStatus( final String jobId );


    /**
     * The response when requesting a re-index operation
     */
    public class ReIndexStatus {
        final String jobId;
        final Status status;
        final long numberProcessed;
        final long lastUpdated;


        public ReIndexStatus( final String jobId, final Status status, final long numberProcessed,
                              final long lastUpdated ) {
            this.jobId = jobId;
            this.status = status;
            this.numberProcessed = numberProcessed;
            this.lastUpdated = lastUpdated;
        }


        /**
         * Get the jobId used to resume this operation
         */
        public String getJobId() {
            return jobId;
        }


        /**
         * Get the last updated time, as a long
         * @return
         */
        public long getLastUpdated() {
            return lastUpdated;
        }


        /**
         * Get the number of records processed
         * @return
         */
        public long getNumberProcessed() {
            return numberProcessed;
        }


        /**
         * Get the status
         * @return
         */
        public Status getStatus() {
            return status;
        }
    }

    enum Status{
        STARTED, INPROGRESS, COMPLETE, UNKNOWN;
    }
}
