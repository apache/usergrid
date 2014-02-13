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

import java.util.ArrayList;
import java.util.List;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.AbstractMvccEntityStageTest;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import static org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator.fromEntity;
import static org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator.generateEntity;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.jukito.UseModules;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@UseModules( TestCollectionModule.class )
public class WriteOptimisticVerifyTest extends AbstractMvccEntityStageTest {

    @Override
    protected void validateStage( final CollectionIoEvent<MvccEntity> event ) {
        MvccLogEntrySerializationStrategy logstrat = mock( MvccLogEntrySerializationStrategy.class);
        new WriteOptimisticVerify( logstrat ).call( event );
    }

    @Test
    public void testNoConflict() throws Exception {

        final CollectionScope collectionScope = mock( CollectionScope.class );
        when( collectionScope.getOrganization() )
            .thenReturn( new SimpleId( UUIDGenerator.newTimeUUID(), "organization" ) );
        when( collectionScope.getOwner() )
            .thenReturn( new SimpleId( UUIDGenerator.newTimeUUID(), "owner" ) );

        final Entity entity = generateEntity();
        final MvccEntity mvccEntity = fromEntity( entity );

        List<MvccLogEntry> logEntries = new ArrayList<MvccLogEntry>();
        logEntries.add( new MvccLogEntryImpl( 
            entity.getId(), UUIDGenerator.newTimeUUID(), Stage.ACTIVE ));
        logEntries.add( new MvccLogEntryImpl( 
            entity.getId(), UUIDGenerator.newTimeUUID(), Stage.COMMITTED));

        MvccLogEntrySerializationStrategy noConflictLog = 
            mock( MvccLogEntrySerializationStrategy.class );
        when( noConflictLog.load( collectionScope, entity.getId(), entity.getVersion(), 2) )
            .thenReturn( logEntries );

        // Run the stage
        WriteOptimisticVerify newStage = new WriteOptimisticVerify( noConflictLog );

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

    @Test
    public void testConflict() throws Exception {

        final CollectionScope collectionScope = mock( CollectionScope.class );
        when( collectionScope.getOrganization() )
            .thenReturn( new SimpleId( UUIDGenerator.newTimeUUID(), "organization" ) );
        when( collectionScope.getOwner() )
            .thenReturn( new SimpleId( UUIDGenerator.newTimeUUID(), "owner" ) );

        final Entity entity = generateEntity();
        final MvccEntity mvccEntity = fromEntity( entity );

        List<MvccLogEntry> logEntries = new ArrayList<MvccLogEntry>();
        logEntries.add( new MvccLogEntryImpl( 
            entity.getId(), UUIDGenerator.newTimeUUID(), Stage.ACTIVE ));
        logEntries.add( new MvccLogEntryImpl( 
            entity.getId(), UUIDGenerator.newTimeUUID(), Stage.ACTIVE));

        MvccLogEntrySerializationStrategy noConflictLog = 
            mock( MvccLogEntrySerializationStrategy.class );
        when( noConflictLog.load( collectionScope, entity.getId(), entity.getVersion(), 2) )
            .thenReturn( logEntries );

        // Run the stage
        WriteOptimisticVerify newStage = new WriteOptimisticVerify( noConflictLog );

        CollectionIoEvent<MvccEntity> result;
        boolean conflictDetected = false;
        try {
            result = newStage.call( new CollectionIoEvent<MvccEntity>(collectionScope, mvccEntity));
        } catch (Exception e) {
            conflictDetected = true;
        }
        assertTrue( conflictDetected );
    }
}


