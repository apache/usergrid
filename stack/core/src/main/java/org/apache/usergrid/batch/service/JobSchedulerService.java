/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.batch.service;


import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.inject.Injector;
import org.apache.usergrid.corepersistence.CpSetup;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.batch.Job;
import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.batch.JobExecution.Status;
import org.apache.usergrid.batch.JobExecutionImpl;
import org.apache.usergrid.batch.JobFactory;
import org.apache.usergrid.batch.JobNotFoundException;
import org.apache.usergrid.batch.repository.JobAccessor;
import org.apache.usergrid.batch.repository.JobDescriptor;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;


/**
 * Service that schedules itself, then schedules jobs in the same pool
 */
public class JobSchedulerService extends AbstractScheduledService {
    protected static final long DEFAULT_DELAY = 1000;

    private static final Logger LOG = LoggerFactory.getLogger( JobSchedulerService.class );

    private long interval = DEFAULT_DELAY;
    private int workerSize = 1;
    private int maxFailCount = 10;

    private JobAccessor jobAccessor;
    private JobFactory jobFactory;

    private Semaphore capacitySemaphore;

    private ListeningScheduledExecutorService service;
    private JobListener jobListener;

    private Timer jobTimer;
    private Counter runCounter;
    private Counter successCounter;
    private Counter failCounter;

    private Injector injector;

    //TODO Add meters for throughput of start and stop


    public JobSchedulerService() { }


    @Override
    protected void runOneIteration() throws Exception {

        MetricsFactory metricsFactory = injector.getInstance( MetricsFactory.class );

        jobTimer = metricsFactory.getTimer( JobSchedulerService.class, "scheduler.job_execution_timer" );
        runCounter = metricsFactory.getCounter( JobSchedulerService.class, "scheduler.running_workers" );
        successCounter = metricsFactory.getCounter( JobSchedulerService.class, "scheduler.successful_jobs" );
        failCounter = metricsFactory.getCounter( JobSchedulerService.class, "scheduler.failed_jobs" );

        try {
            LOG.info( "Running one check iteration ..." );
            List<JobDescriptor> activeJobs;

            // run until there are no more active jobs
            while ( true ) {

                // get the semaphore if we can. This means we have space for at least 1
                // job
                if ( LOG.isDebugEnabled() ) {
                    LOG.debug( "About to acquire semaphore.  Capacity is {}", capacitySemaphore.availablePermits() );
                }

                capacitySemaphore.acquire();
                // release the sempaphore we only need to acquire as a way to stop the
                // loop if there's no capacity
                capacitySemaphore.release();

                int capacity = capacitySemaphore.availablePermits();

                LOG.debug( "Capacity is {}", capacity );

                activeJobs = jobAccessor.getJobs( capacity );

                // nothing to do, we don't have any jobs to run
                if ( activeJobs.size() == 0 ) {
                    LOG.debug( "No jobs returned. Exiting run loop" );
                    return;
                }

                for ( JobDescriptor jd : activeJobs ) {
                    LOG.info( "Submitting work for {}", jd );
                    submitWork( jd );
                    LOG.info( "Work submitted for {}", jd );
                }
            }
        }
        catch ( Throwable t ) {
            LOG.debug( "Scheduler run failed, error is", t );
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see com.google.common.util.concurrent.AbstractScheduledService#scheduler()
     */
    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedDelaySchedule( 0, interval, TimeUnit.MILLISECONDS );
    }


    /**
     * Use the provided BulkJobFactory to build and submit BulkJob items as ListenableFuture objects
     */
    private void submitWork( final JobDescriptor jobDescriptor ) {
        final Job job;

        try {
            job = jobFactory.jobsFrom( jobDescriptor );
        }
        catch ( JobNotFoundException e ) {
            LOG.error( "Could not create jobs", e );
            return;
        }


        // job execution needs to be external to both the callback and the task.
        // This way regardless of any error we can
        // mark a job as failed if required
        final JobExecution execution = new JobExecutionImpl( jobDescriptor );

        // We don't care if this is atomic (not worth using a lock object)
        // we just need to prevent NPEs from ever occurring
        final JobListener currentListener = this.jobListener;

        /**
         * Acquire the semaphore before we schedule.  This way we wont' take things from the Q that end up
         * stuck in the queue for the scheduler and then time out their distributed heartbeat
         */
        try {
            capacitySemaphore.acquire();
        }
        catch ( InterruptedException e ) {
            LOG.error( "Unable to acquire semaphore capacity before submitting job", e );
            //just return, they'll get picked up again later
            return;
        }


        final Timer.Context timer = jobTimer.time();


        ListenableFuture<Void> future = service.submit( new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                LOG.debug( "Starting the job with job id {}", execution.getJobId() );
                runCounter.inc();

                execution.start( maxFailCount );


                //this job is dead, treat it as such
                if ( execution.getStatus() == Status.DEAD ) {

                    try {
                        job.dead( execution );
                        jobAccessor.save( execution );
                    }
                    catch ( Exception t ) {
                        //we purposefully swallow all exceptions here, we don't want it to effect the outcome
                        //of finally popping this job from the queue
                        LOG.error( "Unable to invoke dead event on job", t );
                    }

                    return null;
                }

                jobAccessor.save( execution );

                // TODO wrap and throw specifically typed exception for onFailure,
                // needs jobId

                LOG.info( "Starting job {} with execution data {}", job, execution );

                job.execute( execution );

                if ( currentListener != null ) {
                    currentListener.onSubmit( execution );
                }

                return null;
            }
        } );

