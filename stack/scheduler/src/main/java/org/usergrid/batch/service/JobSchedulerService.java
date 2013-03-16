package org.usergrid.batch.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.batch.Job;
import org.usergrid.batch.JobExecution;
import org.usergrid.batch.JobExecutionException;
import org.usergrid.batch.JobFactory;
import org.usergrid.batch.JobNotFoundException;
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

  private final AtomicInteger runningJobs = new AtomicInteger();

  private long interval = DEFAULT_DELAY;
  private int workerSize = 1;

  private JobAccessor jobAccessor;
  private JobFactory jobFactory;

  private ListeningScheduledExecutorService service;

  public JobSchedulerService() {
  }

  @Timed(name = "BulkJobScheduledService_runOneIteration", group = "scheduler", durationUnit = TimeUnit.MILLISECONDS, rateUnit = TimeUnit.MINUTES)
  @Override
  protected void runOneIteration() throws Exception {
    logger.info("running iteration...");
    List<JobDescriptor> activeJobs = null;
  
    // run until there are no more active jobs
    while (true) {

      int capacity = getCapacity();
      
      logger.debug("capacity = {}", capacity);
      
      // nothing to do, exit the loop, we don't have capacity to run any more jobs
      if (capacity == 0) {
        break;
      }

      activeJobs = jobAccessor.getJobs(capacity);
     
     
      // nothing to do, we don't have any jobs to run
      if (activeJobs.size() == 0) {
        break;
      }

      for (JobDescriptor jd : activeJobs) {
        logger.info("Submitting work for {}", jd);
        submitWork(jd);
        logger.info("Work submitted for {}", jd);
      }
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
   * Get the number of workers we can submit
   * @return
   */
  private int getCapacity(){
    return workerSize - runningJobs.get();
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

      ListenableFuture<JobExecution> future = service.submit(new Callable<JobExecution>() {
        @Override
        public JobExecution call() throws Exception {
          runningJobs.incrementAndGet();

          JobExecution execution = new JobExecution(jobDescriptor);

          // TODO wrap and throw specifically typed exception for onFailure,
          // needs jobId
          job.execute(execution);

          return execution;

        }
      });

      Futures.addCallback(future, new FutureCallback<JobExecution>() {
        @Override
        public void onSuccess(JobExecution execution) {
          logger.info("Successful completion of bulkJob {}", execution);
          execution.completed();
          jobAccessor.save(execution);
          runningJobs.decrementAndGet();
        }

        @Override
        public void onFailure(Throwable throwable) {
          logger.error("Failed execution for bulkJob {}", throwable);
          if (throwable instanceof JobExecutionException) {
            JobExecution execution = ((JobExecutionException) throwable).getExecution();
            execution.failed();
            jobAccessor.save(execution);
          }

          runningJobs.decrementAndGet();
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

  /*
   * (non-Javadoc)
   * 
   * @see com.google.common.util.concurrent.AbstractScheduledService#startUp()
   */
  @Override
  protected void startUp() throws Exception {
    service = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(workerSize));
    super.startUp();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.common.util.concurrent.AbstractScheduledService#shutDown()
   */
  @Override
  protected void shutDown() throws Exception {
    super.shutDown();
  }
}
