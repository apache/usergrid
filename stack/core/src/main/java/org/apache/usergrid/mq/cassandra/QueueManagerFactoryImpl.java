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
package org.apache.usergrid.mq.cassandra;


import java.util.UUID;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.locking.LockManager;
import org.apache.usergrid.mq.QueueManager;
import org.apache.usergrid.mq.QueueManagerFactory;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.CounterUtils;

import static org.apache.usergrid.persistence.cassandra.Serializers.*;


public class QueueManagerFactoryImpl implements QueueManagerFactory {

    public static final Logger logger = LoggerFactory.getLogger( QueueManagerFactoryImpl.class );

    public static String IMPLEMENTATION_DESCRIPTION = "Cassandra Queue Manager Factory 1.0";

    private CassandraService cass;
    private CounterUtils counterUtils;
    private LockManager lockManager;
    private int lockTimeout;

    /**
     * Must be constructed with a CassandraClientPool.
     *
     * @param cass the cassandra client pool
     * @param counterUtils the CounterUtils
     */
    public QueueManagerFactoryImpl(CassandraService cass, CounterUtils counterUtils, final Injector injector, int lockTimeout ) {
        this.cass = cass;
        this.counterUtils = counterUtils;
        lockManager = injector.getInstance(LockManager.class);
        this.lockTimeout = lockTimeout;
    }


    @Override
    public String getImpementationDescription() throws Exception {
        return IMPLEMENTATION_DESCRIPTION;
    }


    @Override
    public QueueManager getQueueManager( UUID applicationId ) {
        QueueManagerImpl qm = new QueueManagerImpl();
        qm.init( cass, counterUtils, lockManager, applicationId, lockTimeout );
        return qm;
        //return applicationContext.getAutowireCapableBeanFactory()
        //		.createBean(QueueManagerImpl.class)
        //		.init(this, cass, counterUtils, applicationId);
    }
}
