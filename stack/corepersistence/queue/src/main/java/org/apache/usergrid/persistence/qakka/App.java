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

package org.apache.usergrid.persistence.qakka;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.actorsystem.ActorSystemManager;
import org.apache.usergrid.persistence.core.migration.schema.MigrationException;
import org.apache.usergrid.persistence.core.migration.schema.MigrationManager;
import org.apache.usergrid.persistence.qakka.core.Queue;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.qakka.distributed.impl.QueueActorRouterProducer;
import org.apache.usergrid.persistence.qakka.distributed.impl.QueueSenderRouterProducer;
import org.apache.usergrid.persistence.qakka.distributed.impl.QueueWriterRouterProducer;
import org.apache.usergrid.persistence.qakka.exceptions.QakkaRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Akka queueing application
 */
@Singleton
public class App implements MetricsService {
    private static final Logger logger = LoggerFactory.getLogger( App.class );

    // TODO: can we avoid this kludge with better Akka-Guice integration?
    static public Injector INJECTOR;

    private final ActorSystemFig          actorSystemFig;
    private final ActorSystemManager      actorSystemManager;
    private final DistributedQueueService distributedQueueService;
    private final MetricRegistry          metrics = new MetricRegistry();


    @Inject
    public App(
            Injector                  injector,
            QakkaFig                  qakkaFig,
            ActorSystemFig            actorSystemFig,
            ActorSystemManager        actorSystemManager,
            DistributedQueueService   distributedQueueService,
            MigrationManager          migrationManager) {

        this.INJECTOR = injector;
        this.actorSystemFig = actorSystemFig;
        this.actorSystemManager = actorSystemManager;
        this.distributedQueueService = distributedQueueService;

        if ( qakkaFig.getStandalone() ) {

            try {
                migrationManager.migrate();
            } catch (MigrationException e) {
                throw new QakkaRuntimeException( "Error running migration", e );
            }
            actorSystemManager.registerRouterProducer( injector.getInstance( QueueActorRouterProducer.class ) );
            actorSystemManager.registerRouterProducer( injector.getInstance( QueueWriterRouterProducer.class ) );
            actorSystemManager.registerRouterProducer( injector.getInstance( QueueSenderRouterProducer.class ) );
        }
    }

    /**
     * Init Akka ActorSystems and wait for request actors to init.
     */
    public void start() {
        start(
            actorSystemFig.getHostname(),
            Integer.parseInt(actorSystemFig.getPort()), // TODO: make port an int in Actor System module
            actorSystemFig.getRegionLocal());
    }

    /**
     * For testing purposes only; does not wait for request actors to init.
     */
    public void start( String h, Integer p, String r ) {
        actorSystemManager.start( h, p, r );
        actorSystemManager.waitForClientActor();
        distributedQueueService.init();
    }


    @Override
    public MetricRegistry getMetricRegistry() {
        return metrics;
    }
}
