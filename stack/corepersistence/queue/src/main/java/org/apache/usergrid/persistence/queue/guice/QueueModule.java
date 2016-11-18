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


import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.apache.usergrid.persistence.qakka.QakkaModule;
import org.apache.usergrid.persistence.queue.*;
import org.apache.usergrid.persistence.queue.impl.QakkaQueueManager;
import org.apache.usergrid.persistence.queue.impl.QueueManagerFactoryImpl;
import org.apache.usergrid.persistence.queue.impl.SNSQueueManagerImpl;
import org.safehaus.guicyfig.GuicyFigModule;

import java.util.Properties;


/**
 * Simple module for wiring our collection api
 */
public class QueueModule extends AbstractModule {

    private LegacyQueueManager.Implementation implementation;

    public QueueModule( String queueManagerType ) {

        if ( "LOCAL".equals( queueManagerType ) ) {
            this.implementation = LegacyQueueManager.Implementation.LOCAL;
        }
        else if ( "DISTRIBUTED_SNS".equals( queueManagerType ) ) {
            this.implementation = LegacyQueueManager.Implementation.DISTRIBUTED_SNS;
        }
        else if ( "DISTRIBUTED".equals( queueManagerType ) ) {
            this.implementation = LegacyQueueManager.Implementation.DISTRIBUTED;
        }
    }


    @Override
    protected void configure() {

        install(new GuicyFigModule(LegacyQueueFig.class));

        bind(LegacyQueueManagerFactory.class).to(QueueManagerFactoryImpl.class);

        switch (implementation) {

            case LOCAL:

                install( new FactoryModuleBuilder().implement( LegacyQueueManager.class, LocalQueueManager.class )
                    .build( LegacyQueueManagerInternalFactory.class ) );
                break;

            case DISTRIBUTED_SNS:
                install( new FactoryModuleBuilder().implement( LegacyQueueManager.class, SNSQueueManagerImpl.class )
                    .build( LegacyQueueManagerInternalFactory.class ) );
                break;

            case DISTRIBUTED:
                install( new FactoryModuleBuilder().implement( LegacyQueueManager.class, QakkaQueueManager.class )
                    .build( LegacyQueueManagerInternalFactory.class ) );
                break;

            default:
                throw new IllegalArgumentException(
                    "Queue implemetation value of " + implementation + " not allowed");

        }

        install( new QakkaModule() );
    }
}
