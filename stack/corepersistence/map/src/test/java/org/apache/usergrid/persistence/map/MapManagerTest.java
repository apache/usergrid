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

package org.apache.usergrid.persistence.map;


import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.map.guice.TestMapModule;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


@RunWith( ITRunner.class )
@UseModules( { TestMapModule.class } )
public class MapManagerTest {


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected MapManagerFactory mmf;

    protected MapScope scope;


    @Before
    public void mockApp() {
        this.scope = new MapScopeImpl( new SimpleId( "application" ), "testMap" );
    }


    @Test
    public void writeReadString() {
        MapManager mm = mmf.createMapManager( this.scope );

        final String key = "key";
        final String value = "value";

        mm.putString( key, value );

        final String returned = mm.getString( key );

        assertEquals( value, returned );
    }


    @Test
    public void multiReadNoKey() {
        MapManager mm = mmf.createMapManager( this.scope );

        final String key = UUIDGenerator.newTimeUUID().toString();

        final Map<String, String> results = mm.getStrings( Collections.singleton( key ) );

        assertNotNull( results );

        final String shouldBeMissing = results.get( key );

        assertNull( shouldBeMissing );
    }


    @Test
    public void writeReadStringBatch() {
        MapManager mm = mmf.createMapManager( this.scope );

        final String key1 = "key1";
        final String value1 = "value1";

        mm.putString( key1, value1 );


        final String key2 = "key2";
        final String value2 = "value2";

        mm.putString( key2, value2 );


        final Map<String, String> returned = mm.getStrings( Arrays.asList( key1, key2 ) );

        assertNotNull( returned );

        assertEquals( value1, returned.get( key1 ) );
        assertEquals( value2, returned.get( key2 ) );
    }


    @Test
    public void writeReadStringTTL() throws InterruptedException {

        MapManager mm = mmf.createMapManager( this.scope );

        final String key = "key";
        final String value = "value";
        final int ttl = 5;


        mm.putString( key, value, ttl );

        final long startTime = System.currentTimeMillis();

        final String returned = mm.getString( key );

        assertEquals( value, returned );

        final long endTime = startTime + TimeUnit.SECONDS.toMillis( ttl + 1 );

        final long remaining = endTime - System.currentTimeMillis();

        //now sleep and assert it gets removed
        Thread.sleep( remaining );

        //now read it should be gone
        final String timedOut = mm.getString( key );

        assertNull( "Value was not returned", timedOut );
    }


    @Test
    public void writeReadUUID() {
        MapManager mm = mmf.createMapManager( this.scope );

        final String key = "key";
        final UUID value = UUID.randomUUID();

        mm.putUuid( key, value );

        final UUID returned = mm.getUuid( key );

        assertEquals( value, returned );
    }


    @Test
    public void writeReadLong() {
        MapManager mm = mmf.createMapManager( this.scope );

        final String key = "key";
        final Long value = 1234L;

        mm.putLong( key, value );

        final Long returned = mm.getLong( key );

        assertEquals( value, returned );
    }


    @Test
    public void readMissingEntry() {
        MapManager mm = mmf.createMapManager( this.scope );

        final String returned = mm.getString( "key" );

        assertNull( returned );

        final Long returnedL = mm.getLong( "key" );

        assertNull( returnedL );

        final UUID returnedUUID = mm.getUuid( "key" );

        assertNull( returnedUUID );
    }


    @Test
    public void deleteString() {
        MapManager mm = mmf.createMapManager( this.scope );

        final String key = "key";
        final String value = "value";

        mm.putString( key, value );

        final String returned = mm.getString( key );

        assertEquals( value, returned );

        mm.delete( key );

        final String postDelete = mm.getString( key );

        assertNull( postDelete );
    }


    @Test
    public void deleteUUID() {
        MapManager mm = mmf.createMapManager( this.scope );

        final String key = "key";
        final UUID value = UUID.randomUUID();

        mm.putUuid( key, value );

        final UUID returned = mm.getUuid( key );

        assertEquals( value, returned );

        mm.delete( key );

        final UUID postDelete = mm.getUuid( key );

        assertNull( postDelete );
    }


    @Test
    public void deleteLong() {
        MapManager mm = mmf.createMapManager( this.scope );

        final String key = "key";
        final Long value = 1L;

        mm.putLong( key, value );

        final Long returned = mm.getLong( key );

        assertEquals( value, returned );

        mm.delete( key );

        final Long postDelete = mm.getLong( key );

        assertNull( postDelete );
    }


    @Test( expected = NullPointerException.class )
    public void nullInputString() {
        MapManager mm = mmf.createMapManager( this.scope );

        mm.putString( null, null );
    }


    @Test( expected = NullPointerException.class )
    public void nullInputLong() {
        MapManager mm = mmf.createMapManager( this.scope );

        mm.putLong( null, null );
    }


    @Test( expected = NullPointerException.class )
    public void nullInputUUID() {
        MapManager mm = mmf.createMapManager( this.scope );

        mm.putUuid( null, null );
    }
}
