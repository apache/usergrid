package org.apache.usergrid.perftest;

import java.io.File;
import java.io.IOException;

/**
 * Logs results as they are produced asynchronously.
 */
public interface ResultsLog {
    String RESULTS_FILE_KEY = "resultsLog.file";
    String WAIT_TIME_KEY = "resultsLog.waitTime";

    /**
     * Opens the result log.
     *
     * @throws IOException on failures to open the results log file.
     */
    void open() throws IOException;


    /**
     * Closes the result log which also causes a flush.
     */
    void close();


    /**
     * Truncates the results effectively deleting previous captures.
     *
     * @throws IOException if there are issues truncating the log file.
     */
    void truncate() throws IOException;


    /**
     * Writes a result record into the log.
     *
     * @param result the result to log.
     */
    void write( String result );


    /**
     * Gets the number of results recorded by the log.
     *
     * @return the number of results.
     */
    long getResultCount();


    /**
     * Gets the path to the results log file.
     *
     * @return the local system path to the results log file
     */
    String getPath();
}
