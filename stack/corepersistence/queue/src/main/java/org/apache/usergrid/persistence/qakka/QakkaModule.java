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

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;
import com.netflix.config.ConfigurationManager;
import org.apache.usergrid.persistence.core.migration.schema.Migration;
import org.apache.usergrid.persistence.qakka.api.URIStrategy;
import org.apache.usergrid.persistence.qakka.api.impl.URIStrategyLocalhost;
import org.apache.usergrid.persistence.qakka.core.*;
import org.apache.usergrid.persistence.qakka.core.impl.QueueManagerImpl;
import org.apache.usergrid.persistence.qakka.core.impl.QueueMessageManagerImpl;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.qakka.distributed.actors.QueueActorHelper;
import org.apache.usergrid.persistence.qakka.distributed.impl.DistributedQueueServiceImpl;
import org.apache.usergrid.persistence.qakka.distributed.impl.QueueActorRouterProducer;
import org.apache.usergrid.persistence.qakka.distributed.impl.QueueSenderRouterProducer;
import org.apache.usergrid.persistence.qakka.distributed.impl.QueueWriterRouterProducer;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLogSerialization;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.impl.AuditLogSerializationImpl;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.QueueMessageSerialization;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.impl.QueueMessageSerializationImpl;
import org.apache.usergrid.persistence.qakka.serialization.queues.QueueSerialization;
import org.apache.usergrid.persistence.qakka.serialization.queues.impl.QueueSerializationImpl;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardCounterSerialization;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardSerialization;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardStrategy;
import org.apache.usergrid.persistence.qakka.serialization.sharding.impl.ShardCounterSerializationImpl;
import org.apache.usergrid.persistence.qakka.serialization.sharding.impl.ShardSerializationImpl;
import org.apache.usergrid.persistence.qakka.serialization.sharding.impl.ShardStrategyImpl;
import org.apache.usergrid.persistence.qakka.serialization.transferlog.TransferLogSerialization;
import org.apache.usergrid.persistence.qakka.serialization.transferlog.impl.TransferLogSerializationImpl;
import org.safehaus.guicyfig.GuicyFigModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class QakkaModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger( QakkaModule.class );

    static {
        try {
            // TODO: reconcile with usergrid props
            // load properties from one properties file using Netflix Archaius so that GuicyFig will see them
            ConfigurationManager.loadCascadedPropertiesFromResources( "qakka" );
        } catch (IOException e) {
            logger.warn("Unable to load qakka.properties");
        }
    }

    @Override
    protected void configure() {

        install( new GuicyFigModule( QakkaFig.class ) );

        bind( App.class );

        bind( CassandraClient.class ).to(           CassandraClientImpl.class );
        bind( MetricsService.class ).to(            App.class );

        bind( QueueManager.class ).to(              QueueManagerImpl.class );
        bind( QueueSerialization.class ).to(        QueueSerializationImpl.class );

        bind( QueueMessageManager.class ).to(       QueueMessageManagerImpl.class );
        bind( QueueMessageSerialization.class ).to( QueueMessageSerializationImpl.class );

        bind( ShardSerialization.class ).to(        ShardSerializationImpl.class );
        bind( ShardStrategy.class ).to(             ShardStrategyImpl.class );

        bind( ShardCounterSerialization.class ).to( ShardCounterSerializationImpl.class );

        bind( TransferLogSerialization.class ).to(  TransferLogSerializationImpl.class );
        bind( AuditLogSerialization.class ).to(     AuditLogSerializationImpl.class );
        bind( DistributedQueueService.class ).to(   DistributedQueueServiceImpl.class );

        bind( QueueActorRouterProducer.class );
        bind( QueueWriterRouterProducer.class );
        bind( QueueSenderRouterProducer.class );
        bind( QueueActorHelper.class );

        bind( Regions.class );
        bind( URIStrategy.class ).to( URIStrategyLocalhost.class );

        Multibinder<Migration> migrationBinder = Multibinder.newSetBinder( binder(), Migration.class );

        migrationBinder.addBinding().to( Key.get( AuditLogSerialization.class ) );
        //migrationBinder.addBinding().to( Key.get( MessageCounterSerialization.class ) );
        migrationBinder.addBinding().to( Key.get( QueueMessageSerialization.class ) );
        migrationBinder.addBinding().to( Key.get( QueueSerialization.class ) );
        migrationBinder.addBinding().to( Key.get( ShardCounterSerialization.class ) );
        migrationBinder.addBinding().to( Key.get( ShardSerialization.class ) );
        migrationBinder.addBinding().to( Key.get( TransferLogSerialization.class ) );
    }
}