        Futures.addCallback( future, new FutureCallback<Void>() {
            @Override
            public void onSuccess( Void param ) {

                /**
                 * Release semaphore first in case there are other problems with communicating with Cassandra
                 */

                LOG.debug( "Job succeeded with the job id {}", execution.getJobId() );
                capacitySemaphore.release();
                timer.stop();
                runCounter.dec();
                successCounter.inc();


                //TODO, refactor into the execution itself for checking if done
                if ( execution.getStatus() == Status.IN_PROGRESS ) {
                    LOG.info( "Successful completion of bulkJob {}", execution );
                    execution.completed();
                }

                jobAccessor.save( execution );


                if ( currentListener != null ) {
                    currentListener.onSuccess( execution );
                }
            }


            @Override
            public void onFailure( Throwable throwable ) {

                /**
                 * Release semaphore first in case there are other problems with communicating with Cassandra
                 */
                LOG.error( "Job failed with the job id {}", execution.getJobId() );
                capacitySemaphore.release();
                timer.stop();
                runCounter.dec();
                failCounter.inc();


                LOG.error( "Failed execution for bulkJob", throwable );
                // mark it as failed
                if ( execution.getStatus() == Status.IN_PROGRESS ) {
                    execution.failed();
                }

                jobAccessor.save( execution );


                if ( currentListener != null ) {
                    currentListener.onFailure( execution );
                }
            }
        } );
    }


    /**
     * @param milliseconds the milliseconds to set to wait if we didn't receive a job to run
     */
    public void setInterval( long milliseconds ) {
        this.interval = milliseconds;
    }


    public long getInterval() {
        return interval;
    }


    /**
     * @param listeners the listeners to set
     */
    public void setWorkerSize( int listeners ) {
        this.workerSize = listeners;
    }


    public int getWorkerSize() {
        return workerSize;
    }


    /**
     * @param jobAccessor the jobAccessor to set
     */
    public void setJobAccessor( JobAccessor jobAccessor ) {
        this.jobAccessor = jobAccessor;
    }


    /**
     * @param jobFactory the jobFactory to set
     */
    public void setJobFactory( JobFactory jobFactory ) {
        this.jobFactory = jobFactory;
    }


    /**
     * @param maxFailCount the maxFailCount to set
     */
    public void setMaxFailCount( int maxFailCount ) {
        this.maxFailCount = maxFailCount;
    }


    /**
     * Set the metrics factory
     */
//    public void setMetricsFactory( MetricsFactory metricsFactory ) {
//
//        jobTimer = metricsFactory.getTimer( JobSchedulerService.class, "job_execution_timer" );
//        runCounter = metricsFactory.getCounter( JobSchedulerService.class, "running_workers" );
//        successCounter = metricsFactory.getCounter( JobSchedulerService.class, "successful_jobs" );
//        failCounter = metricsFactory.getCounter( JobSchedulerService.class, "failed_jobs" );
//    }


    /*
     * (non-Javadoc)
     *
     * @see com.google.common.util.concurrent.AbstractScheduledService#startUp()
     */
    @Override
    protected void startUp() throws Exception {
        service = MoreExecutors
                .listeningDecorator( Executors.newScheduledThreadPool( workerSize, JobThreadFactory.INSTANCE ) );
        capacitySemaphore = new Semaphore( workerSize );

        LOG.info( "Starting executor pool.  Capacity is {}", workerSize );

        super.startUp();

        LOG.info( "Job Scheduler started" );
    }


    /*
     * (non-Javadoc)
     *
     * @see com.google.common.util.concurrent.AbstractScheduledService#shutDown()
     */
    @Override
    protected void shutDown() throws Exception {
        LOG.info( "Shutting down job scheduler" );

        service.shutdown();

        LOG.info( "Job scheduler shut down" );
        super.shutDown();
    }


    /**
     * Sets the JobListener notified of Job events on this SchedulerService.
     *
     * @param jobListener the listener to receive Job events
     *
     * @return the previous listener if set, or null if none was set
     */
    public JobListener setJobListener( JobListener jobListener ) {
        JobListener old = this.jobListener;
        this.jobListener = jobListener;
        return old;
    }


    /**
     * Gets the current JobListener to be notified of Job events on this SchedulerService.
     *
     * @return the current JobListener or null if none was set
     */
    public JobListener getJobListener() {
        return jobListener;
    }

    public void setInjector(Injector injector) {
        this.injector = injector;
    }


    /**
     * Simple factory for labeling job worker threads for easier debugging
     */
    private static final class JobThreadFactory implements ThreadFactory {

        public static final JobThreadFactory INSTANCE = new JobThreadFactory();

        private static final String NAME = "JobWorker-";
        private final AtomicLong counter = new AtomicLong();


        @Override
        public Thread newThread( final Runnable r ) {

            Thread newThread = new Thread( r, NAME + counter.incrementAndGet() );
            newThread.setDaemon( true );

            return newThread;
        }
    }
}
