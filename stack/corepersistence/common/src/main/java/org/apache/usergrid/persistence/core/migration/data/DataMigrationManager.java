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
package org.apache.usergrid.persistence.core.migration.data;


import java.util.List;
import java.util.Set;

import org.apache.usergrid.persistence.core.migration.schema.MigrationException;


/**
 *  A manager that will perform any data migrations necessary.  Setup code should invoke the implementation of this interface
 */
public interface DataMigrationManager {

    /**
     * check for plugin existence
     * @param name
     * @return
     */
    public boolean pluginExists(final String name) ;
    /**
     * Perform any migration necessary in the application.  Will only create keyspaces and column families if they do
     * not exist
     */
    public void migrate() throws MigrationException;
    /**
     * Perform any migration necessary in the application.  Will only create keyspaces and column families if they do
     * not exist
     */
    public void migrate(final String name) throws MigrationException;
    /**
     * Returns true if a migration is running.  False otherwise
     * @return
     */
    public boolean isRunning();

    /**
     * Get the current version of the schema
     * @return
     */
    public int getCurrentVersion(final String pluginName);

    /**
     * Reset the system version to the version specified
     * @param version
     */
    public void resetToVersion(final String pluginName, final int version);


    /**
     * Return that last status of the migration
     */
    public String getLastStatus(final String pluginName);

    /**
     * Return a list of all plugin names
     * @return
     */
    public Set<String> getPluginNames();
}
