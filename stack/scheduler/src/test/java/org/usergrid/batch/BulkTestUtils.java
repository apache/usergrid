package org.usergrid.batch;

import org.junit.Ignore;
import org.usergrid.batch.repository.JobDescriptor;

import java.util.Arrays;
import java.util.List;

/**
 * @author zznate
 */
@Ignore
public class BulkTestUtils {

  public static BulkJobFactory getBulkJobFactory() {
    return new MyBulkJobFactory();
  }



  private static class MyBulkJobFactory implements BulkJobFactory {
    @Override
    public List<BulkJob> jobsFrom(final BulkJobsBuilder bulkJobBuilder) {
      return Arrays.asList(new BulkJob[]{new MyBulkJob()});
    }
  }

  private static class MyBulkJob implements BulkJob {
    @Override
    public BulkJobExecution execute(JobDescriptor jobDescriptor) throws BulkJobExecutionException {
      // do some stuff
      return BulkJobExecution.instance(jobDescriptor);
    }
  }
}
