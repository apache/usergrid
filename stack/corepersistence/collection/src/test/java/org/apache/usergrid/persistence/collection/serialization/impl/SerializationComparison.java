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


import java.io.IOException;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;
import org.apache.usergrid.persistence.model.util.EntityUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

/**
 * TODO We need to get both of these serialization methods working, and benchmark them for
 * comparison Neither works out of the box for us without custom work.
 *
 * @author tnine
 */
public class SerializationComparison {

    private static final Logger logger = LoggerFactory.getLogger( SerializationComparison.class );

    private static final int count = 10000;


    @Test
    @Ignore("Too heavy for normal build process?")
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




