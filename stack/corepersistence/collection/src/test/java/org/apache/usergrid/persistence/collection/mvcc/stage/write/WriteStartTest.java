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


import java.util.UUID;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.stage.AbstractEntityStageTest;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.netflix.astyanax.MutationBatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/** @author tnine */
public class WriteStartTest extends AbstractEntityStageTest {

    /** Standard flow */
    @Test
    public void testStartStage() throws Exception {


        final ApplicationScope context = mock( ApplicationScope.class );


        //mock returning a mock mutation when we do a log entry write
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        final ArgumentCaptor<MvccLogEntry> logEntry = ArgumentCaptor.forClass( MvccLogEntry.class );

        final MutationBatch mutation = mock( MutationBatch.class );

        final UUIDService uuidService = mock ( UUIDService.class );

        when( logStrategy.write( same( context ), logEntry.capture() ) ).thenReturn( mutation );

        //set up the mock to return the entity from the start phase
        final Entity entity = TestEntityGenerator.generateEntity();

        //run the stage
        WriteStart newStage = new WriteStart( logStrategy);


        //verify the observable is correct
        CollectionIoEvent<MvccEntity> result = newStage.call( new CollectionIoEvent<Entity>( context, entity ) );


        //verify the log entry is correct
        MvccLogEntry entry = logEntry.getValue();

        assertEquals( "id correct", entity.getId(), entry.getEntityId() );
        assertEquals( "EventStage is correct", Stage.ACTIVE, entry.getStage() );


        MvccEntity created = result.getEvent();

        //verify uuid and version in both the MvccEntity and the entity itself
        //assertSame is used on purpose.  We want to make sure the same instance is used, not a copy.
        //this way the caller's runtime type is retained.
        assertSame( "id correct", entity.getId(), created.getId() );
        assertSame( "Entity correct", entity, created.getEntity().get() );
    }

    /** If no version then execute not called */
    @Test
    public void testNoVersion() throws Exception {

        final ApplicationScope context = mock( ApplicationScope.class );

        //mock returning a mock mutation when we do a log entry write
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );


        final ArgumentCaptor<MvccLogEntry> logEntry = ArgumentCaptor.forClass( MvccLogEntry.class );

        final MutationBatch mutation = mock( MutationBatch.class );

        final UUIDService uuidService = mock ( UUIDService.class );

        when( logStrategy.write( same( context ), logEntry.capture() ) ).thenReturn( mutation );
        when(mutation.execute()).thenThrow(new RuntimeException("Fail fail fail"));

        //set up the mock to return the entity from the start phase
        final Entity entity = TestEntityGenerator.generateEntity(new SimpleId(UUID.randomUUID(),"test"),null);
        //run the stage
        WriteStart newStage = new WriteStart( logStrategy );

        //verify the observable is correct
        CollectionIoEvent<MvccEntity> result = newStage.call( new CollectionIoEvent<Entity>( context, entity ) );

        verify(mutation,times(0)).execute();

        //verify the log entry is correct
        MvccLogEntry entry = logEntry.getValue();

        assertEquals( "id correct", entity.getId(), entry.getEntityId() );
        assertEquals( "EventStage is correct", Stage.ACTIVE, entry.getStage() );


        MvccEntity created = result.getEvent();

        //verify uuid and version in both the MvccEntity and the entity itself
        //assertSame is used on purpose.  We want to make sure the same instance is used, not a copy.
        //this way the caller's runtime type is retained.
        assertSame( "id correct", entity.getId(), created.getId() );
        assertSame( "Entity correct", entity, created.getEntity().get() );
    }

    @Override
    protected void validateStage( final CollectionIoEvent<Entity> event ) {
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        new WriteStart( logStrategy ).call( event );
    }
}


