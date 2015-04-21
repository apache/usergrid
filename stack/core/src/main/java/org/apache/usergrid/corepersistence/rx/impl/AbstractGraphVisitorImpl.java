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

package org.apache.usergrid.corepersistence.rx.impl;


import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.migration.data.MigrationDataProvider;

import com.google.inject.Inject;

import rx.Observable;


/**
 * An observable that returns all entities in the collections
 */
public abstract class AbstractGraphVisitorImpl<T> implements MigrationDataProvider<T> {

    private final AllApplicationsObservable applicationObservable;
    private final AllEntityIdsObservable allEntityIdsObservable;

    @Inject
    public AbstractGraphVisitorImpl( AllApplicationsObservable applicationObservable,
                                     final AllEntityIdsObservable allEntityIdsObservable ) {

        this.applicationObservable = applicationObservable;
        this.allEntityIdsObservable = allEntityIdsObservable;
    }



    @Override
    public Observable<T> getData() {
      return allEntityIdsObservable.getEntities( applicationObservable.getData() ).map(
          entityIdScope -> generateData( entityIdScope ) );

    }


    /**
     * Generate the data for the observable stream from the scope and the node id
     * @param entityIdScope
     * @return
     */
    protected abstract T generateData(final EntityIdScope entityIdScope);


}
