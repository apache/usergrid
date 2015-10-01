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
package org.apache.usergrid.services;


import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.apache.usergrid.batch.service.SchedulerService;
import org.apache.usergrid.locking.LockManager;
import org.apache.usergrid.mq.QueueManager;
import org.apache.usergrid.mq.QueueManagerFactory;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;


public class ServiceManagerFactory implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private EntityManagerFactory emf;
    private Properties properties;
    private SchedulerService schedulerService;
    private LockManager lockManager;
    private QueueManagerFactory qmf;

    private List<ServiceExecutionEventListener> eventListeners;
    private List<ServiceCollectionEventListener> collectionListeners;


    public ServiceManagerFactory( EntityManagerFactory emf, Properties properties, SchedulerService schedulerService,
                                  LockManager lockManager, QueueManagerFactory qmf ) {
        this.emf = emf;
        this.properties = properties;
        this.schedulerService = schedulerService;
        this.lockManager = lockManager;
        this.qmf = qmf;
    }


    public ServiceManager getServiceManager( UUID applicationId ) {
        EntityManager em = null;
        if ( emf != null ) {
            em = emf.getEntityManager( applicationId );
        }
        QueueManager qm = null;
        if ( qmf != null ) {
            qm = qmf.getQueueManager( applicationId );
        }
        ServiceManager sm = new ServiceManager();
        sm.init( this, em, properties, qm );
        return sm;
    }


    public List<ServiceExecutionEventListener> getExecutionEventListeners() {
        return eventListeners;
    }


    public void setExecutionEventListeners( List<ServiceExecutionEventListener> eventListeners ) {
        this.eventListeners = eventListeners;
    }


    public void notifyExecutionEventListeners( ServiceAction action, ServiceRequest request, ServiceResults results,
                                               ServicePayload payload ) {
        notifyExecutionEventListeners( new ServiceExecutionEvent( action, request, results, payload ) );
    }


    public void notifyExecutionEventListeners( ServiceExecutionEvent event ) {
        if ( eventListeners != null ) {
            for ( ServiceExecutionEventListener listener : eventListeners ) {
                if ( listener != null ) {
                    listener.serviceExecuted( event );
                }
            }
        }
    }


    public void notifyCollectionEventListeners( String path, ServiceResults results ) {
        if ( collectionListeners != null ) {
            for ( ServiceCollectionEventListener listener : collectionListeners ) {
                if ( listener != null ) {
                    listener.collectionModified( path, results );
                }
            }
        }
    }


    @Override
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        this.applicationContext = applicationContext;
    }


    /** @return the applicationContext */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }


    public SchedulerService getSchedulerService() {
        return schedulerService;
    }


    public LockManager getLockManager() {
        return lockManager;
    }

    public UUID getManagementAppId() {
        return emf.getManagementAppId();
    }

}
