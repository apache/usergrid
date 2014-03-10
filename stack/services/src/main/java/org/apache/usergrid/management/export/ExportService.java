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
package org.apache.usergrid.management.export;


import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.batch.JobExecution;


/**
 * Performs all functions related to exporting
 */
public interface ExportService {

    /**
     * Schedules the export to execute
     */
    UUID schedule( Map<String,Object> json) throws Exception;


    /**
     * Perform the export to the external resource
     */
    void doExport( JobExecution jobExecution ) throws Exception;

    /**
     * Returns the current state of the service.
     */
    String getState( UUID appId, UUID state ) throws Exception;

    void setS3Export( S3Export s3Export );
}
