/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.ArrayField;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;
import org.apache.usergrid.persistence.model.util.EntityUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;

//import org.safehaus.chop.api.IterationChop;


/**
 * Tests for serialization strategy
 */
public abstract class MvccEntitySerializationStrategyImplTest {


    protected MvccEntitySerializationStrategy serializationStrategy;


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Inject
    public CassandraFig cassandraFig;


    @Before
    public void setup() {
        assertNotNull( cassandraFig );
        serializationStrategy = getMvccEntitySerializationStrategy();
    }


    @Test
    public void writeLoadDelete() throws ConnectionException {

        final Id organizationId = new SimpleId( "organization" );

        ApplicationScope context = new ApplicationScopeImpl( organizationId );


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

        ArrayField arrayField = new ArrayField("array");
        arrayField.add("item1");
        arrayField.add("item2");

        entity.setField( boolField );
        entity.setField( doubleField );
        entity.setField( intField );
        entity.setField( longField );
        entity.setField( stringField );
        entity.setField( uuidField );
        entity.setField( arrayField );

        MvccEntity saved = new MvccEntityImpl( id, version, MvccEntity.Status.COMPLETE, Optional.of( entity ) );


        //persist the entity
        serializationStrategy.write( context, saved ).execute();

        //now load it back

        MvccEntity returned = serializationStrategy.load( context, Collections.singleton( id), version ).getEntity( id );

        assertEquals( "Mvcc entities are the same", saved, returned );


        assertEquals( id, returned.getId() );


        Field<Boolean> boolFieldReturned = returned.getEntity().get().getField( boolField.getName() );

        assertEquals( boolField, boolFieldReturned );

        Field<Double> doubleFieldReturned = returned.getEntity().get().getField( doubleField.getName() );

        assertEquals( doubleField, doubleFieldReturned );

        Field<Integer> intFieldReturned = returned.getEntity().get().getField( intField.getName() );

        assertEquals( intField, intFieldReturned );

        Field<Long> longFieldReturned = returned.getEntity().get().getField( longField.getName() );

        assertEquals( longField, longFieldReturned );

        Field<String> stringFieldReturned = returned.getEntity().get().getField( stringField.getName() );

        assertEquals( stringField, stringFieldReturned );

        Field<UUID> uuidFieldReturned = returned.getEntity().get().getField( uuidField.getName() );

        assertEquals( uuidField, uuidFieldReturned );

        Field<ArrayField> arrayFieldReturned = returned.getEntity().get().getField( arrayField.getName() );

        assertEquals( arrayField, arrayFieldReturned );


        Set<Field> results = new HashSet<Field>();
        results.addAll( returned.getEntity().get().getFields());


        assertTrue( results.contains( boolField ) );
        assertTrue( results.contains( doubleField ) );
        assertTrue( results.contains( intField ) );
        assertTrue( results.contains( longField ) );
        assertTrue( results.contains( stringField ) );
        assertTrue( results.contains( uuidField ) );

        assertEquals( 7, results.size() );


        assertEquals( id, entity.getId() );
        assertEquals( version, entity.getVersion() );


        //now delete it, should remove it from cass
        serializationStrategy.delete( context, id, version ).execute();

        //now get it, should be gone

        returned = serializationStrategy.load( context, Collections.singleton( id), version ).getEntity( id );

        assertNull( returned );
    }

    @Test
    public void writeLoadClearDelete() throws ConnectionException {

        final Id applicationId = new SimpleId( "application" );

        ApplicationScope context = new ApplicationScopeImpl( applicationId );


        final UUID version = UUIDGenerator.newTimeUUID();

        final Id entityId = new SimpleId( "test" );

        Entity entity = new Entity( entityId );

        EntityUtils.setVersion( entity, version );


        MvccEntity saved = new MvccEntityImpl( entityId, version, MvccEntity.Status.COMPLETE, Optional.of( entity ) );


        //persist the entity
        serializationStrategy.write( context, saved ).execute();

        //now load it back

        MvccEntity returned = serializationStrategy.load( context, Collections.singleton( entityId ), version ).getEntity( entityId );

        assertEquals( "Mvcc entities are the same", saved, returned );


        assertEquals( entityId, returned.getId() );

        //check the target entity has the right id
        assertEquals( entityId, returned.getEntity().get().getId() );


        //now mark it

        serializationStrategy.mark( context, entityId, version ).execute();

        returned = serializationStrategy.load( context, Collections.singleton( entityId ), version ).getEntity( entityId );

        assertEquals( entityId, returned.getId() );
        assertEquals( version, returned.getVersion() );
        assertFalse( returned.getEntity().isPresent() );
        assertEquals( MvccEntity.Status.DELETED, returned.getStatus());

        //now delete it
        serializationStrategy.delete( context, entityId, version ).execute();

        //now get it, should be gone

        returned = serializationStrategy.load( context, Collections.singleton( entityId ), version ).getEntity( entityId );

        assertNull( returned );
    }

