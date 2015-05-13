/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.index.guice;


import org.safehaus.guicyfig.GuicyFigModule;

import org.apache.usergrid.persistence.core.guice.CommonModule;
import org.apache.usergrid.persistence.core.guice.TestModule;
import org.apache.usergrid.persistence.core.migration.data.MigrationDataProvider;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;

import rx.Observable;


public class TestIndexModule extends TestModule {

    @Override
    protected void configure() {

        install( new CommonModule());

        // configure collections and our core astyanax framework
        install( new IndexModule(){
            @Override
            public  void configureMigrationProvider(){

                bind( new TypeLiteral<MigrationDataProvider<ApplicationScope>>() {} ).to(
                    TestAllApplicationsObservable.class );
            }
        });
        install( new GuicyFigModule(IndexTestFig.class) );
    }

    public static class TestAllApplicationsObservable implements MigrationDataProvider<ApplicationScope>{

        @Inject
        public TestAllApplicationsObservable(){

        }


        @Override
        public Observable<ApplicationScope> getData() {
          return Observable.empty();
        }
    }

}
