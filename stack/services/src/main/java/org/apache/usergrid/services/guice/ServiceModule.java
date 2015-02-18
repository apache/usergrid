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
package org.apache.usergrid.services.guice;


import org.apache.usergrid.corepersistence.CoreModule;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.services.ServiceManager;
import org.apache.usergrid.services.ServiceManagerFactory;
import org.apache.usergrid.services.queues.ImportQueueListener;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;


/**
 * Module that handles all of the guice injects for services.
 */


/**
 *   <bean id="notificationsQueueListener" class="org.apache.usergrid.services.notifications.QueueListener"
 scope="singleton">
 <constructor-arg name="emf" ref="entityManagerFactory" />
 <constructor-arg name="metricsService" ref="metricsFactory" />
 <constructor-arg name="props" ref="properties" />
 <constructor-arg name="smf" ref="serviceManagerFactory" />
 </bean>
 */

public class ServiceModule extends AbstractModule {
    @Override
    protected void configure() {

        //TODO: why not just make the ImportQueueListener inject the ServiceManager instead?
        //TODO: I don't know why we need to do the same with emf. We could just inject it like the CoreModule does.

       // install( new CoreModule() );


        //Seems weird, aren't we just binding the factory to the exact same factory when it goes to look for it?
        bind( ServiceManagerFactory.class );
        bind( EntityManagerFactory.class );




    }
}
