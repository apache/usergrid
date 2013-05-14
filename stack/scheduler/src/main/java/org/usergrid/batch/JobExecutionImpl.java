package org.usergrid.batch;

import java.util.UUID;

import org.usergrid.batch.repository.JobDescriptor;
import org.usergrid.batch.service.JobRuntimeService;
import org.usergrid.persistence.entities.JobData;
import org.usergrid.persistence.entities.JobStat;

import com.google.common.base.Preconditions;

/**
 * Models the execution context of the {@link Job} with state transition methods
 * for job status.
 * 
 * @author zznate
 * @author tnine
 */
public class JobExecutionImpl implements JobExecution, JobRuntime {

  private final UUID jobId;
  private final UUID runId;
  private final String jobName;
  private long duration;
  private Status status = Status.NOT_STARTED;
  private long startTime;
  private JobRuntimeService runtime;
  private UUID transactionId;
  private JobData data;
  private JobStat stats;
  private long delay = -1;

  public JobExecutionImpl(JobDescriptor jobDescriptor) {
    this.runId = UUID.randomUUID();
    this.jobId = jobDescriptor.getJobId();
    this.runtime = jobDescriptor.getRuntime();
    this.jobName = jobDescriptor.getJobName();
    this.transactionId = jobDescriptor.getTransactionId();
    this.data = jobDescriptor.getData();
    this.stats = jobDescriptor.getStats();
  }

  public UUID getRunId() {
    return runId;
  }

  public long getDuration() {
    return duration;
  }

  /**
   * @param transactionId
   *          the transactionId to set
   */
  public void setTransactionId(UUID transactionId) {
    this.transactionId = transactionId;
  }

  public UUID getJobId() {
    return jobId;
  }

  /**
   * @return the data
   */
  public JobData getJobData() {
    return data;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.batch.JobExecution#getJobStats()
   */
  @Override
  public JobStat getJobStats() {
    return stats;
  }

  public void start(int maxFailures) {
    Preconditions.checkState(this.status.equals(Status.NOT_STARTED) || this.status.equals(Status.FAILED),
        "Attempted to start job in progress");
    this.status = Status.IN_PROGRESS;

    stats.incrementRuns();
    

    // use >= in case the threshold lowers after the job has passed the failure
    // mark
    if (maxFailures != FOREVER && stats.getTotalAttempts() > maxFailures) {
      status = Status.DEAD;
    }

    startTime = System.currentTimeMillis();
    stats.setStartTime(startTime);
  }

  public void completed() {
    updateState(Status.IN_PROGRESS, "Attempted to complete job not in progress", Status.COMPLETED);
    stats.setDuration(duration);
  }

  /**
   * Mark this execution as failed. Also pass the maxium number of possible
   * failures. Set to JobExecution.FOREVER for no limit
   * 
   * @param maxFailures
   */
  public void failed() {
    updateState(Status.IN_PROGRESS, "Attempted to fail job not in progress", Status.FAILED);
  }

  /**
   * This job should be killed and not retried
   */
  public void killed() {
    updateState(Status.IN_PROGRESS, "Attempted to fail job not in progress", Status.DEAD);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.batch.JobExecution#delay(long)
   */
  @Override
  public void delay(long delay) {
    updateState(Status.IN_PROGRESS, "Attempted to delay a job not in progress", Status.DELAYED);
    stats.incrementDelays();
    this.delay = delay;
    runtime.delay(this);
  }

  /**
   * Update our state
   * @param expected
   * @param message
   * @param newStatus
   */
  private void updateState(Status expected, String message, Status newStatus) {
    Preconditions.checkState(this.status.equals(expected), message);
    this.status = newStatus;
    duration = System.currentTimeMillis() - startTime;
  }

  /**
   * Make sure we're in progress and notifiy the scheduler we're still running
   */
  public void heartbeat() {
    Preconditions.checkState(this.status.equals(Status.IN_PROGRESS), "Attempted to heartbeat job not in progress");
    runtime.heartbeat(this);
  }

  /* (non-Javadoc)
   * @see org.usergrid.batch.JobExecution#heartbeat(long)
   */
  @Override
  public void heartbeat(long milliseconds) {
    Preconditions.checkState(this.status.equals(Status.IN_PROGRESS), "Attempted to heartbeat job not in progress");
    runtime.heartbeat(this, milliseconds);
    this.delay = milliseconds;
  }

  /**
   * @return the startTime
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * @return the transactionId
   */
  public UUID getTransactionId() {
    return transactionId;
  }

  public Status getStatus() {
    return this.status;
  }

  /**
   * @return the delay
   */
  public long getDelay() {
    return delay;
  }

  /**
   * @return the jobName
   */
  public String getJobName() {
    return jobName;
  }

  /* (non-Javadoc)
   * @see org.usergrid.batch.JobRuntime#getExecution()
   */
  @Override
  public JobExecution getExecution() {
    return this;
  }

}
