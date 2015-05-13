/*
 *
 *  *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *  *
 *
 */

package org.apache.usergrid.persistence.core.migration.data;


public interface ProgressObserver {

    /**
     * Signal the process started
     */
    public void start();

    /**
     * Signal the process has stopped
     */
    public void complete();

    /**
     * Mark the migration as failed
     *
     * @param migrationVersion The migration version running during the failure
     * @param reason The reason to save
     */
    public void failed( final int migrationVersion, final String reason );

    /**
     * Mark the migration as failed with a stack trace
     *
     * @param migrationVersion The migration version running during the failure
     * @param reason The error description to save
     * @param throwable The error that happened
     */
    public void failed( final int migrationVersion, final String reason, final Throwable throwable );


    /**
     * Update the status of the migration with the message
     *
     * @param message The message to save for the status
     */
    public void update( final int migrationVersion, final String message );
}
