/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.exception.EntityTooLargeException;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.util.EntityHelper;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.guicyfig.SetConfigTestBypass;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.junit.Assert.assertNotNull;


public abstract class MvccEntitySerializationStrategyV2Test extends MvccEntitySerializationStrategyImplTest {


    @Inject
    protected SerializationFig serializationFig;

    @Inject
    protected CassandraFig cassandraFig;


    private int setMaxEntitySize;


    @Before
    public void setUp() {


        setMaxEntitySize = serializationFig.getMaxEntitySize();
    }


    @After
    public void tearDown() {
        SetConfigTestBypass.setValueByPass( serializationFig, "getMaxEntitySize", setMaxEntitySize + "" );
    }


    /**
     * Tests an entity with more than  65535 bytes worth of data is successfully stored and retrieved
     */
    @Test
    public void largeEntityWriteRead() throws ConnectionException {
        final int setSize = 65535 * 2;


        //this is the size it works out to be when serialized, we want to allow this size

        SetConfigTestBypass.setValueByPass( serializationFig, "getMaxEntitySize", 65535 * 10 + "" );
        final Entity entity = EntityHelper.generateEntity( setSize );

        //now we have one massive, entity, save it and retrieve it.
        final Id applicationId = new SimpleId( "application" );

        ApplicationScope context = new ApplicationScopeImpl( applicationId );


        final Id id = entity.getId();
        ValidationUtils.verifyIdentity( id );
        final UUID version = UUIDGenerator.newTimeUUID();
        final MvccEntity.Status status = MvccEntity.Status.COMPLETE;

        final MvccEntity mvccEntity = new MvccEntityImpl( id, version, status, entity );


        getMvccEntitySerializationStrategy().write( context, mvccEntity ).execute();

        //now load it
        final MvccEntity loadedEntity =
                getMvccEntitySerializationStrategy().load( context, id ).get();



        assertLargeEntity( mvccEntity, loadedEntity );


        MvccEntity returned =
                serializationStrategy.load( context, Collections.singleton( id ), version ).getEntity( id );

        assertLargeEntity( mvccEntity, returned );
    }


    /**
     * Tests an entity with more than  65535 bytes worth of data is successfully stored and retrieved
     */
    @Test( expected = EntityTooLargeException.class )
    public void entityLargerThanAllowedWrite() throws ConnectionException {
        final int setSize = serializationFig.getMaxEntitySize() + 1;

        final Entity entity = EntityHelper.generateEntity( setSize );

        //now we have one massive, entity, save it and retrieve it.
        final Id applicationId = new SimpleId( "application" );

              ApplicationScope context = new ApplicationScopeImpl( applicationId );


        final Id id = entity.getId();
        ValidationUtils.verifyIdentity( id );
        final UUID version = UUIDGenerator.newTimeUUID();
        final MvccEntity.Status status = MvccEntity.Status.COMPLETE;

        final MvccEntity mvccEntity = new MvccEntityImpl( id, version, status, entity );


        getMvccEntitySerializationStrategy().write( context, mvccEntity ).execute();
    }


    /**
     * Tests an entity with more than  65535 bytes worth of data is successfully stored and retrieved
     */
    @Test
    public void largeEntityReadWrite() throws ConnectionException {

        //this is the size it works out to be when serialized, we want to allow this size

        //extreme edge case, we can only get 2 entities per call
        final int thriftBuffer = cassandraFig.getThriftBufferSize();



        //we use 20, using 2 causes cassandra to OOM. We don't have a large enough instance running locally

        final int maxEntitySize = ( int ) ( ( thriftBuffer * .9 ) / 20  );


        SetConfigTestBypass.setValueByPass( serializationFig, "getMaxEntitySize", maxEntitySize + "" );


        final int size = 100;

        final HashMap<Id, MvccEntity> entities = new HashMap<>( size );

        ApplicationScope context =
                new ApplicationScopeImpl( new SimpleId( "organization" ));


        for ( int i = 0; i < size; i++ ) {
            final Entity entity = EntityHelper.generateEntity( ( int ) (maxEntitySize*.4) );

            //now we have one massive, entity, save it and retrieve it.

            final Id id = entity.getId();
            ValidationUtils.verifyIdentity( id );
            final UUID version = UUIDGenerator.newTimeUUID();
            final MvccEntity.Status status = MvccEntity.Status.COMPLETE;

            final MvccEntity mvccEntity = new MvccEntityImpl( id, version, status, entity );


            getMvccEntitySerializationStrategy().write( context, mvccEntity ).execute();

            entities.put( id, mvccEntity );
        }


        //now load it, we ask for 100 and we only are allowed 2 per trip due to our max size constraints.  Should all
        //still load (note that users should not be encouraged to use this strategy, it's a bad idea!)
        final EntitySet loaded =
                getMvccEntitySerializationStrategy().load( context, entities.keySet(), UUIDGenerator.newTimeUUID() );

        assertNotNull( "Entity set was loaded", loaded );


        for ( Map.Entry<Id, MvccEntity> entry : entities.entrySet() ) {

            final MvccEntity returned = loaded.getEntity( entry.getKey() );

            assertLargeEntity( entry.getValue(), returned );
        }
    }




    protected void assertLargeEntity( final MvccEntity expected, final MvccEntity returned ) {

        org.junit.Assert.assertEquals( "The loaded entity should match the stored entity", expected, returned );

        EntityHelper.verifyDeepEquals( expected.getEntity().get(), returned.getEntity().get() );
    }
}
