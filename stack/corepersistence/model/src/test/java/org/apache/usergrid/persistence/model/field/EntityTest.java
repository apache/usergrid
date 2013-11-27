package org.apache.usergrid.persistence.model.field;


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertTrue;


/** Simple test for validating stored entities */
public class EntityTest
{

    @Test
    public void testPutAndGet()
    {


        final UUID uuid = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final String type = "test";
        final long created = 1l;
        final long updated = 2l;

        Entity entity = new Entity( uuid, type );

        entity.setVersion( version );
        entity.setCreated( created );
        entity.setUpdated( updated );

        assertEquals( uuid, entity.getUuid() );
        assertEquals( type, entity.getType() );
        assertEquals( created, entity.getCreated() );
        assertEquals( updated, entity.getUpdated() );


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


        assertEquals( uuid, entity.getUuid() );
        assertEquals( version, entity.getVersion() );
    }


    @Test( expected = NullPointerException.class )
    public void uuidRequired()
    {
        new Entity( null, "test" );
    }


    @Test( expected = NullPointerException.class )
    public void versionRequired()
    {
        final UUID uuid = UUIDGenerator.newTimeUUID();

        new Entity( uuid, null );
    }
}
