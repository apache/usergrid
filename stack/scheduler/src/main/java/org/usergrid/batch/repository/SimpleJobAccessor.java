package org.usergrid.batch.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.batch.BulkJobExecution;
import org.usergrid.batch.BulkJobFactory;

import java.util.List;

/**
 * Set a list of job descriptors by hand
 * @author zznate
 */
public class SimpleJobAccessor implements JobAccessor {
  private Logger logger = LoggerFactory.getLogger(SimpleJobAccessor.class);

  protected List<JobDescriptor> jobDescriptors;

  public void setJobDescriptors(List<JobDescriptor> jobDescriptors) {
    this.jobDescriptors = jobDescriptors;
  }

  @Override
  public List<JobDescriptor> activeJobs() {
    return jobDescriptors;
  }

  /**
   * Prints a logging statement at info level
   * @param bulkJobExecution
   */
  @Override
  public void save(BulkJobExecution bulkJobExecution) {
    logger.info("save() in SimpleJobAccessor with execution: {}", bulkJobExecution);
  }

}
