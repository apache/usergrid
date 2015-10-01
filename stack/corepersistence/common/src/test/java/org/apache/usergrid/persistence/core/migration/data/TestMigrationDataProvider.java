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

package org.apache.usergrid.persistence.core.migration.data;


import org.apache.usergrid.persistence.core.migration.data.MigrationDataProvider;

import rx.Observable;


/**
 * A simple test class that will emit the provided test data when subscribed
 * @param <T>
 */
public class TestMigrationDataProvider<T> implements MigrationDataProvider<T> {



    //default to nothing so that we don't return null
    private Observable<T> observable = Observable.empty();


    public TestMigrationDataProvider(  ) {}


    @Override
    public Observable<T> getData() {
       return observable;
    }


    /**
     * Set this observable to return when invoked
     *
     * @param observable
     */
    public void setObservable( final Observable<T> observable ) {
        this.observable = observable;
    }
}
