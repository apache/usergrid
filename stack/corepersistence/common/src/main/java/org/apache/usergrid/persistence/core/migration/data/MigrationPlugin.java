/*
 *
 *  *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *  *
 *
 */

package org.apache.usergrid.persistence.core.migration.data;


/**
 * A simple interface to return migration plugins.  All versions within this migration plugin should have a name
 */
public interface MigrationPlugin {


    /**
     * Get the name of the plugin.  Must be unique
     * @return
     */
    String getName();

    /**
     * Run any migrations that may need to be run in this plugin
     */
    void run( ProgressObserver observer );


    /**
     * Get the max version this plugin supports
     * @return
     */
    int getMaxVersion();

    /**
     * Get the phase of the plugin
     * @return
     */
    PluginPhase getPhase();
}