    @Test
    public void writeLoadDeleteMinimalFields() throws ConnectionException {

        final Id applicationId = new SimpleId( "application" );

         ApplicationScope context = new ApplicationScopeImpl( applicationId );


        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final String type = "test";

        final Id id = new SimpleId( entityId, type );

        Entity entity = new Entity( id );

        EntityUtils.setVersion( entity, version );

        BooleanField boolField = new BooleanField( "boolean", false );

        entity.setField( boolField );

        MvccEntity saved = new MvccEntityImpl( id, version, MvccEntity.Status.COMPLETE, Optional.of( entity ) );


        //persist the entity
        serializationStrategy.write( context, saved ).execute();

        //now load it back

        MvccEntity returned = serializationStrategy.load( context, Collections.singleton( id ), version ).getEntity( id );

        assertEquals( "Mvcc entities are the same", saved, returned );


        assertEquals( id, entity.getId() );

        //TODO: TN-> shouldn't this be testing the returned value to make sure we were able to load it correctly?
        //YES THIS SHOULD BE DOING WHA TI THOUGHT< BUT ITSN:T
        Field<Boolean> boolFieldReturned = returned.getEntity().get().getField( boolField.getName() );

        assertEquals( boolField, boolFieldReturned );

        Set<Field> results = new HashSet<Field>();
        results.addAll( entity.getFields() );


        assertTrue( results.contains( boolField ) );


        assertEquals( 1, results.size() );

        assertEquals( id, entity.getId() );
        assertEquals( version, entity.getVersion() );


        //now delete it
        serializationStrategy.delete( context, id, version ).execute();

        //now get it, should be gone

        returned = serializationStrategy.load( context, Collections.singleton( id ) , version ).getEntity( id );

        assertNull( returned );
    }

    @Test
    public void writeX2ClearDelete() throws ConnectionException {

        final Id applicationId = new SimpleId( "application" );

        ApplicationScope context = new ApplicationScopeImpl( applicationId );


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

        MvccEntity returnedV1 =
            serializationStrategy.load( context, Collections.singleton( id ), version1 ).getEntity( id );

        assertEquals( "Mvcc entities are the same", saved, returnedV1 );


        //now write a new version of it


        Entity entityv2 = new Entity( id );

        UUID version2 = UUIDGenerator.newTimeUUID();


        EntityUtils.setVersion( entityv1, version2 );


        MvccEntity savedV2 = new MvccEntityImpl( id, version2, MvccEntity.Status.COMPLETE, Optional.of( entityv2 ) );

        serializationStrategy.write( context, savedV2 ).execute();

        MvccEntity returnedV2 =
            serializationStrategy.load( context, Collections.singleton( id ), version2 ).getEntity( id );

        assertEquals( "Mvcc entities are the same", savedV2, returnedV2 );


        //now mark it at v3

        UUID version3 = UUIDGenerator.newTimeUUID();

        serializationStrategy.mark( context, id, version3 ).execute();


        final Optional<Entity> empty = Optional.absent();

        MvccEntity clearedV3 = new MvccEntityImpl( id, version3, MvccEntity.Status.DELETED, empty );

        MvccEntity returnedV3 =
            serializationStrategy.load( context, Collections.singleton( id ), version3 ).getEntity( id );

        assertEquals( "entities are the same", clearedV3, returnedV3 );

        //now ask for up to 10 versions from the current version, we should get cleared, v2, v1
        UUID current = UUIDGenerator.newTimeUUID();

        MvccEntity first = serializationStrategy.load( context, id ).get();

        assertEquals( clearedV3, first );


        //now delete v2 and v1, we should still get v3
        serializationStrategy.delete( context, id, version1 ).execute();
        serializationStrategy.delete( context, id, version2 ).execute();

        first = serializationStrategy.load( context, id ).get();

        assertEquals( clearedV3, first );


        //now get it, should be gone
        serializationStrategy.delete( context, id, version3 ).execute();


        assertFalse( "Not loaded", serializationStrategy.load( context, id ).isPresent() );

    }

