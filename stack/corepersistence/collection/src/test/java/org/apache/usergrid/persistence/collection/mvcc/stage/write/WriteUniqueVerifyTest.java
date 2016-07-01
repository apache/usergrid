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


import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import org.apache.usergrid.persistence.actorsystem.ActorSystemManager;
import org.apache.usergrid.persistence.collection.AbstractUniqueValueTest;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.uniquevalues.UniqueValuesService;
import org.apache.usergrid.persistence.core.astyanax.CassandraConfig;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator.fromEntity;
import static org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator.generateEntity;
import static org.mockito.Mockito.*;


@RunWith( ITRunner.class )
@UseModules( TestCollectionModule.class )
public class WriteUniqueVerifyTest extends AbstractUniqueValueTest {

    @Inject
    private EntityCollectionManagerFactory factory;

    @Inject
    private UniqueValueSerializationStrategy uvstrat;

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Inject
    private SerializationFig fig;

    @Inject
    private CassandraConfig cassandraConfig;

    @Inject
    ActorSystemManager actorSystemManager;

    @Inject
    UniqueValuesService uniqueValuesService;


    @Before
    public void initAkka() {
        // each test class needs unique port number
        initAkka( 2552, actorSystemManager, uniqueValuesService );
    }


    @Test
    public void testNoFields() throws ConnectionException {

        final ApplicationScope collectionScope = mock( ApplicationScope.class );
        final Keyspace keyspace = mock(Keyspace.class);
        final MutationBatch batch = mock(MutationBatch.class);

        when(keyspace.prepareMutationBatch()).thenReturn(batch);

        // set up the mock to return the entity from the start phase
        final Entity entity = generateEntity();

        final MvccEntity mvccEntity = fromEntity( entity );

        // run the stage
        WriteUniqueVerify newStage = new WriteUniqueVerify( uvstrat, fig, keyspace, cassandraConfig, null, null, null );

       newStage.call( new CollectionIoEvent<>( collectionScope, mvccEntity ) ) ;

       // if we get here, it's a success.  We want to test no exceptions are thrown

        verify(batch, never()).execute();
    }

}


