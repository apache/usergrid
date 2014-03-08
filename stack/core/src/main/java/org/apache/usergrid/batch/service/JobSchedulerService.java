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
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.yammer.metrics.annotation.ExceptionMetered;
import com.yammer.metrics.annotation.Timed;
import java.util.HashMap;
import java.util.Map;

import org.apache.usergrid.batch.Job;
import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.batch.JobExecution.Status;
import org.apache.usergrid.batch.JobExecutionImpl;
import org.apache.usergrid.batch.JobFactory;
import org.apache.usergrid.batch.JobNotFoundException;
import org.apache.usergrid.batch.repository.JobAccessor;
import org.apache.usergrid.batch.repository.JobDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Service that schedules itself, then schedules jobs in the same pool
 */
public class JobSchedulerService extends AbstractScheduledService {
    protected static final long DEFAULT_DELAY = 1000;

    private static final Logger LOG = LoggerFactory.getLogger( JobSchedulerService.class );

    // keep track of exceptions thrown in scheduler so we can reduce noise in logs
    private Map<String, Integer> schedulerRunFailures = new HashMap<String, Integer>();

    private long interval = DEFAULT_DELAY;
    private int workerSize = 1;
    private int maxFailCount = 10;

    private JobAccessor jobAccessor;
    private JobFactory jobFactory;

    private Semaphore capacitySemaphore;

    private ListeningScheduledExecutorService service;
    private JobListener jobListener;

    public JobSchedulerService() { }


    @Timed(name = "BulkJobScheduledService_runOneIteration", group = "scheduler", durationUnit = TimeUnit.MILLISECONDS,
            rateUnit = TimeUnit.MINUTES)
    @Override
    protected void runOneIteration() throws Exception {

        try {
            LOG.info( "running iteration..." );
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

            // errors here happen a lot on shutdown, don't fill the logs with them
            String error = t.getClass().getCanonicalName();
            if (schedulerRunFailures.get( error ) == null) {
                LOG.error( "Scheduler run failed, first instance of this exception", t );
                schedulerRunFailures.put( error, 1);

            } else {
                int count = schedulerRunFailures.get(error) + 1; 
                schedulerRunFailures.put(error, count);
                if (LOG.isDebugEnabled()) {
                    LOG.debug( error + " caused scheduler run failure, count =  " + count, t );
                } else {
                    LOG.error( error + " caused scheduler run failure, count =  " + count );
                }
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
        return Scheduler.newFixedDelaySchedule( 0, interval, TimeUnit.MILLISECONDS );
    }


    /** Use the provided BulkJobFactory to build and submit BulkJob items as ListenableFuture objects */
    @ExceptionMetered(name = "BulkJobScheduledService_submitWork_exceptions", group = "scheduler")
    private void submitWork( final JobDescriptor jobDescriptor ) {
        List<Job> jobs;

        try {
            jobs = jobFactory.jobsFrom( jobDescriptor );
        }
        catch ( JobNotFoundException e ) {
            LOG.error( "Could not create jobs", e );
            return;
        }

        for ( final Job job : jobs ) {

            // job execution needs to be external to both the callback and the task.
            // This way regardless of any error we can
            // mark a job as failed if required
            final JobExecution execution = new JobExecutionImpl( jobDescriptor );

            // We don't care if this is atomic (not worth using a lock object)
            // we just need to prevent NPEs from ever occurring
            final JobListener currentListener = this.jobListener;

            ListenableFuture<Void> future = service.submit( new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    capacitySemaphore.acquire();

                    execution.start( maxFailCount );

                    jobAccessor.save( execution );

                    //this job is dead, treat it as such
                    if ( execution.getStatus() == Status.DEAD ) {
                        return null;
                    }

                    // TODO wrap and throw specifically typed exception for onFailure,
                    // needs jobId
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

                    if ( execution.getStatus() == Status.IN_PROGRESS ) {
                        LOG.info( "Successful completion of bulkJob {}", execution );
                        execution.completed();
                    }

                    jobAccessor.save( execution );
                    capacitySemaphore.release();

                    if ( currentListener != null ) {
                        currentListener.onSuccess( execution );
                    }
                }


                @Override
                public void onFailure( Throwable throwable ) {
                    LOG.error( "Failed execution for bulkJob", throwable );
                    // mark it as failed
                    if ( execution.getStatus() == Status.IN_PROGRESS ) {
                        execution.failed();
                    }

                    jobAccessor.save( execution );
                    capacitySemaphore.release();

                    if ( currentListener != null ) {
                        currentListener.onFailure( execution );
                    }
                }
            } );
        }
    }


    /** @param milliseconds the milliseconds to set to wait if we didn't receive a job to run */
    public void setInterval( long milliseconds ) {
        this.interval = milliseconds;
    }


    /** @param listeners the listeners to set */
    public void setWorkerSize( int listeners ) {
        this.workerSize = listeners;
    }


    /** @param jobAccessor the jobAccessor to set */
    public void setJobAccessor( JobAccessor jobAccessor ) {
        this.jobAccessor = jobAccessor;
    }


    /** @param jobFactory the jobFactory to set */
    public void setJobFactory( JobFactory jobFactory ) {
        this.jobFactory = jobFactory;
    }


    /** @param maxFailCount the maxFailCount to set */
    public void setMaxFailCount( int maxFailCount ) {
        this.maxFailCount = maxFailCount;
    }


    /*
     * (non-Javadoc)
     *
     * @see com.google.common.util.concurrent.AbstractScheduledService#startUp()
     */
    @Override
    protected void startUp() throws Exception {
        service = MoreExecutors.listeningDecorator( Executors.newScheduledThreadPool( workerSize ) );
        capacitySemaphore = new Semaphore( workerSize );
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


    /**
     * Sets the JobListener notified of Job events on this SchedulerService.
     *
     * @param jobListener the listener to receive Job events
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
}
