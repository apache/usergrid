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


import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;
import org.apache.usergrid.persistence.model.util.EntityUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


@RunWith( ITRunner.class )
@UseModules( TestCollectionModule.class )
public class MvccEntitySerializationStrategyV1ImplTest extends MvccEntitySerializationStrategyImplTest {

    @Inject
    private MvccEntitySerializationStrategyV1Impl serializationStrategy;



    @Override
    protected MvccEntitySerializationStrategy getMvccEntitySerializationStrategy() {
        return serializationStrategy;
    }



    /**
     * We no longer support partial writes, ensure that an exception is thrown when this occurs after v3
     * @throws com.netflix.astyanax.connectionpool.exceptions.ConnectionException
     */
    @Test
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

}
