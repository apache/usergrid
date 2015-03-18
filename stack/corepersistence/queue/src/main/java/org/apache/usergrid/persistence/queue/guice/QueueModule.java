/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.queue.guice;


import org.safehaus.guicyfig.GuicyFigModule;

import org.apache.usergrid.persistence.queue.QueueFig;
import org.apache.usergrid.persistence.queue.QueueManager;
import org.apache.usergrid.persistence.queue.QueueManagerFactory;
import org.apache.usergrid.persistence.queue.impl.SQSQueueManagerImpl;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;


/**
 * Simple module for wiring our collection api
 *
 * @author tnine
 */
public class QueueModule extends AbstractModule {


    @Override
    protected void configure() {


        install( new GuicyFigModule( QueueFig.class) );

        // create a guice factory for getting our collection manager
        install( new FactoryModuleBuilder().implement( QueueManager.class, SQSQueueManagerImpl.class )
                                           .build( QueueManagerFactory.class ) );

    }



}


