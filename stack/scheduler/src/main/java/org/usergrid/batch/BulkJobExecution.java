package org.usergrid.batch;

import com.google.common.base.Preconditions;
import org.usergrid.batch.repository.JobDescriptor;

import java.util.UUID;

/**
 * Models the execution context of the {@link BulkJob} with state
 * transition methods for job status.
 *
 * @author zznate
 */
public class BulkJobExecution {

  private final UUID jobId;
  private final UUID runId;
  private long duration;
  private Status status = Status.NOT_STARTED;
  private long startTime;

  public static BulkJobExecution instance(JobDescriptor jobDescriptor) {
    return new BulkJobExecution(jobDescriptor);
  }

  private BulkJobExecution(JobDescriptor jobDescriptor) {
    this.runId = UUID.randomUUID();
    this.jobId = jobDescriptor.getJobId();
  }

  public UUID getRunId() {
    return runId;
  }

  public long getDuration() {
    return duration;
  }

  public BulkJobExecution setDuration(long duration) {
    this.duration = duration;
    return this;
  }

  public UUID getJobId() {
    return jobId;
  }

  public void start() {
    Preconditions.checkState(this.status.equals(Status.NOT_STARTED),
            "Attempted to start job in progress");
    this.status = Status.IN_PROGRESS;
    startTime = System.currentTimeMillis();
  }

  public void completed() {
    Preconditions.checkState(this.status.equals(Status.IN_PROGRESS),
            "Attempted to complete job not in progress");
    this.status = Status.COMPLETED;
    duration = System.currentTimeMillis() - startTime;
  }

  public void failed() {
    Preconditions.checkState(this.status.equals(Status.IN_PROGRESS),
            "Attempted to fail job not in progress");
    this.status = Status.FAILED;
    duration = System.currentTimeMillis() - startTime;
  }

  public Status getStatus() {
    return this.status;
  }

  public enum Status {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    FAILED
  }

}
