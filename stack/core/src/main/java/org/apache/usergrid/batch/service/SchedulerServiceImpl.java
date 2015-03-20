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


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import com.google.inject.Injector;
import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.batch.JobExecution.Status;
import org.apache.usergrid.batch.JobRuntime;
import org.apache.usergrid.batch.JobRuntimeException;
import org.apache.usergrid.batch.repository.JobAccessor;
import org.apache.usergrid.batch.repository.JobDescriptor;
import org.apache.usergrid.mq.Message;
import org.apache.usergrid.mq.QueueManager;
import org.apache.usergrid.mq.QueueManagerFactory;
import org.apache.usergrid.mq.QueueQuery;
import org.apache.usergrid.mq.QueueResults;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.entities.JobData;
import org.apache.usergrid.persistence.entities.JobStat;
import org.apache.usergrid.persistence.exceptions.TransactionNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Should be referenced by services as a SchedulerService instance. Only the internal job
 * runtime should refer to this as a JobAccessor
 */
public class SchedulerServiceImpl implements SchedulerService, JobAccessor, JobRuntimeService {

    private static final String STATS_ID = "statsId";

    private static final String JOB_ID = "jobId";

    private static final String JOB_NAME = "jobName";

    private static final Logger LOG = LoggerFactory.getLogger( SchedulerServiceImpl.class );

    private static final String DEFAULT_QUEUE_NAME = "/jobs";

    private QueueManagerFactory qmf;
    private EntityManagerFactory emf;

    private String jobQueueName = DEFAULT_QUEUE_NAME;

    private QueueManager qm;
    private EntityManager em;

    /** Timeout for how long to set the transaction timeout from the queue. Default is 30000 */
    private long jobTimeout = 30000;
    private Injector injector;
    private EntityIndex entityIndex;


    /**
     *
     */
    public SchedulerServiceImpl() {
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.batch.service.SchedulerService#createJob(java.lang.String,
     * long, org.apache.usergrid.persistence.Entity)
     */
    @Override
    public JobData createJob( String jobName, long fireTime, JobData jobData ) {
        Assert.notNull( jobName, "jobName is required" );
        Assert.notNull( jobData, "jobData is required" );

        try {
            jobData.setJobName( jobName );
            JobData job = getEm().create( jobData );
            JobStat stat = getEm().create( new JobStat( jobName, job.getUuid() ) );

            scheduleJob( jobName, fireTime, job.getUuid(), stat.getUuid() );

            return job;
        }
        catch ( Exception e ) {
            throw new JobRuntimeException( e );
        }
    }


