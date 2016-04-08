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
package org.apache.usergrid.batch.job;


import java.util.concurrent.TimeUnit;

import com.google.inject.Injector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.usergrid.batch.Job;
import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.locking.Lock;
import org.apache.usergrid.locking.LockManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;


/**
 * Simple abstract job class that performs additional locking to ensure that the job is only executing once. This can be
 * used if your job could potentially be too slow to invoke JobExceution.heartbeat() before the timeout passes.
 *
 * @author tnine
 */
@Component("OnlyOnceJob")
public abstract class OnlyOnceJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(OnlyOnceJob.class);

    private LockManager lockManager;

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private Injector injector;


    /**
     *
     */
    public OnlyOnceJob() {
    }

    @PostConstruct
    public void initLockManager() throws Exception {
        this.lockManager = injector.getInstance(LockManager.class);
        if (this.lockManager != null) {
            logger.info("LockManager injection successful");
        } else {
            logger.error("LockManager injection unsuccessful");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.batch.Job#execute(org.apache.usergrid.batch.JobExecution)
     */
    @Override
    public void execute( JobExecution execution ) throws Exception {

        String lockId = execution.getJobId().toString();
        logger.info("Executing one-time job {}, LockManager is {}", lockId, lockManager == null ? "null" : "not null");

        Lock lock = lockManager.createLock( emf.getManagementAppId(), String.format( "/jobs/%s", lockId ) );

        // the job is still running somewhere else. Try again in getDelay() milliseconds
        if ( !lock.tryLock( 0, TimeUnit.MILLISECONDS ) ) {
            execution.delay( getDelay( execution ) );
            return;
        }

        //if we get here we can proceed.  Make sure we unlock no matter what.
        try {

            doJob( execution );
        }
        finally {
            lock.unlock();
        }
    }


    /** Delegate the job execution to the subclass */
    protected abstract void doJob( JobExecution execution ) throws Exception;

    /** Get the delay for the next run if we can't acquire the lock */
    protected abstract long getDelay( JobExecution execution ) throws Exception;
}
