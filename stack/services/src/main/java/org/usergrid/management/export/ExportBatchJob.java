package org.usergrid.management.export;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.batch.Job;
import org.usergrid.batch.JobExecution;
import org.usergrid.persistence.entities.JobData;


/**
 *
 *
 */
public class ExportBatchJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger( ExportBatchJob.class );

    //TODO: ask if the service manager is needed here. Maybe in order to see if process.
    @Autowired
    protected ExportService exportService;


    @Override
    public void execute( final JobExecution execution ) throws Exception {

        logger.info("execute ExportBatchJob {}", execution);

        JobData jobData = execution.getJobData();
        UUID jobUUID = (UUID) jobData.getProperty( "jobUUID" );


    }
}
