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


import org.apache.usergrid.batch.JobExecution;


/**
 * Job callbacks in the @{link #SchedularService} are propagated to
 * registered implementations of this JobListener.
 */
public interface JobListener {

    /**
     * Submission of job execution notified onSubmit.
     *
     * @param execution the submitted JobExecution
     */
    void onSubmit( JobExecution execution );

    /**
     * Successful executions of a Job notify onSuccess.
     *
     * @param execution the JobExection associated with the Job
     */
    void onSuccess( JobExecution execution );

    /**
     * Execution failures of a Job notify onFailure.
     *
     * @param execution the JobExection associated with the Job
     */
    void onFailure( JobExecution execution );
}
