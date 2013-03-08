package org.usergrid.batch;

import org.junit.Test;
import org.usergrid.batch.repository.JobDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author zznate
 */
public class BulkJobFactoryUnitTest {

  private static UUID jobId = UUID.randomUUID();

  @Test
  public void verifyBuildup() {
    JobDescriptor jobDescriptor = JobDescriptor.instance("my-job-descriptor",
            BulkTestUtils.getBulkJobFactory(),
            jobId);
    BulkJobsBuilder bulkJobsBuilder = BulkJobsBuilder.newBuilder(jobDescriptor)
            .maxExecTime(1234L)
            .limit(5);

    List<BulkJob> bulkJobs = bulkJobsBuilder.build();
    assertNotNull(bulkJobs);
    assertEquals(1, bulkJobs.size());
  }




}
