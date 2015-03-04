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

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.CollectionDataVersions;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccEntitySerializationStrategyV1Impl;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccEntitySerializationStrategyV3Impl;
import org.apache.usergrid.persistence.core.guice.DataMigrationResetRule;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationManager;
import org.apache.usergrid.persistence.core.migration.data.TestProgressObserver;
import org.apache.usergrid.persistence.core.migration.data.newimpls.MigrationDataProvider;
import org.apache.usergrid.persistence.core.migration.data.newimpls.MigrationRelationship;
import org.apache.usergrid.persistence.core.migration.data.newimpls.VersionedMigrationSet;
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;


@NotThreadSafe
@RunWith( ITRunner.class )
@UseModules( { TestCollectionModule.class } )
public class MvccEntityDataMigrationV1ToV3ImplTest implements DataMigrationResetRule.DataMigrationManagerProvider {

    @Inject
    public DataMigrationManager dataMigrationManager;

    @Inject
    private MvccEntitySerializationStrategyV1Impl v1Impl;

    @Inject
    private MvccEntitySerializationStrategyV3Impl v3Impl;

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

        final Id applicationId = createId("application");
        final String collectionName = "things";

        CollectionScope scope = new CollectionScopeImpl(applicationId, applicationId, collectionName );

        final MvccEntity entity1 = getEntity( "thing" );
        final MvccEntity entity2 = getEntity( "thing" );

        v1Impl.write( scope, entity1 ).execute();
        v1Impl.write( scope, entity2 ).execute();


        MvccEntity returned1 = v1Impl.load( scope, entity1.getId() ).get();
        MvccEntity returned2 = v1Impl.load( scope, entity2.getId() ).get();

        assertEquals("Same entity", entity1, returned1);
        assertEquals("Same entity", entity2, returned2);

        final Observable<EntityIdScope> entityIdScope = Observable.from( new EntityIdScope( scope, entity1.getId() ), new EntityIdScope( scope, entity2.getId() ) );


        final MigrationDataProvider<EntityIdScope> migrationProvider = new MigrationDataProvider<EntityIdScope>() {
            @Override
            public Observable<EntityIdScope> getData() {
                return entityIdScope;
            }
        };

        final TestProgressObserver progressObserver = new TestProgressObserver();

        //now migration
        final int newVersion = mvccEntityDataMigrationImpl.migrate( CollectionDataVersions.INITIAL.getVersion(), migrationProvider, progressObserver  );


        assertEquals( "Correct version returned", newVersion, CollectionDataVersions.LOG_REMOVAL.getVersion() );
        assertFalse( "Progress observer should not have failed", progressObserver.getFailed() );
        assertTrue( "Progress observer should have update messages", progressObserver.getUpdates().size() > 0 );


        //now verify we can read the data correctly in the new version
        returned1 = v3Impl.load( scope, entity1.getId() ).get();
           returned2 = v3Impl.load( scope, entity2.getId() ).get();

           assertEquals("Same entity", entity1, returned1);
           assertEquals("Same entity", entity2, returned2);

        //verify the tuple is correct

        final MigrationRelationship<MvccEntitySerializationStrategy>
            tuple = versions.getMigrationRelationship( newVersion );


        assertSame("Same instance for from", v1Impl, tuple.from);
        assertSame("Same instance for to", v3Impl, tuple.to);



    }


    private MvccEntity getEntity(final String type){

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
}
