package org.apache.usergrid.persistence.model.field;


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import org.apache.commons.lang3.reflect.FieldUtils;

import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertTrue;


/** Simple test for validating stored entities */
public class EntityTest
{

    @Test
    public void testPutAndGet() throws IllegalAccessException {


        final UUID uuid = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final String type = "test";
        final Id simpleId = new SimpleId( uuid, type );


        Entity entity = new Entity(simpleId );

        FieldUtils.writeDeclaredField( entity, "version", version, true );
        assertEquals( uuid, entity.getId().getUuid() );
        assertEquals( type, entity.getId().getType() );
        assertEquals( version, entity.getVersion());


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


        assertEquals( simpleId, entity.getId() );
        assertEquals( version, entity.getVersion() );
    }


    @Test( expected = NullPointerException.class )
    public void idRequired()
    {
        new Entity( (Id)null);
    }

    @Test( expected = NullPointerException.class )
        public void typeRequired()
        {
            new Entity( (String)null);
        }


}
