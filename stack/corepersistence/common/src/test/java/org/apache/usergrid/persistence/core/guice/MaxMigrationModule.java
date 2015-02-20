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

package org.apache.usergrid.persistence.core.guice;


import org.apache.usergrid.persistence.core.migration.data.ApplicationDataMigration;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;


/**
 * Install this module in your tests if you want the max version to always be set
 * this ensures that the system will be on Integer.MAX version, there for
 */
public class MaxMigrationModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<DataMigration> dataMigrationMultibinder = Multibinder.newSetBinder( binder(), DataMigration.class );
        dataMigrationMultibinder.addBinding().to( MaxMigrationVersion.class );
    }
}
