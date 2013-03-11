package org.usergrid.batch;

import com.google.common.base.Preconditions;
import org.usergrid.batch.repository.JobDescriptor;
import org.usergrid.batch.service.JobData;
import org.usergrid.batch.service.SchedulerService;

import java.util.UUID;

/**
 * Models the execution context of the {@link Job} with state transition methods
 * for job status.
 * 
 * @author zznate
 * @author tnine
 */
public class JobExecution {

  private final UUID jobId;
  private final UUID runId;
  private long duration;
  private Status status = Status.NOT_STARTED;
  private long startTime;
  private SchedulerService scheduler;
  private UUID transactionId;
  private JobData data;


  public JobExecution(JobDescriptor jobDescriptor) {
    this.runId = UUID.randomUUID();
    this.jobId = jobDescriptor.getJobId();
    this.scheduler = jobDescriptor.getScheduler();
    this.transactionId = jobDescriptor.getTransactionId();
    this.data = jobDescriptor.getData();
  }

  public UUID getRunId() {
    return runId;
  }

  public long getDuration() {
    return duration;
  }



  /**
   * @param transactionId the transactionId to set
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
  public JobData getData() {
    return data;
  }

  public void start() {
    Preconditions.checkState(this.status.equals(Status.NOT_STARTED) || this.status.equals(Status.FAILED),
        "Attempted to start job in progress");
    this.status = Status.IN_PROGRESS;
    startTime = System.currentTimeMillis();
  }

  public void completed() {
    Preconditions.checkState(this.status.equals(Status.IN_PROGRESS), "Attempted to complete job not in progress");
    this.status = Status.COMPLETED;
    duration = System.currentTimeMillis() - startTime;
  }

  public void failed() {
    Preconditions.checkState(this.status.equals(Status.IN_PROGRESS), "Attempted to fail job not in progress");
    this.status = Status.FAILED;
    duration = System.currentTimeMillis() - startTime;
  }
  
  public void heartbeat() throws JobExecutionException{
    Preconditions.checkState(this.status.equals(Status.IN_PROGRESS), "Attempted to heartbeat job not in progress");
    scheduler.heartbeat(this);
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

  public enum Status {
    NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED
  }

}
