/*
 *
 *
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
 *
 *
 */

package org.apache.usergrid.persistence.collection.serialization.impl;


import java.nio.ByteBuffer;
import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.FloatField;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.junit.Assert.assertEquals;


public class UniqueFieldEntrySerializerTest {


    private static final UniqueFieldEntrySerializer TEST = new UniqueFieldEntrySerializer();


    @Test
    public void stringField() {
        doTest( new StringField( "test", "value", true ) );
    }


    @Test
    public void longField() {
        doTest( new LongField( "test", 10l, true ) );
    }


    @Test
    public void intField() {
        doTest( new IntegerField( "test", 9, true ) );
    }


    @Test
    public void floatField() {
        doTest( new FloatField( "test", .9f, true ) );
    }


    @Test
    public void doubleField() {
        doTest( new DoubleField( "test", .9, true ) );
    }


    @Test
    public void uuidField() {
        doTest( new UUIDField( "test", UUIDGenerator.newTimeUUID(), true ) );
    }


    @Test
    public void booleanField(){
        doTest( new BooleanField( "test", true, true ) );
    }

    public void doTest( Field<?> field ) {


        final UUID version = UUIDGenerator.newTimeUUID();

        final UniqueFieldEntry entry = new UniqueFieldEntry( version, field );

        final ByteBuffer buffer = TEST.toByteBuffer( entry );


        final UniqueFieldEntry returned = TEST.fromByteBuffer( buffer );

        assertEquals( "Same field", field, returned.getField() );

        assertEquals( "Same version", version, returned.getVersion() );
    }
}
