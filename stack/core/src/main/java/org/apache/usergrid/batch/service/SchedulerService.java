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
package org.apache.usergrid.batch.service;


import java.util.UUID;

import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.entities.JobData;
import org.apache.usergrid.persistence.entities.JobStat;


/**
 * Simple interface for performing job scheduling
 */
public interface SchedulerService {

    /**
     * Create a new job
     *
     * @param jobName The name of the job.  There must be an implentation in the spring context of type
     * org.apache.usergrid.batch.Job with the name
     * @param fireTime The time to fire in milliseconds since epoch
     * @param jobData The data to pass to the job
     *
     * @return The newly created job data.  The job data uuid is the job id
     */
    JobData createJob( String jobName, long fireTime, JobData jobData );

    /** Delete the job. */
    void deleteJob( UUID jobId );


    /** Query the job data with the given query object */
    Results queryJobData( Query query ) throws Exception;

    /** Get the stats for a job */
    JobStat getStatsForJob( String jobName, UUID jobId ) throws Exception;

    /** Should only be needed for testing */
    void refreshIndex();
}
