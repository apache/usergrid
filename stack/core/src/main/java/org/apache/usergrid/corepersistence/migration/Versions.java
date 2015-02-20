/*
 *
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
 *
 */

package org.apache.usergrid.corepersistence.migration;


import org.apache.usergrid.persistence.core.guice.V2Impl;
import org.apache.usergrid.persistence.core.guice.V3Impl;
import org.apache.usergrid.persistence.graph.serialization.EdgeMigrationStrategy;


/**
 * Simple class to hold the constants of all versions
 */
public class Versions {

    /**
     * Version 1 of our mappings
     */
    public static final int VERSION_1 = 1;

    /**
     * Version 2.  Edge meta changes
     */
    public static final int VERSION_2 = EdgeMigrationStrategy.MIGRATION_VERSION;

    /**
     * Version 3. migrate from entity serialization 1 -> 2
     */
    public static final int VERSION_3 = V2Impl.MIGRATION_VERSION;

    /**
     * Version 4. migrate from entity serialization 1 -> 2
     */
    public static final int VERSION_4 = V3Impl.MIGRATION_VERSION;

}
