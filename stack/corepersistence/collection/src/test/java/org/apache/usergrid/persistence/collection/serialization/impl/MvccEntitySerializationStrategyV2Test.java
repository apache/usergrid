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


import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.safehaus.guicyfig.Bypass;
import org.safehaus.guicyfig.Env;
import org.safehaus.guicyfig.Option;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.exception.EntityTooLargeException;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.util.EntityHelper;
import org.apache.usergrid.persistence.core.guicyfig.SetConfigTestBypass;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.junit.Assert.assertTrue;


public abstract class MvccEntitySerializationStrategyV2Test extends MvccEntitySerializationStrategyImplTest {


    @Inject
    protected SerializationFig serializationFig;

    private int setMaxEntitySize;


    @Before
    public void setUp(){


      setMaxEntitySize =  serializationFig.getMaxEntitySize();
    }

    @After
    public void tearDown(){
        SetConfigTestBypass.setValueByPass( serializationFig, "getMaxEntitySize", setMaxEntitySize + "" );
    }

    /**
     * Tests an entity with more than  65535 bytes worth of data is successfully stored and retrieved
     */
    @Test
    public void largeEntityWriteRead() throws ConnectionException {
        final int setSize = 65535 * 2;


        //this is the size it works out to be when serialized, we want to allow this size

        SetConfigTestBypass.setValueByPass( serializationFig, "getMaxEntitySize", 65535*10+"");
        final Entity entity = EntityHelper.generateEntity( setSize );

        //now we have one massive, entity, save it and retrieve it.
        CollectionScope context =
                new CollectionScopeImpl( new SimpleId( "organization" ), new SimpleId( "parent" ), "tests" );


        final Id id = entity.getId();
        ValidationUtils.verifyIdentity( id );
        final UUID version = UUIDGenerator.newTimeUUID();
        final MvccEntity.Status status = MvccEntity.Status.COMPLETE;

        final MvccEntity mvccEntity = new MvccEntityImpl( id, version, status, entity );


        getMvccEntitySerializationStrategy().write( context, mvccEntity ).execute();

        //now load it
        final Iterator<MvccEntity> loaded =
                getMvccEntitySerializationStrategy().loadDescendingHistory( context, id, version, 100 );


        assertLargeEntity( mvccEntity, loaded );

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
        CollectionScope context =
                new CollectionScopeImpl( new SimpleId( "organization" ), new SimpleId( "parent" ), "tests" );


        final Id id = entity.getId();
        ValidationUtils.verifyIdentity( id );
        final UUID version = UUIDGenerator.newTimeUUID();
        final MvccEntity.Status status = MvccEntity.Status.COMPLETE;

        final MvccEntity mvccEntity = new MvccEntityImpl( id, version, status, entity );


        getMvccEntitySerializationStrategy().write( context, mvccEntity ).execute();
    }


    protected void assertLargeEntity( final MvccEntity expected, final Iterator<MvccEntity> returned ) {
        assertTrue( returned.hasNext() );

        final MvccEntity loadedEntity = returned.next();

        assertLargeEntity( expected, loadedEntity );
    }


    protected void assertLargeEntity( final MvccEntity expected, final MvccEntity returned ) {

        org.junit.Assert.assertEquals( "The loaded entity should match the stored entity", expected, returned );

        EntityHelper.verifyDeepEquals( expected.getEntity().get(), returned.getEntity().get() );
    }




}
