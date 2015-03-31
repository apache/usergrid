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
package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.collection.serialization.impl.UniqueValueImpl;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@RunWith(ITRunner.class)
@UseModules(TestCollectionModule.class)
public abstract class UniqueValueSerializationStrategyImplTest {


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    private UniqueValueSerializationStrategy strategy;


    @Before
    public void wireUniqueSerializationStrategy(){
        strategy = getUniqueSerializationStrategy();
    }


    /**
     * Get the unique value serialization
     * @return
     */
    protected abstract UniqueValueSerializationStrategy getUniqueSerializationStrategy();


    @Test
    public void testBasicOperation() throws ConnectionException, InterruptedException {

        ApplicationScope scope =
                new ApplicationScopeImpl( new SimpleId( "organization" ) );

        IntegerField field = new IntegerField( "count", 5 );
        Id entityId = new SimpleId( UUIDGenerator.newTimeUUID(), "entity" );
        UUID version = UUIDGenerator.newTimeUUID();
        UniqueValue stored = new UniqueValueImpl( field, entityId, version );
        strategy.write( scope, stored ).execute();

        UniqueValueSet fields = strategy.load( scope, entityId.getType(), Collections.<Field>singleton( field ) );

        UniqueValue retrieved = fields.getValue( field.getName() );
        Assert.assertNotNull( retrieved );
        assertEquals( stored, retrieved );

        Iterator<UniqueValue> allFieldsWritten = strategy.getAllUniqueFields( scope, entityId );

        assertTrue(allFieldsWritten.hasNext());

        //test this interface. In most cases, we won't know the field name, so we want them all
        UniqueValue allFieldsValue = allFieldsWritten.next();
        Assert.assertNotNull( allFieldsValue );

        assertEquals( field, allFieldsValue.getField() );
        assertEquals(version, allFieldsValue.getEntityVersion());

        assertFalse(allFieldsWritten.hasNext());

    }


    @Test
    public void testWriteWithTTL() throws InterruptedException, ConnectionException {


        ApplicationScope scope =
                new ApplicationScopeImpl( new SimpleId( "organization" ) );

        // write object that lives 2 seconds
        IntegerField field = new IntegerField( "count", 5 );
        Id entityId = new SimpleId( UUIDGenerator.newTimeUUID(), "entity" );
        UUID version = UUIDGenerator.newTimeUUID();
        UniqueValue stored = new UniqueValueImpl( field, entityId, version );
        strategy.write( scope, stored, 2 ).execute();

        Thread.sleep( 1000 );

        // waited one sec, should be still here
        UniqueValueSet fields = strategy.load( scope, entityId.getType(), Collections.<Field>singleton( field ) );

        UniqueValue retrieved = fields.getValue( field.getName() );

        Assert.assertNotNull( retrieved );
        assertEquals( stored, retrieved );

        Thread.sleep( 1500 );

        // wait another second, should be gone now
        fields = strategy.load( scope, entityId.getType(), Collections.<Field>singleton( field ) );

        UniqueValue nullExpected = fields.getValue( field.getName() );
        Assert.assertNull( nullExpected );


        //we still want to retain the log entry, even if we don't retain the unique value.  Deleting something
        //that doesn't exist is a tombstone, but so is the timeout.
        Iterator<UniqueValue> allFieldsWritten = strategy.getAllUniqueFields( scope, entityId );

        assertTrue( allFieldsWritten.hasNext() );

        //test this interface. In most cases, we won't know the field name, so we want them all
        UniqueValue writtenFieldEntry = allFieldsWritten.next();
        Assert.assertNotNull( writtenFieldEntry );

        assertEquals( field, writtenFieldEntry.getField() );
        assertEquals( version, writtenFieldEntry.getEntityVersion() );

        assertFalse(allFieldsWritten.hasNext());



    }


    @Test
    public void testDelete() throws ConnectionException {


        ApplicationScope scope =
                new ApplicationScopeImpl( new SimpleId( "organization" ) );

        IntegerField field = new IntegerField( "count", 5 );
        Id entityId = new SimpleId( UUIDGenerator.newTimeUUID(), "entity" );
        UUID version = UUIDGenerator.newTimeUUID();
        UniqueValue stored = new UniqueValueImpl( field, entityId, version );
        strategy.write( scope, stored ).execute();

        strategy.delete( scope, stored ).execute();

        UniqueValueSet fields = strategy.load( scope, entityId.getType(), Collections.<Field>singleton( field ) );

        UniqueValue nullExpected = fields.getValue( field.getName() );


        Assert.assertNull( nullExpected );


        Iterator<UniqueValue> allFieldsWritten = strategy.getAllUniqueFields( scope, entityId );

        assertFalse("No entries left",  allFieldsWritten.hasNext() );
    }


