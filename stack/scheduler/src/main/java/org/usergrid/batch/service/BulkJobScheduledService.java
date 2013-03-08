package org.usergrid.batch.service;

import com.google.common.util.concurrent.*;
import com.yammer.metrics.annotation.ExceptionMetered;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.batch.BulkJob;
import org.usergrid.batch.BulkJobExecution;
import org.usergrid.batch.BulkJobExecutionException;
import org.usergrid.batch.BulkJobsBuilder;
import org.usergrid.batch.repository.JobAccessor;
import org.usergrid.batch.repository.JobDescriptor;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author zznate
 */
public class BulkJobScheduledService extends AbstractScheduledService {
  private Logger logger = LoggerFactory.getLogger(BulkJobScheduledService.class);

  private int seconds = 10;
  private int listeners = 1;
  private ListeningExecutorService service;

  private final JobAccessor jobAccessor;

  public BulkJobScheduledService(JobAccessor jobAccessor) {
    this.jobAccessor = jobAccessor;
  }

  @Timed(name = "BulkJobScheduledService_runOneIteration",
          group = "scheduler",
          durationUnit = TimeUnit.MILLISECONDS,
          rateUnit = TimeUnit.MINUTES)
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
    return Scheduler.newFixedRateSchedule(0, seconds, TimeUnit.SECONDS);
  }

  @Override
  protected void startUp() throws Exception {
    super.startUp();
    service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(listeners));
  }

  @Override
  protected void shutDown() throws Exception {
    super.shutDown();
    service.shutdown();
  }

  private List<JobDescriptor> jobsFor() {
    List<JobDescriptor> actives = jobAccessor.activeJobs();
    return actives;
  }

  /**
   * Use the provided BulkJobFactory to build and submit BulkJob items as
   * ListenableFuture objects
   * @param jobDescriptor
   */
  @ExceptionMetered(name = "BulkJobScheduledService_submitWork_exceptions", group = "scheduler")
  private void submitWork(final JobDescriptor jobDescriptor) {
    // TODO add additional builder parameters
    List<BulkJob> jobs = BulkJobsBuilder.newBuilder(jobDescriptor).build();
    for ( final BulkJob job : jobs ) {
      ListenableFuture<BulkJobExecution> bulkJobExecFuture = service.submit(new Callable<BulkJobExecution>() {
        @Override
        public BulkJobExecution call() throws Exception {
          // TODO wrap and throw specifically typed exception for onFailure, needs jobId
          return job.execute(jobDescriptor);
        }
      });
      Futures.addCallback(bulkJobExecFuture, new FutureCallback<BulkJobExecution>() {
        @Override
        public void onSuccess(BulkJobExecution bulkJobExecution) {
          logger.info("Successful completion of bulkJob {}", bulkJobExecution);
          jobAccessor.save(bulkJobExecution);
        }

        @Override
        public void onFailure(Throwable throwable) {
          logger.error("Failed execution for bulkJob {}", throwable);
          if ( throwable instanceof BulkJobExecutionException) {
            jobAccessor.save(((BulkJobExecutionException)throwable).getBulkJobExecution());
          }
        }
      });
    }
  }
}
