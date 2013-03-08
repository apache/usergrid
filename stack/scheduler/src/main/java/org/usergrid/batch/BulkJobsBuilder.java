package org.usergrid.batch;

import org.usergrid.batch.repository.JobDescriptor;

import java.util.List;
import java.util.UUID;

/**
 * Builder class for BulkJob implementations
 *
 * @author zznate
 */
public final class BulkJobsBuilder {

  private BulkJobsBuilder(JobDescriptor jobDescriptor) {
    this.jobDescriptor = jobDescriptor;
    this.runId = UUID.randomUUID();
  }

  private final JobDescriptor jobDescriptor;
  private final UUID runId;
  private int limit;
  private long maxExecTime;


  /**
   * The {@link JobDescriptor} is required to defer the job creation while maintaining
   * configurability
   *
   * @param jobDescriptor
   * @return
   */
  public static BulkJobsBuilder newBuilder(JobDescriptor jobDescriptor) {
    return new BulkJobsBuilder(jobDescriptor);
  }


  public BulkJobsBuilder limit(int limit) {
    this.limit = limit;
    return this;
  }

  public BulkJobsBuilder maxExecTime(long maxExecTime) {
    this.maxExecTime = maxExecTime;
    return this;
  }

  public List<BulkJob> build() {
    return jobDescriptor.getBulkJobFactory().jobsFrom(this);
  }

  public int getLimit() {
    return limit;
  }

  public long getMaxExecTime() {
    return maxExecTime;
  }

  public JobDescriptor getJobDescriptor() {
    return jobDescriptor;
  }

  public UUID getRunId() {
    return runId;
  }
}
