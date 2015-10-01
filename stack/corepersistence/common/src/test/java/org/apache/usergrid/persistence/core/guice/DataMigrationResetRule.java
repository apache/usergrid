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

package org.apache.usergrid.persistence.core.guice;


import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.migration.data.DataMigrationManager;
import org.apache.usergrid.persistence.core.migration.schema.MigrationException;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * A test rule that will set up a specific version for the plugin before test invocation
 * then set it back afterwards
 */
public class DataMigrationResetRule extends ExternalResource {
    private static final Logger logger = LoggerFactory.getLogger( DataMigrationResetRule.class );


    private DataMigrationManagerProvider dataMigrationManagerProvider;

    private final String pluginName;

    private final int versionToSet;

    private int existingVersion = -1;


    public DataMigrationResetRule( final DataMigrationManagerProvider dataMigrationManagerProvider, final String pluginName, final int versionToSet ) {
        this.dataMigrationManagerProvider = dataMigrationManagerProvider;
        this.pluginName = pluginName;
        this.versionToSet = versionToSet;
    }



    @Override
    protected void before() throws MigrationException {

        existingVersion = dataMigrationManagerProvider.getDataMigrationManager().getCurrentVersion( pluginName );

        dataMigrationManagerProvider.getDataMigrationManager().resetToVersion( pluginName, versionToSet );

        logger.info( "Migration complete" );
    }


    @Override
    protected void after() {
        dataMigrationManagerProvider.getDataMigrationManager().resetToVersion( pluginName, existingVersion );
    }


    /**
     * Interface for getting a data migration manager during testing. Ugly, but required because we
     * can't inject into this member
     */
    public static interface DataMigrationManagerProvider{

        /**
         * Get the data migration manager
         * @return
         */
        public DataMigrationManager getDataMigrationManager();
    }
}