    @Test
    public void loadAscendingHistory()  throws ConnectionException  {
        final Id applicationId = new SimpleId( "application" );

         ApplicationScope context = new ApplicationScopeImpl( applicationId );


        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version1 = UUIDGenerator.newTimeUUID();
        final String type = "test";

        final Id id = new SimpleId(entityId, type);
        Entity entityv1 = new Entity(id);
        EntityUtils.setVersion(entityv1, version1);
        MvccEntity saved = new MvccEntityImpl(id, version1, MvccEntity.Status.COMPLETE, Optional.of(entityv1));
        //persist the entity
        serializationStrategy.write(context, saved).execute();

        //now write a new version of it
        Entity entityv2 = new Entity(id);
        UUID version2 = UUIDGenerator.newTimeUUID();
        EntityUtils.setVersion(entityv1, version2);
        MvccEntity savedV2 = new MvccEntityImpl(id, version2, MvccEntity.Status.COMPLETE, Optional.of(entityv2));
        serializationStrategy.write(context, savedV2).execute();

        Iterator<MvccEntity> entities = serializationStrategy.loadAscendingHistory( context, id, savedV2.getVersion(),
                20 );
        assertTrue(entities.hasNext());
        assertEquals(saved.getVersion(), entities.next().getVersion());
        assertEquals(savedV2.getVersion(), entities.next().getVersion());
        assertFalse(entities.hasNext());

    }


    /**
     * We no longer support partial writes, ensure that an exception is thrown when this occurs after v3
     * @throws ConnectionException
     */
    @Test(expected = UnsupportedOperationException.class)
    public void writeLoadDeletePartial() throws ConnectionException {

        final Id applicationId = new SimpleId( "application" );

        ApplicationScope context = new ApplicationScopeImpl( applicationId );


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

    }




    @Test(expected = NullPointerException.class)
    public void writeParamsContext() throws ConnectionException {
        serializationStrategy.write( null, mock( MvccEntity.class ) );
    }


    @Test(expected = NullPointerException.class)
    public void writeParamsEntity() throws ConnectionException {
        serializationStrategy.write(
                new ApplicationScopeImpl( new SimpleId( "organization" )), null );
    }


    @Test(expected = NullPointerException.class)
    public void deleteParamContext() throws ConnectionException {
        serializationStrategy.delete( null, new SimpleId( "test" ), UUIDGenerator.newTimeUUID() );
    }


    @Test(expected = NullPointerException.class)
    public void deleteParamEntityId() throws ConnectionException {

        serializationStrategy.delete(
                new ApplicationScopeImpl( new SimpleId( "organization" ) ), null,
                UUIDGenerator.newTimeUUID() );
    }


    @Test(expected = NullPointerException.class)
    public void deleteParamVersion() throws ConnectionException {

        serializationStrategy
                .delete( new ApplicationScopeImpl( new SimpleId( "organization" )),
                        new SimpleId( "test" ), null );
    }


    @Test(expected = NullPointerException.class)
    public void loadParamContext() throws ConnectionException {
        serializationStrategy.load( null, Collections.<Id>emptyList(), UUIDGenerator.newTimeUUID() );
    }


    @Test(expected = NullPointerException.class)
    public void loadParamEntityId() throws ConnectionException {

        serializationStrategy
                .load( new ApplicationScopeImpl(new SimpleId( "organization" ) ), null, UUIDGenerator.newTimeUUID() );
    }


    @Test(expected = NullPointerException.class)
    public void loadParamVersion() throws ConnectionException {

        serializationStrategy
                .load( new ApplicationScopeImpl(new SimpleId( "organization" ) ), Collections.<Id>singleton( new SimpleId( "test" )), null );
    }


    @Test(expected = NullPointerException.class)
    public void loadListParamContext() throws ConnectionException {
        serializationStrategy.loadDescendingHistory( null, new SimpleId( "test" ), UUIDGenerator.newTimeUUID(), 1 );
    }


    @Test(expected = NullPointerException.class)
    public void loadListParamEntityId() throws ConnectionException {

        serializationStrategy
                .loadDescendingHistory(
                        new ApplicationScopeImpl( new SimpleId( "organization" ) ), null,
                        UUIDGenerator.newTimeUUID(), 1 );
    }


    @Test(expected = NullPointerException.class)
    public void loadListParamVersion() throws ConnectionException {

        serializationStrategy
                .loadDescendingHistory(
                        new ApplicationScopeImpl( new SimpleId( "organization" ) ),
                        new SimpleId( "test" ), null, 1 );
    }


    @Test(expected = IllegalArgumentException.class)
    public void loadListParamSize() throws ConnectionException {

        serializationStrategy.loadDescendingHistory(
                new ApplicationScopeImpl( new SimpleId( "organization" ) ),
                new SimpleId( "test" ), UUIDGenerator.newTimeUUID(), 0 );
    }


    /**
     * Get the serialization strategy to test
     * @return
     */
    protected abstract MvccEntitySerializationStrategy getMvccEntitySerializationStrategy();


}
