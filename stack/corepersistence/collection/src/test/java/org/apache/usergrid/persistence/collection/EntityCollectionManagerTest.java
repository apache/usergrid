package org.apache.usergrid.persistence.collection;


/** @author tnine */
public class EntityCollectionManagerTest {


    //    @Ignore("Events blows up.  And mocking is difficult.  This needs refactored")
    //    @Test
    //    public void testWriteValidation() {
    //
    //
    //        //mock the uuid service
    //        final UUIDService uuidService = mock( UUIDService.class );
    //
    //
    //        final UUID newVersion = UUIDGenerator.newTimeUUID();
    //
    //
    //        //mock the uuid service
    //        when( uuidService.newTimeUUID() ).thenReturn( newVersion );
    //
    //
    //        final CollectionEventBus eventBus = mock( CollectionEventBus.class );
    //
    //        final CollectionScope collection = mock( CollectionScope.class );
    //
    //        final Result result = new Result();
    //
    //        final MvccEntity mockMvccEntity = mock(MvccEntity.class);
    //
    //        result.addResult( mockMvccEntity );
    //
    //
    //        EntityCollectionManagerImpl collectionManager =
    //                new EntityCollectionManagerImpl( eventBus, uuidService, collection, startWrite, verifyWrite,
    // commit );
    //
    //
    //        //set up the mock to return the entity from the start phase
    //        final Entity entity = new Entity( "test" );
    //        ArgumentCaptor<EventStart> event = ArgumentCaptor.forClass( EventStart.class );
    //
    //
    //        //we need to capture the event
    //        doNothing().when( eventBus ).post( event.capture() );
    //
    //        collectionManager.write( entity );
    //
    //
    //        //now verify our output was correct
    //
    //
    //        verify( eventBus ).post( event.capture() );
    //
    //        Entity created = event.getValue().getData();
    //
    //        //verify uuid and version in both the MvccEntity and the entity itself
    //        assertEquals( "Entity re-set into context", entity, created );
    //        assertEquals( "version did not not match entityId", newVersion, created.getVersion() );
    //    }
    //
    //
    //    @Test( expected = NullPointerException.class )
    //    public void testWriteNoIdValidation() {
    //
    //
    //        //mock the uuid service
    //        final UUIDService uuidService = mock( UUIDService.class );
    //
    //
    //        final UUID newVersion = UUIDGenerator.newTimeUUID();
    //
    //
    //        //mock the uuid service
    //        when( uuidService.newTimeUUID() ).thenReturn( newVersion );
    //
    //
    //        final CollectionEventBus eventBus = mock( CollectionEventBus.class );
    //
    //        final CollectionScope collection = mock( CollectionScope.class );
    //
    //
    //        EntityCollectionManagerImpl collectionManager =
    //                new EntityCollectionManagerImpl( eventBus, uuidService, collection, startWrite, verifyWrite,
    // commit );
    //
    //
    //        //set up the mock to return the entity from the start phase
    //        final Entity entity = new Entity();
    //
    //
    //        collectionManager.write( entity );
    //    }
    //
    //
    //    @Test( expected = NullPointerException.class )
    //    public void testWriteNoIdUuidValidation() {
    //
    //
    //        //mock the uuid service
    //        final UUIDService uuidService = mock( UUIDService.class );
    //
    //
    //
    //        final CollectionEventBus eventBus = mock( CollectionEventBus.class );
    //
    //        final CollectionScope collection = mock( CollectionScope.class );
    //
    //
    //        EntityCollectionManagerImpl collectionManager =
    //                new EntityCollectionManagerImpl( eventBus, uuidService, collection, startWrite, verifyWrite,
    // commit );
    //
    //        final Id id = mock( Id.class );
    //
    //        //set up the mock to return the entity from the start phase
    //        final Entity entity = new Entity(id);
    //
    //        when(id.getUuid()).thenReturn( null );
    //        when(id.getType()).thenReturn( "test" );
    //
    //
    //        collectionManager.write( entity );
    //    }
    //
    //
    //    @Test( expected = NullPointerException.class )
    //    public void testWriteNoIdTypeValidation() {
    //
    //
    //        //mock the uuid service
    //        final UUIDService uuidService = mock( UUIDService.class );
    //
    //
    //
    //        final CollectionEventBus eventBus = mock( CollectionEventBus.class );
    //
    //        final CollectionScope collection = mock( CollectionScope.class );
    //
    //
    //        EntityCollectionManagerImpl collectionManager =
    //                new EntityCollectionManagerImpl( eventBus, uuidService, collection, startWrite, verifyWrite,
    // commit );
    //
    //        final Id id = mock( Id.class );
    //
    //        //set up the mock to return the entity from the start phase
    //        final Entity entity = new Entity(id);
    //
    //        when(id.getUuid()).thenReturn( UUIDGenerator.newTimeUUID() );
    //        when(id.getType()).thenReturn( null );
    //
    //
    //        collectionManager.write( entity );
    //    }
}
