package org.usergrid.batch.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.batch.Job;
import org.usergrid.batch.JobExecution;
import org.usergrid.batch.JobExecutionException;
import org.usergrid.batch.JobExecutionImpl;
import org.usergrid.batch.JobFactory;
import org.usergrid.batch.JobNotFoundException;
import org.usergrid.batch.JobExecution.Status;
import org.usergrid.batch.repository.JobAccessor;
import org.usergrid.batch.repository.JobDescriptor;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.yammer.metrics.annotation.ExceptionMetered;
import com.yammer.metrics.annotation.Timed;

/**
 * Service that schedules itself, then schedules jobs in the same pool
 * 
 * @author zznate
 * @author tnine
 */
public class JobSchedulerService extends AbstractScheduledService {

  protected static final long DEFAULT_DELAY = 1000;
  protected static final long ERROR_DELAY = 10000;
  protected static final List<JobDescriptor> EMPTY = Collections.unmodifiableList(new ArrayList<JobDescriptor>(0));

  private static final Logger logger = LoggerFactory.getLogger(JobSchedulerService.class);

  private long interval = DEFAULT_DELAY;
  private int workerSize = 1;
  private int maxFailCount = 10;

  private JobAccessor jobAccessor;
  private JobFactory jobFactory;

  private Semaphore capacitySemaphore;

  private ListeningScheduledExecutorService service;

  public JobSchedulerService() {
  }

  @Timed(name = "BulkJobScheduledService_runOneIteration", group = "scheduler", durationUnit = TimeUnit.MILLISECONDS, rateUnit = TimeUnit.MINUTES)
  @Override
  protected void runOneIteration() throws Exception {

    try {
      logger.info("running iteration...");
      List<JobDescriptor> activeJobs = null;

      // run until there are no more active jobs
      while (true) {

        // get the semaphore if we can. This means we have space for at least 1
        // job
        if (logger.isDebugEnabled()) {
          logger.debug("About to acquire semaphore.  Capacity is {}", capacitySemaphore.availablePermits());
        }

        capacitySemaphore.acquire();
        // release the sempaphore we only need to acquire as a way to stop the
        // loop if there's no capacity
        capacitySemaphore.release();

        // always +1 since the acquire means we'll be off by 1
        int capacity = capacitySemaphore.availablePermits();

        logger.debug("Capacity is {}", capacity);

        activeJobs = jobAccessor.getJobs(capacity);

        // nothing to do, we don't have any jobs to run
        if (activeJobs.size() == 0) {
          logger.debug("No jobs returned. Exiting run loop");
          return;
        }

        for (JobDescriptor jd : activeJobs) {
          logger.info("Submitting work for {}", jd);
          submitWork(jd);
          logger.info("Work submitted for {}", jd);
        }
      }
    } catch (Throwable t) {
      logger.error("Something really bad happened!  Scheduler run failed", t);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.common.util.concurrent.AbstractScheduledService#scheduler()
   */
  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(0, interval, TimeUnit.MILLISECONDS);
  }

  /**
   * Use the provided BulkJobFactory to build and submit BulkJob items as
   * ListenableFuture objects
   * 
   * @param jobDescriptor
   */
  @ExceptionMetered(name = "BulkJobScheduledService_submitWork_exceptions", group = "scheduler")
  private void submitWork(final JobDescriptor jobDescriptor) {
    List<Job> jobs;

    try {
      jobs = jobFactory.jobsFrom(jobDescriptor);
    } catch (JobNotFoundException e) {
      logger.error("Could not create jobs", e);
      return;
    }

    for (final Job job : jobs) {

      // job execution needs to be external to both the callback and the task.
      // This way regardless of any error we can
      // mark a job as failed if required
      final JobExecution execution = new JobExecutionImpl(jobDescriptor);

      ListenableFuture<Void> future = service.submit(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          capacitySemaphore.acquire();

          execution.start();

          // TODO wrap and throw specifically typed exception for onFailure,
          // needs jobId
          job.execute(execution);

          return null;

        }
      });

      Futures.addCallback(future, new FutureCallback<Void>() {
        @Override
        public void onSuccess(Void param) {
          logger.info("Successful completion of bulkJob {}", execution);
          if (execution.getStatus() == Status.IN_PROGRESS) {
            execution.completed();
          }
          jobAccessor.save(execution);
          capacitySemaphore.release();
        }

        @Override
        public void onFailure(Throwable throwable) {
          logger.error("Failed execution for bulkJob {}", throwable);
          // mark it as failed
          if (execution.getStatus() == Status.IN_PROGRESS) {
            execution.failed(maxFailCount);
          }

          // there's a retry delay, use it
          if (throwable instanceof JobExecutionException) {
            long retryDelay = ((JobExecutionException) throwable).getRetryTimeout();

            if (retryDelay > 0) {
              jobAccessor.delayRetry(execution, retryDelay);
              capacitySemaphore.release();
              return;
            }
          }

          jobAccessor.save(execution);
          capacitySemaphore.release();

        }
      });
    }
  }

  /**
   * @param milliseconds
   *          the milliseconds to set to wait if we didn't receive a job to run
   */
  public void setInterval(long milliseconds) {
    this.interval = milliseconds;
  }

  /**
   * @param listeners
   *          the listeners to set
   */
  public void setWorkerSize(int listeners) {
    this.workerSize = listeners;
  }

  /**
   * @param jobAccessor
   *          the jobAccessor to set
   */
  public void setJobAccessor(JobAccessor jobAccessor) {
    this.jobAccessor = jobAccessor;
  }

  /**
   * @param jobFactory
   *          the jobFactory to set
   */
  public void setJobFactory(JobFactory jobFactory) {
    this.jobFactory = jobFactory;
  }

  /**
   * @param maxFailCount
   *          the maxFailCount to set
   */
  public void setMaxFailCount(int maxFailCount) {
    this.maxFailCount = maxFailCount;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.common.util.concurrent.AbstractScheduledService#startUp()
   */
  @Override
  protected void startUp() throws Exception {
    service = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(workerSize));
    capacitySemaphore = new Semaphore(workerSize);
    super.startUp();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.common.util.concurrent.AbstractScheduledService#shutDown()
   */
  @Override
  protected void shutDown() throws Exception {
    service.shutdown();
    super.shutDown();
  }
}
