package org.apache.usergrid.persistence.collection.mvcc.stage.load;


import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.AbstractIdStageTest;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/** @author tnine */
public class LoadTest  extends AbstractIdStageTest {


    @Test
    public void testLoadWithData(){
        final CollectionScope collection = mock(CollectionScope.class);
        final UUIDService uuidService = mock(UUIDService.class);
        final MvccEntitySerializationStrategy serializationStrategy = mock(MvccEntitySerializationStrategy.class);


        final UUID loadVersion = UUIDGenerator.newTimeUUID();

        //mock up the time uuid
        when(uuidService.newTimeUUID()).thenReturn(loadVersion);

        final Id entityId = TestEntityGenerator.generateId();

        final CollectionIoEvent<Id> entityIoEvent = new CollectionIoEvent<Id>(collection,  entityId );


        final Entity entity = TestEntityGenerator.generateEntity(entityId, loadVersion);

        final MvccEntity mvccEntity = TestEntityGenerator.fromEntity( entity );

        final List<MvccEntity> results = Lists.newArrayList( mvccEntity );

        //mock up returning a list of MvccEntities
        when(serializationStrategy.load( collection, entityId, loadVersion, 5 )).thenReturn( results);



        Load load = new Load( uuidService, serializationStrategy );
        Entity loaded = load.call( entityIoEvent );


        assertSame("Same entity was loaded", entity, loaded);

    }


    /**
     * Handles second trigger condition with partial updates.
     * A read on an entity , and we recognize that the entity we are reading is partial.
     */
    @Test
    public void testLoadWithPartialWrite(){
        final CollectionScope collection = mock(CollectionScope.class);
        final UUIDService uuidService = mock(UUIDService.class);
        final MvccEntitySerializationStrategy serializationStrategy = mock(MvccEntitySerializationStrategy.class);


        final UUID loadVersion = UUIDGenerator.newTimeUUID();

        //mock up the time uuid
        when(uuidService.newTimeUUID()).thenReturn(loadVersion);

        final Id entityId = TestEntityGenerator.generateId();

        final CollectionIoEvent<Id> entityIoEvent = new CollectionIoEvent<Id>(collection,  entityId );


        final Entity entity = TestEntityGenerator.generateEntity(entityId, loadVersion);
        entity.setField( new StringField( "derp","noderp" ) );

        final MvccEntity completeMvccEntity = TestEntityGenerator.fromEntityStatus( entity, MvccEntity.Status.COMPLETE );


        final Entity entity2 = TestEntityGenerator.generateEntity( entityId, UUIDGenerator.newTimeUUID() );
        entity2.setField( new StringField( "derp","noderp" ) );
        entity2.setField( new StringField( "merple","nomerple" ) );

        final MvccEntity partialMvccEntity = TestEntityGenerator.fromEntityStatus( entity2, MvccEntity.Status.PARTIAL );

        final List<MvccEntity> results = Lists.newArrayList( completeMvccEntity );
        results.add( partialMvccEntity );

        //mock up returning a list of MvccEntities
        when( serializationStrategy.load( collection, entityId, loadVersion, 5 ) ).thenReturn( results);

        Load load = new Load( uuidService, serializationStrategy );
        Entity loaded = load.call( entityIoEvent );

        assertNotNull( loaded.getField( "derp" ) );
        assertNotNull( loaded.getField( "merple" ) );

    }

    /**
     * Handles second trigger condition with partial updates.
     * A read on an entity , and we recognize that the entity we are reading is partial.
     */
    @Test
    public void testLoadWithPartialDelete(){
        final CollectionScope collection = mock(CollectionScope.class);
        final UUIDService uuidService = mock(UUIDService.class);
        final MvccEntitySerializationStrategy serializationStrategy = mock(MvccEntitySerializationStrategy.class);


        final UUID loadVersion = UUIDGenerator.newTimeUUID();

        //mock up the time uuid
        when(uuidService.newTimeUUID()).thenReturn(loadVersion);

        final Id entityId = TestEntityGenerator.generateId();

        final CollectionIoEvent<Id> entityIoEvent = new CollectionIoEvent<Id>(collection,  entityId );


        final Entity entity = TestEntityGenerator.generateEntity(entityId, loadVersion);
        entity.setField( new StringField( "derp","noderp" ) );
        entity.setField( new StringField( "derple","somemerple" ) );

        final MvccEntity completeMvccEntity = TestEntityGenerator.fromEntityStatus( entity, MvccEntity.Status.COMPLETE );


        final Entity entity2 = TestEntityGenerator.generateEntity( entityId, UUIDGenerator.newTimeUUID() );
        entity2.setField( new StringField( "derple","somemerple" ) );


        final MvccEntity partialMvccEntity = TestEntityGenerator.fromEntityStatus( entity2, MvccEntity.Status.PARTIAL );

        final List<MvccEntity> results = Lists.newArrayList( completeMvccEntity );
        results.add( partialMvccEntity );

        //mock up returning a list of MvccEntities
        when( serializationStrategy.load( collection, entityId, loadVersion, 5 ) ).thenReturn( results);

        Load load = new Load( uuidService, serializationStrategy );
        Entity loaded = load.call( entityIoEvent );

        assertNull( loaded.getField( "derp" ) );

    }

