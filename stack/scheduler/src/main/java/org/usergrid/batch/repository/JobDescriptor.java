package org.usergrid.batch.repository;

import java.util.UUID;

import me.prettyprint.cassandra.utils.Assert;

import org.usergrid.batch.service.SchedulerService;
import org.usergrid.persistence.TypedEntity;
import org.usergrid.persistence.entities.JobData;

/**
 * @author zznate
 * @author tnine
 */
public class JobDescriptor extends TypedEntity {

  private final String jobName;
  private final UUID jobId;
  private UUID transactionId;
  private JobData data;
  private SchedulerService scheduler;

  public JobDescriptor(String jobName, UUID jobId, UUID transactionId, JobData data, SchedulerService scheduler) {
    Assert.notNull(jobName, "Job name cannot be null");
    Assert.notNull(jobId != null, "A JobId is required");
    Assert.notNull(transactionId != null, "A transactionId is required");
    Assert.notNull(data != null, "Data is required");
    Assert.notNull(scheduler != null, "A scheduler is required");

    this.jobName = jobName;
    this.jobId = jobId;
    this.transactionId = transactionId;
    this.data = data;
    this.scheduler = scheduler;
  }

  /**
   * @return the jobName
   */
  public String getJobName() {
    return jobName;
  }

  /**
   * @return the jobId
   */
  public UUID getJobId() {
    return jobId;
  }

  /**
   * @return the transactionId
   */
  public UUID getTransactionId() {
    return transactionId;
  }

  /**
   * @return the data
   */
  public JobData getData() {
    return data;
  }

  /**
   * @return the scheduler
   */
  public SchedulerService getScheduler() {
    return scheduler;
  }

}
