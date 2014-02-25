package org.apache.usergrid.management.export;


import java.util.UUID;

import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.management.ExportInfo;


/**
 * Performs all functions related to exporting
 *
 */
public interface ExportService {

    /**
     * Schedules the export to execute
     * @param config
     */
    UUID schedule(ExportInfo config) throws Exception;


    /**
     * Perform the export to the external resource
     * @param config
     */
    void doExport(ExportInfo config, JobExecution jobExecution) throws Exception;

    /**
     * Returns the current state of the service.
     * @return
     */
    String getState(UUID appId,UUID state) throws Exception;

    void setS3Export(S3Export s3Export);

}
