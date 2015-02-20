/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.persistence.cassandra;

import java.util.UUID;

/**
 *
 * @author ApigeeCorporation
 */
public interface Setup {

    /**
     * Initialize all configuration for the system setup.  DO NOT actually create any keyspaces.
     * @throws Exception
     */
    void init() throws Exception;

    /**
     * Setup the management keyspaces
     * @throws Exception
     */
    public void setupSystemKeyspace() throws Exception;

    /**
     * Setup the application keyspaces
     * @throws Exception
     */
    public void setupStaticKeyspace() throws Exception;

    /**
     * Returns true if both keyspaces exist
     * @return
     */
    public boolean keyspacesExist();

    /**
     * Bootstrap the root application to allow the system to function.
     * @throws Exception
     */
    public void createDefaultApplications() throws Exception;

    public void setupApplicationKeyspace(UUID applicationId, String appName) throws Exception;
}
