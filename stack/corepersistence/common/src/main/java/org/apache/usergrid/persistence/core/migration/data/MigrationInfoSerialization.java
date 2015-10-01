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


import org.apache.usergrid.persistence.core.migration.schema.Migration;


public interface MigrationInfoSerialization extends Migration {

    /**
     * Save the message to the cluster
     * @param pluginName the name of hte plugin
     * @param message
     */
    public void setStatusMessage(final String pluginName,  final String message );

    /**
     * Get the last status for the plugin
     * @return
     */
    public String getStatusMessage(final String pluginName);

    /**
     * Save the version for the plugin
     * @param version
     */
    public void setVersion(final String pluginName, final int version);

    /**
     * Return the version
     * @return
     */
    public int getVersion(final String pluginName);

    /**
     * Set the status and save them
     * @param pluginName The name of the plugin
     * @param status
     * @return
     */
    public void setStatusCode(final String pluginName, final int status );

    /**
     *  Get the status code for the plugin
     * @return The integer that's saved
     */
    public int getStatusCode(final String pluginName);

    /**
     * This is deprecated, and will be used to migrate from the old version information to the new format.
     * Should return -1 if not set
     * @return
     */
    @Deprecated
    public int getSystemVersion();
}
