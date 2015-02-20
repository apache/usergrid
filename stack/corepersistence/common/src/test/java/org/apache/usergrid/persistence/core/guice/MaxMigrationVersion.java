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
import org.apache.usergrid.persistence.core.scope.ApplicationEntityGroup;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import rx.Observable;


/**
 * A simple migration that sets the version to max. This way our integration tests always test the latest code
 */
public class MaxMigrationVersion implements ApplicationDataMigration {

    @Override
    public Observable migrate(final Observable<ApplicationScope> applicationEntityGroup, final ProgressObserver observer) {
         //no op, just needs to run to be set
        return Observable.empty();
    }

    @Override
    public int getVersion() {
        return Integer.MAX_VALUE;
    }

    @Override
    public MigrationType getType() {
        return MigrationType.Applications;
    }
}
