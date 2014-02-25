package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.safehaus.chop.api.IterationChop;
import org.safehaus.guicyfig.Env;
import org.safehaus.guicyfig.Option;
import org.safehaus.guicyfig.Overrides;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.astyanax.AstyanaxKeyspaceProvider;
import org.apache.usergrid.persistence.collection.astyanax.CassandraFig;
import org.apache.usergrid.persistence.collection.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.migration.MigrationManagerFig;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;


/** @author tnine */
@IterationChop( iterations = 1000, threads = 2 )
@RunWith( JukitoRunner.class )
@UseModules( TestCollectionModule.class )
public class MvccEntitySerializationStrategyImplTest {
    /** Our RX I/O threads and this should have the same value */
    private static final String CONNECTION_COUNT = "20";


    @Inject
    private MvccEntitySerializationStrategy serializationStrategy;


    @ClassRule
    public static CassandraRule rule = new CassandraRule();

    @Inject
    AstyanaxKeyspaceProvider provider;

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Inject
    @Overrides(
        name = "unit-test",
        environments = Env.UNIT,
        options = {
            @Option( method = "getHosts", override = "localhost" ),
            @Option( method = "getConnections", override = CONNECTION_COUNT )
        }
    )
    public CassandraFig cassandraFig;


    @Inject
    public SerializationFig serializationFig;

    @Inject
    public MigrationManagerFig migrationManagerFig;


    @Before
    public void setup() {
        assertNotNull( cassandraFig );
    }


    @After
    public void tearDown() {
        provider.shutdown();
    }


    @Test
    public void writeLoadDelete() throws ConnectionException {

        final Id organizationId = new SimpleId( "organization" );
        final Id applicationId = new SimpleId( "application" );
        final String name = "test";

        CollectionScope context = new CollectionScopeImpl( organizationId,  applicationId, name );


        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final String type = "test";

        final Id id = new SimpleId( entityId, type );

        Entity entity = new Entity( id );

        EntityUtils.setVersion( entity, version );


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


        MvccEntity saved = new MvccEntityImpl( id, version, MvccEntity.Status.COMPLETE, Optional.of( entity ) );


        //persist the entity
        serializationStrategy.write( context, saved ).execute();

        //now load it back

        MvccEntity returned = serializationStrategy.load( context, id, version );

        assertEquals( "Mvcc entities are the same", saved, returned );


        assertEquals( id, entity.getId() );


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


        assertEquals( id, entity.getId() );
        assertEquals( version, entity.getVersion() );


        //now delete it
        serializationStrategy.delete( context, id, version ).execute();

        //now get it, should be gone

        returned = serializationStrategy.load( context, id, version );

        assertNull( returned );
    }


    @Test
    public void writeLoadClearDelete() throws ConnectionException {

        final Id organizationId = new SimpleId( "organization" );
        final Id applicationId = new SimpleId( "application" );
        final String name = "test";

        CollectionScope context = new CollectionScopeImpl(organizationId,  applicationId, name );


        final UUID version = UUIDGenerator.newTimeUUID();

        final Id entityId = new SimpleId( "test" );

        Entity entity = new Entity( entityId );

        EntityUtils.setVersion( entity, version );


        MvccEntity saved = new MvccEntityImpl( entityId, version, MvccEntity.Status.COMPLETE, Optional.of( entity ) );


        //persist the entity
        serializationStrategy.write( context, saved ).execute();

        //now load it back

        MvccEntity returned = serializationStrategy.load( context, entityId, version );

        assertEquals( "Mvcc entities are the same", saved, returned );


        assertEquals( entityId, returned.getId() );

        //check the target entity has the right id
        assertEquals( entityId, returned.getEntity().get().getId() );


        //now mark it

        serializationStrategy.mark( context, entityId, version ).execute();

        returned = serializationStrategy.load( context, entityId, version );

        assertEquals( entityId, returned.getId() );
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

        final Id organizationId = new SimpleId( "organization" );
        final Id applicationId = new SimpleId( "application" );
        final String name = "test";

        CollectionScope context = new CollectionScopeImpl(organizationId, applicationId, name );


        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version1 = UUIDGenerator.newTimeUUID();
        final String type = "test";

        final Id id = new SimpleId( entityId, type );

        Entity entityv1 = new Entity( id );

        EntityUtils.setVersion( entityv1, version1 );


        MvccEntity saved = new MvccEntityImpl( id, version1, MvccEntity.Status.COMPLETE, Optional.of( entityv1 ) );


        //persist the entity
        serializationStrategy.write( context, saved ).execute();

        //now load it back

        MvccEntity returnedV1 = serializationStrategy.load( context, id, version1 );

        assertEquals( "Mvcc entities are the same", saved, returnedV1 );


        //now write a new version of it


        Entity entityv2 = new Entity( id );

        UUID version2 = UUIDGenerator.newTimeUUID();


        EntityUtils.setVersion( entityv1, version2 );


        MvccEntity savedV2 = new MvccEntityImpl( id, version2, MvccEntity.Status.COMPLETE, Optional.of( entityv2 ) );

        serializationStrategy.write( context, savedV2 ).execute();

        MvccEntity returnedV2 = serializationStrategy.load( context, id, version2 );

        assertEquals( "Mvcc entities are the same", savedV2, returnedV2 );


        //now mark it at v3

        UUID version3 = UUIDGenerator.newTimeUUID();

        serializationStrategy.mark( context, id, version3 ).execute();


        final Optional<Entity> empty = Optional.absent();

        MvccEntity clearedV3 = new MvccEntityImpl( id, version3, MvccEntity.Status.COMPLETE, empty );

        MvccEntity returnedV3 = serializationStrategy.load( context, id, version3 );

        assertEquals( "entities are the same", clearedV3, returnedV3 );

        //now ask for up to 10 versions from the current version, we should get cleared, v2, v1
        UUID current = UUIDGenerator.newTimeUUID();

        List<MvccEntity> entities = serializationStrategy.load( context, id, current, 3 );

        assertEquals( 3, entities.size() );

        assertEquals( clearedV3, entities.get( 0 ) );

        assertEquals( id, entities.get( 0 ).getId() );


        assertEquals( returnedV2, entities.get( 1 ) );
        assertEquals( id, entities.get( 1 ).getId() );


        assertEquals( returnedV1, entities.get( 2 ) );
        assertEquals( id, entities.get( 2 ).getId() );


        //now delete v2 and v1, we should still get v3
        serializationStrategy.delete( context, id, version1 ).execute();
        serializationStrategy.delete( context, id, version2 ).execute();

        entities = serializationStrategy.load( context, id, current, 3 );

        assertEquals( 1, entities.size() );

        assertEquals( clearedV3, entities.get( 0 ) );


        //now get it, should be gone
        serializationStrategy.delete( context, id, version3 ).execute();


        entities = serializationStrategy.load( context, id, current, 3 );

        assertEquals( 0, entities.size() );
    }


