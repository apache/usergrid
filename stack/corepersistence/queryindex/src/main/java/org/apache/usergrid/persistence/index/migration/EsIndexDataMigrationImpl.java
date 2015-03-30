/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.index.migration;

import com.google.inject.Inject;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.core.migration.data.MigrationDataProvider;
import org.apache.usergrid.persistence.core.migration.data.ProgressObserver;
import org.apache.usergrid.persistence.core.migration.data.VersionedData;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.AliasedEntityIndex;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexIdentifier;
import org.apache.usergrid.persistence.index.IndexCache;
import org.apache.usergrid.persistence.index.impl.EsProvider;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.indices.InvalidAliasNameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

/**
 * Classy class class.
 */
public class EsIndexDataMigrationImpl implements DataMigration<ApplicationScope> {

    private final AliasedEntityIndex entityIndex;
    private final EsProvider provider;
    private final IndexFig indexFig;
    private final IndexIdentifier indexIdentifier;
    private final IndexCache indexCache;
    private final VersionedData dataVersion;
    private static final Logger log = LoggerFactory.getLogger(EsIndexDataMigrationImpl.class);

    @Inject
    public EsIndexDataMigrationImpl(AliasedEntityIndex entityIndex, EsProvider provider, IndexFig indexFig, IndexIdentifier indexIdentifier, IndexCache indexCache){
        this.entityIndex = entityIndex;
        this.provider = provider;
        this.indexFig = indexFig;
        this.indexIdentifier = indexIdentifier;
        this.indexCache = indexCache;
        this.dataVersion = (VersionedData) entityIndex;
    }

    @Override
    public int migrate(int currentVersion, MigrationDataProvider<ApplicationScope> migrationDataProvider, ProgressObserver observer) {
        final AdminClient adminClient = provider.getClient().admin();
        final int latestVersion = dataVersion.getImplementationVersion();

        entityIndex.initialize();

        observer.start();
        try {
            migrationDataProvider.getData().flatMap(applicationScope -> {
                LegacyIndexIdentifier legacyIndexIdentifier = new LegacyIndexIdentifier(indexFig, applicationScope);
                String[] indexes = indexCache.getIndexes(legacyIndexIdentifier.getAlias(), AliasedEntityIndex.AliasType.Read);
                return Observable.from(indexes);
            })
                .doOnNext(index -> {
                    IndicesAliasesRequestBuilder aliasesRequestBuilder = adminClient.indices().prepareAliases();
                    aliasesRequestBuilder = adminClient.indices().prepareAliases();
                    // add read alias
                    try {
                        aliasesRequestBuilder.addAlias(index, indexIdentifier.getAlias().getReadAlias()).get();
                    } catch (InvalidAliasNameException e) {
                        log.debug("Failed to add alias due to name conflict",e);
                    }
                    observer.update(latestVersion, "EsIndexDataMigrationImpl: fixed index: " + index);
                })
                .doOnCompleted(() -> observer.complete())
                .toBlocking().lastOrDefault(null);
        }catch (Exception e){
            log.error("Failed to migrate index", e);
            throw e;
        }

        return latestVersion;
    }

    @Override
    public boolean supports(int currentVersion) {
        return currentVersion < dataVersion.getImplementationVersion();
    }

    @Override
    public int getMaxVersion() {
        return dataVersion.getImplementationVersion();
    }

}
