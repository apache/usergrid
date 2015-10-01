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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.batch.job.OnlyOnceJob;
import org.apache.usergrid.persistence.entities.JobData;


/**
 * Make an enum here, that contains the state info (look at Scott's
 * code and emulate that to see what you can return in the JSON object).
 */
@Component("exportJob")
public class ExportJob extends OnlyOnceJob {
    public static final String EXPORT_ID = "exportId";
    private static final Logger logger = LoggerFactory.getLogger( ExportJob.class );

    @Autowired
    ExportService exportService;

    public ExportJob() {
        logger.info( "ExportJob created " + this );
    }


    @Override
    public void doJob( JobExecution jobExecution ) throws Exception {
        logger.info( "execute ExportJob {}", jobExecution.getJobId().toString() );

        JobData jobData = jobExecution.getJobData();
        if ( jobData == null ) {
            logger.error( "jobData cannot be null" );
            return;
        }

        jobExecution.heartbeat();
        try {
            exportService.doExport( jobExecution );
        }
        catch ( Exception e ) {
            logger.error( "Export Service failed to complete job" );
            logger.error(e.getMessage());
            return;
        }

        logger.info( "executed ExportJob process completed" );
    }


    @Override
    protected long getDelay( JobExecution jobExecution ) throws Exception {
        //return arbitrary number
        return 100;
    }


    @Autowired
    public void setExportService( final ExportService exportService ) {
        this.exportService = exportService;
    }


    @Override
    public void dead( final JobExecution execution ) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
