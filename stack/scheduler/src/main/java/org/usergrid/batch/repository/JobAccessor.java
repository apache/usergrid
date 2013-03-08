package org.usergrid.batch.repository;

import org.usergrid.batch.BulkJobExecution;
import org.usergrid.batch.BulkJobFactory;

import java.util.List;

/**
 * @author zznate
 */
public interface JobAccessor {

  List<JobDescriptor> activeJobs();

  void save(BulkJobExecution bulkJobExecution);
}
