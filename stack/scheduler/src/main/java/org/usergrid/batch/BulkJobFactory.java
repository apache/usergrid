package org.usergrid.batch;

import org.usergrid.batch.repository.JobDescriptor;

import java.util.List;

/**
 * It is up to the implementation how many BulkJob instances to return,
 * but this should be controled by the BulkJobsBuilder
 *
 * @author zznate
 */
public interface BulkJobFactory {

  /**
   * Return one or more BulkJob ready for execution by a worker thread
   *
   * @param bulkJobsBuilder
   * @return
   */
  List<BulkJob> jobsFrom(BulkJobsBuilder bulkJobsBuilder);

}
