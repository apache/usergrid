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

package org.apache.usergrid.corepersistence.migration;


import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.apache.usergrid.CoreApplication;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationManager;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Injector;


/**
 * This class is required because we cannot create the system's framework on a higher version (the default behavior is
 * the latest) then roll back to a previous version
 *
 * This rule performs the following operations.
 *
 * <ol> <li>Sets up the system's version to be the DataMigration impl's version -1</li> <li>Creates an application using
 * that version of the schema.</li> <li>Sets the new org and app in the CoreApplication to the app created</li> </ol>
 */
public class MigrationTestRule extends ExternalResource {

    protected final CoreApplication core;
    protected final DataMigrationManager dataMigrationManager;
    protected final DataMigration dataMigration;

    protected int currentVersion;


    /**
     * Create a new migration test rule.
     *
     * @param core the CoreApplication rule used in this test
     * @param injector The injector used in this test
     * @param dataMigrationClass The data migration class that is under test
     */
    public MigrationTestRule( final CoreApplication core, final Injector injector,
                              final Class<? extends DataMigration> dataMigrationClass ) {
        this.core = core;
        this.dataMigrationManager = injector.getInstance( DataMigrationManager.class );
        this.dataMigration = injector.getInstance( dataMigrationClass );
    }


    public void resetAndCreateApp( final String className, final String methodName ) throws Exception {
        dataMigrationManager.invalidate();
        currentVersion = dataMigrationManager.getCurrentVersion();

        dataMigrationManager.resetToVersion( dataMigration.getVersion() - 1 );
        dataMigrationManager.invalidate();

        core.createApplication( className + UUIDGenerator.newTimeUUID(), methodName );
    }


    public void resetVersion(){
        dataMigrationManager.resetToVersion( currentVersion );
        dataMigrationManager.invalidate();
    }

    @Override
    public Statement apply( final Statement base, final Description description ) {

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    resetAndCreateApp( description.getClassName(), description.getMethodName() );
                    base.evaluate();
                }finally {
                    resetVersion();
                }
            }
        };
    }
}
