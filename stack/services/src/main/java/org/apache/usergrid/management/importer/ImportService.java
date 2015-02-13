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

package org.apache.usergrid.management.importer;


import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.entities.FailedImportEntity;
import org.apache.usergrid.persistence.entities.FileImport;
import org.apache.usergrid.persistence.entities.Import;

import java.util.Map;
import java.util.UUID;


/**
 * Performs all functions related to importing.
 */
public interface ImportService {

    /**
     * Schedules the import to execute
     */
    Import schedule( final UUID applicationId, Map<String, Object> json ) throws Exception;

    /**
     * Get the imports results for the application
     * @param applicationId
     * @param ql The query executed (nullable)
     * @param cursor  The cursor passed (nullable)
     * @return
     */
    Results getImports(final UUID applicationId, final String ql, final String cursor);

    /**
     * Get the import
     * @param applicationId
     * @param importId
     * @return
     */
    Import getImport(final UUID applicationId, final UUID importId);

    /**
     * Get the results
     *
     * @param applicationId The applicationId
     * @param importId The import id to get files from
     * @param ql The query executed (nullable)
     * @param cursor The cursor passed (nullable)
     */
    Results getFileImports(final UUID applicationId, final UUID importId, final String ql, final String cursor);

    /**
     * Get the results
     *
     * @param applicationId The applicationId
     * @param importId The import id to get files from
     *
     * @return The FileImport
     */
    FileImport getFileImport(final UUID applicationId, final UUID importId, final UUID fileImportId);


    /**
     * Get the results of failed imports
     *
     *
     * @param applicationId The applicationId
     * @param importId The import id to get files from
     * @param ql The query executed (nullable)
     * @param cursor The cursor passed (nullable)
     */
    Results getFailedImportEntities(final UUID applicationId,  final UUID importId, final UUID fileImportId, final String ql,  final String cursor);

    /**
     * Get the failedimport entity from it's parentId
     * @param applicationId
     * @param importId
     * @param fileImportId
     * @param failedImportId
     * @return
     */
    FailedImportEntity getFailedImportEntity(final UUID applicationId, final UUID importId, final UUID fileImportId, final UUID failedImportId);

    /**
     * Perform the import from the external resource
     */
    void doImport(JobExecution jobExecution) throws Exception;

    /**
     * Parses the input file and creates entities
     */
    void downloadAndImportFile(JobExecution jobExecution) throws Exception;

    /**
     * Get the state for the Job with UUID
     * @param uuid Job UUID
     * @return State of Job
     */
    Import.State getState( UUID uuid ) throws Exception;

    /**
     * Returns error message for the job with UUID
     * @param uuid Job UUID
     */
    String getErrorMessage(UUID uuid) throws Exception;

    /**
     * @return FileImportEntity
     */
    FileImport getFileImportEntity(final JobExecution jobExecution) throws Exception;

    /**
     * @param jobExecution
     * @return ImportEntity
     */
    Import getImportEntity(final JobExecution jobExecution) throws Exception;
}