    /** Schedule the job internally */
    private void scheduleJob( String jobName, long fireTime, UUID jobDataId, UUID jobStatId ) {
        Assert.notNull( jobName, "jobName is required" );
        Assert.isTrue( fireTime > -1, "fireTime must be positive" );
        Assert.notNull( jobDataId, "jobDataId is required" );
        Assert.notNull( jobStatId, "jobStatId is required" );

        Message message = new Message();
        message.setTimestamp( fireTime );
        message.setStringProperty( JOB_NAME, jobName );
        message.setProperty( JOB_ID, jobDataId.toString() );
        message.setProperty( STATS_ID, jobStatId.toString() );

        getQm().postToQueue( jobQueueName, message );
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.batch.service.SchedulerService#deleteJob(java.util.UUID)
     */
    @Override
    public void deleteJob( UUID jobId ) {
        /**
         * just delete our target job data. This is easier than attempting to delete
         * from the queue. The runner should catch this and treat the queued message
         * as discarded
         */
        try {
            LOG.debug( "deleteJob {}", jobId );
            getEm().delete( new SimpleEntityRef(
                Schema.getDefaultSchema().getEntityType(JobData.class), jobId ) );
        }
        catch ( Exception e ) {
            throw new JobRuntimeException( e );
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.batch.repository.JobAccessor#getJobs(int)
     */
    @Override
    public List<JobDescriptor> getJobs( int size ) {
        QueueQuery query = new QueueQuery();
        query.setTimeout( jobTimeout );
        query.setLimit( size );

        QueueResults jobs = getQm().getFromQueue( jobQueueName, query );

        List<JobDescriptor> results = new ArrayList<JobDescriptor>( jobs.size() );

        for ( Message job : jobs.getMessages() ) {

            Object jo = job.getStringProperty( JOB_ID );

            UUID jobUuid = UUID.fromString( job.getStringProperty( JOB_ID ) );
            UUID statsUuid = UUID.fromString( job.getStringProperty( STATS_ID ) );
            String jobName = job.getStringProperty( JOB_NAME );

            try {
                JobData data = getEm().get( jobUuid, JobData.class );

                JobStat stats = getEm().get( statsUuid, JobStat.class );

                /**
                 * no job data, which is required even if empty to signal the job should
                 * still fire. Ignore this job
                 */
                if ( data == null || stats == null ) {
                    LOG.info( "Received job with data id '{}' from the queue, but no data was found.  Dropping job",
                            jobUuid );
                    getQm().deleteTransaction( jobQueueName, job.getTransaction(), null );

                    if ( data != null ) {
                        getEm().delete( data );
                    }

                    if ( stats != null ) {
                        getEm().delete( stats );
                    }

                    continue;
                }

                results.add( new JobDescriptor( jobName, job.getUuid(), job.getTransaction(), data, stats, this ) );
            }
            catch ( Exception e ) {
                // log and skip. This is a catastrophic runtime error if we see an
                // exception here. We don't want to cause job loss, so leave the job in
                // the Q.
                LOG.error(
                        "Unable to retrieve job data for jobname {}, job id {}, stats id {}.  Skipping to avoid job "
                                + "loss", new Object[] { jobName, jobUuid, statsUuid, e } );
            }
        }

        return results;
    }


    @Override
    public void heartbeat( JobRuntime execution, long delay ) {
        LOG.debug( "renew transaction {}", execution.getTransactionId() );
        try {
            // @TODO - what's the point to this sychronized block on an argument?
            synchronized ( execution ) {
                UUID newId = getQm().renewTransaction( jobQueueName, execution.getTransactionId(),
                        new QueueQuery().withTimeout( delay ) );

                execution.setTransactionId( newId );
                LOG.debug( "renewed transaction {}", newId );
            }
        }
        catch ( TransactionNotFoundException e ) {
            LOG.error( "Could not renew transaction", e );
            throw new JobRuntimeException( "Could not renew transaction during heartbeat", e );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.batch.service.JobRuntimeService#heartbeat(org.apache.usergrid.batch.JobRuntime)
     */
    @Override
    public void heartbeat( JobRuntime execution ) {
        heartbeat( execution, jobTimeout );
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.batch.service.SchedulerService#delay(org.apache.usergrid.batch.
     * JobExecutionImpl)
     */
    @Override
    public void delay( JobRuntime execution ) {
        delayRetry( execution.getExecution(), execution.getDelay() );
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.batch.repository.JobAccessor#save(org.apache.usergrid.batch.JobExecution
     * )
     */
    @Override
    public void save( JobExecution bulkJobExecution ) {

        JobData data = bulkJobExecution.getJobData();
        JobStat stat = bulkJobExecution.getJobStats();

        Status jobStatus = bulkJobExecution.getStatus();

        try {

            // we're done. Mark the transaction as complete and delete the job info
            if ( jobStatus == Status.COMPLETED ) {
                LOG.info( "Job {} is complete id: {}", data.getJobName(), bulkJobExecution.getTransactionId() );
                getQm().deleteTransaction( jobQueueName, bulkJobExecution.getTransactionId(), null );
                LOG.debug( "delete job data {}", data.getUuid() );
                getEm().delete( data );
            }

            // the job failed too many times. Delete the transaction to prevent it
            // running again and save it for querying later
            else if ( jobStatus == Status.DEAD ) {
                LOG.warn( "Job {} is dead.  Removing", data.getJobName() );
                getQm().deleteTransaction( jobQueueName, bulkJobExecution.getTransactionId(), null );
                getEm().update( data );
            }

            // update the job for the next run
            else {
                getEm().update( data );
            }

            LOG.info( "Updating stats for job {}", data.getJobName() );

            getEm().update( stat );
        }
        catch ( Exception e ) {
            // should never happen
            throw new JobRuntimeException( String.format( "Unable to delete job data with id %s", data.getUuid() ), e );
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.batch.service.SchedulerService#queryJobData(org.apache.usergrid.
     * persistence.Query)
     */
    @Override
    public Results queryJobData( Query query ) throws Exception {

        if ( query == null ) {
            query = new Query();
        }

        String jobDataType = Schema.getDefaultSchema().getEntityType(JobData.class);

        return getEm().searchCollection( getEm().getApplicationRef(),
                Schema.defaultCollectionName(jobDataType), query );
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.batch.repository.JobAccessor#delayRetry(org.apache.usergrid.batch
     * .JobExecution, long)
     */
    @Override
    public void delayRetry( JobExecution execution, long delay ) {

        JobData data = execution.getJobData();
        JobStat stat = execution.getJobStats();

        try {

            // if it's a dead status, it's failed too many times, just kill the job
            if ( execution.getStatus() == Status.DEAD ) {
                getQm().deleteTransaction( jobQueueName, execution.getTransactionId(), null );
                getEm().update( data );
                getEm().update( stat );
                return;
            }

            // re-schedule the job to run again in the future
            scheduleJob( execution.getJobName(), System.currentTimeMillis() + delay, data.getUuid(), stat.getUuid() );

            // delete the pending transaction
            getQm().deleteTransaction( jobQueueName, execution.getTransactionId(), null );

            // update the data for the next run

            getEm().update( data );
            getEm().update( stat );
        }
        catch ( Exception e ) {
            // should never happen
            throw new JobRuntimeException( String.format( "Unable to delete job data with id %s", data.getUuid() ), e );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.batch.service.SchedulerService#getStatsForJob(java.lang.String, java.util.UUID)
     */
    @Override
    public JobStat getStatsForJob( String jobName, UUID jobId ) throws Exception {
        EntityManager em = emf.getEntityManager( emf.getManagementAppId() );


        Query query = new Query();
        query.addEqualityFilter( JOB_NAME, jobName );
        query.addEqualityFilter( JOB_ID, jobId );

        Results r = em.searchCollection( em.getApplicationRef(), "job_stats", query );

        if ( r.size() == 1 ) {
            return ( JobStat ) r.getEntity();
        }

        return null;
    }


    /** @param qmf the qmf to set */
    @Autowired
    public void setQmf( QueueManagerFactory qmf ) {
        this.qmf = qmf;
    }


    /** @param emf the emf to set */
    @Autowired
    public void setEmf( EntityManagerFactory emf ) {
        this.emf = emf;
    }

    /** @param injector **/
    @Autowired
    public void setInjector( Injector injector){ this.injector = injector;}


    /** @param jobQueueName the jobQueueName to set */
    public void setJobQueueName( String jobQueueName ) {
        this.jobQueueName = jobQueueName;
    }


    /** @param timeout the timeout to set */
    public void setJobTimeout( long timeout ) {
        this.jobTimeout = timeout;
    }

    public QueueManager getQm() {
        if ( qm == null ) {
            this.qm = qmf.getQueueManager( emf.getManagementAppId());
        }
        return qm;
    }

    public EntityManager getEm() {
        if ( em == null  ) {
            this.em = emf.getEntityManager( emf.getManagementAppId() );
        }
        return em;
    }

    @Override
    public void refreshIndex() {
        this.entityIndex = entityIndex == null ? injector.getInstance(EntityIndex.class) : entityIndex;
        entityIndex.refresh();
    }
}
