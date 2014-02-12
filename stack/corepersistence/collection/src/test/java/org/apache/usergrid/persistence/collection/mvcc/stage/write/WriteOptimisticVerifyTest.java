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
package org.apache.usergrid.persistence.collection.mvcc.stage.write;

import com.google.inject.Inject;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.AbstractMvccEntityStageTest;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import static org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator.fromEntity;
import static org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator.generateEntity;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Assert;
import static org.junit.Assert.assertSame;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith( JukitoRunner.class )
@UseModules( TestCollectionModule.class )
public class WriteOptimisticVerifyTest extends AbstractMvccEntityStageTest {

    @ClassRule
    public static CassandraRule cassandraRule = new CassandraRule();

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule = new MigrationManagerRule();

    @Inject
    private MvccLogEntrySerializationStrategy logEntryStrat;

    @Test
    public void testStartStage() throws Exception {

        Assert.assertNotNull( logEntryStrat );

        final CollectionScope collectionScope = mock( CollectionScope.class );
        when( collectionScope.getOrganization() )
                .thenReturn( new SimpleId( UUIDGenerator.newTimeUUID(), "organization" ) );
        when( collectionScope.getOwner() )
                .thenReturn( new SimpleId( UUIDGenerator.newTimeUUID(), "owner" ) );

        // Set up the mock to return the entity from the start phase
        final Entity entity = generateEntity();

        final MvccEntity mvccEntity = fromEntity( entity );

        // Run the stage
        WriteOptimisticVerify newStage = new WriteOptimisticVerify( logEntryStrat );

        CollectionIoEvent<MvccEntity> result;
        result = newStage.call( new CollectionIoEvent<MvccEntity>( collectionScope, mvccEntity ) );

        assertSame("Context was correct", collectionScope, result.getEntityCollection()) ;

        // Verify the entity is correct
        MvccEntity entry = result.getEvent();

        // Verify UUID and version in both the MvccEntity and the entity itself. Here assertSame 
        // is used on purpose as we want to make sure the same instance is used, not a copy.
        // This way the caller's runtime type is retained.
        assertSame( "Id correct", entity.getId(), entry.getId() );
        assertSame( "Version did not not match entityId", entity.getVersion(), entry.getVersion() );
        assertSame( "Entity correct", entity, entry.getEntity().get() );
    }

    @Override
    protected void validateStage( final CollectionIoEvent<MvccEntity> event ) {
        new WriteOptimisticVerify( logEntryStrat ).call( event );
    }

}


