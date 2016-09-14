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
import com.google.inject.Key;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import org.apache.usergrid.persistence.actorsystem.ActorSystemModule;
import org.apache.usergrid.persistence.core.guice.CommonModule;
import org.apache.usergrid.persistence.core.migration.schema.Migration;
import org.apache.usergrid.persistence.qakka.App;
import org.apache.usergrid.persistence.qakka.MetricsService;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.QakkaModule;
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
import org.apache.usergrid.persistence.queue.LegacyQueueFig;
import org.apache.usergrid.persistence.queue.LegacyQueueManager;
import org.apache.usergrid.persistence.queue.LegacyQueueManagerFactory;
import org.apache.usergrid.persistence.queue.LegacyQueueManagerInternalFactory;
import org.apache.usergrid.persistence.queue.impl.QakkaQueueManager;
import org.apache.usergrid.persistence.queue.impl.QueueManagerFactoryImpl;
import org.apache.usergrid.persistence.queue.impl.SNSQueueManagerImpl;
import org.safehaus.guicyfig.GuicyFigModule;


/**
 * Simple module for wiring our collection api
 *
 * @author tnine
 */
public class QueueModule extends AbstractModule {


    @Override
    protected void configure() {

        install(new GuicyFigModule(LegacyQueueFig.class));

        bindQakka();

        bind(LegacyQueueManagerFactory.class).to(QueueManagerFactoryImpl.class);
        install( new FactoryModuleBuilder().implement(LegacyQueueManager.class, QakkaQueueManager.class)
            .build(LegacyQueueManagerInternalFactory.class));

    }

    private void bindQakka() {

        install( new CommonModule() );
        install( new ActorSystemModule() );
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
