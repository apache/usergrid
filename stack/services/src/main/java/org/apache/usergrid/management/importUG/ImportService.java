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

package org.apache.usergrid.management.importUG;

import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.persistence.entities.FileImport;
import org.apache.usergrid.persistence.entities.Import;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

/**
 * Performs all functions related to importing
 */
public interface ImportService {

    /**
     * Schedules the import to execute
     */
    UUID schedule(Map<String, Object> json) throws Exception;

    /**
     * Perform the import from the external resource
     */
    void doImport(JobExecution jobExecution) throws Exception;

    /**
     * Parses the input file and creates entities
     *
     * @param jobExecution
     * @throws Exception
     */
    void FileParser(JobExecution jobExecution) throws Exception;

    /**
     * Get the state for the Job with UUID
     * @param uuid Job UUID
     * @return State of Job
     * @throws Exception
     */
    String getState(UUID uuid) throws Exception;

    /**
     * Returns error message for the job with UUID
     * @param uuid Job UUID
     * @return error message
     * @throws Exception
     */
    String getErrorMessage(UUID uuid) throws Exception;

    /**
     * Returns all the temp files downloaded from s3
     * @return the list of downloaded files from S3.
     */
    ArrayList<File> getEphemeralFile();

    /**
     * @param jobExecution
     * @return FileImportEntity
     * @throws Exception
     */
    FileImport getFileImportEntity(final JobExecution jobExecution) throws Exception;

    /**
     * @param jobExecution
     * @return ImportEntity
     * @throws Exception
     */
    Import getImportEntity(final JobExecution jobExecution) throws Exception;


}
