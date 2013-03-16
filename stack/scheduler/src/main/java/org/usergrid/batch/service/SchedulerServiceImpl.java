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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.batch.JobExecution;
import org.usergrid.batch.JobExecution.Status;
import org.usergrid.batch.JobExecutionException;
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
import org.usergrid.persistence.SimpleEntityRef;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.exceptions.TransactionNotFoundException;

/**
 * Should be referenced by services as a SchedulerService instance. Only the
 * internal job runtime should refer to this as a JobAccessor
 * 
 * @author tnine
 * 
 */
public class SchedulerServiceImpl implements SchedulerService, JobAccessor {

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

    JobData job = null;
    try {
      job = em.create(jobData);
    } catch (Exception e) {
      throw new JobRuntimeException(e);
    }

    Message message = new Message();
    message.setTimestamp(fireTime);
    message.setStringProperty("jobName", jobName);
    message.setProperty("jobId", job.getUuid());

    qm.postToQueue(jobQueueName, message);

    return job;

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

      UUID jobUuid = UUID.fromString(job.getStringProperty("jobId"));
      String jobName = job.getStringProperty("jobName"); 

      JobData data = null;
      try {
        data = em.get(jobUuid, JobData.class);
      } catch (Exception e) {
        // swallow
      }

      /**
       * no job data, which is required even if empty to signal the job should
       * still fire. Ignore this job
       */
      if (data == null) {
        logger.info("Received job with data id '{}' from the queue, but no data was found.  Dropping job", jobUuid);
        qm.deleteTransaction(jobQueueName, job.getTransaction(), null);
        continue;
      }

      results.add(new JobDescriptor(jobName, job.getUuid(), job.getTransaction(), data, this));
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
  public void heartbeat(JobExecution execution) throws JobExecutionException {
    try {
      UUID newId = qm.renewTransaction(jobQueueName, execution.getTransactionId(), new QueueQuery().withTimeout(jobTimeout));

      execution.setTransactionId(newId);
    } catch (TransactionNotFoundException e) {
      logger.error("Could not renew transaction", e);
      throw new JobExecutionException(execution, "Could not renew transaction during heartbeat", e);
    }
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
    if (bulkJobExecution.getStatus() == Status.COMPLETED) {
      qm.deleteTransaction(jobQueueName, bulkJobExecution.getTransactionId(), null);
    }
  }
  
  
  @PostConstruct
  public void init() {
    qm = qmf.getQueueManager(CassandraService.MANAGEMENT_APPLICATION_ID);
    em = emf.getEntityManager(CassandraService.MANAGEMENT_APPLICATION_ID);

  }
  


  /**
   * @param qmf the qmf to set
   */
  @Autowired
  public void setQmf(QueueManagerFactory qmf) {
    this.qmf = qmf;
  }

  /**
   * @param emf the emf to set
   */
  @Autowired
  public void setEmf(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /**
   * @param jobQueueName the jobQueueName to set
   */
  public void setJobQueueName(String jobQueueName) {
    this.jobQueueName = jobQueueName;
  }

  /**
   * @param timeout the timeout to set
   */
  public void setJobTimeout(long timeout) {
    this.jobTimeout = timeout;
  }


}
