package org.apache.usergrid.persistence.collection.serialization.impl;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.smile.SmileFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.ArrayField;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.ByteBufferField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.EntityObjectField;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.ListField;
import org.apache.usergrid.persistence.model.field.LocationField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.SetField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.model.field.value.EntityObject;
import org.apache.usergrid.persistence.model.field.value.Location;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.ByteBufferOutputStream;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;


/**
 * TODO We need to get both of these serialization methods working, and benchmark them for comparison Neither works out
 * of the box for us without custom work.
 *
 * @author tnine
 */
public class SerializationComparison {

    private static final Logger logger = LoggerFactory.getLogger( SerializationComparison.class );

    private static final int count = 10000;


    @Test
    @Ignore
    public void smileSerialization() throws IOException {
        SmileFactory smile = new SmileFactory();

        ObjectMapper smileMapper = new ObjectMapper( smile );


        Entity entity = createEntity();

        long writeTime = 0;
        long readTime = 0;

        for ( int i = 0; i < count; i++ ) {

            //capture time in nannos for write
            long writeStart = System.nanoTime();

            byte[] smileData = smileMapper.writeValueAsBytes( entity );

            writeTime += System.nanoTime() - writeStart;

            long readStart = System.nanoTime();

            Entity otherValue = smileMapper.readValue( smileData, Entity.class );

            readTime += System.nanoTime() - readStart;
        }

        logger.info( "Smile took {} nanos for writing {} entities", writeTime, count );
        logger.info( "Smile took {} nanos for reading {} entities", readTime, count );
    }


    @Test
    @Ignore
    public void kyroSerialization() {
        Kryo kryo = new Kryo();

        //container classes
        kryo.register( Entity.class );

        kryo.register( EntityObject.class );
        kryo.register( Location.class );


        //field classes
        kryo.register( ArrayField.class );
        kryo.register( BooleanField.class );
        kryo.register( ByteBufferField.class );
        kryo.register( DoubleField.class );
        kryo.register( EntityObjectField.class );
        kryo.register( IntegerField.class );
        kryo.register( ListField.class );
        kryo.register( LocationField.class );
        kryo.register( LongField.class );
        kryo.register( SetField.class );
        kryo.register( StringField.class );
        kryo.register( UUIDField.class, new de.javakaffee.kryoserializers.UUIDSerializer() );


        long writeTime = 0;
        long readTime = 0;

        for ( int i = 0; i < count; i++ ) {

            //capture time in nanos for write
            long writeStart = System.nanoTime();

            ByteBuffer data = ByteBuffer.allocate( 1024 );
            ByteBufferOutputStream byteBuffOutputStream = new ByteBufferOutputStream( data );
            Output output = new Output( byteBuffOutputStream );

            Entity entity = createEntity();

            kryo.writeObject( output, entity );
            output.close();

            writeTime += System.nanoTime() - writeStart;

            data.rewind();

            long readStart = System.nanoTime();


            Input input = new Input( new ByteBufferInputStream( data ) );
            Entity loaded = kryo.readObject( input, Entity.class );
            input.close();

            readTime += System.nanoTime() - readStart;
        }

        logger.info( "Smile took {} nanos for writing {} entities", writeTime, count );
        logger.info( "Smile took {} nanos for reading {} entities", readTime, count );
    }


    private Entity createEntity() {

        final UUID version = UUIDGenerator.newTimeUUID();

        Entity entity = new Entity( new SimpleId( "test" ) );

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

        return entity;
    }
}




