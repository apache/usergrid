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

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.entities.FileImport;
import org.apache.usergrid.persistence.entities.Import;
import org.apache.usergrid.persistence.entities.JobData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Component("importJob")
public class ImportJob extends OnlyOnceJob {

    public static final String IMPORT_ID = "importId";
    private static final Logger logger = LoggerFactory.getLogger(ImportJob.class);

    @Autowired
    protected EntityManagerFactory emf;

    @Autowired
    ImportService importService;

    public ImportJob(){
        logger.info( "ImportJob created " + this );
    }

    @Override
    protected void doJob(JobExecution jobExecution) throws Exception {
        logger.info( "execute ImportJob {}", jobExecution.getJobId().toString() );

        try {
            JobData jobData = jobExecution.getJobData();
            if (jobData == null) {
                logger.error("jobData cannot be null");
                return;
            }

            // heartbeat to indicate job has started
            jobExecution.heartbeat();

            // call the doImport method from import service which
            // schedules the sub-jobs i.e. parsing of files to FileImport Job
            importService.doImport(jobExecution);

        } catch ( Throwable t ) {
            logger.error("Error calling in importJob", t);

            // update import job record
            UUID importId = (UUID) jobExecution.getJobData().getProperty(IMPORT_ID);
            EntityManager mgmtApp = emf.getEntityManager(emf.getManagementAppId());
            Import importEntity = mgmtApp.get(importId, Import.class);
            importEntity.setState(Import.State.FAILED);
            importEntity.setErrorMessage(t.getMessage());
            mgmtApp.update(importEntity);

            throw t;
        }

        logger.error("Import Service completed job");
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
     * This method is called when the job is retried maximum times by the
     * scheduler but still fails. Thus the scheduler marks it as DEAD.
     */
    @Override
    public void dead( final JobExecution execution ) throws Exception {

        // marks the job as failed as it will not be retried by the scheduler.
        EntityManager rootEm = emf.getEntityManager( emf.getManagementAppId());
        Import importUG = importService.getImportEntity(execution);
        importUG.setErrorMessage("The Job has been tried maximum times but still failed");
        importUG.setState(Import.State.FAILED);
        rootEm.update(importUG);

    }
}
