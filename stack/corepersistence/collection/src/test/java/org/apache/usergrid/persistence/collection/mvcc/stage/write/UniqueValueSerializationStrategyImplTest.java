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
package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import java.util.UUID;

import org.jukito.UseModules;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.cassandra.CassandraRule;
import org.apache.usergrid.persistence.core.cassandra.ITRunner;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

@RunWith( ITRunner.class )
@UseModules( TestCollectionModule.class )
public class UniqueValueSerializationStrategyImplTest {
    private static final Logger LOG = LoggerFactory.getLogger( UniqueValueSerializationStrategyImplTest.class );

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;
    
    @Inject
    UniqueValueSerializationStrategy strategy;


    @Test
    public void testBasicOperation() throws ConnectionException, InterruptedException {

        CollectionScope scope = new CollectionScopeImpl(
                new SimpleId( "organization" ), new SimpleId( "test" ), "test" );

        IntegerField field = new IntegerField( "count", 5 );
        Id entityId = new SimpleId( UUIDGenerator.newTimeUUID(), "entity");
        UUID version = UUIDGenerator.newTimeUUID();
        UniqueValue stored = new UniqueValueImpl( scope, field, entityId, version );
        strategy.write( stored ).execute();

        UniqueValue retrieved = strategy.load( scope, field );
        Assert.assertNotNull( retrieved );
        Assert.assertEquals( stored, retrieved );
    }


    @Test
    public void testWriteWithTTL() throws InterruptedException, ConnectionException {
        
        CollectionScope scope = new CollectionScopeImpl(
                new SimpleId( "organization" ), new SimpleId( "test" ), "test" );

        // write object that lives 2 seconds
        IntegerField field = new IntegerField( "count", 5 );
        Id entityId = new SimpleId( UUIDGenerator.newTimeUUID(), "entity");
        UUID version = UUIDGenerator.newTimeUUID();
        UniqueValue stored = new UniqueValueImpl( scope, field, entityId, version );
        strategy.write( stored, 2 ).execute();

        Thread.sleep( 1000 );

        // waited one sec, should be still here
        UniqueValue retrieved = strategy.load( scope, field );
        Assert.assertNotNull( retrieved );
        Assert.assertEquals( stored, retrieved );

        Thread.sleep( 1500 );

        // wait another second, should be gone now
        UniqueValue nullExpected = strategy.load( scope, field );
        Assert.assertNull( nullExpected );
    }


    @Test
    public void testDelete() throws ConnectionException {

        CollectionScope scope = new CollectionScopeImpl(
                new SimpleId( "organization" ), new SimpleId( "test" ), "test" );

        IntegerField field = new IntegerField( "count", 5 );
        Id entityId = new SimpleId( UUIDGenerator.newTimeUUID(), "entity");
        UUID version = UUIDGenerator.newTimeUUID();
        UniqueValue stored = new UniqueValueImpl( scope, field, entityId, version );
        strategy.write( stored ).execute();

        strategy.delete( stored ).execute();

        UniqueValue nullExpected = strategy.load( scope, field );
        Assert.assertNull( nullExpected );
    }
}
