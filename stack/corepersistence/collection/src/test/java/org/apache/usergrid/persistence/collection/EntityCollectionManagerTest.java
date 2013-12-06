package org.apache.usergrid.persistence.collection;


import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.CollectionEventBus;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.Result;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.EventStart;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/** @author tnine */
public class EntityCollectionManagerTest {


    @Ignore("Events blows up.  And mocking is difficult.  This needs refactored")
    @Test
    public void testWriteValidation() {


        //mock the uuid service
        final UUIDService uuidService = mock( UUIDService.class );


        final UUID newVersion = UUIDGenerator.newTimeUUID();


        //mock the uuid service
        when( uuidService.newTimeUUID() ).thenReturn( newVersion );


        final CollectionEventBus eventBus = mock( CollectionEventBus.class );

        final EntityCollection collection = mock( EntityCollection.class );

        final Result result = new Result();

        final MvccEntity mockMvccEntity = mock(MvccEntity.class);

        result.addResult( mockMvccEntity );


        EntityCollectionManagerImpl collectionManager =
                new EntityCollectionManagerImpl( eventBus, uuidService, collection );


        //set up the mock to return the entity from the start phase
        final Entity entity = new Entity( "test" );
        ArgumentCaptor<EventStart> event = ArgumentCaptor.forClass( EventStart.class );


        //we need to capture the event
        doNothing().when( eventBus ).post( event.capture() );

        collectionManager.write( entity );


        //now verify our output was correct


        verify( eventBus ).post( event.capture() );

        Entity created = event.getValue().getData();

        //verify uuid and version in both the MvccEntity and the entity itself
        assertEquals( "Entity re-set into context", entity, created );
        assertEquals( "version did not not match entityId", newVersion, created.getVersion() );
    }


    @Test( expected = NullPointerException.class )
    public void testWriteNoIdValidation() {


        //mock the uuid service
        final UUIDService uuidService = mock( UUIDService.class );


        final UUID newVersion = UUIDGenerator.newTimeUUID();


        //mock the uuid service
        when( uuidService.newTimeUUID() ).thenReturn( newVersion );


        final CollectionEventBus eventBus = mock( CollectionEventBus.class );

        final EntityCollection collection = mock( EntityCollection.class );


        EntityCollectionManagerImpl collectionManager =
                new EntityCollectionManagerImpl( eventBus, uuidService, collection );


        //set up the mock to return the entity from the start phase
        final Entity entity = new Entity();


        collectionManager.write( entity );
    }


    @Test( expected = NullPointerException.class )
    public void testWriteNoIdUuidValidation() {


        //mock the uuid service
        final UUIDService uuidService = mock( UUIDService.class );



        final CollectionEventBus eventBus = mock( CollectionEventBus.class );

        final EntityCollection collection = mock( EntityCollection.class );


        EntityCollectionManagerImpl collectionManager =
                new EntityCollectionManagerImpl( eventBus, uuidService, collection );

        final Id id = mock( Id.class );

        //set up the mock to return the entity from the start phase
        final Entity entity = new Entity(id);

        when(id.getUuid()).thenReturn( null );
        when(id.getType()).thenReturn( "test" );


        collectionManager.write( entity );
    }


    @Test( expected = NullPointerException.class )
    public void testWriteNoIdTypeValidation() {


        //mock the uuid service
        final UUIDService uuidService = mock( UUIDService.class );



        final CollectionEventBus eventBus = mock( CollectionEventBus.class );

        final EntityCollection collection = mock( EntityCollection.class );


        EntityCollectionManagerImpl collectionManager =
                new EntityCollectionManagerImpl( eventBus, uuidService, collection );

        final Id id = mock( Id.class );

        //set up the mock to return the entity from the start phase
        final Entity entity = new Entity(id);

        when(id.getUuid()).thenReturn( UUIDGenerator.newTimeUUID() );
        when(id.getType()).thenReturn( null );


        collectionManager.write( entity );
    }
}
