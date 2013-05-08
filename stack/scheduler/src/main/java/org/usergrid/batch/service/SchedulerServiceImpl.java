/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.batch.service;

import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.usergrid.batch.JobExecution;
import org.usergrid.batch.JobExecution.Status;
import org.usergrid.batch.JobExecutionImpl;
import org.usergrid.batch.JobRuntimeException;
import org.usergrid.batch.repository.JobAccessor;
import org.usergrid.batch.repository.JobDescriptor;
import org.usergrid.mq.Message;
import org.usergrid.mq.QueueManager;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.mq.QueueQuery;
import org.usergrid.mq.QueueResults;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.SimpleEntityRef;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.entities.JobData;
import org.usergrid.persistence.entities.JobStat;
import org.usergrid.persistence.exceptions.TransactionNotFoundException;

/**
 * Should be referenced by services as a SchedulerService instance. Only the
 * internal job runtime should refer to this as a JobAccessor
 * 
 * @author tnine
 * 
 */
public class SchedulerServiceImpl implements SchedulerService, JobAccessor, JobRuntimeService {

  /**
   * 
   */
  private static final String STATS_ID = "statsId";

  /**
   * 
   */
  private static final String JOB_ID = "jobId";

  /**
   * 
   */
  private static final String JOB_NAME = "jobName";

  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);

  private static final String DEFAULT_QUEUE_NAME = "/jobs";

  private QueueManagerFactory qmf;
  private EntityManagerFactory emf;

  private String jobQueueName = DEFAULT_QUEUE_NAME;

  private QueueManager qm;
  private EntityManager em;

  /**
   * Timeout for how long to set the transaction timeout from the queue. Default
   * is 30000
   */
  private long jobTimeout = 30000;

  /**
   * 
   */
  public SchedulerServiceImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.batch.service.SchedulerService#createJob(java.lang.String,
   * long, org.usergrid.persistence.Entity)
   */
  @Override
  public JobData createJob(String jobName, long fireTime, JobData jobData) {
    Assert.notNull(jobName, "jobName is required");
    Assert.notNull(jobData, "jobData is required");

    try {
      jobData.setJobName(jobName);
      JobData job = em.create(jobData);
      JobStat stat = em.create(new JobStat(jobName, job.getUuid()));

      scheduleJob(jobName, fireTime, job.getUuid(), stat.getUuid());

      return job;

    } catch (Exception e) {
      throw new JobRuntimeException(e);
    }

  }

  /**
   * Schedule the job internally
   * 
   * @param fireTime
   * @param jobName
   * @param jobDataId
   */
  private void scheduleJob(String jobName, long fireTime, UUID jobDataId, UUID jobStatId) {
    Assert.notNull(jobName, "jobName is required");
    Assert.isTrue(fireTime > -1, "fireTime must be positive");
    Assert.notNull(jobDataId, "jobDataId is required");
    Assert.notNull(jobStatId, "jobStatId is required");

    Message message = new Message();
    message.setTimestamp(fireTime);
    message.setStringProperty(JOB_NAME, jobName);
    message.setProperty(JOB_ID, jobDataId);
    message.setProperty(STATS_ID, jobStatId);

    qm.postToQueue(jobQueueName, message);

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.batch.service.SchedulerService#deleteJob(java.util.UUID)
   */
  @Override
  public void deleteJob(UUID jobId) {
    /**
     * just delete our target job data. This is easier than attempting to delete
     * from the queue. The runner should catch this and treat the queued message
     * as discarded
     */
    try {
      em.delete(new SimpleEntityRef("jobData", jobId));
    } catch (Exception e) {
      throw new JobRuntimeException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.batch.repository.JobAccessor#getJobs(int)
   */
  @Override
  public List<JobDescriptor> getJobs(int size) {
    QueueQuery query = new QueueQuery();
    query.setTimeout(jobTimeout);
    query.setLimit(size);

    QueueResults jobs = qm.getFromQueue(jobQueueName, query);

    List<JobDescriptor> results = new ArrayList<JobDescriptor>(jobs.size());

    for (Message job : jobs.getMessages()) {

      UUID jobUuid = UUID.fromString(job.getStringProperty(JOB_ID));
      UUID statsUuid = UUID.fromString(job.getStringProperty(STATS_ID));
      String jobName = job.getStringProperty(JOB_NAME);

      try {
        JobData data = em.get(jobUuid, JobData.class);

        JobStat stats = em.get(statsUuid, JobStat.class);

        /**
         * no job data, which is required even if empty to signal the job should
         * still fire. Ignore this job
         */
        if (data == null || stats == null) {
          logger.info("Received job with data id '{}' from the queue, but no data was found.  Dropping job", jobUuid);
          qm.deleteTransaction(jobQueueName, job.getTransaction(), null);

          if (data != null) {
            em.delete(data);
          }

          if (stats != null) {
            em.delete(stats);
          }

          continue;
        }

        results.add(new JobDescriptor(jobName, job.getUuid(), job.getTransaction(), data, stats, this));

      } catch (Exception e) {
        // log and skip. This is a catastrophic runtime error if we see an
        // exception here. We don't want to cause job loss, so leave the job in
        // the Q.
        logger.error("Unable to retrieve job data for jobname {}, job id {}, stats id {}.  Skipping to avoid job loss",
            new Object[] { jobName, jobUuid, statsUuid, e });
        continue;

      }

    }

    return results;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.batch.service.SchedulerService#heartbeat(org.usergrid.batch
   * .JobExecution)
   */
  @Override
  public void heartbeat(JobExecutionImpl execution) {
    try {
      UUID newId = qm.renewTransaction(jobQueueName, execution.getTransactionId(),
          new QueueQuery().withTimeout(jobTimeout));

      execution.setTransactionId(newId);
    } catch (TransactionNotFoundException e) {
      logger.error("Could not renew transaction", e);
      throw new JobRuntimeException("Could not renew transaction during heartbeat", e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.batch.service.SchedulerService#delay(org.usergrid.batch.
   * JobExecutionImpl)
   */
  @Override
  public void delay(JobExecutionImpl execution) {

    delayRetry(execution, execution.getDelay());

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.batch.repository.JobAccessor#save(org.usergrid.batch.JobExecution
   * )
   */
  @Override
  public void save(JobExecution bulkJobExecution) {

    JobData data = bulkJobExecution.getJobData();
    JobStat stat = bulkJobExecution.getJobStats();

    Status jobStatus = bulkJobExecution.getStatus();
    
    try {

      // we're done. Mark the transaction as complete and delete the job info
      if (jobStatus == Status.COMPLETED) {
        qm.deleteTransaction(jobQueueName, bulkJobExecution.getTransactionId(), null);
        em.delete(data);
      }
      
      // the job failed too many times. Delete the transaction to prevent it
      // running again and save it for querying later
      else if (jobStatus == Status.DEAD) {
        qm.deleteTransaction(jobQueueName, bulkJobExecution.getTransactionId(), null);
        em.update(data);
      }
      
      // update the job for the next run
      else {
        em.update(data);
      }
      
      em.update(stat);
      
    } catch (Exception e) {
      // should never happen
      throw new JobRuntimeException(String.format("Unable to delete job data with id %s", data.getUuid()), e);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.batch.service.SchedulerService#queryJobData(org.usergrid.
   * persistence.Query)
   */
  @Override
  public Results queryJobData(Query query) throws Exception {

    if (query == null) {
      query = new Query();
    }

    return em.searchCollection(em.getApplicationRef(), "job_data", query);

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.batch.repository.JobAccessor#delayRetry(org.usergrid.batch
   * .JobExecution, long)
   */
  @Override
  public void delayRetry(JobExecution execution, long delay) {

    JobData data = execution.getJobData();
    JobStat stat = execution.getJobStats();

    try {

      // if it's a dead status, it's failed too many times, just kill the job
      if (execution.getStatus() == Status.DEAD) {
        qm.deleteTransaction(jobQueueName, execution.getTransactionId(), null);
        em.update(data);
        em.update(stat);
        return;
      }

      // re-schedule the job to run again in the future
      scheduleJob(execution.getJobName(), System.currentTimeMillis() + delay, data.getUuid(), stat.getUuid());

      // delete the pending transaction
      qm.deleteTransaction(jobQueueName, execution.getTransactionId(), null);

      // update the data for the next run

      em.update(data);
      em.update(stat);
    } catch (Exception e) {
      // should never happen
      throw new JobRuntimeException(String.format("Unable to delete job data with id %s", data.getUuid()), e);
    }

  }

  /* (non-Javadoc)
   * @see org.usergrid.batch.service.SchedulerService#getStatsForJob(java.lang.String, java.util.UUID)
   */
  @Override
  public JobStat getStatsForJob(String jobName, UUID jobId) throws Exception {
    EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
    
    
    Query query = new Query();
    query.addEqualityFilter(JOB_NAME, jobName);
    query.addEqualityFilter(JOB_ID, jobId);
    
    Results r =  em.searchCollection(em.getApplicationRef(), "job_stats", query);
    
    if(r.size() == 1){
      return (JobStat) r.getEntity();
    }
    
    return null;
  }

  @PostConstruct
  public void init() {
    qm = qmf.getQueueManager(CassandraService.MANAGEMENT_APPLICATION_ID);
    em = emf.getEntityManager(CassandraService.MANAGEMENT_APPLICATION_ID);

  }

  /**
   * @param qmf
   *          the qmf to set
   */
  @Autowired
  public void setQmf(QueueManagerFactory qmf) {
    this.qmf = qmf;
  }

  /**
   * @param emf
   *          the emf to set
   */
  @Autowired
  public void setEmf(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /**
   * @param jobQueueName
   *          the jobQueueName to set
   */
  public void setJobQueueName(String jobQueueName) {
    this.jobQueueName = jobQueueName;
  }

  /**
   * @param timeout
   *          the timeout to set
   */
  public void setJobTimeout(long timeout) {
    this.jobTimeout = timeout;
  }

}
