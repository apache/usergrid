package org.usergrid.batch;

import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.usergrid.batch.repository.JobDescriptor;

/**
 * @author zznate
 */
@Ignore
public class BulkTestUtils {

  public static JobFactory getBulkJobFactory() {
    return new MyBulkJobFactory();
  }



  private static class MyBulkJobFactory implements JobFactory {
    /* (non-Javadoc)
     * @see org.usergrid.batch.JobFactory#jobsFrom(org.usergrid.batch.repository.JobDescriptor)
     */
    @Override
    public List<Job> jobsFrom(JobDescriptor descriptor) {
      return Arrays.asList(new Job[]{new MyBulkJob()});
    }
  }

  private static class MyBulkJob implements Job {
    @Override
    public void execute(JobExecution execution) throws JobExecutionException {
      // do some stuff
     
    }
  }
}