    @Test
    public void testCapitalizationFixes() throws ConnectionException {

        ApplicationScope scope =
                new ApplicationScopeImpl( new SimpleId( "organization" ) );

        StringField field = new StringField( "count", "MiXeD CaSe" );
        Id entityId = new SimpleId( UUIDGenerator.newTimeUUID(), "entity" );
        UUID version = UUIDGenerator.newTimeUUID();
        UniqueValue stored = new UniqueValueImpl( field, entityId, version );
        strategy.write( scope, stored ).execute();


        UniqueValueSet fields = strategy.load( scope, entityId.getType(), Collections.<Field>singleton( field ) );

        UniqueValue value = fields.getValue( field.getName() );


        assertEquals( field.getName(), value.getField().getName() );

        assertEquals( entityId, value.getEntityId() );

        //now test will all upper and all lower, we should get it all the same
        fields = strategy.load( scope, entityId.getType(),
                Collections.<Field>singleton( new StringField( field.getName(), "MIXED CASE" ) ) );

        value = fields.getValue( field.getName() );


        assertEquals( field.getName(), value.getField().getName() );

        assertEquals( entityId, value.getEntityId() );

        fields = strategy.load( scope, entityId.getType(),
                Collections.<Field>singleton( new StringField( field.getName(), "mixed case" ) ) );

        value = fields.getValue( field.getName() );


        assertEquals( field.getName(), value.getField().getName() );

        assertEquals( entityId, value.getEntityId() );


        Iterator<UniqueValue> allFieldsWritten = strategy.getAllUniqueFields( scope, entityId );

        assertTrue( allFieldsWritten.hasNext() );

        //test this interface. In most cases, we won't know the field name, so we want them all
        UniqueValue writtenFieldEntry = allFieldsWritten.next();
        Assert.assertNotNull( writtenFieldEntry );

        assertEquals( field.getName(), writtenFieldEntry.getField().getName() );
        assertEquals( field.getValue().toLowerCase(), writtenFieldEntry.getField().getValue() );
        assertEquals( version, writtenFieldEntry.getEntityVersion() );

        assertFalse(allFieldsWritten.hasNext());
    }



    @Test
    public void twoFieldsPerVersion() throws ConnectionException, InterruptedException {


        ApplicationScope scope =
                new ApplicationScopeImpl( new SimpleId( "organization" ) );


        Id entityId = new SimpleId( UUIDGenerator.newTimeUUID(), "entity" );
        final UUID version1 = UUIDGenerator.newTimeUUID();


        //write V1 of everything
        IntegerField version1Field1 = new IntegerField( "count", 1 );
        StringField version1Field2 = new StringField("field", "v1value");


        UniqueValue version1Field1Value = new UniqueValueImpl( version1Field1, entityId, version1 );
        UniqueValue version1Field2Value = new UniqueValueImpl( version1Field2, entityId, version1 );

        final MutationBatch batch = strategy.write( scope, version1Field1Value );
        batch.mergeShallow( strategy.write( scope, version1Field2Value ) );


        //write V2 of everything
        final UUID version2 = UUIDGenerator.newTimeUUID();

        IntegerField version2Field1 = new IntegerField( "count", 2 );
        StringField version2Field2 = new StringField( "field", "v2value" );


        UniqueValue version2Field1Value = new UniqueValueImpl( version2Field1, entityId, version2 );
        UniqueValue version2Field2Value = new UniqueValueImpl( version2Field2, entityId, version2 );

        batch.mergeShallow( strategy.write( scope, version2Field1Value ) );
        batch.mergeShallow( strategy.write( scope, version2Field2Value ) );

        batch.execute();


        UniqueValueSet fields = strategy.load( scope, entityId.getType(), Arrays.<Field>asList( version1Field1, version1Field2 ) );

        UniqueValue retrieved = fields.getValue( version1Field1.getName() );

        assertEquals( version1Field1Value, retrieved );


        retrieved = fields.getValue( version1Field2.getName() );
        assertEquals( version1Field2Value, retrieved );


        Iterator<UniqueValue> allFieldsWritten = strategy.getAllUniqueFields( scope, entityId );

        assertTrue(allFieldsWritten.hasNext());

        //test this interface. In most cases, we won't know the field name, so we want them all
        UniqueValue allFieldsValue = allFieldsWritten.next();

        //version 2 fields should come first, ordered by field name
        assertEquals( version2Field1, allFieldsValue.getField() );
        assertEquals( version2, allFieldsValue.getEntityVersion() );

        allFieldsValue = allFieldsWritten.next();

        assertEquals( version2Field2, allFieldsValue.getField() );
        assertEquals( version2, allFieldsValue.getEntityVersion() );


        //version 1 should come next ordered by field name
        allFieldsValue = allFieldsWritten.next();

        assertEquals( version1Field1, allFieldsValue.getField() );
        assertEquals( version1, allFieldsValue.getEntityVersion() );

        allFieldsValue = allFieldsWritten.next();

        assertEquals( version1Field2, allFieldsValue.getField() );
        assertEquals( version1, allFieldsValue.getEntityVersion() );

        assertFalse(allFieldsWritten.hasNext());

    }

}
