/**
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
package org.apache.usergrid.persistence.collection.uniquevalues;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Inject;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.actorsystem.ActorSystemManager;
import org.apache.usergrid.persistence.collection.AbstractUniqueValueTest;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.exception.WriteUniqueVerifyException;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.StringField;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.fail;


/**
 * Test the unique values service.
 */
@RunWith( ITRunner.class )
@UseModules( TestCollectionModule.class )
public class UniqueValuesServiceDeleteTest extends AbstractUniqueValueTest {
    private static final Logger logger = LoggerFactory.getLogger( UniqueValuesServiceDeleteTest.class );

    @Inject
    private EntityCollectionManagerFactory factory;

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Inject
    ActorSystemFig actorSystemFig;

    @Inject
    ActorSystemManager actorSystemManager;

    @Inject
    UniqueValuesService uniqueValuesService;


    int numThreads = 6;
    int poolSize = 5;
    int numUsers = 100;


    @Before
    public void initAkka() {
        // each test class needs unique port number
        initAkka( 2559, actorSystemManager, uniqueValuesService );
    }


    /**
     * Use multiple threads to attempt to create entities with duplicate usernames.
     */
    @Test
    public void testUniqueValueCleanup() throws Exception {

        initAkka();

        ApplicationScope context = new ApplicationScopeImpl( new SimpleId( "organization" ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        String username = RandomStringUtils.randomAlphanumeric( 20 );

        // create user
        Entity originalUser = null;
        {
            Entity newEntity = new Entity( new SimpleId( "user" ) );
            newEntity.setField( new StringField( "username", username, true ) );
            newEntity.setField( new StringField( "email", username + "@example.org", true ) );
            Observable<Entity> observable = manager.write( newEntity, null );
            originalUser = observable.toBlocking().lastOrDefault( null );
        }

        // cannot create another user with same name
        {
            Entity newEntity = new Entity( new SimpleId( "user" ) );
            newEntity.setField( new StringField( "username", username, true ) );
            newEntity.setField( new StringField( "email", username + "@example.org", true ) );
            try {
                Observable<Entity> observable = manager.write( newEntity, null );
                Entity returned = observable.toBlocking().lastOrDefault( null );
                fail("Should not have created dupliate user");
            } catch ( WriteUniqueVerifyException expected ) {}
        }

        // delete user
        manager.mark( originalUser.getId(), null ).toBlocking().firstOrDefault( null );

        // now we can create another user with same name
        {
            Entity newEntity = new Entity( new SimpleId( "user" ) );
            newEntity.setField( new StringField( "username", username, true ) );
            newEntity.setField( new StringField( "email", username + "@example.org", true ) );
            try {
                Observable<Entity> observable = manager.write( newEntity, null );
                Entity returned = observable.toBlocking().lastOrDefault( null );
            } catch ( WriteUniqueVerifyException unexpected ) {
                logger.error("Error creating user", unexpected);
                fail("Still cannot create new user after delete");
            }
        }
    }
}
