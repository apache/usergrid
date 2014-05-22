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

import org.jukito.UseModules;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.exception.WriteOptimisticVerifyException;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.AbstractMvccEntityStageTest;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.netflix.astyanax.MutationBatch;

import static org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator.fromEntity;
import static org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator.generateEntity;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@UseModules( TestCollectionModule.class )
public class WriteOptimisticVerifyTest extends AbstractMvccEntityStageTest {

    private static final Logger log = LoggerFactory.getLogger(WriteOptimisticVerifyTest.class);

    @Override
    protected void validateStage( final CollectionIoEvent<MvccEntity> event ) {
        MvccLogEntrySerializationStrategy logstrat = mock( MvccLogEntrySerializationStrategy.class);
        new WriteOptimisticVerify( logstrat ).call( event );
    }

    @Test
    public void testNoConflict() throws Exception {

        final CollectionScope collectionScope = mock( CollectionScope.class );
        when( collectionScope.getApplication() )
            .thenReturn( new SimpleId( UUIDGenerator.newTimeUUID(), "organization" ) );
        when( collectionScope.getOwner() )
            .thenReturn( new SimpleId( UUIDGenerator.newTimeUUID(), "owner" ) );

        final Entity entity = generateEntity();
        entity.setField(new StringField("name", "FOO", true));
        entity.setField(new StringField("identifier", "BAR", true));

        final MvccEntity mvccEntity = fromEntity( entity );

        List<MvccLogEntry> logEntries = new ArrayList<MvccLogEntry>();
        logEntries.add( new MvccLogEntryImpl( 
            entity.getId(), UUIDGenerator.newTimeUUID(), Stage.ACTIVE, MvccLogEntry.State.COMPLETE ));
        logEntries.add( new MvccLogEntryImpl( 
            entity.getId(), UUIDGenerator.newTimeUUID(), Stage.COMMITTED, MvccLogEntry.State.COMPLETE ));

        MvccLogEntrySerializationStrategy noConflictLog = 
            mock( MvccLogEntrySerializationStrategy.class );

        when( noConflictLog.load( collectionScope, entity.getId(), entity.getVersion(), 2) )
            .thenReturn( logEntries );

        UniqueValueSerializationStrategy uvstrat = mock( UniqueValueSerializationStrategy.class);

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

        final CollectionScope scope = mock( CollectionScope.class );
        when( scope.getApplication() )
            .thenReturn( new SimpleId( UUIDGenerator.newTimeUUID(), "organization" ) );
        when( scope.getOwner() )
            .thenReturn( new SimpleId( UUIDGenerator.newTimeUUID(), "owner" ) );

        // there is an entity
        final Entity entity = generateEntity();
        entity.setField(new StringField("name", "FOO", true));
        entity.setField(new StringField("identifier", "BAR", true));

        // log that one operation is active on entity
        List<MvccLogEntry> logEntries = new ArrayList<MvccLogEntry>();
        logEntries.add( new MvccLogEntryImpl( 
            entity.getId(), UUIDGenerator.newTimeUUID(), Stage.ACTIVE, MvccLogEntry.State.COMPLETE ));

        // log another operation as active on entity
        logEntries.add( new MvccLogEntryImpl( 
            entity.getId(), UUIDGenerator.newTimeUUID(), Stage.ACTIVE, MvccLogEntry.State.COMPLETE ));

        // mock up the log
        MvccLogEntrySerializationStrategy mvccLog = 
            mock( MvccLogEntrySerializationStrategy.class );
        when( mvccLog.load( scope, entity.getId(), entity.getVersion(), 2) )
            .thenReturn( logEntries );

        // mock up unique values interface
        UniqueValueSerializationStrategy uvstrat = mock( UniqueValueSerializationStrategy.class);
        UniqueValue uv1 = new UniqueValueImpl(
            scope, entity.getField("name"), entity.getId(), entity.getVersion());
        UniqueValue uv2 = new UniqueValueImpl(
            scope, entity.getField("identifier"), entity.getId(), entity.getVersion());
        MutationBatch mb = mock( MutationBatch.class );
        when( uvstrat.delete(uv1) ).thenReturn(mb);
        when( uvstrat.delete(uv2) ).thenReturn(mb);

        // Run the stage, conflict should be detected
        final MvccEntity mvccEntity = fromEntity( entity );
        boolean conflictDetected = false;

        WriteOptimisticVerify newStage = new WriteOptimisticVerify( mvccLog );
        RollbackAction rollbackAction = new RollbackAction( mvccLog, uvstrat );

        try {
            newStage.call( new CollectionIoEvent<MvccEntity>(scope, mvccEntity));

        } catch (WriteOptimisticVerifyException e) {
            log.info("Error", e);
            conflictDetected = true;
            rollbackAction.call( e );
        }
        assertTrue( conflictDetected );

        // check that unique values were deleted
        verify( uvstrat, times(1) ).delete( uv1 );
        verify( uvstrat, times(1) ).delete( uv2 );
    }

}


