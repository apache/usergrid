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

import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;

/**
 * Created by ApigeeCorporation on 7/8/14.
 */
@Component("fileImportJob")
public class FileImportJob extends OnlyOnceJob {

    public static final String FILE_IMPORT_ID = "fileImportId";
    private static final Logger logger = LoggerFactory.getLogger(FileImportJob.class);

    @Autowired
    ImportService importService;

    //injected the Entity Manager Factory
    protected EntityManagerFactory emf;

    public FileImportJob() {
        logger.info( "FileImportJob created " + this );
    }

    @Override
    protected void doJob(JobExecution jobExecution) throws Exception {
        logger.info( "execute FileImportJob {}", jobExecution );

        JobData jobData = jobExecution.getJobData();
        if ( jobData == null ) {
            logger.error( "jobData cannot be null" );
            return;
        }

        jobExecution.heartbeat();
        //try {
            importService.FileParser( jobExecution );
        //}
//        catch ( Exception e ) {
//            logger.error( " Import Service failed to complete file import job" );
//            logger.error(e.getMessage());
//            throw e;
//        }
    }

    @Override
    protected long getDelay(JobExecution execution) throws Exception {
        return 100;
    }

    @Autowired
    public void setImportService( final ImportService importService ) {
        this.importService = importService;
    }

    @Override
    public void dead( final JobExecution execution ) throws Exception {

        EntityManager rootEm = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);

        FileImport fileImport = importService.getFileImportEntity(execution);
        fileImport.setErrorMessage("The Job has been tried maximum times but still failed");
        fileImport.setState(FileImport.State.FAILED);
        rootEm.update(fileImport);

        // If one file Job fails, mark the import Job also as failed
        Results ImportJobResults = rootEm.getConnectingEntities(fileImport.getUuid(), "includes", null, Results.Level.ALL_PROPERTIES);
        List<Entity> importEntity = ImportJobResults.getEntities();
        UUID importId = importEntity.get(0).getUuid();
        Import importUG = rootEm.get(importId, Import.class);
        importUG.setState(Import.State.FAILED);
        rootEm.update(importUG);
    }
}
