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


import org.apache.usergrid.persistence.collection.serialization.impl.MvccEntitySerializationStrategyProxyImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeMetadataSerializationProxyImpl;
import org.apache.usergrid.persistence.index.impl.EsEntityIndexImpl;


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
    public static final int VERSION_2 = EdgeMetadataSerializationProxyImpl.MIGRATION_VERSION;

    public static final int VERSION_3 = MvccEntitySerializationStrategyProxyImpl.MIGRATION_VERSION;

    public static final int VERSION_4 = EsEntityIndexImpl.MIGRATION_VERSION;
}
