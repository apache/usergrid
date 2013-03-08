package org.usergrid.batch;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.usergrid.batch.repository.JobDescriptor;

/**
 * @author zznate
 * @author tnine
 */
@Component
public class SimpleBulkJobFactory implements BulkJobFactory {

  private Logger logger = LoggerFactory.getLogger(SimpleBulkJobFactory.class);

  @Override
  public List<BulkJob> jobsFrom(BulkJobsBuilder bulkJobsBuilder) {
    return Arrays.asList(new BulkJob[]{new SimpleBulkJob()});
  }

  class SimpleBulkJob implements BulkJob {
    @Override
    public BulkJobExecution execute(JobDescriptor jobDescriptor) throws BulkJobExecutionException {
      logger.info("execute called for SimpleBulkJob");
      BulkJobExecution bje = BulkJobExecution.instance(jobDescriptor);

      bje.start();

      bje.completed();

      logger.info("execute complete for SimpleBulkJob");
      return bje;
    }
  }
}
