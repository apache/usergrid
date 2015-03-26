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
import org.apache.usergrid.persistence.index.IndexAlias;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexIdentifier;
import org.apache.usergrid.persistence.index.impl.EsIndexCache;
import org.apache.usergrid.persistence.index.impl.EsProvider;
import org.apache.usergrid.persistence.index.impl.IndexingUtils;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.client.AdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classy class class.
 */
public class EsIndexDataMigrationImpl implements DataMigration<ApplicationScope> {

    private final AliasedEntityIndex entityIndex;
    private final EsProvider provider;
    private final IndexFig indexFig;
    private final IndexIdentifier indexIdentifier;
    private final EsIndexCache indexCache;
    private final VersionedData dataVersion;
    private static final Logger log = LoggerFactory.getLogger(EsIndexDataMigrationImpl.class);

    @Inject
    public EsIndexDataMigrationImpl(AliasedEntityIndex entityIndex, EsProvider provider, IndexFig indexFig, IndexIdentifier indexIdentifier, EsIndexCache indexCache){
        this.entityIndex = entityIndex;
        this.provider = provider;
        this.indexFig = indexFig;
        this.indexIdentifier = indexIdentifier;
        this.indexCache = indexCache;
        this.dataVersion = (VersionedData) entityIndex;
    }

    @Override
    public int migrate(int currentVersion, MigrationDataProvider<ApplicationScope> migrationDataProvider, ProgressObserver observer) {
        final AtomicInteger integer = new AtomicInteger();
        migrationDataProvider.getData().doOnNext(applicationScope -> {
            LegacyIndexIdentifier legacyIndexIdentifier = new LegacyIndexIdentifier(indexFig,applicationScope);
            String[] indexes = indexCache.getIndexes(legacyIndexIdentifier.getAlias(), AliasedEntityIndex.AliasType.Read);
            AdminClient adminClient = provider.getClient().admin();

            for (String index : indexes) {
                IndicesAliasesRequestBuilder aliasesRequestBuilder = adminClient.indices().prepareAliases();
                aliasesRequestBuilder = adminClient.indices().prepareAliases();
                // add read alias
                aliasesRequestBuilder.addAlias(index, indexIdentifier.getAlias().getReadAlias());
                integer.incrementAndGet();
            }
        })
        .doOnError(error -> log.error("failed to migrate index",error))
        .toBlocking().last();

        return dataVersion.getImplementationVersion();
    }

    @Override
    public boolean supports(int currentVersion) {
        return currentVersion < dataVersion.getImplementationVersion();
    }

    @Override
    public int getMaxVersion() {
        return dataVersion.getImplementationVersion();
    }
    /**
     * Class is used to generate an index name and alias name the old way via app name
     */
    public class LegacyIndexIdentifier{
        private final IndexFig config;
        private final ApplicationScope applicationScope;

        public LegacyIndexIdentifier(IndexFig config, ApplicationScope applicationScope) {
            this.config = config;
            this.applicationScope = applicationScope;
        }

        /**
         * Get the alias name
         * @return
         */
        public IndexAlias getAlias() {
            return new IndexAlias(config,getIndexBase());
        }

        /**
         * Get index name, send in additional parameter to add incremental indexes
         * @param suffix
         * @return
         */
        public String getIndex(String suffix) {
            if (suffix != null) {
                return getIndexBase() + "_" + suffix;
            } else {
                return getIndexBase();
            }
        }

        /**
         * returns the base name for index which will be used to add an alias and index
         * @return
         */
        private String getIndexBase() {
            StringBuilder sb = new StringBuilder();
            sb.append(config.getIndexPrefix()).append(IndexingUtils.SEPARATOR);
            IndexingUtils.idString(sb, applicationScope.getApplication());
            return sb.toString();
        }



        public String toString() {
            return "application: " + applicationScope.getApplication().getUuid();
        }

    }
}
