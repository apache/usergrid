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

package org.apache.usergrid.persistence.core.migration.data;


/**
 * This is the plugin lifeccyle where plugins should be executed.  Plugins are executed in the following order
 *
 *
 */
public enum PluginPhase {

    /**
     * Runs before any data migration.  This is used to prepare our migration subsystem.
     * For instance, a change in the migration system itself.  Most plugins won't need to use this
     */
    BOOTSTRAP,

    /**
     * This is where data migration actually happens.  Plugins should be able to run concurrently.
     * If a plugin has a race condition with another plugin, they should be refactored into a single plugin,
     * with migration versions encapsulated within the plugin itself
     */
    MIGRATE
}
