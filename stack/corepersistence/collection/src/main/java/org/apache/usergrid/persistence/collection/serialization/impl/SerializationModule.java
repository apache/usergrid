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


import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.CollectionMigration;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.CollectionMigrationPlugin;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.MvccEntityDataMigrationImpl;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.core.migration.data.MigrationPlugin;
import org.apache.usergrid.persistence.core.migration.data.MigrationRelationship;
import org.apache.usergrid.persistence.core.migration.data.VersionedMigrationSet;
import org.apache.usergrid.persistence.core.migration.schema.Migration;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;


/**
 * @author tnine
 */
public class SerializationModule extends AbstractModule {

    @Override
    protected void configure() {


        // bind the serialization strategies


        //bind all 3 implementations
        bind( MvccEntitySerializationStrategyV1Impl.class );
        bind( MvccEntitySerializationStrategyV2Impl.class );
        bind( MvccEntitySerializationStrategyV3Impl.class );
        //We've migrated this one, so we need to set up the previous, current, and proxy


        bind( MvccEntitySerializationStrategy.class ).to( MvccEntitySerializationStrategyProxyImpl.class );


        //bind 2 implementations
        bind( MvccLogEntrySerializationStrategyV1Impl.class );
        bind( MvccLogEntrySerializationStrategyV2Impl.class );


        bind( MvccLogEntrySerializationStrategy.class ).to( MvccLogEntrySerializationProxyImpl.class );


        bind( UniqueValueSerializationStrategyV1Impl.class );
        bind(UniqueValueSerializationStrategyV2Impl.class);

        bind( UniqueValueSerializationStrategy.class ).to( UniqueValueSerializationStrategyProxyImpl.class );

        //do multibindings for migrations
        Multibinder<Migration> migrationBinder = Multibinder.newSetBinder( binder(), Migration.class );
        //entity serialization versions
        migrationBinder.addBinding().to( Key.get( MvccEntitySerializationStrategyV1Impl.class ) );
        migrationBinder.addBinding().to( Key.get( MvccEntitySerializationStrategyV2Impl.class ) );
        migrationBinder.addBinding().to( Key.get( MvccEntitySerializationStrategyV3Impl.class ) );

        //log serialization versions
        migrationBinder.addBinding().to( Key.get( MvccLogEntrySerializationStrategyV1Impl.class ) );
        migrationBinder.addBinding().to( Key.get( MvccLogEntrySerializationStrategyV2Impl.class ) );

        //unique value serialization versions
        migrationBinder.addBinding().to( Key.get( UniqueValueSerializationStrategyV1Impl.class ) );
        migrationBinder.addBinding().to( Key.get( UniqueValueSerializationStrategyV2Impl.class ) );


        //bind our settings as an eager singleton so it's checked on startup
        bind( SettingsValidation.class ).asEagerSingleton();

        //migrations
        //we want to make sure our generics are retained, so we use a typeliteral
        Multibinder<DataMigration> dataMigrationMultibinder = Multibinder
            .newSetBinder( binder(), new TypeLiteral<DataMigration>() {}, CollectionMigration.class );


        dataMigrationMultibinder.addBinding().to( MvccEntityDataMigrationImpl.class );


        //wire up the collection migration plugin
        Multibinder.newSetBinder( binder(), MigrationPlugin.class ).addBinding().to( CollectionMigrationPlugin.class );
    }


    /**
     * Configure via explicit declaration the migration path we can follow
     */
    @Singleton
    @Inject
    @Provides
    public VersionedMigrationSet<MvccEntitySerializationStrategy> getVersions(
        final MvccEntitySerializationStrategyV1Impl v1, final MvccEntitySerializationStrategyV2Impl v2,
        final MvccEntitySerializationStrategyV3Impl v3 ) {


        //we must perform a migration from v1 to v3 in order to maintain consistency
        MigrationRelationship<MvccEntitySerializationStrategy> v1Tov3 = new MigrationRelationship<>( v1, v3 );

        //we must migrate from 2 to 3, this is a bridge that must happen to maintain data consistency

        MigrationRelationship<MvccEntitySerializationStrategy> v2Tov3 = new MigrationRelationship<>( v2, v3 );


        //note that we MUST migrate to v3 before our next migration, if v4 and v5 is implemented we will need a
        // v3->v5 and a v4->v5 set
        MigrationRelationship<MvccEntitySerializationStrategy> current =
            new MigrationRelationship<>( v3, v3 );


        //now create our set of versions
        VersionedMigrationSet<MvccEntitySerializationStrategy> set =
            new VersionedMigrationSet<>( v1Tov3, v2Tov3, current );

        return set;
    }


    /**
     * Configure via explicit declaration the migration path we can follow
     */
    @Singleton
    @Inject
    @Provides
    public VersionedMigrationSet<MvccLogEntrySerializationStrategy> getVersions(
        final MvccLogEntrySerializationStrategyV1Impl v1, final MvccLogEntrySerializationStrategyV2Impl v2) {


        //we must perform a migration from v1 to v3 in order to maintain consistency
        MigrationRelationship<MvccLogEntrySerializationStrategy> v1Tov2 = new MigrationRelationship<>( v1, v2 );


        //note that we MUST migrate to v3 before our next migration, if v4 and v5 is implemented we will need a
        // v3->v5 and a v4->v5 set
        MigrationRelationship<MvccLogEntrySerializationStrategy> current =
            new MigrationRelationship<>( v2, v2 );


        //now create our set of versions
        VersionedMigrationSet<MvccLogEntrySerializationStrategy> set =
            new VersionedMigrationSet<>( v1Tov2, current );

        return set;
    }



    /**
     * Configure via explicit declaration the migration path we can follow
     */
    @Singleton
    @Inject
    @Provides
    public VersionedMigrationSet<UniqueValueSerializationStrategy> getVersions(
        final UniqueValueSerializationStrategyV1Impl v1, final UniqueValueSerializationStrategyV2Impl v2) {


        //we must perform a migration from v1 to v3 in order to maintain consistency
        MigrationRelationship<UniqueValueSerializationStrategy> v1Tov2 = new MigrationRelationship<>( v1, v2 );


        //note that we MUST migrate to v3 before our next migration, if v4 and v5 is implemented we will need a
        // v3->v5 and a v4->v5 set
        MigrationRelationship<UniqueValueSerializationStrategy> current =
            new MigrationRelationship<>( v2, v2 );


        //now create our set of versions
        VersionedMigrationSet<UniqueValueSerializationStrategy> set =
            new VersionedMigrationSet<>( v1Tov2, current );

        return set;
    }
}
