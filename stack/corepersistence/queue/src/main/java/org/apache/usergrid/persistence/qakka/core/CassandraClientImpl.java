/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.qakka.core;

import com.datastax.driver.core.Session;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.core.datastax.DataStaxCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
public class CassandraClientImpl implements CassandraClient {
    private static final Logger logger = LoggerFactory.getLogger( CassandraClientImpl.class );

    private final DataStaxCluster dataStaxCluster;
    private Session applicationSession = null;
    private Session queueMessageSession = null;


    @Inject
    public CassandraClientImpl( DataStaxCluster dataStaxCluster) {
        logger.info("Constructing Cassandra client");
        this.dataStaxCluster = dataStaxCluster;
    }


    @Override
    public synchronized Session getApplicationSession() {
        if ( applicationSession == null || applicationSession.isClosed() ) {
            applicationSession = dataStaxCluster.getApplicationSession();
        }
        return applicationSession;
    }


    @Override
    public synchronized Session getQueueMessageSession() {
        if ( queueMessageSession == null || queueMessageSession.isClosed() ) {
            queueMessageSession = dataStaxCluster.getApplicationLocalSession();
        }
        return queueMessageSession;
    }
}
