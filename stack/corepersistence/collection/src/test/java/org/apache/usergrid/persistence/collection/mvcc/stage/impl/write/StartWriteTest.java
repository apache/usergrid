package org.apache.usergrid.persistence.collection.mvcc.stage.impl.write;


/** @author tnine */
public class StartWriteTest {

    /** Standard flow */
//    @Test
//    public void testStartStage() throws Exception {
//
//
//        final EntityCollection context = mock( EntityCollection.class );
//
//        final CollectionEventBus bus = mock( CollectionEventBus.class );
//
//
//        //mock returning a mock mutation when we do a log entry write
//        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );
//
//        final ArgumentCaptor<MvccLogEntry> logEntry = ArgumentCaptor.forClass( MvccLogEntry.class );
//
//        final MutationBatch mutation = mock( MutationBatch.class );
//
//        when( logStrategy.write( same( context ), logEntry.capture() ) ).thenReturn( mutation );
//
//
//        Result result = new Result();
//
//        //set up the mock to return the entity from the start phase
//        final Entity entity = generateEntity();
//
//
//        //run the stage
//        StartWrite newStage = new StartWrite( bus, logStrategy );
//
//        Observable<IoEvent<MvccEntity>> observable = newStage.call( new IoEvent<Entity>( context, entity ) );
//
//        //verify the observable is correct
//        observable.
//
//
//        //now verify our output was correct
//        ArgumentCaptor<EventVerify> eventVerify = ArgumentCaptor.forClass( EventVerify.class );
//
//
//        //verify the log entry is correct
//        MvccLogEntry entry = logEntry.getValue();
//
//        assertEquals( "version did not not match entityId", entity.getVersion(), entry.getVersion() );
//        assertEquals( "EventStage is correct", Stage.ACTIVE, entry.getStage() );
//
//
//        //now verify we set the message into the write context
//        verify( bus ).post( eventVerify.capture() );
//
//        MvccEntity created = eventVerify.getValue().getData();
//
//        //verify uuid and version in both the MvccEntity and the entity itself
//        assertEquals( "version did not not match entityId", entity.getVersion(), created.getVersion() );
//        assertSame( "Entity correct", entity, created.getEntity().get() );
//    }
//
//
//    /** Test no entity id on the entity */
//    @Test( expected = NullPointerException.class )
//    public void testNoEntityId() throws Exception {
//
//
//        final Entity entity = new Entity();
//        final UUID version = UUIDGenerator.newTimeUUID();
//
//
//        EntityUtils.setVersion( entity, version );
//
//
//        final EntityCollection context = mock( EntityCollection.class );
//        final CollectionEventBus eventBus = mock( CollectionEventBus.class );
//
//        //mock returning a mock mutation when we do a log entry write
//        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );
//
//        //run the stage
//        StartWrite newStage = new StartWrite( eventBus, logStrategy );
//
//        newStage.performStage( new EventStart( context, entity, new Result() ) );
//    }
//
//
//    /** Test no entity id on the entity */
//    @Test( expected = NullPointerException.class )
//    public void testNoEntityVersion() throws Exception {
//
//
//
//        final SimpleId entityId = new SimpleId( "test" );
//
//        final Entity entity = new Entity(entityId);
//
//
//        final EntityCollection context = mock( EntityCollection.class );
//        final CollectionEventBus eventBus = mock( CollectionEventBus.class );
//
//
//        //mock returning a mock mutation when we do a log entry write
//        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );
//
//        //run the stage
//        StartWrite newStage = new StartWrite( eventBus, logStrategy );
//
//        newStage.performStage( new EventStart( context, entity, new Result() ) );
//    }
//
//
//    private Entity generateEntity() throws IllegalAccessException {
//        final Entity entity = new Entity("test");
//        final UUID version = UUIDGenerator.newTimeUUID();
//
//        EntityUtils.setVersion(entity, version);
//
//        return entity;
//    }
}


