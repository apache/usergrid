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
package org.apache.usergrid.batch;


/**
 * Defines only an execute method. Implementation functionality is completely up to the {@link JobFactory}
 *
 * @author zznate
 */
public interface Job {

    /**
     * Invoked when a job should execute
     *
     * @param execution The execution information.  This will be the same from the last run.  By default you should call
     * exeuction.start() once processing starts
     *
     * @throws Exception If the job cannot be executed
     */
    public void execute( JobExecution execution ) throws Exception;


    /**
     * Invoked when a job is marked as dead by the scheduler.  In some instances, jobs need to know
     * this information to handle this case appropriately.  Dead is defined as the retry count has been
     * exceeded.  I.E 10 failures allowed, this is the 11th attempt to start.
     *
     * @param execution
     * @throws Exception
     */
    public void dead(JobExecution execution) throws Exception;
}
