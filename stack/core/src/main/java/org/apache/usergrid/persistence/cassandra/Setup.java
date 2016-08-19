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

public interface Setup {

    /**
     * Initialize all configuration for the system setup. Creates keyspaces and elasticsearch indexes
     * @throws Exception
     */
    void initSchema() throws Exception;


    /**
     * Bootstrap the root application to allow the system to function.
     * @throws Exception
     */
    void initMgmtApp() throws Exception;


    /**
     *
     * Separate interface for triggering the data migration to role new 2.x Migration classes to the latest
     *
     * @throws Exception
     */
    void runDataMigration() throws Exception;


}
