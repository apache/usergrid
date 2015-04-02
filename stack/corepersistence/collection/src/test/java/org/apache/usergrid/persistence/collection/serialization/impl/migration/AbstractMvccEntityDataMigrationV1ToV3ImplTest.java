/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.collection.serialization.impl.migration;


import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.CollectionDataVersions;
import org.apache.usergrid.persistence.core.guice.DataMigrationResetRule;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationManager;
import org.apache.usergrid.persistence.core.migration.data.MigrationDataProvider;
import org.apache.usergrid.persistence.core.migration.data.MigrationRelationship;
import org.apache.usergrid.persistence.core.migration.data.TestProgressObserver;
import org.apache.usergrid.persistence.core.migration.data.VersionedMigrationSet;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import net.jcip.annotations.NotThreadSafe;

import rx.Observable;

import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@NotThreadSafe
@RunWith( ITRunner.class )
@UseModules( { TestCollectionModule.class } )
public abstract class AbstractMvccEntityDataMigrationV1ToV3ImplTest implements DataMigrationResetRule.DataMigrationManagerProvider {


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    public DataMigrationManager dataMigrationManager;


    @Inject
    public MvccEntityDataMigrationImpl mvccEntityDataMigrationImpl;


    @Inject
    public VersionedMigrationSet<MvccEntitySerializationStrategy> versions;

    /**
     * Rule to do the resets we need
     */
    @Rule
    public DataMigrationResetRule migrationTestRule =
        new DataMigrationResetRule( this, CollectionMigrationPlugin.PLUGIN_NAME,
            CollectionDataVersions.INITIAL.getVersion() );


    @Test
    public void testMigration() throws ConnectionException {

        final Id applicationId = createId( "application" );

        ApplicationScope scope = new ApplicationScopeImpl( applicationId );

        final MvccEntity entity1 = getEntity( "thing" );
        final MvccEntity entity2 = getEntity( "thing" );


        MvccEntitySerializationStrategy v1Impl = getExpectedSourceImpl();

        MvccEntitySerializationStrategy v3Impl = getExpectedTargetImpl();


        v1Impl.write( scope, entity1 ).execute();
        v1Impl.write( scope, entity2 ).execute();


        MvccEntity returned1 = v1Impl.load( scope, entity1.getId() ).get();
        MvccEntity returned2 = v1Impl.load( scope, entity2.getId() ).get();

        assertEquals( "Same entity", entity1, returned1 );
        assertEquals( "Same entity", entity2, returned2 );

        final Observable<EntityIdScope> entityIdScope =
            Observable.just( new EntityIdScope( scope, entity1.getId() ), new EntityIdScope( scope, entity2.getId() ) );


        final MigrationDataProvider<EntityIdScope> migrationProvider = new MigrationDataProvider<EntityIdScope>() {
            @Override
            public Observable<EntityIdScope> getData() {
                return entityIdScope;
            }
        };

        final TestProgressObserver progressObserver = new TestProgressObserver();

        final CollectionDataVersions startVersion = getSourceVersion();

        final MigrationRelationship<MvccEntitySerializationStrategy> tuple =
                  versions.getMigrationRelationship( startVersion.getVersion() );


        assertEquals( "Same instance for from", v1Impl.getClass(), tuple.from.getClass() );
        assertEquals( "Same instance for to", v3Impl.getClass(), tuple.to.getClass() );

        //now migration
        final int newVersion = mvccEntityDataMigrationImpl
            .migrate( startVersion.getVersion(), migrationProvider, progressObserver );


        final CollectionDataVersions expectedVersion = expectedTargetVersion();

        assertEquals( "Correct version returned", newVersion, expectedVersion.getVersion() );
        assertFalse( "Progress observer should not have failed", progressObserver.isFailed() );
        assertTrue( "Progress observer should have update messages", progressObserver.getUpdates().size() > 0 );


        //now verify we can read the data correctly in the new version
        returned1 = v3Impl.load( scope, entity1.getId() ).get();
        returned2 = v3Impl.load( scope, entity2.getId() ).get();

        assertEquals( "Same entity", entity1, returned1 );
        assertEquals( "Same entity", entity2, returned2 );

        //verify the tuple is correct

        final MigrationRelationship<MvccEntitySerializationStrategy> newTuple =
            versions.getMigrationRelationship( newVersion );


        assertEquals( "Same instance for from", v3Impl.getClass(), newTuple.from.getClass() );
        assertEquals( "Same instance for to", v3Impl.getClass(), newTuple.to.getClass() );
    }


    private MvccEntity getEntity( final String type ) {

        final SimpleId entityId = new SimpleId( type );
        final UUID version = UUIDGenerator.newTimeUUID();
        final Entity entity = new Entity( entityId );

        MvccEntityImpl logEntry = new MvccEntityImpl( entityId, version, MvccEntity.Status.COMPLETE, entity );


        return logEntry;
    }


    @Override
    public DataMigrationManager getDataMigrationManager() {
        return dataMigrationManager;
    }


    /**
     * Get the expected source mvcc implementation for this test
     * @return
     */
    protected abstract MvccEntitySerializationStrategy getExpectedSourceImpl();

    /**
     * Get the expected target mvcc for this test
     * @return
     */
    protected abstract MvccEntitySerializationStrategy getExpectedTargetImpl();

    /**
     * Get the expected start version
     * @return
     */
    protected abstract CollectionDataVersions getSourceVersion();

    /**
     *
     * @return
     */
    protected abstract CollectionDataVersions expectedTargetVersion();
}
