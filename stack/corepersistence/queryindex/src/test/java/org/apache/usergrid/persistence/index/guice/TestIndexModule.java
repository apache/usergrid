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


import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.migration.data.MigrationDataProvider;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.IndexBufferProducer;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.impl.EsEntityIndexImpl;
import org.apache.usergrid.persistence.index.IndexCache;
import org.apache.usergrid.persistence.index.impl.EsProvider;
import org.apache.usergrid.persistence.index.migration.LegacyIndexIdentifier;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.safehaus.guicyfig.GuicyFigModule;

import org.apache.usergrid.persistence.core.guice.CommonModule;
import org.apache.usergrid.persistence.core.guice.TestModule;
import rx.Observable;

import java.util.UUID;


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

        final ApplicationScope appScope =  new ApplicationScopeImpl(new SimpleId(UUID.randomUUID(),"application"));

        @Inject
        public TestAllApplicationsObservable(
                                             final IndexBufferProducer indexBatchBufferProducer, final EsProvider provider,
                                             final IndexCache indexCache, final MetricsFactory metricsFactory,
                                             final IndexFig indexFig){
            LegacyIndexIdentifier legacyIndexIdentifier = new  LegacyIndexIdentifier(indexFig,appScope);
            EntityIndex entityIndex = new EsEntityIndexImpl(indexBatchBufferProducer,provider,indexCache,metricsFactory,indexFig,legacyIndexIdentifier);
            entityIndex.addIndex(null, 1, 0, indexFig.getWriteConsistencyLevel());
        }


        @Override
        public Observable<ApplicationScope> getData() {
            ApplicationScope[] scopes = new ApplicationScope[]{
               appScope
            };
            return Observable.from(scopes);
        }
    }

}
