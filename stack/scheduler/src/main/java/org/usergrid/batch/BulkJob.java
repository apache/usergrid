package org.usergrid.batch;

import org.usergrid.batch.repository.JobDescriptor;

/**
 * Defines only an execute method. Implementation functionality is completely up
 * to the {@link BulkJobFactory}
 * @author zznate
 */
public interface BulkJob {

  BulkJobExecution execute(JobDescriptor jobDescriptor) throws BulkJobExecutionException;

}
