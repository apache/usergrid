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
     * Initialize all configuration for the system setup. Creates keyspaces, and elasticsearch indexes
     * @throws Exception
     */
    void initSubsystems() throws Exception;

    /**
     * Setup the management keyspaces
     * @throws Exception
     */
    void setupSystemKeyspace() throws Exception;

    /**
     * Setup the application keyspaces
     * @throws Exception
     */
    void setupStaticKeyspace() throws Exception;

    /**
     * Returns true if both keyspaces exist
     * @return
     */
    boolean keyspacesExist();

    /**
     * Bootstrap the root application to allow the system to function.
     * @throws Exception
     */
    void createDefaultApplications() throws Exception;

    void setupApplicationKeyspace( UUID applicationId, String appName ) throws Exception;
}
