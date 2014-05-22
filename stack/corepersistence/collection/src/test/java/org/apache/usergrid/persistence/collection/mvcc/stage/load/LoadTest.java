package org.apache.usergrid.persistence.collection.mvcc.stage.load;


import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.changelog.ChangeLogGenerator;
import org.apache.usergrid.persistence.collection.mvcc.changelog.ChangeLogGeneratorImpl;
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

        final MvccEntity mvccEntity = TestEntityGenerator.fromEntityStatus( entity, MvccEntity.Status.COMPLETE );

        final Iterator<MvccEntity> results = Lists.newArrayList( mvccEntity ).iterator();

        //mock up returning a list of MvccEntities
        when(serializationStrategy.load( collection, entityId, loadVersion, 1 )).thenReturn( results);



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

        final List<MvccEntity> results = Lists.newArrayList( partialMvccEntity );
        results.add( completeMvccEntity );


        //mock up returning a list of MvccEntities
        when( serializationStrategy.load( collection, entityId, loadVersion, 1 ) ).thenReturn( results.iterator());

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
        final ChangeLogGenerator changeLogGenerator = new ChangeLogGeneratorImpl();



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

        final List<MvccEntity> results = Lists.newArrayList( partialMvccEntity );
        results.add( completeMvccEntity );

        //mock up returning a list of MvccEntities
        when( serializationStrategy.load( collection, entityId, loadVersion, 1 ) ).thenReturn( results.iterator());

        Load load = new Load( uuidService, serializationStrategy );
        Entity loaded = load.call( entityIoEvent );

        assertNull( loaded.getField( "derp" ) );

    }

    @Test
    public void testLoadWithPartialWriteDeleteThreeTimes(){
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


        final MvccEntity partialMvccEntity2 = TestEntityGenerator.fromEntityStatus( entity3, MvccEntity.Status.PARTIAL );

        final List<MvccEntity> results = Lists.newArrayList( partialMvccEntity2 );
        results.add( partialMvccEntity );
        results.add( completeMvccEntity );

        //mock up returning a list of MvccEntities
        when( serializationStrategy.load( collection, entityId, loadVersion, 1 ) ).thenReturn( results.iterator());

        Load load = new Load( uuidService, serializationStrategy );
        Entity loaded = load.call( entityIoEvent );

        assertNotNull( loaded.getField( "derp" ) );
        assertNull( loaded.getField( "derple" ) );

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


        final Iterator<MvccEntity> results = Collections.EMPTY_LIST.iterator();

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
