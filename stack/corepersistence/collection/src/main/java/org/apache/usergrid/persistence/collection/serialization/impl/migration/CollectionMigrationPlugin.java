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

package org.apache.usergrid.persistence.collection.serialization.impl.migration;


import java.util.Set;

import org.apache.usergrid.persistence.core.migration.data.newimpls.DataMigration2;
import org.apache.usergrid.persistence.core.migration.data.newimpls.MigrationDataProvider;
import org.apache.usergrid.persistence.core.migration.data.newimpls.MigrationPlugin;
import org.apache.usergrid.persistence.core.migration.data.newimpls.ProgressObserver;


/**
 * Migration plugin for the collection module
 */
public class CollectionMigrationPlugin implements MigrationPlugin {


    private final DataMigration2<EntityIdScope> entityDataMigration;
    private final MigrationDataProvider<EntityIdScope> entityIdScopeDataMigrationProvider;


    public CollectionMigrationPlugin( final DataMigration2<EntityIdScope> entityDataMigration,
                                      final MigrationDataProvider<EntityIdScope> entityIdScopeDataMigrationProvider ) {
        this.entityDataMigration = entityDataMigration;
        this.entityIdScopeDataMigrationProvider = entityIdScopeDataMigrationProvider;
    }


    @Override
    public String getName() {
        return "collections-entity-data";
    }


    @Override
    public void run( final ProgressObserver observer ) {
       entityDataMigration.migrate( entityIdScopeDataMigrationProvider, observer );
    }


    @Override
    public int getMaxVersion() {
        return 0;
    }
}