    @Test
    public void testLoadWithPartialWriteDeleteMultipleTimes(){
        final CollectionScope collection = mock(CollectionScope.class);
        final UUIDService uuidService = mock(UUIDService.class);
        final MvccEntitySerializationStrategy serializationStrategy = mock(MvccEntitySerializationStrategy.class);


        final UUID loadVersion = UUIDGenerator.newTimeUUID();

        //mock up the time uuid
        when(uuidService.newTimeUUID()).thenReturn(loadVersion);

        final Id entityId = TestEntityGenerator.generateId();

        final CollectionIoEvent<Id> entityIoEvent = new CollectionIoEvent<Id>(collection,  entityId );


        final Entity entity = TestEntityGenerator.generateEntity(entityId, loadVersion);
        entity.setField( new StringField( "derp","noderp" ) );
        entity.setField( new StringField( "derple","somemerple" ) );

        final MvccEntity completeMvccEntity = TestEntityGenerator.fromEntityStatus( entity, MvccEntity.Status.COMPLETE );


        final Entity entity2 = TestEntityGenerator.generateEntity( entityId, UUIDGenerator.newTimeUUID() );
        entity2.setField( new StringField( "derple","somemerple" ) );


        final MvccEntity partialMvccEntity = TestEntityGenerator.fromEntityStatus( entity2, MvccEntity.Status.PARTIAL );

        final Entity entity3 = TestEntityGenerator.generateEntity( entityId, UUIDGenerator.newTimeUUID() );
        entity3.setField( new StringField( "derp","noderp" ) );
        entity3.setField( new StringField( "derple","somemerple" ) );


        final MvccEntity partialMvccEntity2 = TestEntityGenerator.fromEntityStatus( entity3, MvccEntity.Status.PARTIAL );

        final List<MvccEntity> results = Lists.newArrayList( completeMvccEntity );
        results.add( partialMvccEntity );
        results.add( partialMvccEntity2 );

        //mock up returning a list of MvccEntities
        when( serializationStrategy.load( collection, entityId, loadVersion, 5 ) ).thenReturn( results);

        Load load = new Load( uuidService, serializationStrategy );
        Entity loaded = load.call( entityIoEvent );

        assertNotNull( loaded.getField( "derp" ) );
        assertNotNull( loaded.getField( "derple" ) );

    }

    @Test
    public void testLoadCleared(){
        final CollectionScope collection = mock(CollectionScope.class);
        final UUIDService uuidService = mock(UUIDService.class);
        final MvccEntitySerializationStrategy serializationStrategy = mock(MvccEntitySerializationStrategy.class);


        final UUID loadVersion = UUIDGenerator.newTimeUUID();

        //mock up the time uuid
        when(uuidService.newTimeUUID()).thenReturn(loadVersion);

        final Id entityId = TestEntityGenerator.generateId();

        final CollectionIoEvent<Id> entityIoEvent = new CollectionIoEvent<Id>(collection,  entityId );




        final List<MvccEntity> results = Collections.EMPTY_LIST;

        //mock up returning a list of MvccEntities
        when(serializationStrategy.load( collection, entityId, loadVersion, 1 )).thenReturn( results);

        Load load = new Load( uuidService, serializationStrategy );
        Entity loaded = load.call( entityIoEvent );

        assertNull( "No entity was loaded", loaded );
    }





    @Override
    protected void validateStage( final CollectionIoEvent<Id> event ) {
        final UUIDService uuidService = mock(UUIDService.class);
        final MvccEntitySerializationStrategy serializationStrategy = mock(MvccEntitySerializationStrategy.class);

        new Load(uuidService, serializationStrategy).call( event );
    }
}
