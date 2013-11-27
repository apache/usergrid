package org.apache.usergrid.persistence.collection.serialization;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;

import org.apache.cassandra.db.marshal.UUIDType;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.CollectionContextImpl;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntityImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.test.CassandraRule;

import com.google.common.base.Optional;
import com.google.guiceberry.junit4.GuiceBerryRule;
import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.serializers.UUIDSerializer;
import com.netflix.astyanax.util.TimeUUIDUtils;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * @author tnine
 */
public class MvccEntitySerializationStrategyImplTest {

    @Rule
    public final GuiceBerryRule guiceBerry = new GuiceBerryRule( TestCollectionModule.class );

    @Rule
    public final CassandraRule rule = new CassandraRule();

    @Inject
    private MvccEntitySerializationStrategy serializationStrategy;


    @Test
    public void writeLoadDelete() throws ConnectionException {

        final UUID applicationId = UUIDGenerator.newTimeUUID();
        final String name = "test";

        CollectionContext context = new CollectionContextImpl( applicationId, applicationId, name );


        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final String type = "test";
        final long created = 1l;
        final long updated = 2l;

        Entity entity = new Entity( entityId, type );

        entity.setVersion( version );
        entity.setCreated( created );
        entity.setUpdated( updated );


        BooleanField boolField = new BooleanField( "boolean", false );
        DoubleField doubleField = new DoubleField( "double", 1d );
        IntegerField intField = new IntegerField( "long", 1 );
        LongField longField = new LongField( "int", 1l );
        StringField stringField = new StringField( "name", "test" );
        UUIDField uuidField = new UUIDField( "uuid", UUIDGenerator.newTimeUUID() );

        entity.setField( boolField );
        entity.setField( doubleField );
        entity.setField( intField );
        entity.setField( longField );
        entity.setField( stringField );
        entity.setField( uuidField );


        MvccEntity saved = new MvccEntityImpl( context, entityId, version, Optional.of( entity ) );


        //persist the entity
        serializationStrategy.write( saved ).execute();

        //now read it back

        MvccEntity returned = serializationStrategy.load( context, entityId, version );

        assertEquals( "Mvcc entities are the same", saved, returned );


        assertEquals( entityId, entity.getUuid() );
        assertEquals( type, entity.getType() );
        assertEquals( created, entity.getCreated() );
        assertEquals( updated, entity.getUpdated() );


        Field<Boolean> boolFieldReturned = entity.getField( boolField.getName() );

        assertSame( boolField, boolFieldReturned );

        Field<Double> doubleFieldReturned = entity.getField( doubleField.getName() );

        assertSame( doubleField, doubleFieldReturned );

        Field<Integer> intFieldReturned = entity.getField( intField.getName() );

        assertSame( intField, intFieldReturned );

        Field<Long> longFieldReturned = entity.getField( longField.getName() );

        assertSame( longField, longFieldReturned );

        Field<String> stringFieldReturned = entity.getField( stringField.getName() );

        assertSame( stringField, stringFieldReturned );

        Field<UUID> uuidFieldReturned = entity.getField( uuidField.getName() );

        assertSame( uuidField, uuidFieldReturned );


        Set<Field> results = new HashSet<Field>();
        results.addAll( entity.getFields() );


        assertTrue( results.contains( boolField ) );
        assertTrue( results.contains( doubleField ) );
        assertTrue( results.contains( intField ) );
        assertTrue( results.contains( longField ) );
        assertTrue( results.contains( stringField ) );
        assertTrue( results.contains( uuidField ) );

        assertEquals( 6, results.size() );


        assertEquals( entityId, entity.getUuid() );
        assertEquals( version, entity.getVersion() );


        //now delete it
        serializationStrategy.delete( context, entityId, version ).execute();

        //now get it, should be gone

        returned = serializationStrategy.load( context, entityId, version );

        assertNull( returned );
    }


