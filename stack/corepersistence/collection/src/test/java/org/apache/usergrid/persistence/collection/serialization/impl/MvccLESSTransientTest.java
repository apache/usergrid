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


import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.safehaus.guicyfig.Bypass;
import org.safehaus.guicyfig.Env;
import org.safehaus.guicyfig.Option;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/** @author tnine */
@RunWith( ITRunner.class )
@UseModules( TestCollectionModule.class )
public class MvccLESSTransientTest {

    @Inject

    public SerializationFig serializationFig;


    @Inject
    private MvccLogEntrySerializationStrategy logEntryStrategy;


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    private int originalTimeout;


    @Before
    public void setTimeout() {
        originalTimeout = serializationFig.getTimeout();
        //set the bypass options
        serializationFig.setBypass( new TestByPass() );
    }


    /**
     * Test bypass that sets all environments to use the timeout of 1 second
     */
    public class TestByPass implements Bypass {


        @Override
        public Option[] options() {
            return new Option[] { new TestOption() };
        }


        @Override
        public Env[] environments() {
            return new Env[] { Env.ALL, Env.UNIT };
        }


        @Override
        public Class<? extends Annotation> annotationType() {
            return Bypass.class;
        }
    }


    /**
     * TestOption
     */
    public class TestOption implements Option {


        @Override
        public Class<? extends Annotation> annotationType() {
            return Bypass.class;
        }


        @Override
        public String method() {
            return "getTimeout";
        }


        @Override
        public String override() {
            return "1";
        }
    }


    @Test
    public void transientTimeout() throws ConnectionException, InterruptedException {
        final Id organizationId = new SimpleId( "organization" );
        final Id applicationId = new SimpleId( "application" );
        final String name = "test";


        CollectionScope context = new CollectionScopeImpl( organizationId, applicationId, name );

        final Id id = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();

        for ( Stage stage : Stage.values() ) {
            MvccLogEntry saved = new MvccLogEntryImpl( id, version, stage, MvccLogEntry.State.COMPLETE );
            logEntryStrategy.write( context, saved ).execute();

            //Read it back after the timeout

            //noinspection PointlessArithmeticExpression
            Thread.sleep( 1000 );

            MvccLogEntry returned =
                    logEntryStrategy.load( context, Collections.singleton( id ), version ).getMaxVersion( id );


            if ( stage.isTransient() ) {
                assertNull( "Active is transient and should time out", returned );
            }
            else {
                assertNotNull( "Committed is not transient and should be returned", returned );
                assertEquals( "Returned should equal the saved", saved, returned );
            }
        }

        // null it out
        serializationFig.bypass( "getTimeout", null );
    }
}