    @Test
    public void writeLoadDeletePartial() throws ConnectionException {

        final Id organizationId = new SimpleId( "organization" );
        final Id applicationId = new SimpleId( "application" );
        final String name = "test";

        CollectionScope context = new CollectionScopeImpl( organizationId,  applicationId, name );


        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final String type = "test";

        final Id id = new SimpleId( entityId, type );

        Entity entity = new Entity( id );

        EntityUtils.setVersion( entity, version );


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


        MvccEntity saved = new MvccEntityImpl( id, version, MvccEntity.Status.PARTIAL, Optional.of( entity ) );


        //persist the entity
        serializationStrategy.write( context, saved ).execute();

        //now load it back

        MvccEntity returned = serializationStrategy.load( context, id, version );

        assertEquals( "Mvcc entities are the same", saved, returned );


        assertEquals( id, entity.getId() );


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


        assertEquals( id, entity.getId() );
        assertEquals( version, entity.getVersion() );


        //now delete it
        serializationStrategy.delete( context, id, version ).execute();

        //now get it, should be gone

        returned = serializationStrategy.load( context, id, version );

        assertNull( returned );
    }


    @Test(expected = NullPointerException.class)
    public void writeParamsContext() throws ConnectionException {
        serializationStrategy.write( null, mock( MvccEntity.class ) );
    }


    @Test(expected = NullPointerException.class)
    public void writeParamsEntity() throws ConnectionException {
        serializationStrategy.write(
                new CollectionScopeImpl( new SimpleId( "organization" ), new SimpleId( "test" ), "test" ), null );
    }


    @Test(expected = NullPointerException.class)
    public void deleteParamContext() throws ConnectionException {
        serializationStrategy.delete( null, new SimpleId( "test" ), UUIDGenerator.newTimeUUID() );
    }


    @Test(expected = NullPointerException.class)
    public void deleteParamEntityId() throws ConnectionException {

        serializationStrategy.delete(
                new CollectionScopeImpl( new SimpleId( "organization" ), new SimpleId( "test" ), "test" ), null,
                UUIDGenerator.newTimeUUID() );
    }


    @Test(expected = NullPointerException.class)
    public void deleteParamVersion() throws ConnectionException {

        serializationStrategy
                .delete( new CollectionScopeImpl( new SimpleId( "organization" ), new SimpleId( "test" ), "test" ),
                        new SimpleId( "test" ), null );
    }


    @Test(expected = NullPointerException.class)
    public void loadParamContext() throws ConnectionException {
        serializationStrategy.load( null, new SimpleId( "test" ), UUIDGenerator.newTimeUUID() );
    }


    @Test(expected = NullPointerException.class)
    public void loadParamEntityId() throws ConnectionException {

        serializationStrategy
                .load( new CollectionScopeImpl(new SimpleId( "organization" ), new SimpleId( "test" ), "test" ), null, UUIDGenerator.newTimeUUID() );
    }


    @Test(expected = NullPointerException.class)
    public void loadParamVersion() throws ConnectionException {

        serializationStrategy
                .load( new CollectionScopeImpl(new SimpleId( "organization" ), new SimpleId( "test" ), "test" ), new SimpleId( "test" ), null );
    }


    @Test(expected = NullPointerException.class)
    public void loadListParamContext() throws ConnectionException {
        serializationStrategy.load( null, new SimpleId( "test" ), UUIDGenerator.newTimeUUID(), 1 );
    }


    @Test(expected = NullPointerException.class)
    public void loadListParamEntityId() throws ConnectionException {

        serializationStrategy
                .load( new CollectionScopeImpl(new SimpleId( "organization" ), new SimpleId( "test" ), "test" ), null, UUIDGenerator.newTimeUUID(),
                        1 );
    }


    @Test(expected = NullPointerException.class)
    public void loadListParamVersion() throws ConnectionException {

        serializationStrategy
                .load( new CollectionScopeImpl(new SimpleId( "organization" ), new SimpleId( "test" ), "test" ), new SimpleId( "test" ), null, 1 );
    }


    @Test(expected = IllegalArgumentException.class)
    public void loadListParamSize() throws ConnectionException {

        serializationStrategy.load( new CollectionScopeImpl(new SimpleId( "organization" ), new SimpleId( "test" ), "test" ), new SimpleId( "test" ),
                UUIDGenerator.newTimeUUID(), 0 );
    }





}
