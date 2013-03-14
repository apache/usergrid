package org.usergrid.batch.service;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.yammer.metrics.annotation.ExceptionMetered;
import com.yammer.metrics.annotation.Timed;

/**
 * @author zznate
 * @author tnine
 */
public class JobSchedulerService extends AbstractScheduledService {
  private Logger logger = LoggerFactory.getLogger(JobSchedulerService.class);

  private int interval = 1000;
  private int workerSize = 1;
  private ListeningExecutorService service;

  private JobAccessor jobAccessor;
  private JobFactory jobFactory;
  private AtomicInteger runningJobs = null;

  public JobSchedulerService() {
  }

  @Timed(name = "BulkJobScheduledService_runOneIteration", group = "scheduler", durationUnit = TimeUnit.MILLISECONDS, rateUnit = TimeUnit.MINUTES)
  @Override
  protected void runOneIteration() throws Exception {
    logger.info("running iteration...");
    try {
      List<JobDescriptor> activeJobs = jobsFor();
      for (JobDescriptor jd : activeJobs) {
        logger.info("Submitting work for {}", jd);
        submitWork(jd);
        logger.info("Work submitted for {}", jd);
      }
    } catch (Exception ex) {
      logger.error("Exception thrown in iteration of scheduled service", ex);
    }

  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedRateSchedule(0, interval, TimeUnit.MILLISECONDS);
  }

  @Override
  protected void startUp() throws Exception {
    super.startUp();
    service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(workerSize));
    runningJobs = new AtomicInteger(workerSize);
  }

  @Override
  protected void shutDown() throws Exception {
    super.shutDown();
    service.shutdown();
  }

  private List<JobDescriptor> jobsFor() {
    List<JobDescriptor> actives = jobAccessor.getJobs(workerSize - runningJobs.get());
    return actives;
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
      
      ListenableFuture<JobExecution> bulkJobExecFuture = service.submit(new Callable<JobExecution>() {
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
      Futures.addCallback(bulkJobExecFuture, new FutureCallback<JobExecution>() {
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
            JobExecution execution =((JobExecutionException) throwable).getExecution();
            execution.failed();
            jobAccessor.save(execution);
          }
          
          
          runningJobs.decrementAndGet();
        }
      });
    }
  }

  /**
   * @param milliseconds the milliseconds to set
   */
  public void setInterval(int milliseconds) {
    this.interval = milliseconds;
  }

  /**
   * @param listeners the listeners to set
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

}
