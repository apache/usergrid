package org.usergrid.management.export;


import org.usergrid.management.ExportInfo;


/**
 * Performs all functions related to exporting
 *
 */
public interface ExportService {

    /**
     * Schedules the export to execute
     * @param config
     */
    void schedule(ExportInfo config);


    /**
     * Perform the export to the external resource
     * @param config
     */
    void doExport(ExportInfo config) throws Exception;

}
