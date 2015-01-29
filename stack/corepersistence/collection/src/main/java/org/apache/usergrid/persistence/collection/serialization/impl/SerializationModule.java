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
package org.apache.usergrid.persistence.collection.serialization.impl;


import org.apache.usergrid.persistence.collection.mvcc.MvccEntityMigrationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.core.guice.*;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.core.migration.schema.Migration;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;


/**
 * @author tnine
 */
public class SerializationModule extends AbstractModule {

    @Override
    protected void configure() {


        // bind the serialization strategies

        //We've migrated this one, so we need to set up the previous, current, and proxy
        bind( MvccEntitySerializationStrategy.class ).annotatedWith( V1Impl.class )
                                                     .to( MvccEntitySerializationStrategyV1Impl.class );
        bind( MvccEntitySerializationStrategy.class ).annotatedWith( V2Impl.class )
                                                     .to(MvccEntitySerializationStrategyV2Impl.class);
        bind( MvccEntitySerializationStrategy.class ).annotatedWith( V3Impl.class )
                                                     .to(MvccEntitySerializationStrategyV3Impl.class);

        bind(MvccEntitySerializationStrategy.class).annotatedWith( V1ProxyImpl.class )
                                                     .to(MvccEntitySerializationStrategyProxyV1Impl.class);
        bind(MvccEntitySerializationStrategy.class ).annotatedWith( ProxyImpl.class )
                                                     .to(MvccEntitySerializationStrategyProxyV2Impl.class);

        Multibinder<DataMigration> dataMigrationMultibinder =
            Multibinder.newSetBinder( binder(), DataMigration.class );
        dataMigrationMultibinder.addBinding().to( MvccEntityDataMigrationImpl.class );

        bind( MvccEntityMigrationStrategy.class ).to(MvccEntitySerializationStrategyProxyV2Impl.class);

        bind( MvccLogEntrySerializationStrategy.class ).to( MvccLogEntrySerializationStrategyImpl.class );
        bind( UniqueValueSerializationStrategy.class ).to( UniqueValueSerializationStrategyImpl.class );

        //do multibindings for migrations
        Multibinder<Migration> uriBinder = Multibinder.newSetBinder( binder(), Migration.class );
        uriBinder.addBinding().to( Key.get( MvccEntitySerializationStrategy.class, V1Impl.class ) );
        uriBinder.addBinding().to( Key.get( MvccEntitySerializationStrategy.class, V2Impl.class ) );
        uriBinder.addBinding().to( Key.get( MvccEntitySerializationStrategy.class, V3Impl.class ) );
        uriBinder.addBinding().to( Key.get( MvccLogEntrySerializationStrategy.class ) );
        uriBinder.addBinding().to( Key.get( UniqueValueSerializationStrategy.class ) );


        //bind our settings as an eager singleton so it's checked on startup
        bind(SettingsValidation.class).asEagerSingleton();
    }
}