    @Test
    public void writeLoadClearDelete() throws ConnectionException {

        final UUID applicationId = UUIDGenerator.newTimeUUID();
        final String name = "test";

        CollectionContext context = new CollectionContextImpl( applicationId, applicationId, name );


        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final String type = "test";
        final long created = 1l;
        final long updated = 2l;

        Entity entity = new Entity( entityId, type );

        entity.setVersion( version );
        entity.setCreated( created );
        entity.setUpdated( updated );


        MvccEntity saved = new MvccEntityImpl( context, entityId, version, Optional.of( entity ) );


        //persist the entity
        serializationStrategy.write( saved ).execute();

        //now read it back

        MvccEntity returned = serializationStrategy.load( context, entityId, version );

        assertEquals( "Mvcc entities are the same", saved, returned );


        assertEquals( entityId, entity.getUuid() );
        assertEquals( type, entity.getType() );
        assertEquals( created, entity.getCreated() );
        assertEquals( updated, entity.getUpdated() );


        //now clear it

        serializationStrategy.clear( context, entityId, version ).execute();

        returned = serializationStrategy.load( context, entityId, version );

        assertEquals( context, returned.getContext() );
        assertEquals( entityId, returned.getUuid() );
        assertEquals( version, returned.getVersion() );
        assertFalse( returned.getEntity().isPresent() );

        //now delete it
        serializationStrategy.delete( context, entityId, version ).execute();

        //now get it, should be gone

        returned = serializationStrategy.load( context, entityId, version );

        assertNull( returned );
    }


    @Test
    public void writeX2ClearDelete() throws ConnectionException {

        final UUID applicationId = UUIDGenerator.newTimeUUID();
        final String name = "test";

        CollectionContext context = new CollectionContextImpl( applicationId, applicationId, name );


        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version1 = UUIDGenerator.newTimeUUID();
        final String type = "test";

        Entity entityv1 = new Entity( entityId, type );

        entityv1.setVersion( version1 );


        MvccEntity saved = new MvccEntityImpl( context, entityId, version1, Optional.of( entityv1 ) );


        //persist the entity
        serializationStrategy.write( saved ).execute();

        //now read it back

        MvccEntity returnedV1 = serializationStrategy.load( context, entityId, version1 );

        assertEquals( "Mvcc entities are the same", saved, returnedV1 );


        assertEquals( entityId, entityv1.getUuid() );
        assertEquals( type, entityv1.getType() );


        //now write a new version of it


        Entity entityv2 = new Entity( entityId, type );

        UUID version2 = UUIDGenerator.newTimeUUID();
        entityv2.setVersion( version2 );


        UUIDType comparator = UUIDType.instance;

        int value = comparator.compare( UUIDSerializer.get().toByteBuffer( version1 ), UUIDSerializer.get().toByteBuffer( version2 ) );

        assertTrue(value < 0);

        value = comparator.compare( UUIDSerializer.get().toByteBuffer( version2 ), UUIDSerializer.get().toByteBuffer( version2 ) );

        assertEquals(0, value);

        MvccEntity savedV2 = new MvccEntityImpl( context, entityId, version2, Optional.of( entityv2 ) );

        serializationStrategy.write( savedV2 ).execute();

        MvccEntity returnedV2 = serializationStrategy.load( context, entityId, version2 );

        assertEquals( "Mvcc entities are the same", savedV2, returnedV2 );


        //now clear it at v3

        UUID version3 = UUIDGenerator.newTimeUUID();

        serializationStrategy.clear( context, entityId, version3 ).execute();


        final Optional<Entity> empty = Optional.absent();

        MvccEntity clearedV3 = new MvccEntityImpl( context, entityId, version3, empty );

        MvccEntity returnedV3 = serializationStrategy.load( context, entityId, version3 );

        assertEquals("entities are the same", clearedV3, returnedV3);

        //now ask for up to 10 versions from the current version, we should get cleared, v2, v1
        UUID current = UUIDGenerator.newTimeUUID();

        List<MvccEntity> entities = serializationStrategy.load( context, entityId, current, 3 );

        assertEquals( 3, entities.size() );

        assertEquals( clearedV3, entities.get( 0 ) );

        assertEquals( returnedV2, entities.get( 1 ) );

        assertEquals( returnedV1, entities.get( 2 ) );


        //now delete v2 and v1, we should still get v3
        serializationStrategy.delete( context, entityId, version1 ).execute();
        serializationStrategy.delete( context, entityId, version2 ).execute();

        entities = serializationStrategy.load( context, entityId, current, 3 );

        assertEquals( 1, entities.size() );

        assertEquals( clearedV3, entities.get( 0 ) );


        //now get it, should be gone
        serializationStrategy.delete( context, entityId, version3 ).execute();


        entities = serializationStrategy.load( context, entityId, current, 3 );

        assertEquals( 0, entities.size() );
    }
}
