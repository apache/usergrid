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
import org.apache.usergrid.batch.job.OnlyOnceJob;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.entities.FileImport;
import org.apache.usergrid.persistence.entities.Import;
import org.apache.usergrid.persistence.entities.JobData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;
import org.apache.usergrid.persistence.Query.Level;


@Component("fileImportJob")
public class FileImportJob extends OnlyOnceJob {

    public static final String FILE_IMPORT_ID = "fileImportId";
    private static final Logger logger = LoggerFactory.getLogger(FileImportJob.class);

    @Autowired
    EntityManagerFactory emf;

    @Autowired
    ImportService importService;

    public FileImportJob() {
        logger.info( "FileImportJob created " + this );
    }

    @Override
    protected void doJob(JobExecution jobExecution) throws Exception {
        logger.info("execute FileImportJob {}", jobExecution.toString());

        try {
            JobData jobData = jobExecution.getJobData();
            if (jobData == null) {
                logger.error("jobData cannot be null");
                return;
            }

            // heartbeat to indicate job has started
            jobExecution.heartbeat();

            // call the File Parser for the file set in job execution
            importService.downloadAndImportFile(jobExecution);

        } catch ( Throwable t ) {
            logger.debug("Error importing file", t);

            // update file import record
            UUID fileImportId = (UUID) jobExecution.getJobData().getProperty(FILE_IMPORT_ID);
            EntityManager em = emf.getEntityManager(emf.getManagementAppId());
            FileImport fileImport = em.get(fileImportId, FileImport.class);
            fileImport.setState( FileImport.State.FAILED );
            em.update( fileImport );

            throw t;
        }

        logger.info("File Import Service completed job: " + jobExecution.getJobName() );
    }

    @Override
    protected long getDelay(JobExecution execution) throws Exception {
        return 100;
    }

    @Autowired
    public void setImportService( final ImportService importService ) {
        this.importService = importService;
    }

    /**
     * This method is called when the job is retried maximum times by the scheduler but still fails.
     * Thus the scheduler marks it as DEAD.
     */
    @Override
    public void dead( final JobExecution execution ) throws Exception {

        // Get the root entity manager
        EntityManager rootEm = emf.getEntityManager( emf.getManagementAppId() );

        // Mark the sub-job i.e. File Import Job as Failed
        FileImport fileImport = null;//importService.getFileImportEntity(execution);
        fileImport.setErrorMessage("The Job has been tried maximum times but still failed");
        fileImport.setState(FileImport.State.FAILED);
        rootEm.update(fileImport);

        // If one file Job fails, mark the main import Job also as failed
        Results ImportJobResults = rootEm.getSourceEntities(
            fileImport, "includes", null, Level.ALL_PROPERTIES);
        List<Entity> importEntity = ImportJobResults.getEntities();
        UUID importId = importEntity.get(0).getUuid();
        Import importUG = rootEm.get(importId, Import.class);
        importUG.setState(Import.State.FAILED);
        rootEm.update(importUG);
    }
}
