package org.usergrid.batch;

import java.util.UUID;

import org.usergrid.batch.repository.JobDescriptor;
import org.usergrid.batch.service.SchedulerService;
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
public class JobExecutionImpl implements JobExecution{

  
  private final UUID jobId;
  private final UUID runId;
  private final String jobName;
  private long duration;
  private Status status = Status.NOT_STARTED;
  private long startTime;
  private SchedulerService scheduler;
  private UUID transactionId;
  private JobData data;
  private JobStat stats;


  public JobExecutionImpl(JobDescriptor jobDescriptor) {
    this.runId = UUID.randomUUID();
    this.jobId = jobDescriptor.getJobId();
    this.scheduler = jobDescriptor.getScheduler();
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
  public JobData getJobData() {
    return data;
  }

  /* (non-Javadoc)
   * @see org.usergrid.batch.JobExecution#getJobStats()
   */
  @Override
  public JobStat getJobStats() {
    return stats;
  }

  public void start() {
    Preconditions.checkState(this.status.equals(Status.NOT_STARTED) || this.status.equals(Status.FAILED),
        "Attempted to start job in progress");
    this.status = Status.IN_PROGRESS;
    startTime = System.currentTimeMillis();
    stats.setStartTime(startTime);
  }

  public void completed() {
    Preconditions.checkState(this.status.equals(Status.IN_PROGRESS), "Attempted to complete job not in progress");
    this.status = Status.COMPLETED;
    duration = System.currentTimeMillis() - startTime;
    stats.setDuration(duration);
  }

  /**
   * Mark this execution as failed.  Also pass the maxium number of possible failures.  Set to JobExecution.FOREVER for no limit
   * @param maxFailures
   */
  public void failed(int maxFailures) {
    Preconditions.checkState(this.status.equals(Status.IN_PROGRESS), "Attempted to fail job not in progress");
    status = Status.FAILED;
    duration = System.currentTimeMillis() - startTime;
    stats.incrementFailures();
    
    //use >= in case the threshold lowers after the job has passed the failure mark
    if(maxFailures != FOREVER && stats.getFailCount() > maxFailures){
      status = Status.DEAD;
    }
  }
  
  /**
   * This job should be killed and not retried
   */
  public void killed(){
    failed(0);
  }
  
  
  public void heartbeat() {
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

  /**
   * @return the jobName
   */
  public String getJobName() {
    return jobName;
  }

  


}
