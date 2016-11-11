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

package org.apache.usergrid.persistence.qakka;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.usergrid.persistence.core.migration.schema.MigrationException;
import org.apache.usergrid.persistence.core.migration.schema.MigrationManager;
import org.apache.usergrid.persistence.queue.TestModule;
import org.apache.usergrid.persistence.queue.guice.QueueModule;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class AbstractTest {
    private static final Logger logger = LoggerFactory.getLogger( AbstractTest.class );

    static AtomicInteger nextPort = new AtomicInteger(3551);

    protected static Injector sharedInjector;

    static AtomicBoolean migrated = new AtomicBoolean( false );

    static { new KeyspaceDropper(); }


    public AbstractTest() {
        if ( !migrated.getAndSet( true ) ) {
            setInjector( Guice.createInjector( new TestModule() ) );
            MigrationManager migrationManager = getInjector().getInstance( MigrationManager.class );
            try {
                migrationManager.migrate();
            } catch (MigrationException e) {
                logger.error("Error in migration", e);
            }
        }
    }

    protected Injector getInjector() {
        return sharedInjector;
    }

    protected static void setInjector(Injector injector) {
        AbstractTest.sharedInjector = injector;
    }


    protected int getNextAkkaPort() {
        int ret = nextPort.getAndIncrement();
        logger.info("Returning port {} for this {}", ret, this.hashCode());
        return ret;
    }


    @BeforeClass
    public static void startCassandra() throws Exception {
        //EmbeddedCassandraServerHelper.startEmbeddedCassandra("/cassandra.yaml");
    }



}
